package io.drishti.audio

import io.drishti.core.*
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Renders [SceneGraph] nodes as spatial audio sources positioned in 3D space.
 *
 * Spatial mapping:
 * - Node X position → azimuth (-180° to 180°)
 * - Node Y position → elevation (-90° to 90°)
 * - Node depth → distance (closer = louder)
 * - Edge weight → volume mixing
 * - Node type → sound type (DataPointNode = musical tone, TextNode = speech, ShapeNode = ambient)
 *
 * Produces [SpatialAudioScene] consumed by the Android Spatializer API (API 32+)
 * via Oboe for low-latency rendering.
 */
class SpatialRenderer {

    /**
     * Render a [SceneGraph] into a [SpatialAudioScene] with positioned sources
     * and speech descriptions.
     *
     * @param sceneGraph The scene graph to render.
     * @param focusNodeId Optional node id to highlight with increased volume.
     */
    fun renderScene(sceneGraph: SceneGraph, focusNodeId: String? = null): SpatialAudioScene {
        val volumeByNode = computeVolumeByNode(sceneGraph)

        val sources = sceneGraph.nodes.map { node ->
            val position = nodeToPosition(node, sceneGraph.bounds)
            val baseVolume = volumeByNode[node.id] ?: 1.0f
            val volume = if (node.id == focusNodeId) (baseVolume * 1.2f).coerceAtMost(1.0f) else baseVolume
            val soundType = nodeTypeToSoundType(node)
            val frequency = nodeToFrequency(node)
            val speechText = nodeToSpeechText(node)

            SpatialAudioSource(
                position = position,
                volume = volume,
                soundType = soundType,
                frequency = frequency,
                speechText = speechText,
                nodeId = node.id
            )
        }

        val speechDescriptions = sources
            .filter { it.speechText.isNotEmpty() }
            .map { source ->
                SpeechDescription(
                    text = source.speechText,
                    sourceNodeId = source.nodeId,
                    position = source.position
                )
            }

        return SpatialAudioScene(
            sources = sources,
            speechDescriptions = speechDescriptions,
            sceneBounds = sceneGraph.bounds
        )
    }

    /**
     * Convert a [SpatialAudioScene] to core [AudioOutput] for backward compatibility.
     */
    fun toAudioOutput(scene: SpatialAudioScene): AudioOutput {
        val audioSources = scene.sources.map { source ->
            val cartesian = sphericalToCartesian(source.position)
            val maxRange = MAX_DISTANCE.coerceAtLeast(1f)
            val x = ((cartesian.x + maxRange) / (2 * maxRange)).coerceIn(0.05f, 0.95f)
            val y = ((cartesian.y + maxRange) / (2 * maxRange)).coerceIn(0.05f, 0.95f)
            val z = ((cartesian.z + maxRange) / (2 * maxRange)).coerceIn(0.05f, 0.95f)
            AudioSource(
                frequency = source.frequency,
                amplitude = source.volume,
                spatialX = x,
                spatialY = y,
                spatialZ = z
            )
        }
        return AudioOutput(sources = audioSources, spatial = true)
    }

    /**
     * Render a [SceneGraph] directly to core [AudioOutput].
     */
    fun render(sceneGraph: SceneGraph, focusNodeId: String? = null): AudioOutput {
        val scene = renderScene(sceneGraph, focusNodeId)
        return toAudioOutput(scene)
    }

    /**
     * Render content items as spatial audio (legacy interface).
     */
    fun render(items: List<ContentItem>, focusIndex: Int = 0): AudioOutput {
        val sceneGraph = buildSceneGraphFromItems(items, focusIndex)
        val focusNodeId = sceneGraph.nodes.getOrNull(focusIndex)?.id
        return render(sceneGraph, focusNodeId)
    }

    /**
     * Render exploration sequence.
     */
    fun renderExploration(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): AudioOutput {
        val sources = when (item) {
            is GraphContent -> renderGraphExploration(item, direction)
            is FormulaContent -> renderFormulaExploration(item, direction)
            is MoleculeContent -> renderMoleculeExploration(item, direction)
            else -> emptyList()
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    // ── SceneGraph node → spatial position ──────────────────────────

    /**
     * Map a [SceneNode] to a [SpatialPosition] using spherical coordinates.
     *
     * X position maps to azimuth: center = 0°, left edge = -180°, right edge = +180°.
     * Y position maps to elevation: center = 0°, top = -90° (above), bottom = +90° (below).
     * Depth maps to distance: depth 0 = MIN_DISTANCE, depth 5+ = MAX_DISTANCE.
     */
    fun nodeToPosition(node: SceneNode, bounds: SceneBounds): SpatialPosition {
        val azimuth = if (bounds.width > 0f) {
            ((node.position.x / bounds.width) * 360f - 180f).coerceIn(-180f, 180f)
        } else {
            0f
        }

        val elevation = if (bounds.height > 0f) {
            ((node.position.y / bounds.height) * 180f - 90f).coerceIn(-90f, 90f)
        } else {
            0f
        }

        val distance = (MIN_DISTANCE + node.depth * 1.5f).coerceAtMost(MAX_DISTANCE)

        return SpatialPosition(
            azimuth = azimuth,
            elevation = elevation,
            distance = distance
        )
    }

    // ── Volume from edge weights ────────────────────────────────────

    /**
     * Compute per-node volume from incident edge weights.
     *
     * A node's volume is the average weight of all edges connected to it.
     * Nodes with no edges default to full volume (1.0).
     */
    fun computeVolumeByNode(sceneGraph: SceneGraph): Map<String, Float> {
        return sceneGraph.nodes.associate { node ->
            val incidentEdges = sceneGraph.edgesFor(node.id)
            val volume = if (incidentEdges.isNotEmpty()) {
                incidentEdges.map { it.weight }.average().toFloat().coerceIn(0f, 1f)
            } else {
                1.0f
            }
            node.id to volume
        }
    }

    // ── Node type → sound type ──────────────────────────────────────

    /**
     * Map [SceneNode] type to [SoundType].
     *
     * - DataPointNode → MUSICAL_TONE (pitch encodes Y value)
     * - TextNode → SPEECH (text label spoken aloud)
     * - ShapeNode → AMBIENT (structural reference)
     * - AxisNode → AMBIENT (axis reference tone)
     */
    fun nodeTypeToSoundType(node: SceneNode): SoundType = when (node) {
        is SceneNode.DataPointNode -> SoundType.MUSICAL_TONE
        is SceneNode.TextNode -> SoundType.SPEECH
        is SceneNode.ShapeNode -> SoundType.AMBIENT
        is SceneNode.AxisNode -> SoundType.AMBIENT
    }

    // ── Frequency from node data ────────────────────────────────────

    /**
     * Compute base frequency for a node.
     *
     * DataPointNode: Y value mapped to [MIN_FREQUENCY]..[MAX_FREQUENCY].
     * TextNode: fixed speech-synthesis range (440 Hz reference).
     * ShapeNode/AxisNode: low ambient drone (150 Hz).
     */
    fun nodeToFrequency(node: SceneNode): Float = when (node) {
        is SceneNode.DataPointNode -> {
            // Frequency encodes Y value: higher Y = higher pitch
            MIN_FREQUENCY + (node.y.coerceIn(0f, 100f) / 100f) * (MAX_FREQUENCY - MIN_FREQUENCY)
        }
        is SceneNode.TextNode -> 440f
        is SceneNode.ShapeNode -> 150f
        is SceneNode.AxisNode -> 150f
    }

    // ── Speech text from node ───────────────────────────────────────

    /**
     * Generate speech text for a node.
     *
     * TextNode → its text content.
     * DataPointNode → "data point at X, Y".
     * Other types → empty (no speech generated).
     */
    fun nodeToSpeechText(node: SceneNode): String = when (node) {
        is SceneNode.TextNode -> node.text
        is SceneNode.DataPointNode -> "Data point at ${node.x}, ${node.y}"
        else -> ""
    }

    // ── Spherical → Cartesian conversion ────────────────────────────

    /**
     * Triple of cartesian coordinates for spatial audio rendering.
     */
    data class Cartesian3D(val x: Float, val y: Float, val z: Float)

    /**
     * Convert [SpatialPosition] (spherical) to cartesian (x, y, z).
     *
     * Used to produce core [AudioSource] coordinates for backward compatibility.
     */
    fun sphericalToCartesian(position: SpatialPosition): Cartesian3D {
        val azimuthRad = Math.toRadians(position.azimuth.toDouble())
        val elevationRad = Math.toRadians(position.elevation.toDouble())
        val x = (position.distance * kotlin.math.cos(elevationRad) * kotlin.math.sin(azimuthRad)).toFloat()
        val y = (position.distance * kotlin.math.sin(elevationRad)).toFloat()
        val z = (position.distance * kotlin.math.cos(elevationRad) * kotlin.math.cos(azimuthRad)).toFloat()
        return Cartesian3D(x, y, z)
    }

    // ── SceneGraph construction from ContentItems ───────────────────

    /**
     * Build a minimal [SceneGraph] from a list of [ContentItem]s.
     *
     * Each ContentItem becomes one or more SceneNodes. Edges are created
     * between sequential items (TEMPORAL type) with weight 1.0.
     */
    fun buildSceneGraphFromItems(items: List<ContentItem>, focusIndex: Int = 0): SceneGraph {
        val nodes = mutableListOf<SceneNode>()
        var xPosition = 50f

        items.forEachIndexed { index, item ->
            val node = when (item) {
                is GraphContent -> buildGraphNodes(item, xPosition, index)
                is FormulaContent -> buildFormulaNodes(item, xPosition, index)
                is MoleculeContent -> buildMoleculeNodes(item, xPosition, index)
                else -> listOf(
                    SceneNode.TextNode(
                        id = "item_$index",
                        position = Point(xPosition, 50f),
                        text = item.contentType.name,
                        depth = if (index == focusIndex) 0 else 1
                    )
                )
            }
            nodes.addAll(node)
            xPosition += 150f
        }

        val edges = buildTemporalEdges(nodes)

        val maxX = nodes.maxOfOrNull { it.position.x } ?: 100f
        val maxY = nodes.maxOfOrNull { it.position.y } ?: 100f

        return SceneGraph(
            nodes = nodes,
            edges = edges,
            bounds = SceneBounds(width = maxX + 50f, height = maxY + 50f)
        )
    }

    private fun buildGraphNodes(graph: GraphContent, startX: Float, itemIndex: Int): List<SceneNode> {
        val nodes = mutableListOf<SceneNode>()

        graph.dataPoints.forEachIndexed { pointIndex, point ->
            nodes.add(
                SceneNode.DataPointNode(
                    id = "graph_${itemIndex}_dp_$pointIndex",
                    position = Point(startX + point.x * 0.3f, point.y),
                    x = point.x,
                    y = point.y,
                    depth = 0
                )
            )
        }

        if (graph.title.isNotEmpty()) {
            nodes.add(
                SceneNode.TextNode(
                    id = "graph_${itemIndex}_title",
                    position = Point(startX, 0f),
                    text = graph.title,
                    depth = 0
                )
            )
        }

        return nodes
    }

    private fun buildFormulaNodes(formula: FormulaContent, startX: Float, itemIndex: Int): List<SceneNode> {
        val nodes = mutableListOf<SceneNode>()

        formula.symbols.forEachIndexed { symIndex, symbol ->
            nodes.add(
                SceneNode.TextNode(
                    id = "formula_${itemIndex}_sym_$symIndex",
                    position = Point(startX + symbol.position.x * 0.3f, symbol.position.y),
                    text = "${symbol.type.name}: ${symbol.value}",
                    depth = 0
                )
            )
        }

        return nodes
    }

    private fun buildMoleculeNodes(molecule: MoleculeContent, startX: Float, itemIndex: Int): List<SceneNode> {
        val nodes = mutableListOf<SceneNode>()

        molecule.atoms.forEach { atom ->
            nodes.add(
                SceneNode.DataPointNode(
                    id = "molecule_${itemIndex}_atom_${atom.id}",
                    position = Point(startX + atom.position.x * 0.3f, atom.position.y),
                    x = atom.position.x,
                    y = atom.position.y,
                    depth = 0
                )
            )
        }

        return nodes
    }

    private fun buildTemporalEdges(nodes: List<SceneNode>): List<SceneEdge> {
        if (nodes.size < 2) return emptyList()
        return nodes.windowed(2).map { (a, b) ->
            SceneEdge(
                sourceId = a.id,
                targetId = b.id,
                edgeType = EdgeType.TEMPORAL,
                weight = 1.0f
            )
        }
    }

    // ── Exploration rendering (ContentItem-based) ───────────────────

    private fun renderGraphExploration(graph: GraphContent, direction: ExplorationDirection): List<AudioSource> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextDataPoint(graph)
            ExplorationDirection.PREVIOUS -> renderPreviousDataPoint(graph)
            ExplorationDirection.POSITION -> renderCurrentPosition()
        }
    }

    private fun renderFormulaExploration(formula: FormulaContent, direction: ExplorationDirection): List<AudioSource> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextSymbol(formula)
            ExplorationDirection.PREVIOUS -> renderPreviousSymbol(formula)
            ExplorationDirection.POSITION -> renderCurrentPosition()
        }
    }

    private fun renderMoleculeExploration(molecule: MoleculeContent, direction: ExplorationDirection): List<AudioSource> {
        return when (direction) {
            ExplorationDirection.NEXT -> renderNextAtom(molecule)
            ExplorationDirection.PREVIOUS -> renderPreviousAtom(molecule)
            ExplorationDirection.POSITION -> renderCurrentPosition()
        }
    }

    private fun renderNextDataPoint(graph: GraphContent): List<AudioSource> {
        return graph.dataPoints.takeLast(1).map { point ->
            AudioSource(
                frequency = mapToFrequency(point.y, graph.axes.y.range),
                amplitude = 0.8f,
                spatialX = normalizePosition(point.x, graph.axes.x.range),
                spatialY = normalizePosition(point.y, graph.axes.y.range),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderPreviousDataPoint(graph: GraphContent): List<AudioSource> {
        return graph.dataPoints.take(1).map { point ->
            AudioSource(
                frequency = mapToFrequency(point.y, graph.axes.y.range),
                amplitude = 0.6f,
                spatialX = normalizePosition(point.x, graph.axes.x.range),
                spatialY = normalizePosition(point.y, graph.axes.y.range),
                spatialZ = 0.5f
            )
        }
    }

    private fun renderNextSymbol(formula: FormulaContent): List<AudioSource> {
        return formula.symbols.takeLast(1).map { symbol ->
            AudioSource(
                frequency = symbolFrequency(symbol.type),
                amplitude = 0.8f,
                spatialX = symbol.position.x,
                spatialY = symbol.position.y,
                spatialZ = 0.5f
            )
        }
    }

    private fun renderPreviousSymbol(formula: FormulaContent): List<AudioSource> {
        return formula.symbols.take(1).map { symbol ->
            AudioSource(
                frequency = symbolFrequency(symbol.type),
                amplitude = 0.6f,
                spatialX = symbol.position.x,
                spatialY = symbol.position.y,
                spatialZ = 0.5f
            )
        }
    }

    private fun renderNextAtom(molecule: MoleculeContent): List<AudioSource> {
        return molecule.atoms.takeLast(1).map { atom ->
            AudioSource(
                frequency = atomFrequency(atom.element),
                amplitude = 0.8f,
                spatialX = atom.position.x,
                spatialY = atom.position.y,
                spatialZ = 0.5f
            )
        }
    }

    private fun renderPreviousAtom(molecule: MoleculeContent): List<AudioSource> {
        return molecule.atoms.take(1).map { atom ->
            AudioSource(
                frequency = atomFrequency(atom.element),
                amplitude = 0.6f,
                spatialX = atom.position.x,
                spatialY = atom.position.y,
                spatialZ = 0.5f
            )
        }
    }

    private fun renderCurrentPosition(): List<AudioSource> {
        return listOf(AudioSource(frequency = 800f, amplitude = 1.0f, spatialX = 0.5f, spatialY = 0.5f, spatialZ = 0.5f))
    }

    // ── Utility mapping functions ───────────────────────────────────

    private fun normalizePosition(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        return ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    }

    private fun mapToFrequency(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val normalized = normalizePosition(value, range)
        return MIN_FREQUENCY + (normalized * (MAX_FREQUENCY - MIN_FREQUENCY))
    }

    private fun symbolFrequency(type: SymbolType): Float = when (type) {
        SymbolType.OPERATOR -> 300f
        SymbolType.FUNCTION -> 500f
        SymbolType.NUMBER -> 400f
        SymbolType.VARIABLE -> 350f
        SymbolType.BRACKET -> 250f
        SymbolType.SUBSCRIPT -> 300f
        SymbolType.SUPERSCRIPT -> 450f
        SymbolType.FRACTION -> 600f
        SymbolType.SUMMATION -> 700f
        SymbolType.INTEGRAL -> 700f
        SymbolType.GREEK_LETTER -> 400f
        SymbolType.EQUALS -> 350f
        SymbolType.RELATION -> 350f
    }

    private fun atomFrequency(element: String): Float = when (element) {
        "C" -> 300f
        "N" -> 400f
        "O" -> 450f
        "H" -> 600f
        "S" -> 250f
        "P" -> 350f
        "Fe" -> 200f
        "Na" -> 500f
        "Cl" -> 550f
        else -> 400f
    }
}
