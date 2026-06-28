package io.drishti.molecule

import io.drishti.core.BondType
import io.drishti.core.MoleculeType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PubChemClientTest {

    @Test
    fun fetchByNameReturnsNullOnNotFound() = runTest {
        val client = createClientWithResponse(null)
        val result = client.fetchByName("nonexistent999")
        assertNull(result)
    }

    @Test
    fun fetchByNameReturnsMoleculeForValidCompound() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[1983]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C2H6O","MolecularWeight":46.07,"IUPACName":"ethanol","CanonicalSMILES":"CCO","InChIKey":"LFQSCWFLJHTTHZ-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[{"atoms":{"aid":[1,2,3],"element":[6,6,8],"coords":[{"x":[0.0],"y":[0.0],"z":[0.0]}]},"bonds":{"aid1":[1],"aid2":[2],"order":[1]}}]}"""

        val client = createClientWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = client.fetchByName("ethanol")

        assertNotNull(result)
        assertEquals(1983, result.cid)
        assertEquals("C2H6O", result.molecularFormula)
        assertEquals(46.07, result.molecularWeight, 0.01)
        assertEquals("ethanol", result.iupacName)
        assertEquals("CCO", result.canonicalSmiles)
        assertEquals(MoleculeType.ORGANIC, result.moleculeType)
    }

    @Test
    fun fetchBySmilesReturnsMolecule() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[2244]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":2244,"MolecularFormula":"C7H8","MolecularWeight":92.14,"IUPACName":"toluene","CanonicalSMILES":"Cc1ccccc1","InChIKey":"QQWJEVZSMJNCOY-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        val client = createClientWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = client.fetchBySmiles("Cc1ccccc1")

        assertNotNull(result)
        assertEquals(2244, result.cid)
        assertEquals("C7H8", result.molecularFormula)
        assertEquals("toluene", result.iupacName)
    }

    @Test
    fun fetchByFormulaReturnsMolecule() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[962]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":962,"MolecularFormula":"H2O","MolecularWeight":18.015,"IUPACName":"water","CanonicalSMILES":"O","InChIKey":"XLYOFNOQVPJJNP-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[{"atoms":{"aid":[1,2,3],"element":[8,1,1],"coords":[{"x":[0.0],"y":[0.0],"z":[0.0]}]},"bonds":{"aid1":[1,1],"aid2":[2,3],"order":[1,1]}}]}"""

        val client = createClientWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = client.fetchByFormula("H2O")

        assertNotNull(result)
        assertEquals(962, result.cid)
        assertEquals("H2O", result.molecularFormula)
        assertEquals("water", result.iupacName)
        assertEquals(MoleculeType.SIMPLE, result.moleculeType)
    }

    @Test
    fun fetchByInchiReturnsMolecule() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[887]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":887,"MolecularFormula":"CH4","MolecularWeight":16.04,"IUPACName":"methane","CanonicalSMILES":"C","InChIKey":"VNWKTOKETHGBQD-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        val client = createClientWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = client.fetchByInchi("InChI=1S/CH4/h1H4")

        assertNotNull(result)
        assertEquals(887, result.cid)
        assertEquals("CH4", result.molecularFormula)
    }

    @Test
    fun cacheReturnsStoredResult() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[1983]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C2H6O","MolecularWeight":46.07,"IUPACName":"ethanol","CanonicalSMILES":"CCO","InChIKey":"LFQSCWFLJHTTHZ-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        var requestCount = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                requestCount++
                val body = when (requestCount) {
                    1 -> cidResponse
                    2 -> propsResponse
                    3 -> conformerResponse
                    else -> """{"PropertyTable":{"Properties":[]}}"""
                }
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = HttpClient(mockEngine)
        val pubChemClient = PubChemClient(httpClient = client)

        // First call - should make 3 requests (CID, properties, conformer)
        val first = pubChemClient.fetchByName("ethanol")
        assertNotNull(first)
        assertEquals(3, requestCount)

        // Second call - should use cache (0 additional requests)
        val second = pubChemClient.fetchByName("ethanol")
        assertNotNull(second)
        assertEquals(3, requestCount) // No additional requests
    }

    @Test
    fun clearCacheRemovesAllEntries() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[1983]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C2H6O","MolecularWeight":46.07,"IUPACName":"ethanol","CanonicalSMILES":"CCO","InChIKey":"LFQSCWFLJHTTHZ-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        var requestCount = 0
        val responses = listOf(cidResponse, propsResponse, conformerResponse)
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                requestCount++
                val body = responses[(requestCount - 1) % responses.size]
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val client = HttpClient(mockEngine)
        val pubChemClient = PubChemClient(httpClient = client)

        pubChemClient.fetchByName("ethanol")
        assertEquals(3, requestCount)

        pubChemClient.clearCache()

        // After clearing cache, should make requests again
        pubChemClient.fetchByName("ethanol")
        assertEquals(6, requestCount)
    }

    @Test
    fun elementMapperMapsCorrectly() {
        assertEquals("H", ElementMapper.symbolFor(1))
        assertEquals("C", ElementMapper.symbolFor(6))
        assertEquals("N", ElementMapper.symbolFor(7))
        assertEquals("O", ElementMapper.symbolFor(8))
        assertEquals("S", ElementMapper.symbolFor(16))
        assertEquals("Fe", ElementMapper.symbolFor(26))
        assertEquals("Br", ElementMapper.symbolFor(35))
        assertEquals("X", ElementMapper.symbolFor(999)) // Unknown
    }

    @Test
    fun elementMapperBondTypesCorrect() {
        assertEquals(BondType.SINGLE, ElementMapper.bondTypeFor(1))
        assertEquals(BondType.DOUBLE, ElementMapper.bondTypeFor(2))
        assertEquals(BondType.TRIPLE, ElementMapper.bondTypeFor(3))
        assertEquals(BondType.AROMATIC, ElementMapper.bondTypeFor(5))
        assertEquals(BondType.SINGLE, ElementMapper.bondTypeFor(99)) // Fallback
    }

    @Test
    fun responseParserExtractsCid() {
        val json = """{"IdentifierList":{"CID":[1983]}}"""
        assertEquals(1983, PubChemResponseParser.parseCidResponse(json))
    }

    @Test
    fun responseParserExtractsProperties() {
        val json = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C2H6O","MolecularWeight":46.07,"IUPACName":"ethanol","CanonicalSMILES":"CCO","InChIKey":"LFQSCWFLJHTTHZ-UHFFFAOYSA-N"}]}}"""
        val result = PubChemResponseParser.parsePropertiesResponse(json)
        assertNotNull(result)
        assertEquals(1983, result.cid)
        assertEquals("C2H6O", result.molecularFormula)
        assertEquals(46.07, result.molecularWeight, 0.01)
        assertEquals("ethanol", result.iupacName)
    }

    @Test
    fun responseParserExtractsConformer() {
        val json = """{"PC_Compounds":[{"atoms":{"aid":[1,2,3],"element":[6,8,1],"coords":[{"x":[0.0],"y":[1.0],"z":[2.0]}]},"bonds":{"aid1":[1,1],"aid2":[2,3],"order":[1,1]}}]}"""
        val (atoms, bonds) = PubChemResponseParser.parseConformerResponse(json)
        assertEquals(3, atoms.size)
        assertEquals(2, bonds.size)
        assertEquals(1, atoms[0].id)
        assertEquals(6, atoms[0].element)
        assertEquals(0.0, atoms[0].x)
        assertEquals(1.0, atoms[0].y)
        assertEquals(1, bonds[0].from)
        assertEquals(2, bonds[0].to)
    }

    @Test
    fun fetchByNameThrowsOnHttp500() = runTest {
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                respond(
                    content = "Internal Server Error",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }
        val client = PubChemClient(httpClient = HttpClient(mockEngine))
        assertFailsWith<PubChemNetworkException> {
            client.fetchByName("ethanol")
        }
    }

    @Test
    fun fetchCidRetriesOn429ThenThrows() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                requestCount++
                respond(
                    content = "Rate limit exceeded",
                    status = HttpStatusCode.TooManyRequests,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append("Retry-After", "1")
                    }
                )
            }
        }
        val client = PubChemClient(httpClient = HttpClient(mockEngine))
        assertFailsWith<PubChemRateLimitException> {
            client.fetchByName("ethanol")
        }
        assertEquals(3, requestCount)
    }

    @Test
    fun fetchCidRetriesOn503ThenThrows() = runTest {
        var requestCount = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                requestCount++
                respond(
                    content = "Service Unavailable",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
        }
        val client = PubChemClient(httpClient = HttpClient(mockEngine))
        assertFailsWith<PubChemNetworkException> {
            client.fetchByName("ethanol")
        }
        assertEquals(3, requestCount)
    }

    @Test
    fun fetchPropertiesReturnsNullOnNonOkStatus() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[1983]}}"""
        var requestCount = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                requestCount++
                if (requestCount == 1) {
                    respond(
                        content = cidResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                } else {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }
        val client = PubChemClient(httpClient = HttpClient(mockEngine))
        val result = client.fetchByName("ethanol")
        assertNull(result)
    }

    @Test
    fun fetchByFormulaRetriesOn429ThenSucceeds() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[962]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":962,"MolecularFormula":"H2O","MolecularWeight":18.015,"IUPACName":"water","CanonicalSMILES":"O","InChIKey":"XLYOFNOQVPJJNP-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        var cidRequestCount = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { req ->
                val url = req.url.toString()
                when {
                    url.contains("/compound/formula/") -> {
                        cidRequestCount++
                        if (cidRequestCount <= 2) {
                            respond(
                                content = "Rate limited",
                                status = HttpStatusCode.TooManyRequests,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, "text/plain")
                                    append("Retry-After", "1")
                                }
                            )
                        } else {
                            respond(
                                content = cidResponse,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                    }
                    url.contains("/property/") -> respond(
                        content = propsResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                    url.contains("/conformer/") -> respond(
                        content = conformerResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                    else -> respond(
                        content = "",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
        }
        val client = PubChemClient(httpClient = HttpClient(mockEngine))
        val result = client.fetchByFormula("H2O")
        assertNotNull(result)
        assertEquals(962, result.cid)
        assertEquals("water", result.iupacName)
        assertEquals(3, cidRequestCount)
    }

    private fun createClientWithResponse(responseBody: String?): PubChemClient {
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                val body = responseBody ?: ""
                respond(
                    content = body,
                    status = if (body.isNotEmpty()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        return PubChemClient(httpClient = HttpClient(mockEngine))
    }

    private fun createClientWithSequence(vararg responses: String): PubChemClient {
        var index = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                val body = if (index < responses.size) responses[index++] else ""
                respond(
                    content = body,
                    status = if (body.isNotEmpty()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        return PubChemClient(httpClient = HttpClient(mockEngine))
    }
}
