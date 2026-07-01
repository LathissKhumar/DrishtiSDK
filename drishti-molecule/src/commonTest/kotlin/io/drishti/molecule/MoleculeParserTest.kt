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

import kotlin.test.*

class MoleculeParserTest {

    private val parser = MoleculeParser()

    // -- Input type detection --

    @Test
    fun detectSmilesSimple() {
        assertEquals(MoleculeInputType.SMILES, parser.parse("CCO").type)
    }

    @Test
    fun detectSmilesAromatic() {
        assertEquals(MoleculeInputType.SMILES, parser.parse("c1ccccc1").type)
    }

    @Test
    fun detectSmilesWithBonds() {
        assertEquals(MoleculeInputType.SMILES, parser.parse("C=C").type)
    }

    @Test
    fun detectSmilesWithBranches() {
        assertEquals(MoleculeInputType.SMILES, parser.parse("CC(O)C").type)
    }

    @Test
    fun detectFormulaSimple() {
        assertEquals(MoleculeInputType.MOLECULAR_FORMULA, parser.parse("H2O").type)
    }

    @Test
    fun detectFormulaComplex() {
        assertEquals(MoleculeInputType.MOLECULAR_FORMULA, parser.parse("C6H12O6").type)
    }

    @Test
    fun detectFormulaWithSubscripts() {
        assertEquals(MoleculeInputType.MOLECULAR_FORMULA, parser.parse("C₆H₁₂O₆").type)
    }

    @Test
    fun detectInchi() {
        val input = "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3"
        assertEquals(MoleculeInputType.INCHI, parser.parse(input).type)
    }

    @Test
    fun detectInchiCaseInsensitive() {
        assertEquals(MoleculeInputType.INCHI, parser.parse("inchi=1S/CH4/h1H4").type)
    }

    @Test
    fun detectCommonName() {
        assertEquals(MoleculeInputType.COMMON_NAME, parser.parse("water").type)
    }

    @Test
    fun detectIupacName() {
        assertEquals(MoleculeInputType.COMMON_NAME, parser.parse("ethanol").type)
    }

    @Test
    fun detectChemicalNameWithHyphen() {
        assertEquals(MoleculeInputType.COMMON_NAME, parser.parse("2-propanol").type)
    }

    // -- Formula normalization --

    @Test
    fun normalizeFormulaSubscripts() {
        assertEquals("C6H12O6", parser.normalizeFormula("C₆H₁₂O₆"))
    }

    @Test
    fun normalizeFormulaMixedSubscriptsAndAscii() {
        assertEquals("C6H5OH", parser.normalizeFormula("C₆H₅OH"))
    }

    @Test
    fun normalizeFormulaNoSubscripts() {
        assertEquals("H2O", parser.normalizeFormula("H2O"))
    }

    @Test
    fun normalizeFormulaEmpty() {
        assertEquals("", parser.normalizeFormula(""))
    }

    // -- isSmiles --

    @Test
    fun isSmilesWithEquals() {
        assertTrue(parser.isSmiles("C=C"))
    }

    @Test
    fun isSmilesWithHash() {
        assertTrue(parser.isSmiles("C#N"))
    }

    @Test
    fun isSmilesWithRingClosure() {
        assertTrue(parser.isSmiles("C1CCCCC1"))
    }

    @Test
    fun isSmilesWithBrackets() {
        assertTrue(parser.isSmiles("[NH3+]"))
    }

    @Test
    fun isSmilesAromaticMixed() {
        assertTrue(parser.isSmiles("c1ccncc1"))
    }

    @Test
    fun isNotSmilesSimpleWord() {
        assertFalse(parser.isSmiles("water"))
    }

    @Test
    fun isNotSmilesTooShort() {
        assertFalse(parser.isSmiles("C"))
    }

    // -- isFormula --

    @Test
    fun isFormulaWater() {
        assertTrue(parser.isFormula("H2O"))
    }

    @Test
    fun isFormulaGlucose() {
        assertTrue(parser.isFormula("C6H12O6"))
    }

    @Test
    fun isFormulaWithUnicode() {
        assertTrue(parser.isFormula("C₆H₁₂O₆"))
    }

    @Test
    fun isNotFormulaNoDigits() {
        assertFalse(parser.isFormula("water"))
    }

    @Test
    fun isNotFormulaEmpty() {
        assertFalse(parser.isFormula(""))
    }

    @Test
    fun isNotFormulaStartsWithLowercase() {
        assertFalse(parser.isFormula("h2o"))
    }

    // -- isInchi --

    @Test
    fun isInchiStandard() {
        assertTrue(parser.isInchi("InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3"))
    }

    @Test
    fun isInchiVersion1() {
        assertTrue(parser.isInchi("InChI=1/CH4/h1H4"))
    }

    @Test
    fun isNotInchi() {
        assertFalse(parser.isInchi("ethanol"))
    }

    @Test
    fun isNotInchiFormula() {
        assertFalse(parser.isInchi("C2H6O"))
    }

    // -- isLikelyChemicalName --

    @Test
    fun isLikelyNameSimple() {
        assertTrue(parser.isLikelyChemicalName("water"))
    }

    @Test
    fun isLikelyNameComplex() {
        assertTrue(parser.isLikelyChemicalName("acetylsalicylic acid"))
    }

    @Test
    fun isLikelyNameWithHyphen() {
        assertTrue(parser.isLikelyChemicalName("2-propanol"))
    }

    @Test
    fun isNotLikelyNameFormula() {
        assertFalse(parser.isLikelyChemicalName("H2O"))
    }

    @Test
    fun isNotLikelyNameSmiles() {
        assertFalse(parser.isLikelyChemicalName("CCO"))
    }

    @Test
    fun isNotLikelyNameSingleChar() {
        assertFalse(parser.isLikelyChemicalName("C"))
    }

    // -- parse normalization --

    @Test
    fun parseFormulaNormalizesUnicode() {
        val result = parser.parse("C₆H₁₂O₆")
        assertEquals("C6H12O6", result.normalizedValue)
        assertEquals("C₆H₁₂O₆", result.value)
    }

    @Test
    fun parseNamePreservesOriginal() {
        val result = parser.parse("ethanol")
        assertEquals("ethanol", result.normalizedValue)
        assertEquals("ethanol", result.value)
    }

    @Test
    fun parseSmilesPreservesOriginal() {
        val result = parser.parse("c1ccccc1")
        assertEquals("c1ccccc1", result.normalizedValue)
        assertEquals("c1ccccc1", result.value)
    }
}
