package io.drishti.graph

import io.drishti.core.*

/**
 * Detects and parses graph content from structured data input.
 *
 * The [GraphDetector] is the primary entry point for converting graph data
 * from JSON, CSV, or programmatic sources into [GraphContent] objects. It
 * also provides a secondary path for parsing OCR text from camera frames
 * when structured data is unavailable.
 *
 * This replaces the previous vision-based detection approach with a
 * data-first architecture that is more reliable and accessible.
 *
 * Primary usage (data-first):
 * ```kotlin
 * val detector = GraphDetector()
 * val graph = detector.detectFromJson(jsonString)
 * ```
 *
 * Secondary usage (OCR fallback):
 * ```kotlin
 * val graph = detector.detectFromOcrText(ocrOutput)
 * ```
 */
class GraphDetector : DetectorPlugin {

    override val contentType = ContentType.GRAPH
    override val confidence = 0.95f

    private val extractor = DataExtractor()

    /**
     * Detect graph content from a camera frame.
     *
     * This method implements the [DetectorPlugin] interface but relies
     * on OCR text extraction within the frame data. For data-first
     * workflows, prefer [detectFromJson], [detectFromCsv], or
     * [detectFromDataPoints] instead.
     *
     * @param frame The input image frame
     * @return Detected [GraphContent], or null if detection fails
     */
    override suspend fun detect(frame: Frame): ContentItem? {
        val text = try {
            frame.data?.decodeToString()
        } catch (_: Exception) {
            null
        } ?: return null
        if (text.isBlank()) return null
        return detectFromOcrText(text)
    }

    /**
     * Parse graph content from a JSON string.
     *
     * This is the primary entry point for the data-first workflow.
     * The JSON should conform to the graph data schema with fields:
     * `type`, `title`, `x_label`, `y_label`, `data`, `labels`.
     *
     * @param json JSON string describing the graph
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun detectFromJson(json: String): GraphContent? {
        return try {
            extractor.fromJson(json).graph
        } catch (e: GraphDataException) {
            null
        }
    }

    /**
     * Parse graph content from a CSV string.
     *
     * The first row is treated as column headers. The parser auto-detects
     * x and y columns based on common header names.
     *
     * @param csv CSV string with header row
     * @param chartType Optional chart type override
     * @param title Optional chart title
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun detectFromCsv(
        csv: String,
        chartType: String? = null,
        title: String = ""
    ): GraphContent? {
        return try {
            extractor.fromCsv(csv, chartType, title).graph
        } catch (e: GraphDataException) {
            null
        }
    }

    /**
     * Parse graph content from programmatic data points.
     *
     * @param type Chart type string (e.g., "line_chart", "bar_chart")
     * @param title Chart title
     * @param xLabel X-axis label
     * @param yLabel Y-axis label
     * @param points List of (x, y) number pairs
     * @return Parsed [GraphContent]
     */
    fun detectFromDataPoints(
        type: String = "line_chart",
        title: String = "",
        xLabel: String = "",
        yLabel: String = "",
        points: List<Pair<Number, Number>>
    ): GraphContent {
        return extractor.fromDataPoints(
            type = type,
            title = title,
            xLabel = xLabel,
            yLabel = yLabel,
            points = points
        ).graph
    }

    /**
     * Try to auto-detect the input format and parse accordingly.
     *
     * @param input Raw input string (JSON or CSV)
     * @param title Optional title override
     * @return Parsed [GraphContent], or null if parsing fails
     */
    fun detectAuto(input: String, title: String = ""): GraphContent? {
        return try {
            extractor.autoDetect(input, title).graph
        } catch (e: GraphDataException) {
            null
        }
    }

    /**
     * Attempt to parse graph content from OCR text output.
     *
     * This is a secondary path for when structured data is unavailable
     * and the only source is text extracted from a camera frame via OCR.
     * The method tries to identify tabular data patterns in the text
     * and convert them to graph data.
     *
     * @param ocrText Text extracted from an image via OCR
     * @param title Optional title
     * @return Parsed [GraphContent], or null if no graph data can be extracted
     */
    fun detectFromOcrText(ocrText: String, title: String = ""): GraphContent? {
        val trimmed = ocrText.trim()
        if (trimmed.isEmpty()) return null

        // Try CSV-like parsing (tab or comma separated values with line breaks)
        if (trimmed.contains("\n") && trimmed.contains(Regex("[,\t]"))) {
            val normalizedCsv = trimmed.replace("\t", ",")
            return try {
                extractor.fromCsv(normalizedCsv, title = title).graph
            } catch (e: GraphDataException) {
                null
            }
        }

        // Try JSON parsing
        if (trimmed.startsWith("{")) {
            return detectFromJson(trimmed)
        }

        // Try to extract numeric pairs from text
        val numericPairs = extractNumericPairs(trimmed)
        if (numericPairs.size >= 2) {
            return detectFromDataPoints(
                type = "line_chart",
                title = title,
                points = numericPairs
            )
        }

        return null
    }

    /**
     * Get the [DataExtractor] used by this detector.
     *
     * Useful for advanced usage where callers need access to
     * extraction metadata (warnings, source type).
     */
    fun dataExtractor(): DataExtractor = extractor

    private fun extractNumericPairs(text: String): List<Pair<Number, Number>> {
        val pairs = mutableListOf<Pair<Number, Number>>()
        val numberPattern = Regex("""-?\d+\.?\d*""")

        val lines = text.lines().filter { it.isNotBlank() }
        for (line in lines) {
            val numbers = numberPattern.findAll(line).map {
                it.value.toFloatOrNull() ?: it.value.toDoubleOrNull() ?: return@map null
            }.filterNotNull().toList()

            if (numbers.size >= 2) {
                pairs.add(numbers[0] to numbers[1])
            }
        }

        return pairs
    }
}
