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

import io.drishti.core.ContentItem
import io.drishti.core.ContentType
import io.drishti.core.DetectorPlugin
import io.drishti.core.Frame
import io.drishti.core.MoleculeContent
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout

/**
 * Detects molecule content using the PubChem API.
 *
 * This is the primary API-first detector. It accepts text input (compound name,
 * SMILES string, InChI identifier, or molecular formula), queries the PubChem
 * PUG REST API, and returns structured [MoleculeContent] with real molecular data.
 *
 * The legacy [detect] method (frame-based) is retained for interface compatibility
 * but returns `null` since vision-based detection has been replaced by API-first lookup.
 *
 * @param pubChemClient Client for PubChem API communication
 * @param parser Parser for molecule input strings
 */
public class MoleculeDetector(
    private val pubChemClient: PubChemClient = PubChemClient(
        httpClient = HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000L
                socketTimeoutMillis = 10_000L
                requestTimeoutMillis = 30_000L
            }
        }
    ),
    private val parser: MoleculeParser = MoleculeParser()
) : DetectorPlugin {
    override val contentType: ContentType = ContentType.Molecule
    override val confidence: Float = 0.95f

    /**
     * Detect molecule from a text input string.
     *
     * Accepts molecule names ("water", "caffeine"), SMILES ("CCO", "c1ccccc1"),
     * InChI identifiers ("InChI=1S/..."), or molecular formulas ("C2H6O", "C₆H₁₂O₆").
     *
     * Queries PubChem for compound data and returns a [MoleculeContent] with
     * real atoms, bonds, molecular formula, and IUPAC name.
     *
     * @param input Text description of the molecule
     * @return [MoleculeContent] with real PubChem data, or `null` if not found
     */
    public suspend fun detectFromText(input: String): MoleculeContent? {
        val parsed = parser.parse(input)
        return when (parsed.type) {
            MoleculeInputType.SMILES -> pubChemClient.fetchBySmiles(parsed.normalizedValue)
            MoleculeInputType.MOLECULAR_FORMULA -> pubChemClient.fetchByFormula(parsed.normalizedValue)
            MoleculeInputType.INCHI -> pubChemClient.fetchByInchi(parsed.normalizedValue)
            MoleculeInputType.IUPAC_NAME, MoleculeInputType.COMMON_NAME ->
                pubChemClient.fetchByName(parsed.normalizedValue)
        }?.toMoleculeContent()
    }

    /**
     * Detect molecule from a text input string, returning rich [MoleculeData].
     *
     * Same as [detectFromText] but returns the full [MoleculeData] including
     * molecular weight, SMILES, InChIKey, and other PubChem properties.
     *
     * @param input Text description of the molecule
     * @return [MoleculeData] with full PubChem data, or `null` if not found
     */
    public suspend fun detectMoleculeData(input: String): MoleculeData? {
        val parsed = parser.parse(input)
        return when (parsed.type) {
            MoleculeInputType.SMILES -> pubChemClient.fetchBySmiles(parsed.normalizedValue)
            MoleculeInputType.MOLECULAR_FORMULA -> pubChemClient.fetchByFormula(parsed.normalizedValue)
            MoleculeInputType.INCHI -> pubChemClient.fetchByInchi(parsed.normalizedValue)
            MoleculeInputType.IUPAC_NAME, MoleculeInputType.COMMON_NAME ->
                pubChemClient.fetchByName(parsed.normalizedValue)
        }
    }

    /**
     * Legacy frame-based detection (interface compatibility).
     *
     * Returns `null` because vision-based molecule detection has been
     * replaced by API-first text lookup. Use [detectFromText] instead.
     */
    override suspend fun detect(frame: Frame): ContentItem? {
        val text = try {
            frame.data?.decodeToString()
        } catch (_: IllegalArgumentException) {
            null
        } ?: return null
        if (text.isBlank()) return null
        return detectFromText(text)
    }
}
