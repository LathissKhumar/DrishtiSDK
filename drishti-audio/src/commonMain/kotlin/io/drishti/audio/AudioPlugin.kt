package io.drishti.audio

import io.drishti.core.*

/**
 * Audio renderer plugin facade.
 *
 * Produces spatial audio descriptions from [ContentItem] lists and [SceneGraph]s.
 * Delegates spatial rendering to [SpatialRenderer] and tone generation to [ToneGenerator].
 *
 * The Android HAL layer consumes [SpatialAudioScene] to configure the
 * Spatializer API (API 32+) and Oboe for low-latency playback.
 */
class AudioPlugin : AudioRenderer {
    private val renderer = SpatialRenderer()
    private val toneGenerator = ToneGenerator()

    override val name = "audio"

    // ── AudioRenderer interface ─────────────────────────────────────

    /**
     * Render content items as spatial audio.
     */
    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        return renderer.render(items, focusIndex)
    }

    /**
     * Render exploration sequence.
     */
    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        return renderer.renderExploration(item, direction, elementIndex)
    }

    // ── SceneGraph-based API ────────────────────────────────────────

    /**
     * Render a [SceneGraph] into a full [SpatialAudioScene].
     *
     * This is the primary entry point for the new spatial audio pipeline.
     * The returned scene contains positioned audio sources and speech
     * descriptions for the Android Spatializer API.
     *
     * @param sceneGraph The scene graph from the vision pipeline.
     * @param focusNodeId Optional node id to highlight.
     */
    fun renderSpatialScene(sceneGraph: SceneGraph, focusNodeId: String? = null): SpatialAudioScene {
        return renderer.renderScene(sceneGraph, focusNodeId)
    }

    /**
     * Render content items into a [SpatialAudioScene].
     *
     * Builds a [SceneGraph] from the content items internally, then
     * renders it through the spatial pipeline.
     *
     * @param items Content items from the vision pipeline.
     * @param focusIndex Index of the focused item.
     */
    fun renderSpatialFromItems(items: List<ContentItem>, focusIndex: Int = 0): SpatialAudioScene {
        val sceneGraph = renderer.buildSceneGraphFromItems(items, focusIndex)
        val focusNodeId = sceneGraph.nodes.getOrNull(focusIndex)?.id
        return renderer.renderScene(sceneGraph, focusNodeId)
    }

    /**
     * Generate speech descriptions for content items.
     *
     * Each item produces a human-readable description suitable for TTS output.
     *
     * @param items Content items to describe.
     * @return List of speech texts in rendering order.
     */
    fun describeContent(items: List<ContentItem>): List<String> {
        return items.map { item -> describeItem(item) }
    }

    // ── Tone generation ─────────────────────────────────────────────

    /**
     * Generate sine wave tone.
     */
    fun generateTone(frequency: Float, duration: Long): FloatArray {
        return toneGenerator.generateSineWave(frequency, duration)
    }

    /**
     * Apply envelope to audio samples.
     */
    fun applyEnvelope(samples: FloatArray): FloatArray {
        return toneGenerator.applyEnvelope(samples)
    }

    // ── Private helpers ─────────────────────────────────────────────

    private fun describeItem(item: ContentItem): String = when (item) {
        is GraphContent -> {
            val typeName = item.graphType.name.lowercase().replaceFirstChar { it.uppercase() }
            val pointCount = item.dataPoints.size
            val title = item.title.ifEmpty { "Untitled" }
            "$typeName graph '$title' with $pointCount data points. " +
                "X axis: ${item.axes.x.label}, Y axis: ${item.axes.y.label}."
        }
        is FormulaContent -> {
            val typeName = item.formulaType.name.lowercase().replaceFirstChar { it.uppercase() }
            "$typeName formula: ${item.expression}."
        }
        is MoleculeContent -> {
            val name = item.name.ifEmpty { "Unknown molecule" }
            val atomCount = item.atoms.size
            val bondCount = item.bonds.size
            "$name with $atomCount atoms and $bondCount bonds."
        }
        is ShapeContent -> {
            "${item.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }} shape."
        }
        is TableContent -> {
            "Table with ${item.rows} rows and ${item.columns} columns."
        }
        else -> {
            "${item.contentType.name.lowercase().replaceFirstChar { it.uppercase() }} content."
        }
    }
}
