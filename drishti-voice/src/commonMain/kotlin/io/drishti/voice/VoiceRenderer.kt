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

package io.drishti.voice

import io.drishti.core.BondType
import io.drishti.core.ContentItem
import io.drishti.core.DataPoint
import io.drishti.core.ExplorationDirection
import io.drishti.core.FormulaContent
import io.drishti.core.FormulaSymbol
import io.drishti.core.GraphContent
import io.drishti.core.GraphType
import io.drishti.core.MoleculeContent
import io.drishti.core.ShapeContent
import io.drishti.core.SpeechSegment
import io.drishti.core.SymbolType
import io.drishti.core.TableContent
import io.drishti.core.VoiceOutput

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
public class VoiceRenderer(private val voiceData: VoiceData = VoiceData.default()) {

    private val describer = ContentDescriber()

    /**
     * Render content items as voice output.
     *
     * Items are described in natural language. If [focusIndex] is specified,
     * that item is announced first with a positional cue.
     */
    public fun render(items: List<ContentItem>, focusIndex: Int = 0): VoiceOutput {
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
    public fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): VoiceOutput {
        val speech = when (item) {
            is GraphContent -> renderGraphExploration(item, direction, elementIndex)
            is FormulaContent -> renderFormulaExploration(item, direction, elementIndex)
            is MoleculeContent -> renderMoleculeExploration(item, direction, elementIndex)
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

    private fun renderGraphExploration(
        graph: GraphContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): SpeechSegment {
        val points = graph.dataPoints
        val currentIndex = elementIndex.coerceIn(-1, points.size - 1)
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val nextIdx = currentIndex + 1
                val point = points.getOrNull(nextIdx)
                val text = if (point != null) {
                    "Data point ${nextIdx + 1} of ${points.size} at x equals ${"%.1f".format(point.x)}, " +
                        "y equals ${"%.1f".format(point.y)}."
                } else {
                    "No more data points."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val prevIdx = (currentIndex - 1).coerceAtLeast(0)
                val point = points.getOrNull(prevIdx)
                val text = if (point != null && currentIndex > 0) {
                    "Data point ${prevIdx + 1} of ${points.size} at x equals ${"%.1f".format(point.x)}, " +
                        "y equals ${"%.1f".format(point.y)}."
                } else {
                    "No previous data points."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val total = points.size
                val pos = (currentIndex + 1).coerceIn(1, total.coerceAtLeast(1))
                SpeechSegment(
                    text = "Position $pos of $total data points.",
                    rate = 1.0f,
                    pitch = 1.0f
                )
            }
        }
    }

    private fun renderFormulaExploration(
        formula: FormulaContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): SpeechSegment {
        val symbols = formula.symbols
        val currentIndex = elementIndex.coerceIn(-1, symbols.size - 1)
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val nextIdx = currentIndex + 1
                val symbol = symbols.getOrNull(nextIdx)
                val text = if (symbol != null) {
                    "Symbol ${nextIdx + 1} of ${symbols.size}: ${symbolToSpeechName(symbol)}."
                } else {
                    "No more symbols."
                }
                SpeechSegment(text = text, rate = 0.85f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val prevIdx = (currentIndex - 1).coerceAtLeast(0)
                val symbol = symbols.getOrNull(prevIdx)
                val text = if (symbol != null && currentIndex > 0) {
                    "Symbol ${prevIdx + 1} of ${symbols.size}: ${symbolToSpeechName(symbol)}."
                } else {
                    "No previous symbols."
                }
                SpeechSegment(text = text, rate = 0.85f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val total = symbols.size
                val pos = (currentIndex + 1).coerceIn(1, total.coerceAtLeast(1))
                val expressionSpeech = FormulaSpeech.expressionOnly(formula)
                SpeechSegment(
                    text = "Position $pos of $total symbols. Expression: $expressionSpeech.",
                    rate = 0.85f,
                    pitch = 1.0f
                )
            }
        }
    }

    private fun renderMoleculeExploration(
        molecule: MoleculeContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): SpeechSegment {
        val atoms = molecule.atoms
        val currentIndex = elementIndex.coerceIn(-1, atoms.size - 1)
        return when (direction) {
            ExplorationDirection.NEXT -> {
                val nextIdx = currentIndex + 1
                val atom = atoms.getOrNull(nextIdx)
                val text = if (atom != null) {
                    "Atom ${nextIdx + 1} of ${atoms.size}: ${atom.element}${chargeLabel(atom.charge)}."
                } else {
                    "No more atoms."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.PREVIOUS -> {
                val prevIdx = (currentIndex - 1).coerceAtLeast(0)
                val atom = atoms.getOrNull(prevIdx)
                val text = if (atom != null && currentIndex > 0) {
                    "Atom ${prevIdx + 1} of ${atoms.size}: ${atom.element}${chargeLabel(atom.charge)}."
                } else {
                    "No previous atoms."
                }
                SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f)
            }
            ExplorationDirection.POSITION -> {
                val atomCount = atoms.size
                val bondCount = molecule.bonds.size
                val pos = (currentIndex + 1).coerceIn(1, atomCount.coerceAtLeast(1))
                SpeechSegment(
                    text = "Atom $pos of $atomCount. Molecule with $atomCount atoms and $bondCount bonds.",
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
        if (speeches.isEmpty()) {
            return SpeechSegment(text = combinedText)
        }
        // Weighted average by text length so longer segments dominate the prosody.
        val totalLen = speeches.sumOf { it.text.length }.coerceAtLeast(1)
        val avgRate = speeches.sumOf { (it.text.length * it.rate).toDouble() }.toFloat() / totalLen
        val avgPitch = speeches.sumOf { (it.text.length * it.pitch).toDouble() }.toFloat() / totalLen
        return SpeechSegment(text = combinedText, rate = avgRate, pitch = avgPitch)
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
