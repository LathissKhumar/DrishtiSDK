package io.drishti.haptics

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlin.test.*

class HapticsPluginTest {

    @Test
    fun nameIsHaptics() {
        val plugin = HapticsPlugin()
        assertEquals("haptics", plugin.name)
    }

    @Test
    fun renderHapticWithGraphContent() {
        val plugin = HapticsPlugin()
        val items = listOf(TestFixtures.graphContent())
        val output = plugin.renderHaptic(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderHapticWithEmptyItems() {
        val plugin = HapticsPlugin()
        val output = plugin.renderHaptic(emptyList())
        assertNotNull(output)
        assertTrue(output.pulses.isEmpty())
    }

    @Test
    fun renderHapticWithMixedContent() {
        val plugin = HapticsPlugin()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent()
        )
        val output = plugin.renderHaptic(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderExplorationHapticReturnsOutput() {
        val plugin = HapticsPlugin()
        val item = TestFixtures.graphContent()
        val output = plugin.renderExplorationHaptic(item, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun encodeReturnsEncodedPattern() {
        val plugin = HapticsPlugin()
        val output = TestFixtures.hapticOutput()
        val encoded = plugin.encode(output)
        assertNotNull(encoded)
        assertEquals(1, encoded.timings.size)
        assertEquals(1, encoded.amplitudes.size)
    }

    @Test
    fun encodeCompositionReturnsPrimitives() {
        val plugin = HapticsPlugin()
        val output = TestFixtures.hapticOutput()
        val primitives = plugin.encodeComposition(output)
        assertNotNull(primitives)
        assertEquals(1, primitives.size)
    }
}
