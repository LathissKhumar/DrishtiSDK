package io.drishti.graph

import io.drishti.core.*

/**
 * Complete graph plugin combining data parsing, detection, and rendering.
 *
 * The [GraphPlugin] is the main facade for the graph module. It provides
 * both the traditional [DetectorPlugin] interface and data-first entry
 * points for JSON, CSV, and programmatic data input.
 *
 * Data-first usage (recommended):
 * ```kotlin
 * val plugin = GraphPlugin()
 *
 * // From JSON
 * val graph = plugin.fromJson(jsonString)
 *
 * // From CSV
 * val graph = plugin.fromCsv(csvString, title = "Sales")
 *
 * // From programmatic data
 * val graph = plugin.fromDataPoints(
 *     type = "bar_chart",
 *     title = "Revenue",
 *     points = listOf(1f to 100f, 2f to 200f)
 * )
 *
 * // Render
 * val haptic = plugin.renderHaptic(listOf(graph))
 * val vegaSpec = plugin.renderVegaLiteSpec(graph)
 * ```
 *
 * Legacy usage (DetectorPlugin interface):
 * ```kotlin
 * val graph = plugin.detectFromJson(jsonString) // recommended
 * val item = plugin.detect(frame) // returns null (no vision dependency)
 * ```
 */
class GraphPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
    private val detector = GraphDetector()
    private val renderer = GraphRenderer()

    override val name = "graph"
    override val contentType = ContentType.GRAPH
    override val confidence = detector.confidence

    // ── DetectorPlugin interface ─────────────────────────────────────

    /**
     * Detect content from a camera frame.
     *
     * This returns null because the graph module no longer depends on
     * vision processing. Use the data-first methods instead.
     *
     * @param frame The input image frame (unused)
     * @return Always null; use data-first methods for graph detection
     */
    override suspend fun detect(frame: Frame): ContentItem? {
        return detector.detect(frame)
    }

    // ── Data-first entry points ──────────────────────────────────────

    /**
     * Parse graph content from a JSON string.
     *
     * @param json JSON string conforming to the graph data schema
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun fromJson(json: String): GraphContent? {
        return detector.detectFromJson(json)
    }

    /**
     * Parse graph content from a CSV string.
     *
     * @param csv CSV string with header row
     * @param chartType Optional chart type override
     * @param title Optional chart title
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun fromCsv(csv: String, chartType: String? = null, title: String = ""): GraphContent? {
        return detector.detectFromCsv(csv, chartType, title)
    }

    /**
     * Parse graph content from programmatic data points.
     *
     * @param type Chart type string
     * @param title Chart title
     * @param xLabel X-axis label
     * @param yLabel Y-axis label
     * @param points List of (x, y) number pairs
     * @return Parsed [GraphContent]
     */
    fun fromDataPoints(
        type: String = "line_chart",
        title: String = "",
        xLabel: String = "",
        yLabel: String = "",
        points: List<Pair<Number, Number>>
    ): GraphContent {
        return detector.detectFromDataPoints(type, title, xLabel, yLabel, points)
    }

    /**
     * Try to auto-detect the input format and parse accordingly.
     *
     * @param input Raw input string (JSON or CSV)
     * @param title Optional title override
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun autoDetect(input: String, title: String = ""): GraphContent? {
        return detector.detectAuto(input, title)
    }

    // ── Vega-Lite rendering ──────────────────────────────────────────

    /**
     * Generate a Vega-Lite specification string for a graph.
     *
     * @param graph The graph content
     * @return Vega-Lite v5 JSON specification string
     */
    fun renderVegaLiteSpec(graph: GraphContent): String {
        return renderer.renderVegaLiteSpec(graph)
    }

    /**
     * Get the accessibility description for a graph.
     *
     * @param graph The graph content
     * @return Human-readable accessibility description
     */
    fun getAccessibilityDescription(graph: GraphContent): String {
        return renderer.getAccessibilityDescription(graph)
    }

    /**
     * Get statistical summary of graph data.
     *
     * @param graph The graph content
     * @return [DataSummary] with computed statistics
     */
    fun getDataSummary(graph: GraphContent): DataSummary {
        return renderer.getDataSummary(graph)
    }

    // ── HapticsRenderer interface ────────────────────────────────────

    /**
     * Render detected graph items as haptic output.
     */
    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        val graphItems = items.filterIsInstance<GraphContent>()
        if (graphItems.isEmpty()) {
            return HapticOutput(pulses = emptyList(), pattern = "empty")
        }
        val pulses = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            if (item !is GraphContent) return@mapIndexedNotNull null
            renderer.renderHaptic(item).pulses
        }.flatten()
        val pattern = if (items.size > 1 && focusIndex in items.indices) {
            "graph_haptic_focus_$focusIndex"
        } else {
            "graph_haptic"
        }
        return HapticOutput(pulses = pulses, pattern = pattern)
    }

    // ── AudioRenderer interface ──────────────────────────────────────

    /**
     * Render detected graph items as audio output.
     */
    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        val graphItems = items.filterIsInstance<GraphContent>()
        if (graphItems.isEmpty()) {
            return AudioOutput(sources = emptyList(), spatial = true)
        }
        val sources = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            if (item !is GraphContent) return@mapIndexedNotNull null
            renderer.renderAudio(item).sources
        }.flatten()
        return AudioOutput(sources = sources, spatial = true)
    }

    // ── VoiceOutputRenderer interface ────────────────────────────────

    /**
     * Render detected graph items as voice output.
     */
    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        val graphItems = items.filterIsInstance<GraphContent>()
        if (graphItems.isEmpty()) {
            return VoiceOutput(
                speech = SpeechSegment(text = "No graph content", rate = 1.0f, pitch = 1.0f),
                language = "en-US"
            )
        }
        val speeches = items.mapIndexedNotNull { index, item ->
            if (focusIndex in items.indices && index != focusIndex) return@mapIndexedNotNull null
            if (item !is GraphContent) return@mapIndexedNotNull null
            val speech = renderer.renderVoice(item).speech
            if (focusIndex in items.indices && index == focusIndex) {
                SpeechSegment(
                    text = "Graph ${index + 1} of ${items.size}. ${speech.text}",
                    rate = speech.rate,
                    pitch = speech.pitch
                )
            } else {
                speech
            }
        }
        val combinedText = speeches.joinToString(" ") { it.text }
        return VoiceOutput(
            speech = SpeechSegment(text = combinedText, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    // ── Exploration interfaces ───────────────────────────────────────

    /**
     * Render exploration sequence for haptic.
     */
    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        val base = when (item) {
            is GraphContent -> {
                val idx = if (elementIndex >= 0) elementIndex else {
                    when (direction) {
                        ExplorationDirection.NEXT -> item.dataPoints.size - 1
                        ExplorationDirection.PREVIOUS -> 0
                        ExplorationDirection.POSITION -> 0
                    }
                }
                val point = item.dataPoints.getOrNull(idx)
                if (point != null) {
                    renderer.renderHaptic(item).let { baseOutput ->
                        val singlePulse = baseOutput.pulses.getOrNull(idx)
                        HapticOutput(
                            pulses = if (singlePulse != null) listOf(singlePulse.copy(intensity = (singlePulse.intensity * 1.2f).coerceAtMost(1f))) else emptyList(),
                            pattern = "graph_explore_point_$idx"
                        )
                    }
                } else {
                    renderer.renderHaptic(item)
                }
            }
            else -> return HapticOutput(pulses = emptyList(), pattern = "exploration")
        }
        return base
    }

    /**
     * Render exploration sequence for audio.
     */
    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        val base = when (item) {
            is GraphContent -> {
                val idx = if (elementIndex >= 0) elementIndex else {
                    when (direction) {
                        ExplorationDirection.NEXT -> item.dataPoints.size - 1
                        ExplorationDirection.PREVIOUS -> 0
                        ExplorationDirection.POSITION -> 0
                    }
                }
                val point = item.dataPoints.getOrNull(idx)
                if (point != null) {
                    renderer.renderAudio(item).let { baseOutput ->
                        val singleSource = baseOutput.sources.getOrNull(idx)
                        AudioOutput(
                            sources = if (singleSource != null) listOf(singleSource) else emptyList(),
                            spatial = true
                        )
                    }
                } else {
                    renderer.renderAudio(item)
                }
            }
            else -> return AudioOutput(sources = emptyList(), spatial = true)
        }
        return base
    }

    /**
     * Render exploration sequence for voice.
     */
    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput = when (item) {
        is GraphContent -> {
            val idx = if (elementIndex >= 0) elementIndex else {
                when (direction) {
                    ExplorationDirection.NEXT -> item.dataPoints.size - 1
                    ExplorationDirection.PREVIOUS -> 0
                    ExplorationDirection.POSITION -> 0
                }
            }
            val point = item.dataPoints.getOrNull(idx)
            val text = if (point != null) {
                "Point ${idx + 1}: x=${"%.1f".format(point.x)}, y=${"%.1f".format(point.y)}${point.label?.let { ", label=$it" } ?: ""}"
            } else {
                when (direction) {
                    ExplorationDirection.NEXT -> "No more data points"
                    ExplorationDirection.PREVIOUS -> "No previous data points"
                    ExplorationDirection.POSITION -> renderer.renderVoice(item).speech.text
                }
            }
            VoiceOutput(speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f), language = "en-US")
        }
        else -> VoiceOutput(
            speech = SpeechSegment(text = "Exploration", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
