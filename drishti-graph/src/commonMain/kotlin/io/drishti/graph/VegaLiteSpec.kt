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
import kotlinx.serialization.json.*

/**
 * Generates Vega-Lite specification JSON for graph content.
 *
 * Vega-Lite is a high-level grammar of interactive graphics based on
 * Vega. This class converts [GraphContent] into a complete Vega-Lite
 * specification that can be rendered by any Vega-Lite compatible viewer.
 *
 * Supported chart types and their Vega-Lite marks:
 * - [GraphType.LINE_CHART] → `"mark": "line"`
 * - [GraphType.BAR_CHART] → `"mark": "bar"`
 * - [GraphType.PIE_CHART] → `"mark": "arc"`
 * - [GraphType.SCATTER_PLOT] → `"mark": "point"`
 * - [GraphType.AREA_CHART] → `"mark": "area"`
 * - [GraphType.HISTOGRAM] → `"mark": "bar"` (with binning)
 *
 * Usage:
 * ```kotlin
 * val specGenerator = VegaLiteSpec()
 * val spec = specGenerator.generate(graphContent)
 * // spec is a JsonObject conforming to Vega-Lite v5 schema
 * val specString = specGenerator.generateString(graphContent)
 * ```
 *
 * @see <a href="https://vega.github.io/vega-lite/">Vega-Lite Documentation</a>
 */
public class VegaLiteSpec {

    /**
     * Generate a Vega-Lite specification as a [JsonObject].
     *
     * @param graph The [GraphContent] to generate a spec for
     * @param width Optional width in pixels (default 600)
     * @param height Optional height in pixels (default 400)
     * @return Complete Vega-Lite v5 JSON specification
     */
    public fun generate(graph: GraphContent, width: Int = 600, height: Int = 400): JsonObject {
        val mark = buildMark(graph)
        val encoding = buildEncoding(graph)
        val data = buildData(graph)
        val title = buildTitle(graph)

        return buildJsonObject {
            put("\$schema", "https://vega.github.io/schema/vega-lite/v5.json")
            if (title != null) put("title", title)
            put("mark", mark)
            put("encoding", encoding)
            put("data", data)
            put("width", width)
            put("height", height)
        }
    }

    /**
     * Generate a Vega-Lite specification as a JSON string.
     *
     * @param graph The [GraphContent] to generate a spec for
     * @param width Optional width in pixels (default 600)
     * @param height Optional height in pixels (default 400)
     * @return Pretty-printed Vega-Lite v5 JSON string
     */
    public fun generateString(graph: GraphContent, width: Int = 600, height: Int = 400): String {
        return generate(graph, width, height).toString()
    }

    /**
     * Get the Vega-Lite mark type for a given [GraphType].
     *
     * @param graphType The type of graph
     * @return The Vega-Lite mark type string
     */
    public fun markTypeForGraph(graphType: GraphType): String {
        return when (graphType) {
            GraphType.LINE_CHART -> "line"
            GraphType.BAR_CHART -> "bar"
            GraphType.PIE_CHART -> "arc"
            GraphType.SCATTER_PLOT -> "point"
            GraphType.AREA_CHART -> "area"
            GraphType.HISTOGRAM -> "bar"
        }
    }

    private fun buildMark(graph: GraphContent): JsonElement {
        return when (graph.graphType) {
            GraphType.LINE_CHART -> buildJsonObject {
                put("type", "line")
                put("point", true)
                put("interpolate", "monotone")
            }
            GraphType.BAR_CHART -> buildJsonObject {
                put("type", "bar")
                put("cornerRadiusTopLeft", 4)
                put("cornerRadiusTopRight", 4)
            }
            GraphType.PIE_CHART -> buildJsonObject {
                put("type", "arc")
                put("innerRadius", 0)
                put("outerRadius", 120)
            }
            GraphType.SCATTER_PLOT -> buildJsonObject {
                put("type", "point")
                put("filled", true)
                put("size", 100)
            }
            GraphType.AREA_CHART -> buildJsonObject {
                put("type", "area")
                put("interpolate", "monotone")
                put("opacity", 0.5)
            }
            GraphType.HISTOGRAM -> buildJsonObject {
                put("type", "bar")
            }
        }
    }

    private fun buildEncoding(graph: GraphContent): JsonObject {
        return when (graph.graphType) {
            GraphType.PIE_CHART -> buildPieEncoding(graph)
            else -> buildCartesianEncoding(graph)
        }
    }

    private fun buildCartesianEncoding(graph: GraphContent): JsonObject {
        val xLabel = graph.axes.x.label.ifEmpty { "X" }
        val yLabel = graph.axes.y.label.ifEmpty { "Y" }
        val xIsNumeric = isNumericAxis(graph)

        return buildJsonObject {
            put("x", buildJsonObject {
                put("field", "x")
                put("type", if (xIsNumeric) "quantitative" else "nominal")
                put("title", xLabel)
                if (graph.graphType == GraphType.HISTOGRAM) {
                    put("bin", true)
                }
                if (xIsNumeric) {
                    put("scale", buildJsonObject {
                        put("domain", buildJsonArray {
                            add(graph.axes.x.range.start.toJson())
                            add(graph.axes.x.range.endInclusive.toJson())
                        })
                    })
                }
            })
            put("y", buildJsonObject {
                if (graph.graphType == GraphType.HISTOGRAM) {
                    put("aggregate", "count")
                } else {
                    put("field", "y")
                }
                put("type", "quantitative")
                put("title", yLabel)
                if (graph.graphType != GraphType.HISTOGRAM) {
                    put("scale", buildJsonObject {
                        put("domain", buildJsonArray {
                            add(graph.axes.y.range.start.toJson())
                            add(graph.axes.y.range.endInclusive.toJson())
                        })
                    })
                }
            })
        }
    }

    private fun buildPieEncoding(graph: GraphContent): JsonObject {
        return buildJsonObject {
            put("theta", buildJsonObject {
                put("field", "y")
                put("type", "quantitative")
                put("stack", true)
            })
            put("color", buildJsonObject {
                put("field", "x")
                put("type", "nominal")
                put("title", graph.axes.x.label.ifEmpty { "Category" })
                put("legend", buildJsonObject {
                    put("orient", "right")
                })
            })
        }
    }

    private fun buildData(graph: GraphContent): JsonObject {
        val values = buildJsonArray {
            if (graph.graphType == GraphType.PIE_CHART && graph.labels.isNotEmpty()) {
                graph.labels.forEachIndexed { index, label ->
                    val yValue = if (index < graph.dataPoints.size) {
                        graph.dataPoints[index].y
                    } else {
                        1f
                    }
                    add(buildJsonObject {
                        put("x", label)
                        put("y", yValue.toJson())
                    })
                }
            } else {
                graph.dataPoints.forEach { point ->
                    add(buildJsonObject {
                        put("x", point.x.toJson())
                        put("y", point.y.toJson())
                        if (point.label != null) {
                            put("label", point.label)
                        }
                    })
                }
            }
        }

        return buildJsonObject {
            put("values", values)
        }
    }

    private fun buildTitle(graph: GraphContent): String? {
        return graph.title.ifEmpty { null }
    }

    /**
     * Determine whether the x-axis is numeric by inspecting actual data points.
     *
     * A numeric axis is one where all x values are parsed from numbers (no string-derived
     * labels). This replaces the previous range-based heuristic which misclassified real
     * 0..100% data as categorical.
     */
    private fun isNumericAxis(graph: GraphContent): Boolean {
        if (graph.dataPoints.isEmpty()) return false
        // If all data points have null labels, x values were originally numeric
        return graph.dataPoints.all { it.label == null }
    }

    private fun Float.toJson(): JsonPrimitive = JsonPrimitive(this)
}

/**
 * Extension data: statistical summary of graph data.
 *
 * Contains computed statistics useful for voice descriptions
 * and accessibility information.
 */
public data class DataSummary(
    val min: Float,
    val max: Float,
    val mean: Float,
    val median: Float,
    val count: Int,
    val trend: TrendDirection,
    val trendStrength: Float
)

/**
 * Direction of the data trend.
 */
public enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}

/**
 * Compute statistical summary for graph data points.
 *
 * @param dataPoints The data points to summarize
 * @return [DataSummary] with computed statistics
 */
public fun computeDataSummary(dataPoints: List<DataPoint>): DataSummary {
    if (dataPoints.isEmpty()) {
        return DataSummary(
            min = 0f, max = 0f, mean = 0f, median = 0f,
            count = 0, trend = TrendDirection.STABLE, trendStrength = 0f
        )
    }

    val yValues = dataPoints.map { it.y }.sorted()
    val min = yValues.first()
    val max = yValues.last()
    val mean = yValues.average().toFloat()
    val median = if (yValues.size % 2 == 0) {
        (yValues[yValues.size / 2 - 1] + yValues[yValues.size / 2]) / 2f
    } else {
        yValues[yValues.size / 2]
    }

    val trend = computeTrend(dataPoints)

    return DataSummary(
        min = min,
        max = max,
        mean = mean,
        median = median,
        count = dataPoints.size,
        trend = trend.direction,
        trendStrength = trend.strength
    )
}

private data class TrendResult(
    val direction: TrendDirection,
    val strength: Float
)

private fun computeTrend(dataPoints: List<DataPoint>): TrendResult {
    if (dataPoints.size < 2) {
        return TrendResult(TrendDirection.STABLE, 0f)
    }

    val yValues = dataPoints.map { it.y }
    val range = yValues.max() - yValues.min()
    if (range == 0f) {
        return TrendResult(TrendDirection.STABLE, 0f)
    }

    // Simple linear regression slope
    val n = dataPoints.size
    val sumX = dataPoints.indices.sumOf { it.toDouble() }.toFloat()
    val sumY = yValues.sum()
    val sumXY = dataPoints.mapIndexed { i, p -> i.toFloat() * p.y }.sum()
    val sumX2 = dataPoints.indices.sumOf { (it * it).toDouble() }.toFloat()

    val slope = if (n * sumX2 - sumX * sumX != 0f) {
        (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
    } else 0f

    val normalizedSlope = slope * (n - 1) / range

    return when {
        normalizedSlope > 0.2f -> TrendResult(TrendDirection.INCREASING, normalizedSlope.coerceIn(0f, 1f))
        normalizedSlope < -0.2f -> TrendResult(TrendDirection.DECREASING, (-normalizedSlope).coerceIn(0f, 1f))
        else -> TrendResult(TrendDirection.STABLE, (1f - kotlin.math.abs(normalizedSlope) * 5f).coerceIn(0f, 1f))
    }
}

/**
 * Generate an accessibility description for a graph.
 *
 * Follows DIAGRAM Center guidelines for accessible image descriptions.
 * The description includes chart type, title, data range, and trend.
 *
 * @param graph The [GraphContent] to describe
 * @param summary Optional pre-computed [DataSummary]
 * @return Human-readable accessibility description
 */
public fun generateAccessibilityDescription(
    graph: GraphContent,
    summary: DataSummary? = null
): String {
    val stats = summary ?: computeDataSummary(graph.dataPoints)

    return buildString {
        // Chart type
        append("${graphTypeLabel(graph.graphType)}")

        // Title
        if (graph.title.isNotEmpty()) {
            append(" titled '${graph.title}'")
        }

        // Data count
        append(". Contains ${stats.count} data points")

        // Axis labels
        if (graph.axes.x.label.isNotEmpty() || graph.axes.y.label.isNotEmpty()) {
            append(". ")
            if (graph.axes.x.label.isNotEmpty()) {
                append("X-axis: ${graph.axes.x.label}")
            }
            if (graph.axes.x.label.isNotEmpty() && graph.axes.y.label.isNotEmpty()) {
                append(", ")
            }
            if (graph.axes.y.label.isNotEmpty()) {
                append("Y-axis: ${graph.axes.y.label}")
            }
        }

        // Data range
        if (stats.count > 0) {
            append(". Y values range from ${formatNumber(stats.min)} to ${formatNumber(stats.max)}")
            append(", with average ${formatNumber(stats.mean)}")
        }

        // Trend
        if (stats.count >= 2) {
            append(". Trend is ${stats.trend.name.lowercase()}")
            if (stats.trendStrength > 0.5f) {
                append(" (strong)")
            } else if (stats.trendStrength > 0.2f) {
                append(" (moderate)")
            } else {
                append(" (weak)")
            }
        }

        append(".")
    }
}

internal fun graphTypeLabel(graphType: GraphType): String {
    return when (graphType) {
        GraphType.LINE_CHART -> "Line chart"
        GraphType.BAR_CHART -> "Bar chart"
        GraphType.PIE_CHART -> "Pie chart"
        GraphType.SCATTER_PLOT -> "Scatter plot"
        GraphType.AREA_CHART -> "Area chart"
        GraphType.HISTOGRAM -> "Histogram"
    }
}

internal fun formatNumber(value: Float): String {
    return if (value == value.toLong().toFloat()) {
        value.toLong().toString()
    } else {
        val rounded = kotlin.math.round(value * 100) / 100
        rounded.toString()
    }
}
