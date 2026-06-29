package io.drishti.voice

import io.drishti.core.*

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
class VoicePlugin(
    private val voiceData: VoiceData = VoiceData.default()
) : VoiceOutputRenderer {

    private val renderer = VoiceRenderer(voiceData)
    private val speechGenerator = SpeechGenerator()
    private val describer = ContentDescriber()

    override val name = "voice"

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
    fun generateSpeech(text: String): SpeechSegment {
        return speechGenerator.generate(text)
    }

    /**
     * Convert a LaTeX string to MathCAT-style speech text.
     *
     * @param latex LaTeX math expression
     * @return Natural language speech description
     */
    fun latexToSpeech(latex: String): String {
        return FormulaSpeech.fromLatex(latex)
    }

    /**
     * Convert a [FormulaContent] to speech text.
     *
     * @param formula Formula content from the vision pipeline
     * @return Complete speech text with type intro
     */
    fun formulaToSpeech(formula: FormulaContent): String {
        return FormulaSpeech.fromContent(formula)
    }

    /**
     * Describe a content item in natural language.
     *
     * @param item Content item to describe
     * @return Natural language description
     */
    fun describe(item: ContentItem): String {
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
