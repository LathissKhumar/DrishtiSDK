package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Dimensions of the scene in logical units.
 *
 * @param width Scene width (horizontal extent of all content).
 * @param height Scene height (vertical extent of all content).
 */
@Serializable
data class SceneBounds(val width: Float, val height: Float)

/**
 * Unified semantic representation of visual content.
 *
 * A [SceneGraph] is the primary output of [Pipeline.buildSceneGraph]. It holds
 * spatially-positioned [SceneNode] objects connected by [SceneEdge] relationships
 * that encode spatial proximity, semantic association, and temporal detection order.
 *
 * @param nodes Positioned content nodes detected in the frame.
 * @param edges Relationships between nodes (spatial, semantic, temporal).
 * @param bounds Overall scene dimensions computed from node positions.
 * @param metadata Optional key-value pairs for rendering hints or debug info.
 */
@Serializable
data class SceneGraph(
    val nodes: List<SceneNode>,
    val edges: List<SceneEdge>,
    val bounds: SceneBounds = SceneBounds(0f, 0f),
    val metadata: Map<String, String> = emptyMap()
) {
    // Build indexes at construction time for O(1) lookups
    private val nodeIndex: Map<String, SceneNode> by lazy { nodes.associateBy { it.id } }
    private val adjacencyIndex: Map<String, List<SceneEdge>> by lazy {
        val bySource = edges.groupBy { it.sourceId }
        val byTarget = edges.groupBy { it.targetId }
        val allKeys = bySource.keys + byTarget.keys
        allKeys.associateWith { key ->
            (bySource[key] ?: emptyList()) + (byTarget[key] ?: emptyList())
        }
    }

    /** Human-readable summary of the scene graph. */
    fun describe(): String {
        return "Scene with ${nodes.size} nodes and ${edges.size} connections, " +
            "bounds=${bounds.width.toInt()}x${bounds.height.toInt()}"
    }

    /** Lookup a node by its unique [id], or null if not found. */
    fun nodeById(id: String): SceneNode? = nodeIndex[id]

    /** Return all edges incident on the given [nodeId]. */
    fun edgesFor(nodeId: String): List<SceneEdge> = adjacencyIndex[nodeId] ?: emptyList()

    /** Return neighbor node ids for the given [nodeId]. */
    fun neighbors(nodeId: String): List<String> =
        edgesFor(nodeId).map { if (it.sourceId == nodeId) it.targetId else it.sourceId }.distinct()
}

/**
 * A positioned node in the scene graph.
 *
 * Each subclass represents a different visual content type with its own
 * spatial data. The [position] field is the canonical center point used
 * for edge-distance calculations. The [depth] field tracks nesting level
 * (0 = top-level, 1 = contained inside another item, etc.).
 */
@Serializable
sealed class SceneNode {
    abstract val id: String
    abstract val position: Point

    /** Nesting depth: 0 for top-level items, 1+ for contained items. */
    open val depth: Int get() = 0

    @Serializable
    data class DataPointNode(
        override val id: String,
        override val position: Point,
        val x: Float,
        val y: Float,
        override val depth: Int = 0
    ) : SceneNode()

    @Serializable
    data class AxisNode(
        override val id: String,
        override val position: Point,
        val axis: Axis,
        override val depth: Int = 0
    ) : SceneNode()

    @Serializable
    data class TextNode(
        override val id: String,
        override val position: Point,
        val text: String,
        override val depth: Int = 0
    ) : SceneNode()

    @Serializable
    data class ShapeNode(
        override val id: String,
        override val position: Point,
        val shapeType: ShapeType,
        override val depth: Int = 0
    ) : SceneNode()
}

/**
 * A weighted, typed edge between two [SceneNode]s.
 *
 * @param sourceId The originating node id.
 * @param targetId The destination node id.
 * @param edgeType The semantic relationship between the nodes.
 * @param weight Strength of the connection (0.0 = weakest, 1.0 = strongest).
 *                Used by renderers to prioritize haptic/audio output.
 */
@Serializable
data class SceneEdge(
    val sourceId: String,
    val targetId: String,
    val edgeType: EdgeType,
    val weight: Float = 1.0f
)

/**
 * Types of relationships between scene nodes.
 *
 * - **CONNECTS** – Spatial proximity: two items are near each other.
 * - **CONTAINS** – Containment: one item's bounding box overlaps another.
 * - **LABELS** – Temporal/detection order: items detected sequentially.
 * - **MEASURES** – Semantic complement: e.g. formula describes a graph.
 * - **SPATIAL** – Explicit spatial adjacency (distance below threshold).
 * - **SEMANTIC** – Content-type complementarity (formula ↔ graph).
 * - **TEMPORAL** – Detection-order sequence edge.
 */
@Serializable
enum class EdgeType {
    CONTAINS,
    CONNECTS,
    LABELS,
    MEASURES,
    SPATIAL,
    SEMANTIC,
    TEMPORAL
}
