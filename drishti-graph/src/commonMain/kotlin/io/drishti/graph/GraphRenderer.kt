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

package io.drishti.graph

import io.drishti.core.*

/**
 * Renders graph content as haptic, audio, and voice outputs.
 *
 * The renderer uses real data values from [GraphContent] to generate
 * meaningful multi-modal output. It also provides Vega-Lite specification
 * generation for visual rendering.
 *
 * Key improvements over previous implementation:
 * - Uses actual axis labels and ranges from data
 * - Computes statistical summaries for voice descriptions
 * - Generates complete Vega-Lite specifications
 * - Provides accessibility descriptions following DIAGRAM Center guidelines
 *
 * Usage:
 * ```kotlin
 * val renderer = GraphRenderer()
 * val haptic = renderer.renderHaptic(graph)
 * val audio = renderer.renderAudio(graph)
 * val voice = renderer.renderVoice(graph)
 * val vegaSpec = renderer.renderVegaLiteSpec(graph)
 * ```
 */
public class GraphRenderer {

    private val vegaLiteSpec = VegaLiteSpec()

    /**
     * Render graph as haptic output.
     *
     * Maps data points to haptic pulses with intensity proportional to
     * y-values and duration varying by chart type.
     *
     * @param graph The graph content to render
     * @return [HapticOutput] with pulses mapped from data points
     */
    public fun renderHaptic(graph: GraphContent): HapticOutput {
        val pulses = when (graph.graphType) {
            GraphType.LINE_CHART, GraphType.AREA_CHART -> renderLineChartHaptic(graph)
            GraphType.BAR_CHART, GraphType.HISTOGRAM -> renderBarChartHaptic(graph)
            GraphType.PIE_CHART -> renderPieChartHaptic(graph)
            GraphType.SCATTER_PLOT -> renderScatterPlotHaptic(graph)
        }
        return HapticOutput(pulses = pulses, pattern = "graph_exploration")
    }

    /**
     * Render graph as audio output.
     *
     * Maps data points to spatial audio sources with frequency proportional
     * to y-values and spatial positioning from normalized coordinates.
     *
     * @param graph The graph content to render
     * @return [AudioOutput] with sources mapped from data points
     */
    public fun renderAudio(graph: GraphContent): AudioOutput {
        val sources = when (graph.graphType) {
            GraphType.LINE_CHART, GraphType.AREA_CHART -> renderLineChartAudio(graph)
            GraphType.BAR_CHART, GraphType.HISTOGRAM -> renderBarChartAudio(graph)
            GraphType.PIE_CHART -> renderPieChartAudio(graph)
            GraphType.SCATTER_PLOT -> renderScatterPlotAudio(graph)
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    /**
     * Render graph as voice output.
     *
     * Generates a spoken description including chart type, title,
     * data summary (min, max, average), and trend direction.
     *
     * @param graph The graph content to render
     * @return [VoiceOutput] with descriptive speech segment
     */
    public fun renderVoice(graph: GraphContent): VoiceOutput {
        val speech = when (graph.graphType) {
            GraphType.LINE_CHART, GraphType.AREA_CHART -> renderLineChartVoice(graph)
            GraphType.BAR_CHART, GraphType.HISTOGRAM -> renderBarChartVoice(graph)
            GraphType.PIE_CHART -> renderPieChartVoice(graph)
            GraphType.SCATTER_PLOT -> renderScatterPlotVoice(graph)
        }
        return VoiceOutput(speech = speech, language = "en-US")
    }

    /**
     * Generate a Vega-Lite specification for the graph.
     *
     * The returned JSON conforms to the Vega-Lite v5 schema and can be
     * rendered by any compatible viewer (web, Android WebView, etc.).
     *
     * @param graph The graph content
     * @return Vega-Lite specification as a JSON string
     */
    public fun renderVegaLiteSpec(graph: GraphContent): String {
        return vegaLiteSpec.generateString(graph)
    }

    /**
     * Generate a Vega-Lite specification as a [kotlinx.serialization.json.JsonObject].
     *
     * @param graph The graph content
     * @return Vega-Lite specification object
     */
    public fun renderVegaLiteSpecObject(graph: GraphContent): kotlinx.serialization.json.JsonObject {
        return vegaLiteSpec.generate(graph)
    }

    /**
     * Get the accessibility description for a graph.
     *
     * Follows DIAGRAM Center guidelines for accessible image descriptions.
     *
     * @param graph The graph content
     * @return Human-readable accessibility description
     */
    public fun getAccessibilityDescription(graph: GraphContent): String {
        return generateAccessibilityDescription(graph)
    }

    /**
     * Get statistical summary of graph data.
     *
     * @param graph The graph content
     * @return [DataSummary] with computed statistics
     */
    public fun getDataSummary(graph: GraphContent): DataSummary {
        return computeDataSummary(graph.dataPoints)
    }

    private fun renderLineChartHaptic(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.map { point ->
            HapticPulse(
                intensity = normalizeValue(point.y, graph.axes.y.range),
                duration = 50L,
                x = normalizeValue(point.x, graph.axes.x.range),
                y = normalizeValue(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderBarChartHaptic(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.map { point ->
            HapticPulse(
                intensity = normalizeValue(point.y, graph.axes.y.range),
                duration = 100L,
                x = normalizeValue(point.x, graph.axes.x.range),
                y = normalizeValue(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderPieChartHaptic(graph: GraphContent): List<HapticPulse> {
        val slices = if (graph.labels.isNotEmpty()) graph.labels.size else graph.dataPoints.size
        if (slices == 0) return emptyList()

        val total = graph.dataPoints.sumOf { it.y.toDouble() }.toFloat().coerceAtLeast(1f)

        return (0 until slices).map { index ->
            val angle = (index.toFloat() / slices) * 360f
            val sliceValue = if (index < graph.dataPoints.size) graph.dataPoints[index].y else 0f
            val proportion = (sliceValue / total).coerceIn(0.1f, 1.0f)
            HapticPulse(
                intensity = proportion,
                duration = (50L + proportion * 150L).toLong(),
                x = (kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() + 1f) / 2f,
                y = (kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() + 1f) / 2f
            )
        }
    }

    private fun renderScatterPlotHaptic(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.map { point ->
            HapticPulse(
                intensity = 0.7f,
                duration = 30L,
                x = normalizeValue(point.x, graph.axes.x.range),
                y = normalizeValue(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderLineChartAudio(graph: GraphContent): List<AudioSource> {
        return graph.dataPoints.map { point ->
            AudioSource(
                frequency = mapToFrequency(point.y, graph.axes.y.range),
                amplitude = normalizeValue(point.y, graph.axes.y.range),
                spatialX = normalizeValue(point.x, graph.axes.x.range),
                spatialY = normalizeValue(point.y, graph.axes.y.range),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderBarChartAudio(graph: GraphContent): List<AudioSource> {
        return graph.dataPoints.map { point ->
            AudioSource(
                frequency = mapToFrequency(point.y, graph.axes.y.range),
                amplitude = normalizeValue(point.y, graph.axes.y.range),
                spatialX = normalizeValue(point.x, graph.axes.x.range),
                spatialY = normalizeValue(point.y, graph.axes.y.range),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderPieChartAudio(graph: GraphContent): List<AudioSource> {
        val slices = if (graph.labels.isNotEmpty()) graph.labels.size else graph.dataPoints.size
        if (slices == 0) return emptyList()

        val total = graph.dataPoints.sumOf { it.y.toDouble() }.toFloat().coerceAtLeast(1f)

        return (0 until slices).map { index ->
            val angle = (index.toFloat() / slices) * 360f
            val cosVal = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val sinVal = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            val sliceValue = if (index < graph.dataPoints.size) graph.dataPoints[index].y else 0f
            val proportion = (sliceValue / total).coerceIn(0.1f, 1.0f)
            AudioSource(
                frequency = 130f + (proportion * 393f), // 130Hz–523Hz proportional to slice value
                amplitude = proportion,
                spatialX = ((cosVal + 1f) / 2f).coerceIn(0.05f, 0.95f),
                spatialY = ((sinVal + 1f) / 2f).coerceIn(0.05f, 0.95f),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderScatterPlotAudio(graph: GraphContent): List<AudioSource> {
        return graph.dataPoints.map { point ->
            AudioSource(
                frequency = mapToFrequency(point.y, graph.axes.y.range),
                amplitude = 0.5f,
                spatialX = normalizeValue(point.x, graph.axes.x.range),
                spatialY = normalizeValue(point.y, graph.axes.y.range),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderLineChartVoice(graph: GraphContent): SpeechSegment {
        val summary = computeDataSummary(graph.dataPoints)
        val description = generateVoiceDescription(graph, summary)
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    private fun renderBarChartVoice(graph: GraphContent): SpeechSegment {
        val summary = computeDataSummary(graph.dataPoints)
        val description = generateVoiceDescription(graph, summary)
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    private fun renderPieChartVoice(graph: GraphContent): SpeechSegment {
        val description = buildString {
            append("${graphTypeLabel(graph.graphType)}")
            if (graph.title.isNotEmpty()) {
                append(" titled '${graph.title}'")
            }

            val sliceCount = if (graph.labels.isNotEmpty()) graph.labels.size else graph.dataPoints.size
            append(" with $sliceCount slices")

            if (graph.labels.isNotEmpty()) {
                append(": ${graph.labels.joinToString(", ")}")
            }

            append(".")
        }
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    private fun renderScatterPlotVoice(graph: GraphContent): SpeechSegment {
        val summary = computeDataSummary(graph.dataPoints)
        val description = generateVoiceDescription(graph, summary)
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    private fun generateVoiceDescription(graph: GraphContent, summary: DataSummary): String {
        return buildString {
            append("${graphTypeLabel(graph.graphType)}")
            if (graph.title.isNotEmpty()) {
                append(" titled '${graph.title}'")
            }
            append(". ${summary.count} data points")

            if (graph.axes.x.label.isNotEmpty() || graph.axes.y.label.isNotEmpty()) {
                append(". ")
                if (graph.axes.x.label.isNotEmpty()) {
                    append("X: ${graph.axes.x.label}")
                }
                if (graph.axes.x.label.isNotEmpty() && graph.axes.y.label.isNotEmpty()) {
                    append(", ")
                }
                if (graph.axes.y.label.isNotEmpty()) {
                    append("Y: ${graph.axes.y.label}")
                }
            }

            if (summary.count > 0) {
                append(". Values from ${formatNumber(summary.min)} to ${formatNumber(summary.max)}")
                append(", average ${formatNumber(summary.mean)}")
            }

            if (summary.count >= 2) {
                append(". Trend: ${summary.trend.name.lowercase()}")
            }

            append(".")
        }
    }

    private fun normalizeValue(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        if (span == 0f) return 0.5f
        return ((value - range.start) / span).coerceIn(0f, 1f)
    }

    private fun mapToFrequency(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val normalized = normalizeValue(value, range)
        return 130f + (normalized * 393f) // 130Hz to 523Hz (musical range)
    }
}
