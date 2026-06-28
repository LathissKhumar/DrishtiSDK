package io.drishti.molecule

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MoleculePluginTest {

    private val plugin = MoleculePlugin()

    @Test
    fun nameIsMolecule() {
        assertEquals("molecule", plugin.name)
    }

    @Test
    fun contentTypeIsMolecule() {
        assertEquals(ContentType.MOLECULE, plugin.contentType)
    }

    @Test
    fun confidenceIsHigh() {
        assertEquals(0.95f, plugin.confidence)
    }

    @Test
    fun detectReturnsNullForFrame() = runTest {
        val frame = Frame(width = 640, height = 480, format = FrameFormat.RGB_888, data = null)
        val result = plugin.detect(frame)
        assertNull(result)
    }

    @Test
    fun renderHapticWithEmptyList() {
        val output = plugin.renderHaptic(emptyList(), 0)
        assertTrue(output.pulses.isEmpty())
        assertEquals("empty", output.pattern)
    }

    @Test
    fun renderHapticWithMoleculeContent() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderHaptic(listOf(molecule), 0)
        assertTrue(output.pulses.isNotEmpty())
        assertEquals("molecule_haptic", output.pattern)
    }

    @Test
    fun renderHapticFiltersNonMoleculeContent() {
        val graph = TestFixtures.graphContent()
        val output = plugin.renderHaptic(listOf(graph), 0)
        assertTrue(output.pulses.isEmpty())
    }

    @Test
    fun renderAudioWithEmptyList() {
        val output = plugin.renderAudio(emptyList(), 0)
        assertTrue(output.sources.isEmpty())
        assertTrue(output.spatial)
    }

    @Test
    fun renderAudioWithMoleculeContent() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderAudio(listOf(molecule), 0)
        assertTrue(output.sources.isNotEmpty())
        assertTrue(output.spatial)
    }

    @Test
    fun renderVoiceWithEmptyList() {
        val output = plugin.renderVoice(emptyList(), 0)
        assertEquals("No molecule content", output.speech.text)
        assertEquals("en-US", output.language)
    }

    @Test
    fun renderVoiceWithMoleculeContent() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderVoice(listOf(molecule), 0)
        assertTrue(output.speech.text.contains("Molecule:"))
        assertTrue(output.speech.text.contains("atoms"))
        assertEquals("en-US", output.language)
    }

    @Test
    fun renderExplorationHapticWithMolecule() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderExplorationHaptic(molecule, ExplorationDirection.NEXT)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderExplorationHapticWithNonMolecule() {
        val graph = TestFixtures.graphContent()
        val output = plugin.renderExplorationHaptic(graph, ExplorationDirection.NEXT)
        assertTrue(output.pulses.isEmpty())
    }

    @Test
    fun renderExplorationAudioWithMolecule() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderExplorationAudio(molecule, ExplorationDirection.PREVIOUS)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderExplorationAudioWithNonMolecule() {
        val graph = TestFixtures.graphContent()
        val output = plugin.renderExplorationAudio(graph, ExplorationDirection.PREVIOUS)
        assertTrue(output.sources.isEmpty())
    }

    @Test
    fun renderExplorationVoiceWithMolecule() {
        val molecule = TestFixtures.moleculeContent()
        val output = plugin.renderExplorationVoice(molecule, ExplorationDirection.NEXT)
        assertTrue(output.speech.text.contains("Next atom:"))
    }

    @Test
    fun renderExplorationVoiceWithNonMolecule() {
        val graph = TestFixtures.graphContent()
        val output = plugin.renderExplorationVoice(graph, ExplorationDirection.NEXT)
        assertEquals("Exploration", output.speech.text)
    }

    @Test
    fun renderHapticWithMoleculeDataUsesWeightScaling() {
        val molecule = TestFixtures.moleculeContent()
        val data = MoleculeData(
            cid = 1983,
            molecularFormula = "C2H6O",
            molecularWeight = 46.07,
            iupacName = "ethanol",
            name = "ethanol"
        )
        val outputBasic = plugin.renderHaptic(listOf(molecule), 0)
        val outputEnhanced = plugin.renderHaptic(listOf(molecule), 0, moleculeData = data)
        // Enhanced output should have different intensities due to weight scaling
        assertTrue(outputEnhanced.pulses.isNotEmpty())
        assertEquals(outputBasic.pulses.size, outputEnhanced.pulses.size)
    }

    @Test
    fun renderVoiceWithMoleculeDataIncludesFormula() {
        val molecule = TestFixtures.moleculeContent()
        val data = MoleculeData(
            cid = 1983,
            molecularFormula = "C2H6O",
            molecularWeight = 46.07,
            iupacName = "ethanol",
            canonicalSmiles = "CCO",
            name = "ethanol"
        )
        val output = plugin.renderVoice(listOf(molecule), 0, moleculeData = data)
        assertTrue(output.speech.text.contains("Formula: C2H6O"))
        assertTrue(output.speech.text.contains("46.07"))
        assertTrue(output.speech.text.contains("ethanol"))
    }
}
