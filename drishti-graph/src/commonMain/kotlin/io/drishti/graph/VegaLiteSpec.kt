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
class VegaLiteSpec {

    /**
     * Generate a Vega-Lite specification as a [JsonObject].
     *
     * @param graph The [GraphContent] to generate a spec for
     * @return Complete Vega-Lite v5 JSON specification
     */
    fun generate(graph: GraphContent): JsonObject {
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
            put("width", 600)
            put("height", 400)
        }
    }

    /**
     * Generate a Vega-Lite specification as a JSON string.
     *
     * @param graph The [GraphContent] to generate a spec for
     * @return Pretty-printed Vega-Lite v5 JSON string
     */
    fun generateString(graph: GraphContent): String {
        return generate(graph).toString()
    }

    /**
     * Get the Vega-Lite mark type for a given [GraphType].
     *
     * @param graphType The type of graph
     * @return The Vega-Lite mark type string
     */
    fun markTypeForGraph(graphType: GraphType): String {
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
        val markType = markTypeForGraph(graph.graphType)

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

        return buildJsonObject {
            put("x", buildJsonObject {
                put("field", "x")
                put("type", if (isNumericAxis(graph.axes.x.range)) "quantitative" else "nominal")
                put("title", xLabel)
                if (graph.graphType == GraphType.HISTOGRAM) {
                    put("bin", true)
                }
                if (isNumericAxis(graph.axes.x.range)) {
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

    private fun isNumericAxis(range: ClosedFloatingPointRange<Float>): Boolean {
        return range.start != 0f || range.endInclusive != 100f
    }

    private fun Float.toJson(): JsonPrimitive = JsonPrimitive(this)
}

/**
 * Extension data: statistical summary of graph data.
 *
 * Contains computed statistics useful for voice descriptions
 * and accessibility information.
 */
data class DataSummary(
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
enum class TrendDirection {
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
fun computeDataSummary(dataPoints: List<DataPoint>): DataSummary {
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

    val firstY = dataPoints.first().y
    val lastY = dataPoints.last().y
    val range = dataPoints.maxOf { it.y } - dataPoints.minOf { it.y }

    if (range == 0f) {
        return TrendResult(TrendDirection.STABLE, 0f)
    }

    val changeRatio = (lastY - firstY) / range

    return when {
        changeRatio > 0.2f -> TrendResult(TrendDirection.INCREASING, changeRatio.coerceIn(0f, 1f))
        changeRatio < -0.2f -> TrendResult(TrendDirection.DECREASING, (-changeRatio).coerceIn(0f, 1f))
        else -> TrendResult(TrendDirection.STABLE, (1f - kotlin.math.abs(changeRatio) * 5f).coerceIn(0f, 1f))
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
fun generateAccessibilityDescription(
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

private fun graphTypeLabel(graphType: GraphType): String {
    return when (graphType) {
        GraphType.LINE_CHART -> "Line chart"
        GraphType.BAR_CHART -> "Bar chart"
        GraphType.PIE_CHART -> "Pie chart"
        GraphType.SCATTER_PLOT -> "Scatter plot"
        GraphType.AREA_CHART -> "Area chart"
        GraphType.HISTOGRAM -> "Histogram"
    }
}

private fun formatNumber(value: Float): String {
    return if (value == value.toLong().toFloat()) {
        value.toLong().toString()
    } else {
        "%.2f".format(value)
    }
}
