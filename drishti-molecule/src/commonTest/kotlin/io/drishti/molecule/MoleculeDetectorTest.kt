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

import io.drishti.core.ContentType
import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MoleculeDetectorTest {

    @Test
    fun contentTypeIsMolecule() {
        val detector = MoleculeDetector(
            pubChemClient = createClientWithResponse(null),
            parser = MoleculeParser()
        )
        assertEquals(ContentType.Molecule, detector.contentType)
    }

    @Test
    fun confidenceIsHigh() {
        val detector = MoleculeDetector(
            pubChemClient = createClientWithResponse(null),
            parser = MoleculeParser()
        )
        assertEquals(0.95f, detector.confidence)
    }

    @Test
    fun detectReturnsNullForFrame() = runTest {
        val detector = MoleculeDetector(
            pubChemClient = createClientWithResponse(null),
            parser = MoleculeParser()
        )
        val frame = Frame(width = 640, height = 480, format = FrameFormat.RGB_888, data = null)
        val result = detector.detect(frame)
        assertNull(result)
    }

    @Test
    fun detectFromTextReturnsNullForUnknownMolecule() = runTest {
        val detector = MoleculeDetector(
            pubChemClient = createClientWithResponse(null),
            parser = MoleculeParser()
        )
        val result = detector.detectFromText("xyznonexistent123")
        assertNull(result)
    }

    @Test
    fun detectFromTextReturnsMoleculeForValidName() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[1983]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":1983,"MolecularFormula":"C2H6O","MolecularWeight":46.07,"IUPACName":"ethanol","CanonicalSMILES":"CCO","InChIKey":"LFQSCWFLJHTTHZ-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[{"atoms":{"aid":[1,2,3],"element":[6,6,8],"coords":[{"x":[0.0],"y":[0.0],"z":[0.0]}]},"bonds":{"aid1":[1],"aid2":[2],"order":[1]}}]}"""

        val detector = createDetectorWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = detector.detectFromText("ethanol")
        assertNotNull(result)
        assertEquals("ethanol", result.name)
        assertEquals(io.drishti.core.MoleculeType.ORGANIC, result.moleculeType)
    }

    @Test
    fun detectFromTextAcceptsFormula() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[962]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":962,"MolecularFormula":"H2O","MolecularWeight":18.015,"IUPACName":"water","CanonicalSMILES":"O","InChIKey":"XLYOFNOQVPJJNP-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[{"atoms":{"aid":[1,2,3],"element":[8,1,1],"coords":[{"x":[0.0],"y":[0.0],"z":[0.0]}]},"bonds":{"aid1":[1,1],"aid2":[2,3],"order":[1,1]}}]}"""

        val detector = createDetectorWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = detector.detectFromText("H2O")
        assertNotNull(result)
        assertEquals("water", result.name)
        assertEquals(io.drishti.core.MoleculeType.SIMPLE, result.moleculeType)
    }

    @Test
    fun detectMoleculeDataReturnsRichData() = runTest {
        val cidResponse = """{"IdentifierList":{"CID":[2244]}}"""
        val propsResponse = """{"PropertyTable":{"Properties":[{"CID":2244,"MolecularFormula":"C7H8","MolecularWeight":92.14,"IUPACName":"toluene","CanonicalSMILES":"Cc1ccccc1","InChIKey":"QQWJEVZSMJNCOY-UHFFFAOYSA-N"}]}}"""
        val conformerResponse = """{"PC_Compounds":[]}"""

        val detector = createDetectorWithSequence(cidResponse, propsResponse, conformerResponse)
        val result = detector.detectMoleculeData("toluene")
        assertNotNull(result)
        assertEquals(2244, result.cid)
        assertEquals("C7H8", result.molecularFormula)
        assertEquals(92.14, result.molecularWeight, 0.01)
        assertEquals("toluene", result.iupacName)
        assertEquals("Cc1ccccc1", result.canonicalSmiles)
    }

    @Test
    fun parserCorrectlyIdentifiesInputTypes() {
        val parser = MoleculeParser()
        assertEquals(MoleculeInputType.SMILES, parser.parse("CCO").type)
        assertEquals(MoleculeInputType.MOLECULAR_FORMULA, parser.parse("C2H6O").type)
        assertEquals(MoleculeInputType.INCHI, parser.parse("InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3").type)
        assertEquals(MoleculeInputType.COMMON_NAME, parser.parse("ethanol").type)
        assertEquals(MoleculeInputType.COMMON_NAME, parser.parse("water").type)
    }

    @Test
    fun parserHandlesUnicodeSubscripts() {
        val parser = MoleculeParser()
        val result = parser.parse("C₆H₁₂O₆")
        assertEquals(MoleculeInputType.MOLECULAR_FORMULA, result.type)
        assertEquals("C6H12O6", result.normalizedValue)
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
        val client = HttpClient(mockEngine)
        return PubChemClient(httpClient = client)
    }

    private fun createDetectorWithSequence(vararg responses: String): MoleculeDetector {
        var callIndex = 0
        val mockEngine = MockEngine.create {
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined
            addHandler { _ ->
                val index = callIndex++
                val body = if (index < responses.size) responses[index] else ""
                respond(
                    content = body,
                    status = if (body.isNotEmpty()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val client = HttpClient(mockEngine)
        return MoleculeDetector(
            pubChemClient = PubChemClient(httpClient = client),
            parser = MoleculeParser()
        )
    }
}
