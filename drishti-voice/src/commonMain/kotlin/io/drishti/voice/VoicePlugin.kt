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

import io.drishti.core.ContentItem
import io.drishti.core.ExplorationDirection
import io.drishti.core.FormulaContent
import io.drishti.core.GraphContent
import io.drishti.core.MoleculeContent
import io.drishti.core.ShapeContent
import io.drishti.core.SpeechSegment
import io.drishti.core.TableContent
import io.drishti.core.VoiceOutput
import io.drishti.core.VoiceOutputRenderer

/**
 * Voice renderer plugin facade.
 *
 * Provides a single entry point for converting [ContentItem] lists
 * into natural-language voice output using Sherpa-ONNX TTS and
 * MathCAT-style formula verbalization.
 *
 * Usage:
 * ```
 * val plugin = VoicePlugin()
 *
 * // Render content items as speech
 * val output = plugin.renderVoice(listOf(formulaContent))
 *
 * // Convert LaTeX directly to speech
 * val speech = plugin.latexToSpeech("\\frac{a}{b}")
 * // "a over b"
 *
 * // Get a natural description of any content item
 * val desc = plugin.describe(graphContent)
 * ```
 */
public class VoicePlugin(
    private val voiceData: VoiceData = VoiceData.default()
) : VoiceOutputRenderer {

    private val renderer = VoiceRenderer(voiceData)
    private val speechGenerator = SpeechGenerator()
    private val describer = ContentDescriber()

    override val name: String = "voice"

    /**
     * Render content items as voice output.
     *
     * Each item is described in natural language following MathCAT
     * (formulas) and DIAGRAM Center (graphs) conventions.
     */
    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        return renderer.render(items, focusIndex)
    }

    /**
     * Render exploration sequence for interactive navigation.
     */
    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput {
        return renderer.renderExplorationVoice(item, direction, elementIndex)
    }

    /**
     * Generate a raw speech segment from text.
     *
     * Useful for custom announcements outside the content pipeline.
     */
    public fun generateSpeech(text: String): SpeechSegment {
        return speechGenerator.generate(text)
    }

    /**
     * Convert a LaTeX string to MathCAT-style speech text.
     *
     * @param latex LaTeX math expression
     * @return Natural language speech description
     */
    public fun latexToSpeech(latex: String): String {
        return FormulaSpeech.fromLatex(latex)
    }

    /**
     * Convert a [FormulaContent] to speech text.
     *
     * @param formula Formula content from the vision pipeline
     * @return Complete speech text with type intro
     */
    public fun formulaToSpeech(formula: FormulaContent): String {
        return FormulaSpeech.fromContent(formula)
    }

    /**
     * Describe a content item in natural language.
     *
     * @param item Content item to describe
     * @return Natural language description
     */
    public fun describe(item: ContentItem): String {
        return when (item) {
            is GraphContent -> describer.describeGraph(item)
            is FormulaContent -> FormulaSpeech.fromContent(item)
            is MoleculeContent -> describer.describeMolecule(item)
            is ShapeContent -> describer.describeShape(item)
            is TableContent -> describer.describeTable(item)
            else -> "${item.contentType.name.lowercase().replaceFirstChar { it.uppercase() }} detected."
        }
    }
}
