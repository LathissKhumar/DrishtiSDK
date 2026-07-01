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

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlin.test.*

class MoleculeRendererTest {

    private val renderer = MoleculeRenderer()

    @Test
    fun renderHapticCreatesPulsesForAtoms() {
        val molecule = TestFixtures.moleculeContent()
        val output = renderer.renderHaptic(molecule)
        // 3 atoms + 2 bonds = 5 pulses
        assertEquals(5, output.pulses.size)
        assertEquals("molecule_exploration", output.pattern)
    }

    @Test
    fun renderHapticCarbonHasHighIntensity() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "C", position = Point(50f, 50f), charge = 0, label = "C")),
            bonds = emptyList(),
            name = "Carbon"
        )
        val output = renderer.renderHaptic(molecule)
        assertEquals(1, output.pulses.size)
        assertEquals(0.9f, output.pulses[0].intensity)
        assertEquals(60L, output.pulses[0].duration)
    }

    @Test
    fun renderHapticHydrogenHasLowIntensity() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "H", position = Point(50f, 50f), charge = 0, label = "H")),
            bonds = emptyList(),
            name = "Hydrogen"
        )
        val output = renderer.renderHaptic(molecule)
        assertEquals(0.5f, output.pulses[0].intensity)
    }

    @Test
    fun renderHapticIronHasHighestIntensity() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "Fe", position = Point(50f, 50f), charge = 0, label = "Fe")),
            bonds = emptyList(),
            name = "Iron"
        )
        val output = renderer.renderHaptic(molecule)
        assertEquals(0.95f, output.pulses[0].intensity)
    }

    @Test
    fun renderHapticBondIntensities() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(
                Atom(id = 0, element = "C", position = Point(0f, 0f), charge = 0, label = "C"),
                Atom(id = 1, element = "C", position = Point(10f, 0f), charge = 0, label = "C")
            ),
            bonds = listOf(Bond(from = 0, to = 1, type = BondType.SINGLE, strength = 1.0f)),
            name = "C-C"
        )
        val output = renderer.renderHaptic(molecule)
        val bondPulse = output.pulses.last()
        assertEquals(0.5f, bondPulse.intensity)
        assertEquals(40L, bondPulse.duration)
    }

    @Test
    fun renderHapticTripleBondHigherIntensity() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(
                Atom(id = 0, element = "C", position = Point(0f, 0f), charge = 0, label = "C"),
                Atom(id = 1, element = "N", position = Point(10f, 0f), charge = 0, label = "N")
            ),
            bonds = listOf(Bond(from = 0, to = 1, type = BondType.TRIPLE, strength = 1.0f)),
            name = "C≡N"
        )
        val output = renderer.renderHaptic(molecule)
        val bondPulse = output.pulses.last()
        assertEquals(0.9f, bondPulse.intensity)
    }

    @Test
    fun renderAudioCreatesSourcePerAtom() {
        val molecule = TestFixtures.moleculeContent()
        val output = renderer.renderAudio(molecule)
        assertEquals(3, output.sources.size)
        assertTrue(output.spatial)
    }

    @Test
    fun renderAudioCarbonFrequency() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "C", position = Point(50f, 50f), charge = 0, label = "C")),
            bonds = emptyList(),
            name = "Carbon"
        )
        val output = renderer.renderAudio(molecule)
        assertEquals(300f, output.sources[0].frequency)
        assertEquals(0.8f, output.sources[0].amplitude)
    }

    @Test
    fun renderAudioHydrogenFrequency() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "H", position = Point(50f, 50f), charge = 0, label = "H")),
            bonds = emptyList(),
            name = "Hydrogen"
        )
        val output = renderer.renderAudio(molecule)
        assertEquals(600f, output.sources[0].frequency)
        assertEquals(0.4f, output.sources[0].amplitude)
    }

    @Test
    fun renderAudioSpatialPosition() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(Atom(id = 0, element = "O", position = Point(100f, 200f), charge = 0, label = "O")),
            bonds = emptyList(),
            name = "Oxygen"
        )
        val output = renderer.renderAudio(molecule)
        assertEquals(0.95f, output.sources[0].spatialX)
        assertEquals(0.95f, output.sources[0].spatialY)
        assertEquals(0.5f, output.sources[0].spatialZ)
    }

    @Test
    fun renderVoiceIncludesMoleculeName() {
        val molecule = TestFixtures.moleculeContent()
        val output = renderer.renderVoice(molecule)
        assertTrue(output.speech.text.contains("Methanol"))
        assertTrue(output.speech.text.contains("atoms"))
        assertTrue(output.speech.text.contains("bonds"))
        assertEquals("en-US", output.language)
    }

    @Test
    fun renderVoiceAtomCount() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(
                Atom(id = 0, element = "H", position = Point(0f, 0f), charge = 0, label = "H"),
                Atom(id = 1, element = "H", position = Point(10f, 0f), charge = 0, label = "H"),
                Atom(id = 2, element = "O", position = Point(5f, 10f), charge = 0, label = "O")
            ),
            bonds = listOf(Bond(from = 0, to = 2, type = BondType.SINGLE, strength = 1.0f)),
            name = "Water"
        )
        val output = renderer.renderVoice(molecule)
        assertTrue(output.speech.text.contains("3 atoms"))
        assertTrue(output.speech.text.contains("1 bonds"))
    }

    @Test
    fun renderVoiceGroupedAtomCount() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(
                Atom(id = 0, element = "H", position = Point(0f, 0f), charge = 0, label = "H"),
                Atom(id = 1, element = "H", position = Point(10f, 0f), charge = 0, label = "H"),
                Atom(id = 2, element = "O", position = Point(5f, 10f), charge = 0, label = "O")
            ),
            bonds = emptyList(),
            name = "Water"
        )
        val output = renderer.renderVoice(molecule)
        assertTrue(output.speech.text.contains("2 H"))
        assertTrue(output.speech.text.contains("1 O"))
    }

    @Test
    fun renderHapticHandlesUnknownElement() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.COMPLEX,
            atoms = listOf(Atom(id = 0, element = "Xx", position = Point(50f, 50f), charge = 0, label = "Xx")),
            bonds = emptyList(),
            name = "Unknown"
        )
        val output = renderer.renderHaptic(molecule)
        assertEquals(0.6f, output.pulses[0].intensity)
    }

    @Test
    fun renderAudioSpatialZScalesWithAtomZ() {
        val molecule = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = listOf(
                Atom(id = 0, element = "O", position = Point(100f, 200f), z = -50f, charge = 0, label = "O"),
                Atom(id = 1, element = "O", position = Point(100f, 200f), z = 50f, charge = 0, label = "O")
            ),
            bonds = emptyList(),
            name = "Oxygen2"
        )
        val output = renderer.renderAudio(molecule)
        // maxZ should be 50f.
        // spatialZ for atom 0: (-50 + 50) / 100 = 0.0f -> coerced to 0.05f
        // spatialZ for atom 1: (50 + 50) / 100 = 1.0f -> coerced to 0.95f
        assertEquals(0.05f, output.sources[0].spatialZ)
        assertEquals(0.95f, output.sources[1].spatialZ)
    }
}
