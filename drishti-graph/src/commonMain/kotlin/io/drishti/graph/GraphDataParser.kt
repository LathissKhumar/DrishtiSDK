package io.drishti.graph

import io.drishti.core.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * JSON-serializable representation of graph data input.
 *
 * This is the primary input format for the graph module. Users provide
 * structured data describing a chart, and the parser converts it to
 * the internal [GraphContent] representation.
 *
 * Example JSON:
 * ```json
 * {
 *   "type": "line_chart",
 *   "title": "Sales Over Time",
 *   "x_label": "Month",
 *   "y_label": "Revenue ($)",
 *   "data": [
 *     {"x": "Jan", "y": 1000},
 *     {"x": "Feb", "y": 1500}
 *   ]
 * }
 * ```
 */
@Serializable
data class GraphDataInput(
    val type: String = "line_chart",
    val title: String = "",
    val x_label: String = "",
    val y_label: String = "",
    val data: List<DataPointInput> = emptyList(),
    val labels: List<String> = emptyList()
)

/**
 * A single data point in graph input format.
 */
@Serializable
data class DataPointInput(
    val x: String,
    val y: String,
    val label: String? = null
)

/**
 * Result of parsing graph data, containing the parsed [GraphContent]
 * along with metadata about the parse operation.
 */
data class ParseResult(
    val graph: GraphContent,
    val warnings: List<String> = emptyList(),
    val inferredType: Boolean = false
)

/**
 * Result of CSV parsing, including data points and any parse errors.
 *
 * Unlike JSON parsing, CSV parsing may encounter rows with non-numeric
 * y-values. Rather than silently dropping these rows (data loss), this
 * result captures both the successfully parsed points and any errors
 * encountered during parsing.
 */
data class CsvParseResult(
    val dataPoints: List<DataPoint>,
    val errors: List<String> = emptyList()
)

/**
 * Parses JSON and CSV data into [GraphContent] objects.
 *
 * This parser handles:
 * - JSON graph specifications (primary format)
 * - CSV data with header-based column mapping
 * - Automatic chart type inference from data shape
 * - Data validation and cleaning
 *
 * Usage:
 * ```kotlin
 * val parser = GraphDataParser()
 * val result = parser.parseJson(jsonString)
 * val graph = result.graph
 * ```
 */
class GraphDataParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Parse a JSON string into a [ParseResult].
     *
     * @param input JSON string conforming to the [GraphDataInput] schema
     * @return [ParseResult] containing the parsed graph and any warnings
     * @throws GraphDataException if the input is malformed or invalid
     */
    fun parseJson(input: String): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw GraphDataException("Input JSON is empty")
        }

        val element = try {
            json.parseToJsonElement(trimmed)
        } catch (e: Exception) {
            throw GraphDataException("Invalid JSON syntax: ${e.message}", e)
        }

        if (element !is JsonObject) {
            throw GraphDataException("Root element must be a JSON object")
        }

        return parseJsonObject(element)
    }

    /**
     * Parse a CSV string into a [ParseResult].
     *
     * The first row is treated as headers. Column mapping:
     * - If headers contain "x" and "y" (case-insensitive), they map to data points
     * - If headers contain "label" or "name", it maps to point labels
     * - First numeric column defaults to x, second to y
     *
     * @param input CSV string with header row
     * @param chartType Optional chart type override (inferred if omitted)
     * @param title Optional chart title
     * @return [ParseResult] containing the parsed graph
     * @throws GraphDataException if the CSV is malformed
     */
    fun parseCsv(
        input: String,
        chartType: String? = null,
        title: String = ""
    ): ParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw GraphDataException("Input CSV is empty")
        }

        val lines = trimmed.lines().filter { it.isNotBlank() }
        if (lines.size < 2) {
            throw GraphDataException("CSV must have a header row and at least one data row")
        }

        val headers = parseCsvLine(lines[0]).map { it.trim().lowercase() }
        val dataLines = lines.drop(1)

        val xIndex = headers.indexOfFirst { it == "x" || it == "category" || it == "label" || it == "name" }
            .coerceAtLeast(0)
        val yIndex = headers.indexOfFirst { it == "y" || it == "value" || it == "count" }
            .coerceAtLeast(1).coerceAtMost(headers.size - 1)
        val labelIndex = headers.indexOfFirst { it == "label" || it == "name" || it == "series" }

        val warnings = mutableListOf<String>()
        if (xIndex == yIndex) {
            warnings.add("Could not distinguish x and y columns, using first two numeric columns")
        }

        val dataPoints = dataLines.mapNotNull { line ->
            val cells = parseCsvLine(line).map { it.trim() }
            if (cells.size <= maxOf(xIndex, yIndex)) {
                warnings.add("Skipping incomplete row: $line")
                return@mapNotNull null
            }

            val xValue = cells.getOrElse(xIndex) { "" }
            val yValue = cells.getOrElse(yIndex) { "" }
            val label = if (labelIndex >= 0 && labelIndex < cells.size) cells[labelIndex] else null

            DataPointInput(x = xValue, y = yValue, label = label)
        }

        val inferredType = chartType == null
        val resolvedType = chartType ?: inferChartType(dataPoints)

        val input = GraphDataInput(
            type = resolvedType,
            title = title,
            x_label = headers.getOrElse(xIndex) { "X" },
            y_label = headers.getOrElse(yIndex) { "Y" },
            data = dataPoints,
            labels = emptyList()
        )

        val parseErrors = mutableListOf<String>()
        val graph = buildGraphContent(input, resolvedType, errors = parseErrors)
        warnings.addAll(parseErrors)
        return ParseResult(graph = graph, warnings = warnings, inferredType = inferredType)
    }

    /**
     * Parse a list of programmatic data points into [GraphContent].
     *
     * @param graphType The chart type
     * @param title Chart title
     * @param xLabel X-axis label
     * @param yLabel Y-axis label
     * @param dataPoints List of x/y pairs with optional labels
     * @return [GraphContent] ready for rendering
     */
    fun parseDataPoints(
        graphType: String = "line_chart",
        title: String = "",
        xLabel: String = "",
        yLabel: String = "",
        dataPoints: List<Pair<Number, Number>>,
        labels: List<String> = emptyList()
    ): GraphContent {
        val inputs = dataPoints.map { (x, y) ->
            DataPointInput(x = x.toString(), y = y.toString())
        }
        val input = GraphDataInput(
            type = graphType,
            title = title,
            x_label = xLabel,
            y_label = yLabel,
            data = inputs,
            labels = labels
        )
        return buildGraphContent(input, graphType)
    }

    internal fun parseJsonObject(element: JsonObject): ParseResult {
        val warnings = mutableListOf<String>()
        var inferredType = false

        val type = element["type"]?.jsonPrimitive?.contentOrNull
        val resolvedType = type ?: run {
            inferredType = true
            warnings.add("No chart type specified, inferred: line_chart")
            "line_chart"
        }

        val title = element["title"]?.jsonPrimitive?.contentOrNull ?: ""
        val xLabel = element["x_label"]?.jsonPrimitive?.contentOrNull ?: ""
        val yLabel = element["y_label"]?.jsonPrimitive?.contentOrNull ?: ""

        val dataElement = element["data"]
        val dataPoints = parseDataArray(dataElement, warnings)

        val labels = mutableListOf<String>()
        val labelsElement = element["labels"]
        if (labelsElement is JsonArray) {
            labelsElement.forEach { elem ->
                if (elem is JsonPrimitive) {
                    labels.add(elem.contentOrNull ?: "")
                }
            }
        }

        // Handle pie chart labels from data
        if (resolvedType == "pie_chart" && labels.isEmpty()) {
            dataPoints.forEach { dp ->
                if (dp.label != null) {
                    labels.add(dp.label)
                }
            }
        }

        val input = GraphDataInput(
            type = resolvedType,
            title = title,
            x_label = xLabel,
            y_label = yLabel,
            data = dataPoints,
            labels = labels
        )

        val graph = buildGraphContent(input, resolvedType)
        return ParseResult(graph = graph, warnings = warnings, inferredType = inferredType)
    }

    private fun parseDataArray(dataElement: JsonElement?, warnings: MutableList<String>): List<DataPointInput> {
        if (dataElement == null || dataElement is JsonNull) {
            warnings.add("No data array found")
            return emptyList()
        }

        if (dataElement !is JsonArray) {
            warnings.add("'data' field is not an array")
            return emptyList()
        }

        return dataElement.mapNotNull { elem ->
            when {
                elem is JsonObject -> {
                    val x = elem["x"]?.jsonPrimitive?.contentOrNull ?: ""
                    val y = elem["y"]?.jsonPrimitive?.contentOrNull ?: ""
                    val label = elem["label"]?.jsonPrimitive?.contentOrNull
                    DataPointInput(x = x, y = y, label = label)
                }
                elem is JsonArray && elem.size >= 2 -> {
                    val x = elem[0].jsonPrimitive.contentOrNull ?: ""
                    val y = elem[1].jsonPrimitive.contentOrNull ?: ""
                    DataPointInput(x = x, y = y)
                }
                else -> {
                    warnings.add("Skipping invalid data point: $elem")
                    null
                }
            }
        }
    }

    internal fun buildGraphContent(
        input: GraphDataInput,
        resolvedType: String,
        errors: MutableList<String>? = null
    ): GraphContent {
        val graphType = resolveGraphType(resolvedType)

        val numericDataPoints = mutableListOf<DataPoint>()
        input.data.forEachIndexed { index, dp ->
            val yFloat = dp.y.toFloatOrNull()
            if (yFloat == null) {
                val errorMsg = "Could not parse y-value '${dp.y}' at row ${index + 1}"
                if (errors != null) {
                    errors.add(errorMsg)
                    return@forEachIndexed
                } else {
                    throw GraphDataException(errorMsg)
                }
            }
            val xFloat = dp.x.toFloatOrNull() ?: index.toFloat()
            val label = dp.label ?: dp.x.toFloatOrNull()?.let { null } ?: dp.x
            numericDataPoints.add(DataPoint(x = xFloat, y = yFloat, label = label))
        }

        val xValues = numericDataPoints.map { it.x }
        val yValues = numericDataPoints.map { it.y }

        val xRange = if (xValues.isNotEmpty()) {
            xValues.min()..xValues.max()
        } else {
            0f..100f
        }

        val yRange = if (yValues.isNotEmpty()) {
            yValues.min()..yValues.max()
        } else {
            0f..100f
        }

        return GraphContent(
            graphType = graphType,
            title = input.title,
            axes = Axes(
                x = Axis(label = input.x_label, range = xRange),
                y = Axis(label = input.y_label, range = yRange)
            ),
            dataPoints = numericDataPoints,
            labels = input.labels
        )
    }

    internal fun resolveGraphType(typeStr: String): GraphType {
        return when (typeStr.lowercase().trim()) {
            "line_chart", "line", "linechart" -> GraphType.LINE_CHART
            "bar_chart", "bar", "barchart" -> GraphType.BAR_CHART
            "pie_chart", "pie", "piechart" -> GraphType.PIE_CHART
            "scatter_plot", "scatter", "scatterplot" -> GraphType.SCATTER_PLOT
            "area_chart", "area", "areachart" -> GraphType.AREA_CHART
            "histogram", "hist" -> GraphType.HISTOGRAM
            else -> throw GraphDataException("Unknown chart type: $typeStr")
        }
    }

    internal fun inferChartType(dataPoints: List<DataPointInput>): String {
        if (dataPoints.isEmpty()) return "line_chart"

        val allNumeric = dataPoints.all { it.x.toFloatOrNull() != null && it.y.toFloatOrNull() != null }
        val hasLabels = dataPoints.any { it.label != null }

        return when {
            hasLabels && !allNumeric -> "pie_chart"
            dataPoints.size in 2..8 && allNumeric -> "bar_chart"
            dataPoints.size > 20 -> "scatter_plot"
            else -> "line_chart"
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when {
                line[i] == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip escaped quote per RFC 4180
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                line[i] == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(line[i])
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}

/**
 * Exception thrown when graph data parsing fails.
 */
class GraphDataException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)
