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

package io.drishti.voice

import io.drishti.core.ContentType
import io.drishti.core.ExplorationDirection
import io.drishti.core.FormulaContent
import io.drishti.core.FormulaType
import io.drishti.core.ShapeContent
import io.drishti.core.ShapeType
import io.drishti.core.TableContent
import io.drishti.core.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoicePluginTest {

    @Test
    fun nameIsVoice() {
        val plugin = VoicePlugin()
        assertEquals("voice", plugin.name)
    }

    @Test
    fun renderVoiceWithGraphContent() {
        val plugin = VoicePlugin()
        val items = listOf(TestFixtures.graphContent())
        val output = plugin.renderVoice(items)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("Line chart"))
    }

    @Test
    fun renderVoiceWithEmptyItems() {
        val plugin = VoicePlugin()
        val output = plugin.renderVoice(emptyList())
        assertNotNull(output)
        assertEquals("", output.speech.text)
    }

    @Test
    fun renderExplorationVoiceReturnsOutput() {
        val plugin = VoicePlugin()
        val item = TestFixtures.graphContent()
        val output = plugin.renderExplorationVoice(item, ExplorationDirection.NEXT)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
    }

    @Test
    fun generateSpeech() {
        val plugin = VoicePlugin()
        val speech = plugin.generateSpeech("Hello world")
        assertEquals("Hello world", speech.text)
        assertEquals(1.0f, speech.rate)
    }

    @Test
    fun describeGraph() {
        val plugin = VoicePlugin()
        val item = TestFixtures.graphContent()
        val desc = plugin.describe(item)
        assertTrue(desc.contains("Line chart", ignoreCase = true))
    }

    @Test
    fun describeFormula() {
        val plugin = VoicePlugin()
        val item = TestFixtures.formulaContent()
        val desc = plugin.describe(item)
        assertTrue(desc.contains("formula", ignoreCase = true))
    }

    @Test
    fun describeMolecule() {
        val plugin = VoicePlugin()
        val item = TestFixtures.moleculeContent()
        val desc = plugin.describe(item)
        assertTrue(desc.contains("Molecule", ignoreCase = true))
    }

    @Test
    fun latexToSpeechFraction() {
        val plugin = VoicePlugin()
        val speech = plugin.latexToSpeech("\\frac{a}{b}")
        assertEquals("a over b", speech)
    }

    @Test
    fun latexToSpeechSquareRoot() {
        val plugin = VoicePlugin()
        val speech = plugin.latexToSpeech("\\sqrt{x}")
        assertEquals("square root of x", speech)
    }

    @Test
    fun formulaToSpeech() {
        val plugin = VoicePlugin()
        val formula = TestFixtures.formulaContent(
            expression = "x + y = z"
        )
        val speech = plugin.formulaToSpeech(formula)
        assertTrue(speech.contains("algebraic formula", ignoreCase = true))
        assertTrue(speech.contains("x plus y equals z", ignoreCase = true))
    }
}

class VoiceRendererTest {

    @Test
    fun renderGraphContent() {
        val renderer = VoiceRenderer()
        val items = listOf(TestFixtures.graphContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("Line chart"))
    }

    @Test
    fun renderFormulaContent() {
        val renderer = VoiceRenderer()
        val items = listOf(TestFixtures.formulaContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("formula", ignoreCase = true))
    }

    @Test
    fun renderMoleculeContent() {
        val renderer = VoiceRenderer()
        val items = listOf(TestFixtures.moleculeContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("Molecule"))
    }

    @Test
    fun renderExplorationNext() {
        val renderer = VoiceRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExplorationVoice(item, ExplorationDirection.NEXT)
        assertNotNull(output)
        assertTrue(output.speech.text.contains("Data point"))
    }

    @Test
    fun renderExplorationPrevious() {
        val renderer = VoiceRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExplorationVoice(item, ExplorationDirection.PREVIOUS)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
    }

    @Test
    fun renderExplorationPosition() {
        val renderer = VoiceRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExplorationVoice(item, ExplorationDirection.POSITION)
        assertNotNull(output)
        assertTrue(output.speech.text.contains("position", ignoreCase = true))
    }

    @Test
    fun graphDescriptionIncludesTrend() {
        val renderer = VoiceRenderer()
        val items = listOf(TestFixtures.graphContent())
        val output = renderer.render(items)
        assertTrue(output.speech.text.contains("trend", ignoreCase = true))
    }

    @Test
    fun formulaVerbalizationUsesMathCAT() {
        val renderer = VoiceRenderer()
        val formula = TestFixtures.formulaContent(
            expression = "\\frac{1}{2}"
        )
        val items = listOf(formula)
        val output = renderer.render(items)
        assertTrue(output.speech.text.contains("1 over 2"))
    }
}

class FormulaSpeechTest {

    @Test
    fun fromLatexFraction() {
        val speech = FormulaSpeech.fromLatex("\\frac{a}{b}")
        assertEquals("a over b", speech)
    }

    @Test
    fun fromLatexSquareRoot() {
        val speech = FormulaSpeech.fromLatex("\\sqrt{x}")
        assertEquals("square root of x", speech)
    }

    @Test
    fun fromLatexSuperscript() {
        val speech = FormulaSpeech.fromLatex("x^{2}")
        assertEquals("x squared", speech)
    }

    @Test
    fun fromLatexSubscript() {
        val speech = FormulaSpeech.fromLatex("x_{i}")
        assertEquals("x sub i", speech)
    }

    @Test
    fun fromLatexIntegral() {
        val speech = FormulaSpeech.fromLatex("\\int_{0}^{1} x^{2} \\, dx")
        assertTrue(speech.contains("integral from 0 to 1"))
    }

    @Test
    fun fromLatexSummation() {
        val speech = FormulaSpeech.fromLatex("\\sum_{i=1}^{n} i")
        assertTrue(speech.contains("sum from"))
    }

    @Test
    fun fromLatexGreekLetter() {
        val speech = FormulaSpeech.fromLatex("\\alpha + \\beta")
        assertEquals("alpha plus beta", speech)
    }

    @Test
    fun fromLatexInvalidReturnsRaw() {
        val speech = FormulaSpeech.fromLatex("\\invalid{}{")
        assertEquals("\\invalid{}{", speech)
    }

    @Test
    fun fromContentFormula() {
        val content = FormulaContent(
            formulaType = FormulaType.ALGEBRAIC,
            expression = "x + y = z",
            confidence = 0.85f
        )
        val speech = FormulaSpeech.fromContent(content)
        assertTrue(speech.startsWith("This is an algebraic formula:"))
        assertTrue(speech.contains("x plus y equals z"))
    }

    @Test
    fun fromContentCalculusFormula() {
        val content = FormulaContent(
            formulaType = FormulaType.CALCULUS,
            expression = "\\int_{0}^{1} x^{2} \\, dx",
            confidence = 0.85f
        )
        val speech = FormulaSpeech.fromContent(content)
        assertTrue(speech.contains("calculus formula"))
        assertTrue(speech.contains("integral"))
    }

    @Test
    fun expressionOnlyReturnsJustExpression() {
        val content = FormulaContent(
            formulaType = FormulaType.TRIGONOMETRIC,
            expression = "\\sin(\\theta)",
            confidence = 0.85f
        )
        val speech = FormulaSpeech.expressionOnly(content)
        assertEquals("sine of theta", speech)
        assertFalse(speech.startsWith("This is"))
    }

    @Test
    fun fromLatexCubed() {
        val speech = FormulaSpeech.fromLatex("x^{3}")
        assertEquals("x cubed", speech)
    }

    @Test
    fun fromLatexPower() {
        val speech = FormulaSpeech.fromLatex("x^{n}")
        assertEquals("x to the power n", speech)
    }
}

class SpeechGeneratorTest {

    @Test
    fun generate() {
        val gen = SpeechGenerator()
        val speech = gen.generate("Hello")
        assertEquals("Hello", speech.text)
        assertEquals(1.0f, speech.rate)
        assertEquals(1.0f, speech.pitch)
    }

    @Test
    fun generateWithCustomRatePitch() {
        val gen = SpeechGenerator()
        val speech = gen.generate("Hello", 0.8f, 1.2f)
        assertEquals("Hello", speech.text)
        assertEquals(0.8f, speech.rate)
        assertEquals(1.2f, speech.pitch)
    }

    @Test
    fun generateNumber() {
        val gen = SpeechGenerator()
        val speech = gen.generateNumber(3.14159f)
        assertEquals("3.1", speech.text)
    }

    @Test
    fun generateCoordinate() {
        val gen = SpeechGenerator()
        val speech = gen.generateCoordinate(10f, 20f)
        assertTrue(speech.text.contains("10.0"))
        assertTrue(speech.text.contains("20.0"))
    }

    @Test
    fun generateListEmpty() {
        val gen = SpeechGenerator()
        val speech = gen.generateList(emptyList())
        assertEquals("Empty list.", speech.text)
    }

    @Test
    fun generateListSingle() {
        val gen = SpeechGenerator()
        val speech = gen.generateList(listOf("A"))
        assertEquals("One item: A.", speech.text)
    }

    @Test
    fun generateListMultiple() {
        val gen = SpeechGenerator()
        val speech = gen.generateList(listOf("A", "B", "C"))
        assertEquals("3 items: A, B, C.", speech.text)
    }
}

class ContentDescriberTest {

    @Test
    fun describeGraph() {
        val describer = ContentDescriber()
        val graph = TestFixtures.graphContent()
        val desc = describer.describeGraph(graph)
        assertTrue(desc.contains("Line chart"))
        assertTrue(desc.contains("3 data points"))
    }

    @Test
    fun describeGraphWithTitle() {
        val describer = ContentDescriber()
        val graph = TestFixtures.graphContent(title = "Sales")
        val desc = describer.describeGraph(graph)
        assertTrue(desc.contains("Sales"))
    }

    @Test
    fun describeFormula() {
        val describer = ContentDescriber()
        val formula = TestFixtures.formulaContent()
        val desc = describer.describeFormula(formula)
        assertTrue(desc.contains("Algebraic"))
        assertTrue(desc.contains("symbols"))
    }

    @Test
    fun describeMolecule() {
        val describer = ContentDescriber()
        val molecule = TestFixtures.moleculeContent()
        val desc = describer.describeMolecule(molecule)
        assertTrue(desc.contains("Methanol"))
        assertTrue(desc.contains("atoms"))
    }

    @Test
    fun describeShape() {
        val describer = ContentDescriber()
        val shape = ShapeContent(ShapeType.CIRCLE, 100f, 35f, confidence = 0.85f)
        val desc = describer.describeShape(shape)
        assertTrue(desc.contains("Circle"))
        assertTrue(desc.contains("100.0"))
    }

    @Test
    fun describeTable() {
        val describer = ContentDescriber()
        val table = TableContent(3, 4, emptyList(), confidence = 0.85f)
        val desc = describer.describeTable(table)
        assertTrue(desc.contains("3 rows"))
        assertTrue(desc.contains("4 columns"))
    }
}

class VoiceDataTest {

    @Test
    fun defaultParameters() {
        val data = VoiceData.default()
        assertEquals("en-US", data.language)
        assertEquals(1.0f, data.rate)
        assertEquals(1.0f, data.pitch)
    }

    @Test
    fun rateForFormulaIsSlower() {
        val data = VoiceData.default()
        val rate = data.rateForContentType(ContentType.Formula)
        assertTrue(rate < 1.0f, "Formula rate should be slower than default")
    }

    @Test
    fun rateForGraphIsDefault() {
        val data = VoiceData.default()
        val rate = data.rateForContentType(ContentType.Graph)
        assertEquals(1.0f, rate)
    }

    @Test
    fun withLanguage() {
        val data = VoiceData.withLanguage("hi-IN")
        assertEquals("hi-IN", data.language)
        assertEquals(1.0f, data.rate)
    }

    @Test
    fun pitchForContentType() {
        val data = VoiceData.default()
        val pitch = data.pitchForContentType(ContentType.Molecule)
        assertEquals(1.0f, pitch)
    }
}
