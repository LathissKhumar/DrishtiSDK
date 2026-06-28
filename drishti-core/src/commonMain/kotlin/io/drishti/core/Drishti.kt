package io.drishti.core

/**
 * Main entry point for the Drishti SDK.
 *
 * Usage:
 * ```
 * val drishti = Drishti.Builder()
 *     .addDetector(GraphPlugin())
 *     .addRenderer(HapticsPlugin())
 *     .build()
 *
 * val diagram = drishti.readAsync(frame)
 * diagram.haptics()
 * ```
 */
class Drishti private constructor(
    private val detectors: List<DetectorPlugin>,
    private val renderers: List<RendererPlugin>,
    private val pipeline: Pipeline
) {
    /**
     * Read visual content from a frame (suspend).
     */
    suspend fun readAsync(frame: Frame): DrishtiDiagram {
        val contentItems = pipeline.detect(frame, detectors)
        val sceneGraph = pipeline.buildSceneGraph(contentItems)
        return DrishtiDiagram(contentItems, sceneGraph, renderers, pipeline)
    }

    class Builder {
        private val detectors = mutableListOf<DetectorPlugin>()
        private val renderers = mutableListOf<RendererPlugin>()

        fun addDetector(plugin: DetectorPlugin) = apply { detectors.add(plugin) }
        fun addRenderer(plugin: RendererPlugin) = apply { renderers.add(plugin) }

        fun build(): Drishti {
            require(detectors.isNotEmpty()) { "At least one DetectorPlugin is required" }
            val pipeline = Pipeline()
            return Drishti(detectors.toList(), renderers.toList(), pipeline)
        }
    }
}
