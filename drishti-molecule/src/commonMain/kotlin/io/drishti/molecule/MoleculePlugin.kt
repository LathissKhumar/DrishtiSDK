package io.drishti.molecule

import io.drishti.core.*

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
class MoleculePlugin(
    private val detector: MoleculeDetector = MoleculeDetector(),
    private val renderer: MoleculeRenderer = MoleculeRenderer()
) : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    override val name = "molecule"
    override val contentType = ContentType.MOLECULE
    override val confidence = detector.confidence

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
    suspend fun detectMolecule(input: String): MoleculeContent? = detector.detectFromText(input)

    /**
     * Detect molecule from text, returning rich [MoleculeData].
     *
     * Same as [detectMolecule] but includes full PubChem properties
     * (molecular weight, SMILES, IUPAC name, etc.) for enhanced rendering.
     *
     * @param input Text description of the molecule
     * @return [MoleculeData] with full PubChem data, or `null`
     */
    suspend fun detectMoleculeData(input: String): MoleculeData? = detector.detectMoleculeData(input)

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
    fun renderHaptic(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): HapticOutput {
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
    fun renderAudio(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): AudioOutput {
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
    fun renderVoice(items: List<ContentItem>, focusIndex: Int, moleculeData: MoleculeData?): VoiceOutput {
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
        val combinedText = speeches.joinToString(" ") { it.text }
        return VoiceOutput(speech = SpeechSegment(text = combinedText, rate = 1.0f, pitch = 1.0f), language = "en-US")
    }

    // -- Exploration renderers --

    override fun renderExplorationHaptic(item: ContentItem, direction: ExplorationDirection): HapticOutput {
        val base = when (item) {
            is MoleculeContent -> renderer.renderHaptic(item)
            else -> return HapticOutput(pulses = emptyList(), pattern = "exploration")
        }
        val pulses = when (direction) {
            ExplorationDirection.NEXT -> base.pulses.takeLast(1).map { it.copy(intensity = (it.intensity * 1.2f).coerceAtMost(1f)) }
            ExplorationDirection.PREVIOUS -> base.pulses.take(1).map { it.copy(intensity = (it.intensity * 1.2f).coerceAtMost(1f)) }
            ExplorationDirection.POSITION -> base.pulses
        }
        return HapticOutput(pulses = pulses, pattern = "molecule_explore_${direction.name.lowercase()}")
    }

    override fun renderExplorationAudio(item: ContentItem, direction: ExplorationDirection): AudioOutput {
        val base = when (item) {
            is MoleculeContent -> renderer.renderAudio(item)
            else -> return AudioOutput(sources = emptyList(), spatial = true)
        }
        val sources = when (direction) {
            ExplorationDirection.NEXT -> base.sources.takeLast(1)
            ExplorationDirection.PREVIOUS -> base.sources.take(1)
            ExplorationDirection.POSITION -> base.sources
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    override fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection): VoiceOutput = when (item) {
        is MoleculeContent -> {
            val text = when (direction) {
                ExplorationDirection.NEXT -> "Next atom: ${item.atoms.lastOrNull()?.element ?: "none"}"
                ExplorationDirection.PREVIOUS -> "Previous atom: ${item.atoms.firstOrNull()?.element ?: "none"}"
                ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 0.95f, pitch = 1.0f), language = "en-US")
        }
        else -> VoiceOutput(
            speech = SpeechSegment(text = "Exploration", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
