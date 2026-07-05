/*
 * Copyright 2026 DrishtiSTEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.drishti.molecule

import io.drishti.core.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeMark
import kotlin.time.TimeSource

public open class PubChemException(message: String, cause: Throwable? = null) : Exception(message, cause)
public class PubChemCompoundNotFoundException(message: String) : PubChemException(message)
public class PubChemNetworkException(message: String, cause: Throwable? = null) : PubChemException(message, cause)
public class PubChemRateLimitException(message: String) : PubChemException(message)


/**
 * HTTP client for the PubChem PUG REST API.
 *
 * Provides compound lookup by name, SMILES, InChI, or molecular formula.
 * Implements rate limiting (5 requests/sec), in-memory caching, and request
 * coalescing to respect PubChem usage policies and improve response times.
 *
 * Handles transient HTTP errors (429 rate limit, 503 service unavailable)
 * with automatic retry and exponential backoff. Individual HTTP calls are
 * guarded by a [NETWORK_TIMEOUT_MS] timeout to prevent hung connections.
 *
 * All public methods return `null` when the compound is not found
 * or when a network error occurs, enabling graceful fallback.
 *
 * @param httpClient Ktor HTTP client instance for network communication.
 *   Configure request timeouts (connectTimeoutMillis, requestTimeoutMillis)
 *   on this client for defense-in-depth; this class adds a coroutine-level
 *   timeout as an additional safety net.
 * @param baseUrl PubChem PUG REST base URL
 * @param cacheSize Maximum number of molecules to cache (default 100)
 * @param cacheTtlMs Cache time-to-live in milliseconds. Cached entries older
 *   than this are evicted on next access. Default: [DEFAULT_CACHE_TTL_MS].
 * @param maxRequestsPerSecond Maximum PubChem API requests per second.
 *   PubChem enforces a 5 req/sec policy. Default: [DEFAULT_MAX_REQUESTS_PER_SECOND].
 *
 * @see <a href="https://pubchem.ncbi.nlm.nih.gov/rest/pug/">PUG REST API</a>
 */
public class PubChemClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://pubchem.ncbi.nlm.nih.gov/rest/pug",
    private val cacheSize: Int = 100,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val maxRequestsPerSecond: Int = DEFAULT_MAX_REQUESTS_PER_SECOND,
) {
    private data class CacheEntry(val data: MoleculeData, val timestamp: TimeMark)
    private val cache = linkedMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()
    private val rateLimitMutex = Mutex()
    private val minRequestIntervalMs = 1000L / maxRequestsPerSecond

    // Request coalescing: concurrent callers for the same key share one in-flight request
    private val inflight = mutableMapOf<String, CompletableDeferred<MoleculeData?>>()
    private val inflightMutex = Mutex()

    public companion object {
        /** Default cache TTL: 1 hour (3,600,000 ms) */
        public const val DEFAULT_CACHE_TTL_MS: Long = 3_600_000L
        /** Default rate limit: 5 requests per second (PubChem policy) */
        public const val DEFAULT_MAX_REQUESTS_PER_SECOND: Int = 5
        internal const val NETWORK_TIMEOUT_MS: Long = 30_000L
        internal const val MAX_RETRY_ATTEMPTS: Int = 3
    }

    /**
     * Fetch molecule data by compound name.
     *
     * Accepts common names (e.g., "water", "caffeine") or IUPAC names
     * (e.g., "ethanol", "acetylsalicylic acid").
     *
     * @param name Compound name to search for
     * @return [MoleculeData] if found, `null` otherwise
     */
    public suspend fun fetchByName(name: String): MoleculeData? {
        val cacheKey = "name:${name.lowercase()}"
        return fetchWithCoalescing(cacheKey) {
            val cid = fetchCid("compound/name/${encodePathSegment(name)}/JSON")
            cid?.let { fetchFullMoleculeData(it) }
        }
    }

    /**
     * Fetch molecule data by SMILES string.
     *
     * @param smiles Canonical or isomeric SMILES representation
     * @return [MoleculeData] if found, `null` otherwise
     */
    public suspend fun fetchBySmiles(smiles: String): MoleculeData? {
        val cacheKey = "smiles:$smiles"
        return fetchWithCoalescing(cacheKey) {
            val cid = fetchCid("compound/smiles/${encodePathSegment(smiles)}/JSON")
            cid?.let { fetchFullMoleculeData(it) }
        }
    }

    /**
     * Fetch molecule data by molecular formula.
     *
     * @param formula Molecular formula (e.g., "C2H6O", "C6H12O6")
     * @return [MoleculeData] if found, `null` otherwise
     */
    public suspend fun fetchByFormula(formula: String): MoleculeData? {
        val cacheKey = "formula:${formula.lowercase()}"
        return fetchWithCoalescing(cacheKey) {
            val cid = fetchCid("compound/formula/${encodePathSegment(formula)}/JSON")
            cid?.let { fetchFullMoleculeData(it) }
        }
    }

    /**
     * Fetch molecule data by InChI string.
     *
     * @param inchi InChI identifier
     * @return [MoleculeData] if found, `null` otherwise
     */
    public suspend fun fetchByInchi(inchi: String): MoleculeData? {
        val cacheKey = "inchi:${inchi.lowercase()}"
        return fetchWithCoalescing(cacheKey) {
            val cid = fetchCid("compound/inchi/${encodePathSegment(inchi)}/JSON")
            cid?.let { fetchFullMoleculeData(it) }
        }
    }

    /**
     * Clear all cached molecule data.
     */
    public suspend fun clearCache(): Unit = cacheMutex.withLock { cache.clear() }

    // ── Request coalescing ─────────────────────────────────────────────

    /**
     * Ensures only one in-flight network request per cache key.
     * Concurrent callers for the same key await the shared result
     * instead of duplicating HTTP calls — prevents cache stampedes.
     */
    private suspend fun fetchWithCoalescing(
        cacheKey: String,
        block: suspend () -> MoleculeData?
    ): MoleculeData? {
        getCached(cacheKey)?.let { return it }

        val deferred = inflightMutex.withLock {
            inflight[cacheKey]?.let { existing ->
                return existing.await()
            }
            CompletableDeferred<MoleculeData?>().also { inflight[cacheKey] = it }
        }

        // Only the first caller for this key executes the network block
        try {
            val result = block()
            if (result != null) putCache(cacheKey, result)
            deferred.complete(result)
        } catch (e: CancellationException) {
            deferred.completeExceptionally(e)
            throw e
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            throw e
        } finally {
            inflightMutex.withLock { inflight.remove(cacheKey) }
        }

        return deferred.await()
    }

    // ── HTTP fetch methods ─────────────────────────────────────────────

    private suspend fun fetchCid(path: String): Int? {
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                rateLimit()
                val response = withTimeout(NETWORK_TIMEOUT_MS) {
                    httpClient.get("$baseUrl/$path")
                }
                when (response.status) {
                    HttpStatusCode.OK ->
                        return PubChemResponseParser.parseCidResponse(response.bodyAsText())
                    HttpStatusCode.NotFound -> return null
                    HttpStatusCode.BadRequest -> return null
                    HttpStatusCode.TooManyRequests -> {
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 2L
                        delay(retryAfter * 1000)
                        if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                            throw PubChemRateLimitException("Rate limit exceeded")
                        }
                        continue
                    }
                    HttpStatusCode.ServiceUnavailable -> {
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            delay(2000L * (attempt + 1))
                        }
                        continue
                    }
                    else -> throw PubChemNetworkException("Unexpected HTTP status: ${response.status}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is PubChemException) throw e
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        if (lastException != null) {
            throw PubChemNetworkException("Network request failed after retries", lastException)
        }
        throw PubChemNetworkException("Network request failed after retries")
    }

    private suspend fun fetchProperties(cid: Int): PubChemCompoundData? {
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                rateLimit()
                val response = withTimeout(NETWORK_TIMEOUT_MS) {
                    httpClient.get(
                        "$baseUrl/compound/cid/$cid/property/MolecularFormula,MolecularWeight,IUPACName,CanonicalSMILES,InChIKey/JSON"
                    )
                }
                when (response.status) {
                    HttpStatusCode.OK ->
                        return PubChemResponseParser.parsePropertiesResponse(response.bodyAsText())
                    HttpStatusCode.NotFound -> return null
                    HttpStatusCode.BadRequest -> return null
                    HttpStatusCode.TooManyRequests -> {
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 2L
                        delay(retryAfter * 1000)
                        if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                            throw PubChemRateLimitException("Rate limit exceeded")
                        }
                        continue
                    }
                    HttpStatusCode.ServiceUnavailable -> {
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            delay(2000L * (attempt + 1))
                        }
                        continue
                    }
                    else -> throw PubChemNetworkException("Unexpected HTTP status: ${response.status}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is PubChemException) throw e
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        if (lastException != null) {
            throw PubChemNetworkException("Network request failed after retries", lastException)
        }
        throw PubChemNetworkException("Network request failed after retries")
    }

    private suspend fun fetchConformer(cid: Int): Pair<List<PubChemAtomData>, List<PubChemBondData>>? {
        var lastException: Exception? = null
        for (attempt in 0 until MAX_RETRY_ATTEMPTS) {
            try {
                rateLimit()
                val response = withTimeout(NETWORK_TIMEOUT_MS) {
                    httpClient.get("$baseUrl/compound/cid/$cid/conformer/JSON")
                }
                when (response.status) {
                    HttpStatusCode.OK ->
                        return PubChemResponseParser.parseConformerResponse(response.bodyAsText())
                    HttpStatusCode.NotFound -> return null
                    HttpStatusCode.BadRequest -> return null
                    HttpStatusCode.TooManyRequests -> {
                        val retryAfter = response.headers["Retry-After"]?.toLongOrNull() ?: 2L
                        delay(retryAfter * 1000)
                        if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                            throw PubChemRateLimitException("Rate limit exceeded")
                        }
                        continue
                    }
                    HttpStatusCode.ServiceUnavailable -> {
                        if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                            delay(2000L * (attempt + 1))
                        }
                        continue
                    }
                    else -> throw PubChemNetworkException("Unexpected HTTP status: ${response.status}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is PubChemException) throw e
                lastException = e
                if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                    delay(1000L * (attempt + 1))
                }
            }
        }
        if (lastException != null) {
            throw PubChemNetworkException("Network request failed after retries", lastException)
        }
        throw PubChemNetworkException("Network request failed after retries")
    }

    private suspend fun fetchFullMoleculeData(cid: Int): MoleculeData? {
        val properties = fetchProperties(cid) ?: return null
        val conformer = fetchConformer(cid)

        val atoms = conformer?.first?.map { atomData ->
            Atom(
                id = atomData.id,
                element = ElementMapper.symbolFor(atomData.element),
                position = Point(atomData.x.toFloat() * 50f, atomData.y.toFloat() * 50f),
                charge = 0,
                label = ElementMapper.symbolFor(atomData.element),
                z = atomData.z.toFloat() * 50f
            )
        } ?: emptyList()

        val atomIds = atoms.map { it.id }.toSet()
        val bonds = conformer?.second?.mapNotNull { bondData ->
            if (bondData.from in atomIds && bondData.to in atomIds) {
                Bond(
                    from = bondData.from,
                    to = bondData.to,
                    type = ElementMapper.bondTypeFor(bondData.order),
                    strength = bondData.order.toFloat().coerceIn(0.5f, 1.0f)
                )
            } else null
        } ?: emptyList()

        return MoleculeData(
            cid = cid,
            molecularFormula = properties.molecularFormula,
            molecularWeight = properties.molecularWeight,
            iupacName = properties.iupacName,
            canonicalSmiles = properties.canonicalSmiles,
            inchiKey = properties.inchiKey,
            atoms = atoms,
            bonds = bonds,
            moleculeType = classifyMolecule(properties.molecularFormula),
            name = properties.iupacName
        )
    }

    private fun classifyMolecule(formula: String): MoleculeType {
        val hasCarbon = formula.contains("C")
        val atomCount = parseFormulaAtomCount(formula)
        return when {
            !hasCarbon && atomCount <= 3 -> MoleculeType.SIMPLE
            hasCarbon && atomCount <= 10 -> MoleculeType.ORGANIC
            atomCount > 10 -> MoleculeType.COMPLEX
            else -> MoleculeType.INORGANIC
        }
    }

    private fun parseFormulaAtomCount(formula: String): Int {
        val stack = mutableListOf<Int>()
        stack.add(0)
        var i = 0
        while (i < formula.length) {
            val c = formula[i]
            when {
                c == '(' -> {
                    stack.add(0)
                    i++
                }
                c == ')' -> {
                    i++
                    var multiplier = 0
                    while (i < formula.length && formula[i].isDigit()) {
                        multiplier = multiplier * 10 + (formula[i] - '0')
                        i++
                    }
                    if (multiplier == 0) multiplier = 1
                    val top = stack.removeAt(stack.size - 1)
                    val outer = stack.removeAt(stack.size - 1)
                    stack.add(outer + top * multiplier)
                }
                c.isUpperCase() -> {
                    i++
                    while (i < formula.length && formula[i].isLowerCase()) i++
                    var num = 0
                    while (i < formula.length && formula[i].isDigit()) {
                        num = num * 10 + (formula[i] - '0')
                        i++
                    }
                    val count = if (num > 0) num else 1
                    val current = stack.removeAt(stack.size - 1)
                    stack.add(current + count)
                }
                else -> {
                    i++
                }
            }
        }
        return stack.firstOrNull() ?: 0
    }

    private suspend fun rateLimit() = rateLimitMutex.withLock {
        delay(minRequestIntervalMs)
    }

    private suspend fun getCached(key: String): MoleculeData? = cacheMutex.withLock {
        val entry = cache[key] ?: return@withLock null
        if (entry.timestamp.elapsedNow().inWholeMilliseconds > cacheTtlMs) {
            cache.remove(key)
            return@withLock null
        }
        entry.data
    }

    private suspend fun putCache(key: String, data: MoleculeData) = cacheMutex.withLock {
        if (cache.size >= cacheSize) {
            cache.remove(cache.keys.first())
        }
        cache[key] = CacheEntry(data, TimeSource.Monotonic.markNow())
    }

    /**
     * Percent-encode a string for use as a single URL path segment.
     * Encodes all characters except unreserved characters per RFC 3986
     * (ALPHA / DIGIT / "-" / "." / "_" / "~").
     */
    private fun encodePathSegment(value: String): String = buildString {
        for (c in value) {
            when {
                c.isLetterOrDigit() || c == '-' || c == '.' || c == '_' || c == '~' -> append(c)
                else -> {
                    append('%')
                    append(c.code.toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }
}
