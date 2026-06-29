package io.drishti.haptics

import io.drishti.core.*

/**
 * Main haptic renderer that orchestrates haptic output generation.
 *
 * Converts SceneGraph edges/nodes and ContentItem lists into meaningful
 * haptic patterns using VibrationEffect.Composition specs (API 30+).
 *
 * Pattern mapping for SceneGraphs:
 * - Edge weight → vibration amplitude (0.0-1.0 → 1-255)
 * - Edge type → waveform (SPATIAL=proximity buzz, CONTAINS=double tap, SEMANTIC=pulse)
 * - Node position → spatial mapping for dual-motor phones
 * - Node depth → vibration frequency (deeper = faster pulses)
 */
class HapticRenderer {
    private val encoder = HapticEncoder()
    private val patternBuilder = PatternBuilder()
    private val spatialMapper = SpatialMapper()

    // ── ContentItem-based rendering ──────────────────────────────────────

    /**
     * Render content items as haptic output.
     */
    fun render(items: List<ContentItem>, focusIndex: Int = 0): HapticOutput {
        val pulses = mutableListOf<HapticPulse>()

        items.forEachIndexed { index, item ->
            val itemPulses = when (item) {
                is GraphContent -> renderGraphHaptic(item)
                is FormulaContent -> renderFormulaHaptic(item)
                is MoleculeContent -> renderMoleculeHaptic(item)
                else -> emptyList()
            }

            if (index == focusIndex) {
                pulses.addAll(addFocusIndicator(itemPulses))
            } else {
                pulses.addAll(itemPulses)
            }
        }

        return HapticOutput(
            pulses = pulses,
            pattern = patternBuilder.buildPattern(items.size, focusIndex)
        )
    }

    /**
     * Render exploration sequence.
     */
    fun renderExploration(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): HapticOutput {
        val pulses = when (item) {
            is GraphContent -> renderGraphExploration(item, direction)
            is FormulaContent -> renderFormulaExploration(item, direction)
            is MoleculeContent -> renderMoleculeExploration(item, direction)
            else -> emptyList()
        }

        return HapticOutput(
            pulses = pulses,
            pattern = "exploration_${direction.name.lowercase()}"
        )
    }

    // ── SceneGraph-based rendering ───────────────────────────────────────

    /**
     * Render a [SceneGraph] into [HapticOutput].
     *
     * Each edge becomes a haptic pulse with amplitude derived from [SceneEdge.weight]
     * and waveform derived from [SceneEdge.edgeType]. Each node becomes a spatial
     * anchor pulse whose duration is modulated by [SceneNode.depth].
     *
     * Compound waveforms:
     * - DOUBLE_TAP edges produce two pulses (primary + 70% secondary)
     * - RAPID_TAP edges produce three sequential pulses with decaying amplitude
     */
    fun renderFromSceneGraph(graph: SceneGraph): HapticOutput {
        if (graph.nodes.isEmpty()) {
            return HapticOutput(pulses = emptyList(), pattern = "empty_scene")
        }

        val pulses = mutableListOf<HapticPulse>()

        graph.edges.forEach { edge ->
            pulses.addAll(renderEdgePulses(edge, graph))
        }

        graph.nodes.forEach { node ->
            pulses.add(renderNodePulse(node, graph.bounds))
        }

        return HapticOutput(
            pulses = pulses,
            pattern = "scene_${graph.nodes.size}_nodes_${graph.edges.size}_edges"
        )
    }

    /**
     * Render a [SceneGraph] into rich [HapticPatternDefinition]s.
     *
     * Returns one pattern per edge and one per node, each with a single
     * [HapticEventSpec] that maps directly to VibrationEffect.Composition:
     * - API 31+: Composition primitive with waveform, scale, delay
     * - API 30: createWaveform() timing/amplitude arrays
     */
    fun renderSceneGraphPatterns(graph: SceneGraph): List<HapticPatternDefinition> {
        if (graph.nodes.isEmpty()) return emptyList()

        val patterns = mutableListOf<HapticPatternDefinition>()

        graph.edges.forEach { edge ->
            val sourceNode = graph.nodeById(edge.sourceId)
            val targetNode = graph.nodeById(edge.targetId)
            if (sourceNode != null && targetNode != null) {
                val waveform = EdgeWaveformMapper.mapWaveform(edge.edgeType)
                val baseDuration = EdgeWaveformMapper.mapBaseDuration(edge.edgeType)
                val duration = (baseDuration * edge.weight).toLong().coerceIn(10L, 200L)

                val midX = (sourceNode.position.x + targetNode.position.x) / 2f
                val midY = (sourceNode.position.y + targetNode.position.y) / 2f
                val normX = normalizeCoordinate(midX, graph.bounds.width)
                val normY = normalizeCoordinate(midY, graph.bounds.height)

                patterns.add(
                    HapticPatternDefinition(
                        events = listOf(
                            HapticEventSpec(
                                intensity = edge.weight,
                                duration = duration,
                                waveform = waveform,
                                spatialX = normX,
                                spatialY = normY
                            )
                        ),
                        totalDuration = duration,
                        patternName = "edge_${edge.edgeType.name.lowercase()}_${edge.sourceId}_${edge.targetId}",
                        sourceNodeId = edge.sourceId
                    )
                )
            }
        }

        graph.nodes.forEach { node ->
            val nodeType = NodeWaveformMapper.classifyNode(node)
            val baseDuration = NodeWaveformMapper.mapBaseDuration(nodeType)
            val depthMod = NodeWaveformMapper.depthFrequencyModifier(node.depth)
            val duration = (baseDuration * depthMod).toLong().coerceIn(15L, 150L)
            val intensity = NodeWaveformMapper.mapIntensityModifier(nodeType)
            val waveform = NodeWaveformMapper.mapWaveform(nodeType)

            val normX = normalizeCoordinate(node.position.x, graph.bounds.width)
            val normY = normalizeCoordinate(node.position.y, graph.bounds.height)

            patterns.add(
                HapticPatternDefinition(
                    events = listOf(
                        HapticEventSpec(
                            intensity = intensity,
                            duration = duration,
                            waveform = waveform,
                            spatialX = normX,
                            spatialY = normY
                        )
                    ),
                    totalDuration = duration,
                    patternName = "node_${nodeType.name.lowercase()}_${node.id}",
                    sourceNodeId = node.id
                )
            )
        }

        return patterns
    }

    // ── Private: SceneGraph helpers ──────────────────────────────────────

    private fun renderEdgePulses(edge: SceneEdge, graph: SceneGraph): List<HapticPulse> {
        val sourceNode = graph.nodeById(edge.sourceId) ?: return emptyList()
        val targetNode = graph.nodeById(edge.targetId) ?: return emptyList()

        val waveform = EdgeWaveformMapper.mapWaveform(edge.edgeType)
        val baseDuration = EdgeWaveformMapper.mapBaseDuration(edge.edgeType)
        val duration = (baseDuration * edge.weight).toLong().coerceIn(10L, 200L)
        val intensity = edge.weight

        val midX = (sourceNode.position.x + targetNode.position.x) / 2f
        val midY = (sourceNode.position.y + targetNode.position.y) / 2f
        val normX = normalizeCoordinate(midX, graph.bounds.width)
        val normY = normalizeCoordinate(midY, graph.bounds.height)

        val pulses = mutableListOf<HapticPulse>()
        pulses.add(HapticPulse(intensity = intensity, duration = duration, x = normX, y = normY))

        if (waveform == HapticWaveform.DOUBLE_TAP) {
            pulses.add(
                HapticPulse(
                    intensity = (intensity * 0.7f).coerceIn(0f, 1f),
                    duration = (duration * 0.6).toLong().coerceAtLeast(5L),
                    x = normX,
                    y = normY
                )
            )
        }

        if (waveform == HapticWaveform.RAPID_TAP) {
            val rapidDuration = (duration / 3).coerceAtLeast(5L)
            repeat(2) { i ->
                pulses.add(
                    HapticPulse(
                        intensity = (intensity * (0.8f - i * 0.1f)).coerceIn(0f, 1f),
                        duration = rapidDuration,
                        x = normX,
                        y = normY
                    )
                )
            }
        }

        return pulses
    }

    private fun renderNodePulse(node: SceneNode, bounds: SceneBounds): HapticPulse {
        val nodeType = NodeWaveformMapper.classifyNode(node)
        val baseDuration = NodeWaveformMapper.mapBaseDuration(nodeType)
        val depthMod = NodeWaveformMapper.depthFrequencyModifier(node.depth)
        val duration = (baseDuration * depthMod).toLong().coerceIn(15L, 150L)
        val intensity = NodeWaveformMapper.mapIntensityModifier(nodeType)

        val normX = normalizeCoordinate(node.position.x, bounds.width)
        val normY = normalizeCoordinate(node.position.y, bounds.height)

        return HapticPulse(intensity = intensity, duration = duration, x = normX, y = normY)
    }

    private fun normalizeCoordinate(value: Float, bound: Float): Float =
        if (bound > 0) (value / bound).coerceIn(0f, 1f) else 0.5f

    // ── Private: ContentItem rendering ───────────────────────────────────

    private fun renderGraphHaptic(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.map { point ->
            HapticPulse(
                intensity = normalizeIntensity(point.y, graph.axes.y.range),
                duration = 50L,
                x = normalizePosition(point.x, graph.axes.x.range),
                y = normalizePosition(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderFormulaHaptic(formula: FormulaContent): List<HapticPulse> {
        val maxX = formula.symbols.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = formula.symbols.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        return formula.symbols.map { symbol ->
            HapticPulse(
                intensity = symbolIntensity(symbol.type),
                duration = symbolDuration(symbol.type),
                x = (symbol.position.x / maxX).coerceIn(0.05f, 0.95f),
                y = (symbol.position.y / maxY).coerceIn(0.05f, 0.95f)
            )
        }
    }

    private fun renderMoleculeHaptic(molecule: MoleculeContent): List<HapticPulse> {
        val pulses = mutableListOf<HapticPulse>()

        val maxX = molecule.atoms.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = molecule.atoms.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        fun normX(value: Float) = (value / maxX).coerceIn(0.05f, 0.95f)
        fun normY(value: Float) = (value / maxY).coerceIn(0.05f, 0.95f)

        molecule.atoms.forEach { atom ->
            pulses.add(
                HapticPulse(
                    intensity = atomIntensity(atom.element),
                    duration = 60L,
                    x = normX(atom.position.x),
                    y = normY(atom.position.y)
                )
            )
        }

        molecule.bonds.forEach { bond ->
            val from = molecule.atoms.getOrNull(bond.from)
            val to = molecule.atoms.getOrNull(bond.to)
            if (from != null && to != null) {
                pulses.add(
                    HapticPulse(
                        intensity = bondIntensity(bond.type),
                        duration = 40L,
                        x = normX((from.position.x + to.position.x) / 2),
                        y = normY((from.position.y + to.position.y) / 2)
                    )
                )
            }
        }

        return pulses
    }

    private fun addFocusIndicator(pulses: List<HapticPulse>): List<HapticPulse> {
        val result = pulses.toMutableList()
        if (pulses.isNotEmpty()) {
            val focusPulse = pulses.first().copy(
                intensity = 1.0f,
                duration = 100L
            )
            result.add(0, focusPulse)
        }
        return result
    }

    private fun renderGraphExploration(graph: GraphContent, direction: ExplorationDirection): List<HapticPulse> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextDataPoint(graph)
            ExplorationDirection.PREVIOUS -> renderPreviousDataPoint(graph)
            ExplorationDirection.POSITION -> renderCurrentPosition(graph)
        }
    }

    private fun renderFormulaExploration(formula: FormulaContent, direction: ExplorationDirection): List<HapticPulse> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextSymbol(formula)
            ExplorationDirection.PREVIOUS -> renderPreviousSymbol(formula)
            ExplorationDirection.POSITION -> renderCurrentPosition(formula)
        }
    }

    private fun renderMoleculeExploration(molecule: MoleculeContent, direction: ExplorationDirection): List<HapticPulse> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextAtom(molecule)
            ExplorationDirection.PREVIOUS -> renderPreviousAtom(molecule)
            ExplorationDirection.POSITION -> renderCurrentPosition(molecule)
        }
    }

    private fun renderNextDataPoint(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.takeLast(1).map { point ->
            HapticPulse(
                intensity = 0.8f,
                duration = 80L,
                x = normalizePosition(point.x, graph.axes.x.range),
                y = normalizePosition(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderPreviousDataPoint(graph: GraphContent): List<HapticPulse> {
        return graph.dataPoints.take(1).map { point ->
            HapticPulse(
                intensity = 0.6f,
                duration = 60L,
                x = normalizePosition(point.x, graph.axes.x.range),
                y = normalizePosition(point.y, graph.axes.y.range)
            )
        }
    }

    private fun renderCurrentPosition(graph: GraphContent): List<HapticPulse> {
        return listOf(HapticPulse(intensity = 1.0f, duration = 150L, x = 0.5f, y = 0.5f))
    }

    private fun renderNextSymbol(formula: FormulaContent): List<HapticPulse> {
        val maxX = formula.symbols.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = formula.symbols.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        return formula.symbols.takeLast(1).map { symbol ->
            HapticPulse(
                intensity = symbolIntensity(symbol.type),
                duration = 60L,
                x = (symbol.position.x / maxX).coerceIn(0.05f, 0.95f),
                y = (symbol.position.y / maxY).coerceIn(0.05f, 0.95f)
            )
        }
    }

    private fun renderPreviousSymbol(formula: FormulaContent): List<HapticPulse> {
        val maxX = formula.symbols.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = formula.symbols.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        return formula.symbols.take(1).map { symbol ->
            HapticPulse(
                intensity = symbolIntensity(symbol.type),
                duration = 60L,
                x = (symbol.position.x / maxX).coerceIn(0.05f, 0.95f),
                y = (symbol.position.y / maxY).coerceIn(0.05f, 0.95f)
            )
        }
    }

    private fun renderCurrentPosition(formula: FormulaContent): List<HapticPulse> {
        return listOf(HapticPulse(intensity = 1.0f, duration = 150L, x = 0.5f, y = 0.5f))
    }

    private fun renderNextAtom(molecule: MoleculeContent): List<HapticPulse> {
        val maxX = molecule.atoms.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = molecule.atoms.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        return molecule.atoms.takeLast(1).map { atom ->
            HapticPulse(
                intensity = atomIntensity(atom.element),
                duration = 60L,
                x = (atom.position.x / maxX).coerceIn(0.05f, 0.95f),
                y = (atom.position.y / maxY).coerceIn(0.05f, 0.95f)
            )
        }
    }

    private fun renderPreviousAtom(molecule: MoleculeContent): List<HapticPulse> {
        val maxX = molecule.atoms.maxOfOrNull { it.position.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = molecule.atoms.maxOfOrNull { it.position.y }?.coerceAtLeast(1f) ?: 1f
        return molecule.atoms.take(1).map { atom ->
            HapticPulse(
                intensity = atomIntensity(atom.element),
                duration = 60L,
                x = (atom.position.x / maxX).coerceIn(0.05f, 0.95f),
                y = (atom.position.y / maxY).coerceIn(0.05f, 0.95f)
            )
        }
    }

    private fun renderCurrentPosition(molecule: MoleculeContent): List<HapticPulse> {
        return listOf(HapticPulse(intensity = 1.0f, duration = 150L, x = 0.5f, y = 0.5f))
    }

    private fun normalizeIntensity(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    }

    private fun normalizePosition(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    }

    private fun symbolIntensity(type: SymbolType): Float = when (type) {
        SymbolType.OPERATOR -> 0.9f
        SymbolType.FUNCTION -> 0.8f
        SymbolType.NUMBER -> 0.7f
        SymbolType.VARIABLE -> 0.6f
        SymbolType.BRACKET -> 0.4f
        SymbolType.SUBSCRIPT -> 0.5f
        SymbolType.SUPERSCRIPT -> 0.5f
        SymbolType.FRACTION -> 0.8f
        SymbolType.SUMMATION -> 0.9f
        SymbolType.INTEGRAL -> 0.9f
        SymbolType.GREEK_LETTER -> 0.7f
        SymbolType.EQUALS -> 0.8f
        SymbolType.RELATION -> 0.8f
    }

    private fun symbolDuration(type: SymbolType): Long = when (type) {
        SymbolType.OPERATOR -> 80L
        SymbolType.FUNCTION -> 100L
        SymbolType.NUMBER -> 50L
        SymbolType.VARIABLE -> 50L
        SymbolType.BRACKET -> 30L
        SymbolType.SUBSCRIPT -> 40L
        SymbolType.SUPERSCRIPT -> 40L
        SymbolType.FRACTION -> 120L
        SymbolType.SUMMATION -> 150L
        SymbolType.INTEGRAL -> 150L
        SymbolType.GREEK_LETTER -> 60L
        SymbolType.EQUALS -> 80L
        SymbolType.RELATION -> 80L
    }

    private fun atomIntensity(element: String): Float = when (element) {
        "C" -> 0.9f
        "N" -> 0.8f
        "O" -> 0.85f
        "H" -> 0.5f
        "S" -> 0.7f
        "P" -> 0.7f
        "Fe" -> 0.95f
        "Na" -> 0.75f
        "Cl" -> 0.8f
        else -> 0.6f
    }

    private fun bondIntensity(type: BondType): Float = when (type) {
        BondType.SINGLE -> 0.5f
        BondType.DOUBLE -> 0.7f
        BondType.TRIPLE -> 0.9f
        BondType.AROMATIC -> 0.8f
        BondType.IONIC -> 0.6f
        BondType.HYDROGEN -> 0.3f
    }
}
