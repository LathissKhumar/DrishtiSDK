package io.drishti.haptics

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlin.test.*

class HapticRendererTest {

    @Test
    fun renderGraphContent() {
        val renderer = HapticRenderer()
        val items = listOf(TestFixtures.graphContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
        assertTrue(output.pattern.contains("content_"))
    }

    @Test
    fun renderFormulaContent() {
        val renderer = HapticRenderer()
        val items = listOf(TestFixtures.formulaContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderMoleculeContent() {
        val renderer = HapticRenderer()
        val items = listOf(TestFixtures.moleculeContent())
        val output = renderer.render(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderWithFocusIndex() {
        val renderer = HapticRenderer()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent()
        )
        val output = renderer.render(items, focusIndex = 1)
        assertNotNull(output)
        assertTrue(output.pattern.contains("focus_1"))
    }

    @Test
    fun renderExplorationNext() {
        val renderer = HapticRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExploration(item, ExplorationDirection.NEXT)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderExplorationPrevious() {
        val renderer = HapticRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExploration(item, ExplorationDirection.PREVIOUS)
        assertNotNull(output)
    }

    @Test
    fun renderExplorationPosition() {
        val renderer = HapticRenderer()
        val item = TestFixtures.graphContent()
        val output = renderer.renderExploration(item, ExplorationDirection.POSITION)
        assertNotNull(output)
        assertEquals(1, output.pulses.size)
        assertEquals(1.0f, output.pulses[0].intensity)
    }
}
