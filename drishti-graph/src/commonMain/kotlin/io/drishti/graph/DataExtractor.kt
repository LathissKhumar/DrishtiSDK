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
 * Extracts graph data from various input sources.
 *
 * The [DataExtractor] serves as the unified entry point for converting
 * raw input (JSON strings, CSV strings, or programmatic data) into
 * structured [GraphContent] objects ready for rendering.
 *
 * Usage:
 * ```kotlin
 * val extractor = DataExtractor()
 *
 * // From JSON
 * val graph = extractor.fromJson(jsonString)
 *
 * // From CSV
 * val graph = extractor.fromCsv(csvString, title = "Sales Data")
 *
 * // From programmatic data
 * val graph = extractor.fromDataPoints(
 *     type = "bar_chart",
 *     title = "Quarterly Revenue",
 *     points = listOf(1f to 100f, 2f to 200f, 3f to 150f)
 * )
 * ```
 */
public class DataExtractor {

    private val parser = GraphDataParser()

    /**
     * Extract graph content from a JSON string.
     *
     * Accepts the standard graph JSON format with fields:
     * `type`, `title`, `x_label`, `y_label`, `data`, `labels`.
     *
     * @param json JSON string describing the graph
     * @return [ExtractionResult] with parsed graph and metadata
     * @throws GraphDataException if JSON is invalid or missing required fields
     */
    public fun fromJson(json: String): ExtractionResult {
        val parseResult = parser.parseJson(json)
        return ExtractionResult(
            graph = parseResult.graph,
            source = DataSource.JSON,
            warnings = parseResult.warnings,
            inferredType = parseResult.inferredType
        )
    }

    /**
     * Extract graph content from a CSV string.
     *
     * The first row is treated as column headers. The parser auto-detects
     * x and y columns based on common header names.
     *
     * @param csv CSV string with header row
     * @param chartType Optional chart type override
     * @param title Optional chart title
     * @return [ExtractionResult] with parsed graph and metadata
     * @throws GraphDataException if CSV is malformed
     */
    public fun fromCsv(
        csv: String,
        chartType: String? = null,
        title: String = ""
    ): ExtractionResult {
        val parseResult = parser.parseCsv(csv, chartType, title)
        return ExtractionResult(
            graph = parseResult.graph,
            source = DataSource.CSV,
            warnings = parseResult.warnings,
            inferredType = parseResult.inferredType
        )
    }

    /**
     * Extract graph content from programmatic data points.
     *
     * @param type Chart type string (e.g., "line_chart", "bar_chart")
     * @param title Chart title
     * @param xLabel X-axis label
     * @param yLabel Y-axis label
     * @param points List of (x, y) number pairs
     * @param pointLabels Optional labels for each data point
     * @return [ExtractionResult] with parsed graph
     */
    public fun fromDataPoints(
        type: String = "line_chart",
        title: String = "",
        xLabel: String = "",
        yLabel: String = "",
        points: List<Pair<Number, Number>>,
        pointLabels: List<String> = emptyList()
    ): ExtractionResult {
        val graph = parser.parseDataPoints(
            graphType = type,
            title = title,
            xLabel = xLabel,
            yLabel = yLabel,
            dataPoints = points,
            labels = pointLabels
        )
        return ExtractionResult(
            graph = graph,
            source = DataSource.PROGRAMMATIC,
            warnings = emptyList(),
            inferredType = false
        )
    }

    /**
     * Extract graph content from a map of string key-value pairs.
     *
     * Useful for form inputs or configuration-based graph definitions.
     *
     * @param properties Map with keys matching the JSON schema
     * @return [ExtractionResult] with parsed graph
     * @throws GraphDataException if properties are invalid
     */
    public fun fromProperties(properties: Map<String, String>): ExtractionResult {
        val jsonObject = buildJsonObject {
            properties.forEach { (key, value) ->
                when (key) {
                    "data" -> {
                        // JSON→CSV fallback: try parsing as JSON array first,
                        // fall back to semicolon/comma-separated key-value pairs
                        try {
                            val dataElement = Json.parseToJsonElement(value)
                            put(key, dataElement)
                        } catch (e: IllegalArgumentException) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            val points = value.split(";").mapNotNull { pair ->
                                val parts = pair.split(",")
                                if (parts.size >= 2) {
                                    buildJsonObject {
                                        put("x", parts[0].trim())
                                        put("y", parts[1].trim())
                                    }
                                } else null
                            }
                            put(key, JsonArray(points))
                        }
                    }
                    "labels" -> {
                        val labelArray = value.split(",").map { JsonPrimitive(it.trim()) }
                        put(key, JsonArray(labelArray))
                    }
                    else -> put(key, JsonPrimitive(value))
                }
            }
        }

        val parseResult = parser.parseJsonObject(jsonObject)
        return ExtractionResult(
            graph = parseResult.graph,
            source = DataSource.PROPERTIES,
            warnings = parseResult.warnings,
            inferredType = parseResult.inferredType
        )
    }

    /**
     * Try to auto-detect the input format and parse accordingly.
     *
     * Detection heuristics:
     * - Starts with `{` → JSON
     * - Contains `,` with line breaks → CSV
     * - Otherwise → attempt JSON, fall back to error
     *
     * @param input Raw input string
     * @param title Optional title override
     * @return [ExtractionResult] with parsed graph
     * @throws GraphDataException if format cannot be determined or parsed
     */
    public fun autoDetect(input: String, title: String = ""): ExtractionResult {
        val trimmed = input.trim()

        return when {
            trimmed.startsWith("{") -> fromJson(trimmed)
            trimmed.contains(",") && trimmed.contains("\n") -> fromCsv(trimmed, title = title)
            else -> throw GraphDataException(
                "Cannot auto-detect input format. " +
                    "Provide JSON (starts with '{') or CSV (contains commas and newlines)."
            )
        }
    }
}

/**
 * The source format of extracted graph data.
 */
public enum class DataSource {
    /** Input was a JSON string */
    JSON,
    /** Input was a CSV string */
    CSV,
    /** Input was programmatic data points */
    PROGRAMMATIC,
    /** Input was a key-value properties map */
    PROPERTIES
}

/**
 * Result of extracting graph data from a source.
 *
 * @property graph The parsed [GraphContent] ready for rendering
 * @property source The format the data was extracted from
 * @property warnings Any non-fatal issues encountered during parsing
 * @property inferredType Whether the chart type was auto-inferred
 */
public data class ExtractionResult(
    val graph: GraphContent,
    val source: DataSource,
    val warnings: List<String> = emptyList(),
    val inferredType: Boolean = false
)
