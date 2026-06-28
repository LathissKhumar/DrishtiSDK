package io.drishti.voice

import io.drishti.core.*

/**
 * Renders content items as natural-language voice output.
 *
 * Follows MathCAT conventions for formula verbalization and
 * DIAGRAM Center guidelines for graph/molecule descriptions.
 *
 * Each content type gets a natural speech description rather than
 * a structural dump. Formulas are verbalized using the formula module's
 * LaTeX parser and speech rule engine.
 *
 * Usage:
 * ```
 * val renderer = VoiceRenderer()
 * val output = renderer.render(listOf(formulaContent))
 * // output.speech.text contains natural language description
 * ```
 */
class VoiceRenderer(private val voiceData: VoiceData = VoiceData.default()) {

    private val describer = ContentDescriber()

    /**
     * Render content items as voice output.
     *
     * Items are described in natural language. If [focusIndex] is specified,
     * that item is announced first with a positional cue.
     */
    fun render(items: List<ContentItem>, focusIndex: Int = 0): VoiceOutput {
        if (items.isEmpty()) {
            return VoiceOutput(speech = SpeechSegment(text = ""), language = voiceData.language)
        }

        val speeches = mutableListOf<SpeechSegment>()

        items.forEachIndexed { index, item ->
            val speech = when (item) {
                is GraphContent -> renderGraph(item)
                is FormulaContent -> renderFormula(item)
                is MoleculeContent -> renderMolecule(item)
                is ShapeContent -> renderShape(item)
                is TableContent -> renderTable(item)
                else -> renderGeneric(item)
            }

            val adjusted = SpeechSegment(
                text = speech.text,
                rate = voiceData.rateForContentType(item.contentType),
                pitch = voiceData.pitchForContentType(item.contentType)
            )

            if (index == focusIndex) {
                speeches.add(0, SpeechSegment(
                    text = "Item ${index + 1} of ${items.size}. ${adjusted.text}",
                    rate = adjusted.rate,
                    pitch = adjusted.pitch
                ))
            } else {
                speeches.add(adjusted)
            }
        }

        val combined = combineSpeeches(speeches)
        return VoiceOutput(speech = combined, language = voiceData.language)
    }

    /**
     * Render exploration sequence for interactive content navigation.
     */
    fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection): VoiceOutput {
        val speech = when (item) {
            is GraphContent -> renderGraphExploration(item, direction)
            is FormulaContent -> renderFormulaExploration(item, direction)
            is MoleculeContent -> renderMoleculeExploration(item, direction)
            else -> renderGeneric(item)
        }

        return VoiceOutput(speech = speech, language = voiceData.language)
    }

    // ── Graph descriptions (DIAGRAM Center guidelines) ──────────────

    private fun renderGraph(graph: GraphContent): SpeechSegment {
        val description = describeGraphNaturally(graph)
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    private fun describeGraphNaturally(graph: GraphContent): String = buildString {
        val typeLabel = graphTypeLabel(graph.graphType)
        append(typeLabel)

        if (graph.title.isNotEmpty()) {
            append(" titled '${graph.title}'")
        }

        append(" with ${graph.dataPoints.size} data points")

        if (graph.dataPoints.size >= 2) {
            val trend = calculateTrend(graph.dataPoints)
            append(", showing $trend trend")
        }

        append(". ")

        if (graph.labels.isNotEmpty()) {
            val categories = graph.labels.size
            append("Comparing $categories categories")
            if (graph.labels.size <= 5) {
                append(": ${graph.labels.joinToString(", ")}")
            }
            append(". ")
        }

        append("X axis: ${graph.axes.x.label}, range ${formatRange(graph.axes.x.range)}. ")
        append("Y axis: ${graph.axes.y.label}, range ${formatRange(graph.axes.y.range)}. ")

        if (graph.dataPoints.isNotEmpty()) {
            append(valueHighlights(graph))
        }
    }

    private fun graphTypeLabel(type: GraphType): String = when (type) {
        GraphType.LINE_CHART -> "Line chart"
        GraphType.BAR_CHART -> "Bar chart"
        GraphType.PIE_CHART -> "Pie chart"
        GraphType.SCATTER_PLOT -> "Scatter plot"
        GraphType.AREA_CHART -> "Area chart"
        GraphType.HISTOGRAM -> "Histogram"
    }

    private fun valueHighlights(graph: GraphContent): String {
        val points = graph.dataPoints
        if (points.size < 2) return ""

        val maxY = points.maxByOrNull { it.y } ?: return ""
        val minY = points.minByOrNull { it.y } ?: return ""

        return buildString {
            if (maxY.y > minY.y) {
                append("Highest value is ${"%.1f".format(maxY.y)}")
                if (maxY.label != null) {
                    append(" at ${maxY.label}")
                }
                append(", lowest is ${"%.1f".format(minY.y)}")
                append(". ")
            }
        }
    }

    // ── Formula descriptions (MathCAT verbalization) ────────────────

    private fun renderFormula(formula: FormulaContent): SpeechSegment {
        val speechText = FormulaSpeech.fromContent(formula)
        return SpeechSegment(text = speechText, rate = 0.85f, pitch = 1.0f)
    }

    // ── Molecule descriptions ───────────────────────────────────────

    private fun renderMolecule(molecule: MoleculeContent): SpeechSegment {
        val description = describeMoleculeNaturally(molecule)
        return SpeechSegment(text = description, rate = 0.95f, pitch = 1.0f)
    }

    private fun describeMoleculeNaturally(molecule: MoleculeContent): String = buildString {
        append("Molecule")
        if (molecule.name.isNotEmpty()) {
            append(" named '${molecule.name}'")
        }

        val atomCounts = molecule.atoms.groupBy { it.element }
        val parts = atomCounts.map { (element, list) ->
            if (list.size == 1) "1 $element atom" else "${list.size} $element atoms"
        }
        append(" containing ${parts.joinToString(" and ")}")

        val bondTypes = molecule.bonds.groupBy { it.type }
        val bondParts = bondTypes.map { (type, list) ->
            val typeLabel = when (type) {
                BondType.SINGLE -> "single"
                BondType.DOUBLE -> "double"
                BondType.TRIPLE -> "triple"
                BondType.AROMATIC -> "aromatic"
                BondType.IONIC -> "ionic"
                BondType.HYDROGEN -> "hydrogen"
            }
            if (list.size == 1) "1 $typeLabel bond" else "${list.size} $typeLabel bonds"
        }
        if (bondParts.isNotEmpty()) {
            append(" with ${bondParts.joinToString(" and ")}")
        }
        append(". ")
    }

    // ── Shape descriptions ──────────────────────────────────────────

    private fun renderShape(shape: ShapeContent): SpeechSegment {
        val label = shape.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }
        val description = "$label shape with area ${"%.1f".format(shape.area)} " +
            "and perimeter ${"%.1f".format(shape.perimeter)}."
        return SpeechSegment(text = description, rate = 1.0f, pitch = 1.0f)
    }

    // ── Table descriptions ──────────────────────────────────────────

    private fun renderTable(table: TableContent): SpeechSegment {
        val description = "Table with ${table.rows} rows and ${table.columns} columns."
        return SpeechSegment(text = description, rate = 0.9f, pitch = 1.0f)
    }

    // ── Generic fallback ────────────────────────────────────────────

    private fun renderGeneric(item: ContentItem): SpeechSegment {
        val label = item.contentType.name.lowercase().replaceFirstChar { it.uppercase() }
        return SpeechSegment(
            text = "$label content detected with ${"%.0f".format(item.confidence * 100)}% confidence.",
            rate = 1.0f,
            pitch = 1.0f
        )
    }

    // ── Exploration modes ───────────────────────────────────────────

    private fun renderGraphExploration(graph: GraphContent, direction: ExplorationDirection): SpeechSegment {
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val point = graph.dataPoints.lastOrNull()
                val text = if (point != null) {
                    "Next data point at x equals ${"%.1f".format(point.x)}, " +
                        "y equals ${"%.1f".format(point.y)}."
                } else {
                    "No more data points."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val point = graph.dataPoints.firstOrNull()
                val text = if (point != null) {
                    "Previous data point at x equals ${"%.1f".format(point.x)}, " +
                        "y equals ${"%.1f".format(point.y)}."
                } else {
                    "No previous data points."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val total = graph.dataPoints.size
                SpeechSegment(
                    text = "Position: graph contains $total data points. Use next and previous to navigate.",
                    rate = 1.0f,
                    pitch = 1.0f
                )
            }
        }
    }

    private fun renderFormulaExploration(
        formula: FormulaContent,
        direction: ExplorationDirection
    ): SpeechSegment {
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val symbol = formula.symbols.lastOrNull()
                val text = if (symbol != null) {
                    "Next symbol: ${symbolToSpeechName(symbol)}."
                } else {
                    "No more symbols."
                }
                SpeechSegment(text = text, rate = 0.85f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val symbol = formula.symbols.firstOrNull()
                val text = if (symbol != null) {
                    "Previous symbol: ${symbolToSpeechName(symbol)}."
                } else {
                    "No previous symbols."
                }
                SpeechSegment(text = text, rate = 0.85f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val total = formula.symbols.size
                val expressionSpeech = FormulaSpeech.expressionOnly(formula)
                SpeechSegment(
                    text = "Formula with $total symbols. Expression: $expressionSpeech.",
                    rate = 0.85f,
                    pitch = 1.0f
                )
            }
        }
    }

    private fun renderMoleculeExploration(
        molecule: MoleculeContent,
        direction: ExplorationDirection
    ): SpeechSegment {
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val atom = molecule.atoms.lastOrNull()
                val text = if (atom != null) {
                    "Next atom: ${atom.element}${chargeLabel(atom.charge)}."
                } else {
                    "No more atoms."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val atom = molecule.atoms.firstOrNull()
                val text = if (atom != null) {
                    "Previous atom: ${atom.element}${chargeLabel(atom.charge)}."
                } else {
                    "No previous atoms."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val atomCount = molecule.atoms.size
                val bondCount = molecule.bonds.size
                SpeechSegment(
                    text = "Molecule with $atomCount atoms and $bondCount bonds.",
                    rate = 1.0f,
                    pitch = 1.0f
                )
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun symbolToSpeechName(symbol: FormulaSymbol): String {
        val typeLabel = when (symbol.type) {
            SymbolType.GREEK_LETTER -> "Greek letter ${symbol.value}"
            SymbolType.FRACTION -> "fraction"
            SymbolType.SUPERSCRIPT -> "superscript ${symbol.value}"
            SymbolType.SUBSCRIPT -> "subscript ${symbol.value}"
            SymbolType.SUMMATION -> "summation"
            SymbolType.INTEGRAL -> "integral"
            SymbolType.OPERATOR -> "operator ${symbol.value}"
            SymbolType.FUNCTION -> "function ${symbol.value}"
            else -> symbol.value
        }
        return typeLabel
    }

    private fun chargeLabel(charge: Int): String = when {
        charge > 1 -> " with charge plus $charge"
        charge == 1 -> " with charge plus 1"
        charge < -1 -> " with charge minus ${-charge}"
        charge == -1 -> " with charge minus 1"
        else -> ""
    }

    private fun combineSpeeches(speeches: List<SpeechSegment>): SpeechSegment {
        val combinedText = speeches.joinToString(" ") { it.text }
        return SpeechSegment(text = combinedText, rate = 1.0f, pitch = 1.0f)
    }

    private fun formatRange(range: ClosedFloatingPointRange<Float>): String {
        return "${"%.1f".format(range.start)} to ${"%.1f".format(range.endInclusive)}"
    }

    private fun calculateTrend(points: List<DataPoint>): String {
        if (points.size < 2) return "insufficient data"
        val firstY = points.first().y
        val lastY = points.last().y
        return when {
            lastY > firstY * 1.1f -> "increasing"
            lastY < firstY * 0.9f -> "decreasing"
            else -> "stable"
        }
    }
}
