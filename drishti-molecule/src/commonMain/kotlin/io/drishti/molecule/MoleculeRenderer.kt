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

/**
 * Renders molecule content as haptic, audio, and voice outputs.
 *
 * Supports both basic [MoleculeContent] rendering (backward-compatible)
 * and enhanced rendering with rich [MoleculeData] from PubChem.
 *
 * When [MoleculeData] is provided:
 * - Haptic intensity scales with molecular weight (heavier = stronger)
 * - Voice output includes molecular formula, IUPAC name, and weight
 * - Audio frequency range adapts to atom count
 */
public class MoleculeRenderer {

    /**
     * Render molecule as haptic output.
     *
     * When [data] is provided, atom intensities are scaled by molecular weight.
     * Without [data], uses base element intensities for backward compatibility.
     *
     * @param molecule Base molecule content
     * @param data Optional rich PubChem data for enhanced rendering
     */
    public fun renderHaptic(molecule: MoleculeContent, data: MoleculeData? = null): HapticOutput {
        val weight = data?.molecularWeight ?: molecule.molecularWeight
        val weightScale = if (weight > 0.0) computeWeightScale(weight) else 1.0f
        val pulses = mutableListOf<HapticPulse>()

        val maxX = molecule.atoms.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = molecule.atoms.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        fun normX(v: Float) = (v / maxX).coerceIn(0.05f, 0.95f)
        fun normY(v: Float) = (v / maxY).coerceIn(0.05f, 0.95f)

        molecule.atoms.forEach { atom ->
            pulses.add(
                HapticPulse(
                    intensity = (atomIntensity(atom.element) * weightScale).coerceIn(0.1f, 1.0f),
                    duration = atomDuration(atom.element),
                    x = normX(atom.position.x),
                    y = normY(atom.position.y)
                )
            )
        }

        val atomById = molecule.atoms.associateBy { it.id }

        molecule.bonds.forEach { bond ->
            val from = atomById[bond.from]
            val to = atomById[bond.to]
            if (from != null && to != null) {
                pulses.add(
                    HapticPulse(
                        intensity = bondIntensity(bond.type),
                        duration = bondDuration(bond.type),
                        x = normX((from.position.x + to.position.x) / 2),
                        y = normY((from.position.y + to.position.y) / 2)
                    )
                )
            }
        }

        return HapticOutput(pulses = pulses, pattern = "molecule_exploration")
    }

    /**
     * Render molecule as audio output.
     *
     * When [data] is provided, the frequency range adapts to atom count.
     * Without [data], uses base element frequencies for backward compatibility.
     *
     * @param molecule Base molecule content
     * @param data Optional rich PubChem data for enhanced rendering
     */
    public fun renderAudio(molecule: MoleculeContent, data: MoleculeData? = null): AudioOutput {
        val atomCount = molecule.atoms.size
        val frequencyShift = computeFrequencyShift(atomCount)

        val maxX = molecule.atoms.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = molecule.atoms.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        val maxZ = molecule.atoms.maxOfOrNull { kotlin.math.abs(it.z) }?.coerceAtLeast(1f) ?: 1f

        val sources = molecule.atoms.map { atom ->
            AudioSource(
                frequency = (atomFrequency(atom.element) + frequencyShift).coerceIn(100f, 1000f),
                amplitude = atomAmplitude(atom.element),
                spatialX = (atom.position.x / maxX).coerceIn(0.05f, 0.95f),
                spatialY = (atom.position.y / maxY).coerceIn(0.05f, 0.95f),
                spatialZ = ((atom.z + maxZ) / (2 * maxZ)).coerceIn(0.05f, 0.95f)
            )
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    /**
     * Render molecule as voice output.
     *
     * With [data], includes molecular formula, weight, and IUPAC name.
     * Without [data], provides basic atom and bond description.
     *
     * @param molecule Base molecule content
     * @param data Optional rich PubChem data for enhanced description
     */
    public fun renderVoice(molecule: MoleculeContent, data: MoleculeData? = null): VoiceOutput {
        val description = when {
            data != null -> buildEnhancedVoiceDescription(molecule, data)
            molecule.molecularFormula.isNotEmpty() -> buildString {
                append("Molecule: ${molecule.name.ifEmpty { molecule.iupacName }}. ")
                if (molecule.molecularFormula.isNotEmpty()) {
                    append("Formula: ${molecule.molecularFormula}. ")
                }
                if (molecule.molecularWeight > 0.0) {
                    append("Molecular weight: ${"%.2f".format(molecule.molecularWeight)} grams per mole. ")
                }
                append("Contains ${molecule.atoms.size} atoms: ")
                append(groupAtoms(molecule.atoms))
                append(". ${molecule.bonds.size} bonds. ")
                if (molecule.canonicalSmiles.isNotEmpty()) {
                    append("SMILES: ${molecule.canonicalSmiles}. ")
                }
            }
            else -> buildBasicVoiceDescription(molecule)
        }
        return VoiceOutput(
            speech = SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    private fun computeWeightScale(molecularWeight: Double): Float {
        // Scale from 0.7 (light molecules like H2) to 1.3 (heavy molecules like proteins)
        return (BASE_WEIGHT_SCALE + molecularWeight / WEIGHT_NORMALIZATION_FACTOR)
            .coerceIn(MIN_WEIGHT_SCALE, MAX_WEIGHT_SCALE).toFloat()
    }

    public companion object {
        private const val BASE_WEIGHT_SCALE = 0.5
        private const val WEIGHT_NORMALIZATION_FACTOR = 200.0
        private const val MIN_WEIGHT_SCALE = 0.7
        private const val MAX_WEIGHT_SCALE = 1.3
    }

    private fun computeFrequencyShift(atomCount: Int): Float {
        // Larger molecules shift frequencies down slightly for richer sound
        return when {
            atomCount > 20 -> -50f
            atomCount > 10 -> -25f
            else -> 0f
        }
    }

    private fun buildEnhancedVoiceDescription(molecule: MoleculeContent, data: MoleculeData): String = buildString {
        append("Molecule: ${data.name.ifEmpty { molecule.name }}. ")
        if (data.molecularFormula.isNotEmpty()) {
            append("Formula: ${data.molecularFormula}. ")
        }
        if (data.molecularWeight > 0) {
            append("Molecular weight: ${"%.2f".format(data.molecularWeight)} grams per mole. ")
        }
        append("Contains ${molecule.atoms.size} atoms: ")
        append(groupAtoms(molecule.atoms))
        append(". ${molecule.bonds.size} bonds. ")
        if (data.canonicalSmiles.isNotEmpty()) {
            append("SMILES: ${data.canonicalSmiles}. ")
        }
    }

    private fun buildBasicVoiceDescription(molecule: MoleculeContent): String = buildString {
        append("Molecule: ${molecule.name}. ")
        append("Contains ${molecule.atoms.size} atoms: ")
        append(groupAtoms(molecule.atoms))
        append(". ${molecule.bonds.size} bonds.")
    }

    private fun atomIntensity(element: String): Float = when (element) {
        "C" -> 0.9f
        "N" -> 0.8f
        "O" -> 0.85f
        "H" -> 0.5f
        "S" -> 0.7f
        "P" -> 0.7f
        "Fe" -> 0.95f
        "Na" -> 0.75f
        "Cl" -> 0.8f
        else -> 0.6f
    }

    private fun atomDuration(element: String): Long = when (element) {
        "H" -> 40L
        "C", "N", "O" -> 60L
        "Fe", "Na", "Cl" -> 80L
        else -> 50L
    }

    private fun bondIntensity(type: BondType): Float = when (type) {
        BondType.SINGLE -> 0.5f
        BondType.DOUBLE -> 0.7f
        BondType.TRIPLE -> 0.9f
        BondType.AROMATIC -> 0.8f
        BondType.IONIC -> 0.6f
        BondType.HYDROGEN -> 0.3f
    }

    private fun bondDuration(type: BondType): Long = when (type) {
        BondType.SINGLE -> 40L
        BondType.DOUBLE -> 50L
        BondType.TRIPLE -> 60L
        BondType.AROMATIC -> 55L
        BondType.IONIC -> 45L
        BondType.HYDROGEN -> 30L
    }

    private fun atomFrequency(element: String): Float = when (element) {
        "C" -> 300f
        "N" -> 400f
        "O" -> 450f
        "H" -> 600f
        "S" -> 250f
        "P" -> 350f
        "Fe" -> 200f
        "Na" -> 500f
        "Cl" -> 550f
        else -> 400f
    }

    private fun atomAmplitude(element: String): Float = when (element) {
        "C" -> 0.8f
        "N" -> 0.7f
        "O" -> 0.75f
        "H" -> 0.4f
        "S" -> 0.6f
        "P" -> 0.6f
        "Fe" -> 0.9f
        "Na" -> 0.65f
        "Cl" -> 0.7f
        else -> 0.5f
    }

    private fun groupAtoms(atoms: List<Atom>): String {
        return atoms.groupBy { it.element }
            .map { (element, list) -> "${list.size} $element" }
            .joinToString(", ")
    }
}
