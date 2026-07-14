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

import io.drishti.core.AudioOutput
import io.drishti.core.AudioRenderer
import io.drishti.core.ContentItem
import io.drishti.core.ContentType
import io.drishti.core.DetectorPlugin
import io.drishti.core.ExplorationDirection
import io.drishti.core.Frame
import io.drishti.core.HapticOutput
import io.drishti.core.HapticsRenderer
import io.drishti.core.MoleculeContent
import io.drishti.core.SpeechSegment
import io.drishti.core.VoiceOutput
import io.drishti.core.VoiceOutputRenderer

/**
 * Complete molecule plugin combining PubChem-based detection and multi-modal rendering.
 *
 * This plugin exposes two detection modes:
 * - **Frame-based** (legacy): Returns `null` — vision-based detection replaced by API-first
 * - **Text-based** (primary): Accepts molecule names, SMILES, formulas, or InChI strings
 *
 * Rendering supports both basic [MoleculeContent] and enhanced [MoleculeData] from PubChem.
 *
 * Usage:
 * ```kotlin
 * val plugin = MoleculePlugin()
 *
 * // Text-based detection (primary)
 * val molecule = plugin.detectMolecule("caffeine")
 *
 * // Render with rich data
 * plugin.renderHaptic(listOf(molecule), 0, moleculeData)
 * ```
 *
 * @param detector The molecule detector (injected for testability)
 * @param renderer The molecule renderer
 */
public class MoleculePlugin(
    private val detector: MoleculeDetector = MoleculeDetector(),
    private val renderer: MoleculeRenderer = MoleculeRenderer()
) : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    override val name: String = "molecule"
    override val contentType: ContentType = ContentType.Molecule
    override val confidence: Float = detector.confidence

    /**
     * Detect molecule from a text input string.
     *
     * Accepts compound names, SMILES notation, InChI identifiers,
     * or molecular formulas. Returns `null` if PubChem cannot find
     * a matching compound.
     *
     * @param input Text description of the molecule
     * @return [MoleculeContent] with real PubChem data, or `null`
     */
    public suspend fun detectMolecule(input: String): MoleculeContent? = detector.detectFromText(input)

    /**
     * Detect molecule from text, returning rich [MoleculeData].
     *
     * Same as [detectMolecule] but includes full PubChem properties
     * (molecular weight, SMILES, IUPAC name, etc.) for enhanced rendering.
     *
     * @param input Text description of the molecule
     * @return [MoleculeData] with full PubChem data, or `null`
     */
    public suspend fun detectMoleculeData(input: String): MoleculeData? = detector.detectMoleculeData(input)

    // -- DetectorPlugin (legacy frame-based) --

    override suspend fun detect(frame: Frame): ContentItem? = null

    // -- HapticsRenderer --

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        return renderHaptic(items, focusIndex, moleculeData = null)
    }

    /**
     * Render haptic output with optional rich molecule data.
     *
     * @param items Content items to render
     * @param focusIndex Index of the focused item
     * @param moleculeData Optional PubChem data for enhanced haptic rendering
     */
    public fun renderHaptic(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): HapticOutput {
        val hasMolecule = items.any { it is MoleculeContent }
        if (!hasMolecule) {
            return HapticOutput(pulses = emptyList(), pattern = "empty")
        }
        val pulses = items.mapIndexedNotNull { index, item ->
            if (item !is MoleculeContent) return@mapIndexedNotNull null
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            renderer.renderHaptic(item, moleculeData).pulses
        }.flatten()
        val pattern = if (items.size > 1 && focusIndex in items.indices) {
            "molecule_haptic_focus_$focusIndex"
        } else {
            "molecule_haptic"
        }
        return HapticOutput(pulses = pulses, pattern = pattern)
    }

    // -- AudioRenderer --

    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        return renderAudio(items, focusIndex, moleculeData = null)
    }

    /**
     * Render audio output with optional rich molecule data.
     *
     * @param items Content items to render
     * @param focusIndex Index of the focused item
     * @param moleculeData Optional PubChem data for enhanced audio rendering
     */
    public fun renderAudio(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): AudioOutput {
        val sources = items.mapIndexedNotNull { index, item ->
            if (item !is MoleculeContent) return@mapIndexedNotNull null
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            renderer.renderAudio(item, moleculeData).sources
        }.flatten()
        return AudioOutput(sources = sources, spatial = true)
    }

    // -- VoiceOutputRenderer --

    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        return renderVoice(items, focusIndex, moleculeData = null)
    }

    /**
     * Render voice output with optional rich molecule data.
     *
     * With molecule data, voice description includes formula, weight, and IUPAC name.
     *
     * @param items Content items to render
     * @param focusIndex Index of the focused item
     * @param moleculeData Optional PubChem data for enhanced voice description
     */
    public fun renderVoice(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): VoiceOutput {
        val moleculeItems = items.filterIsInstance<MoleculeContent>()
        if (moleculeItems.isEmpty()) {
            return VoiceOutput(
                speech = SpeechSegment(text = "No molecule content", rate = 1.0f, pitch = 1.0f),
                language = "en-US"
            )
        }
        val speeches = items.mapIndexedNotNull { index, item ->
            if (item !is MoleculeContent) return@mapIndexedNotNull null
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            val speech = renderer.renderVoice(item, moleculeData).speech
            if (focusIndex in items.indices && index == focusIndex) {
                SpeechSegment(
                    text = "Molecule ${index + 1} of ${items.size}. ${speech.text}",
                    rate = speech.rate,
                    pitch = speech.pitch
                )
            } else {
                speech
            }
        }
        if (speeches.isEmpty()) {
            return VoiceOutput(
                speech = SpeechSegment(text = "No molecule content", rate = 1.0f, pitch = 1.0f),
                language = "en-US"
            )
        }
        val combinedText = speeches.joinToString(" ") { it.text }
        return VoiceOutput(speech = SpeechSegment(text = combinedText, rate = 1.0f, pitch = 1.0f), language = "en-US")
    }

    // -- Exploration renderers --

    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        val base = when (item) {
            is MoleculeContent -> {
                val idx = if (elementIndex >= 0) elementIndex else {
                    when (direction) {
                        ExplorationDirection.NEXT -> 0
                        ExplorationDirection.PREVIOUS -> item.atoms.size - 1
                        ExplorationDirection.POSITION -> 0
                    }
                }
                renderer.renderHaptic(item).let { baseOutput ->
                    val singlePulse = baseOutput.pulses.getOrNull(idx)
                    HapticOutput(
                        pulses = if (singlePulse != null) listOf(singlePulse.copy(intensity = (singlePulse.intensity * 1.2f).coerceAtMost(1f))) else emptyList(),
                        pattern = "molecule_explore_atom_$idx"
                    )
                }
            }
            else -> return HapticOutput(pulses = emptyList(), pattern = "exploration")
        }
        return base
    }

    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        val base = when (item) {
            is MoleculeContent -> {
                val idx = if (elementIndex >= 0) elementIndex else {
                    when (direction) {
                        ExplorationDirection.NEXT -> 0
                        ExplorationDirection.PREVIOUS -> item.atoms.size - 1
                        ExplorationDirection.POSITION -> 0
                    }
                }
                renderer.renderAudio(item).let { baseOutput ->
                    val singleSource = baseOutput.sources.getOrNull(idx)
                    AudioOutput(
                        sources = if (singleSource != null) listOf(singleSource) else emptyList(),
                        spatial = true
                    )
                }
            }
            else -> return AudioOutput(sources = emptyList(), spatial = true)
        }
        return base
    }

    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput = when (item) {
        is MoleculeContent -> {
            val idx = if (elementIndex >= 0) elementIndex else {
                when (direction) {
                    ExplorationDirection.NEXT -> 0
                    ExplorationDirection.PREVIOUS -> item.atoms.size - 1
                    ExplorationDirection.POSITION -> 0
                }
            }
            val atom = item.atoms.getOrNull(idx)
            val text = if (atom != null) {
                "Atom ${idx + 1}: ${atom.element} at coordinates x=${"%.1f".format(atom.position.x)}, y=${"%.1f".format(atom.position.y)}"
            } else {
                when (direction) {
                    ExplorationDirection.NEXT -> "No more atoms"
                    ExplorationDirection.PREVIOUS -> "No previous atoms"
                    ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
                }
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.95f, pitch = 1.0f), language = "en-US")
        }
        else -> VoiceOutput(
            speech = SpeechSegment(text = "Exploration", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
