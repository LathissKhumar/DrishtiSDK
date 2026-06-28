package io.drishti.core

import kotlin.test.*

class SceneGraphTest {

    @Test
    fun sceneGraphDescribeEmpty() {
        val graph = SceneGraph(nodes = emptyList(), edges = emptyList())
        assertEquals(
            "Scene with 0 nodes and 0 connections, bounds=0x0",
            graph.describe()
        )
    }

    @Test
    fun sceneGraphDescribeWithNodes() {
        val nodes = listOf(
            SceneNode.DataPointNode("n1", Point(0f, 0f), 1f, 2f),
            SceneNode.TextNode("n2", Point(1f, 1f), "text")
        )
        val edges = listOf(SceneEdge("n1", "n2", EdgeType.CONNECTS))
        val bounds = SceneBounds(101f, 101f)
        val graph = SceneGraph(nodes = nodes, edges = edges, bounds = bounds)
        val desc = graph.describe()
        assertTrue(desc.contains("2 nodes"), "Should contain node count")
        assertTrue(desc.contains("1 connections"), "Should contain edge count")
        assertTrue(desc.contains("bounds="), "Should contain bounds")
    }

    @Test
    fun sceneGraphDefaults() {
        val graph = SceneGraph(nodes = emptyList(), edges = emptyList())
        assertTrue(graph.metadata.isEmpty())
        assertEquals(SceneBounds(0f, 0f), graph.bounds)
    }

    @Test
    fun sceneGraphNodeTypes() {
        val dpNode = SceneNode.DataPointNode("dp", Point(0f, 0f), 1f, 2f)
        val axNode = SceneNode.AxisNode("ax", Point(0f, 0f), Axis("X", 0f..10f))
        val txNode = SceneNode.TextNode("tx", Point(0f, 0f), "hello")
        val shNode = SceneNode.ShapeNode("sh", Point(0f, 0f), ShapeType.CIRCLE)

        assertEquals("dp", dpNode.id)
        assertEquals(1f, dpNode.x)
        assertEquals(2f, dpNode.y)
        assertEquals("X", axNode.axis.label)
        assertEquals("hello", txNode.text)
        assertEquals(ShapeType.CIRCLE, shNode.shapeType)
    }

    @Test
    fun edgeTypes() {
        val values = EdgeType.entries
        assertEquals(7, values.size)
        assertTrue(values.contains(EdgeType.CONTAINS))
        assertTrue(values.contains(EdgeType.CONNECTS))
        assertTrue(values.contains(EdgeType.LABELS))
        assertTrue(values.contains(EdgeType.MEASURES))
        assertTrue(values.contains(EdgeType.SPATIAL))
        assertTrue(values.contains(EdgeType.SEMANTIC))
        assertTrue(values.contains(EdgeType.TEMPORAL))
    }

    @Test
    fun contentTypeAllValues() {
        val values = ContentType.entries
        assertEquals(6, values.size)
        assertTrue(values.contains(ContentType.GRAPH))
        assertTrue(values.contains(ContentType.FORMULA))
        assertTrue(values.contains(ContentType.MOLECULE))
        assertTrue(values.contains(ContentType.SHAPE))
        assertTrue(values.contains(ContentType.TABLE))
        assertTrue(values.contains(ContentType.CUSTOM))
    }

    // --- New tests: bounds ---

    @Test
    fun sceneBoundsDataClass() {
        val bounds = SceneBounds(640f, 480f)
        assertEquals(640f, bounds.width)
        assertEquals(480f, bounds.height)
    }

    @Test
    fun sceneGraphWithBounds() {
        val bounds = SceneBounds(1024f, 768f)
        val graph = SceneGraph(
            nodes = emptyList(),
            edges = emptyList(),
            bounds = bounds
        )
        assertEquals(1024f, graph.bounds.width)
        assertEquals(768f, graph.bounds.height)
    }

    // --- New tests: edge weight ---

    @Test
    fun sceneEdgeWeightDefault() {
        val edge = SceneEdge("a", "b", EdgeType.CONNECTS)
        assertEquals(1.0f, edge.weight)
    }

    @Test
    fun sceneEdgeWeightCustom() {
        val edge = SceneEdge("a", "b", EdgeType.SPATIAL, weight = 0.7f)
        assertEquals(0.7f, edge.weight)
    }

    // --- New tests: depth ---

    @Test
    fun sceneNodeDepthDefault() {
        val node = SceneNode.TextNode("t1", Point(0f, 0f), "text")
        assertEquals(0, node.depth)
    }

    @Test
    fun dataPointNodeWithDepth() {
        val node = SceneNode.DataPointNode(
            id = "dp1",
            position = Point(10f, 20f),
            x = 10f,
            y = 20f,
            depth = 2
        )
        assertEquals(2, node.depth)
    }

    @Test
    fun textNodeWithDepth() {
        val node = SceneNode.TextNode(
            id = "tx1",
            position = Point(5f, 5f),
            text = "nested",
            depth = 1
        )
        assertEquals(1, node.depth)
    }

    @Test
    fun shapeNodeWithDepth() {
        val node = SceneNode.ShapeNode(
            id = "sh1",
            position = Point(0f, 0f),
            shapeType = ShapeType.RECTANGLE,
            depth = 3
        )
        assertEquals(3, node.depth)
    }

    // --- New tests: nodeById, edgesFor, neighbors ---

    @Test
    fun nodeByIdFindsNode() {
        val nodes = listOf(
            SceneNode.DataPointNode("a", Point(0f, 0f), 0f, 0f),
            SceneNode.TextNode("b", Point(1f, 1f), "text")
        )
        val graph = SceneGraph(nodes = nodes, edges = emptyList())
        assertEquals("a", graph.nodeById("a")?.id)
        assertEquals("b", graph.nodeById("b")?.id)
    }

    @Test
    fun nodeByIdReturnsNullForMissing() {
        val graph = SceneGraph(nodes = emptyList(), edges = emptyList())
        assertNull(graph.nodeById("x"))
    }

    @Test
    fun edgesForReturnsIncidentEdges() {
        val edges = listOf(
            SceneEdge("a", "b", EdgeType.SPATIAL),
            SceneEdge("b", "c", EdgeType.TEMPORAL),
            SceneEdge("a", "c", EdgeType.SEMANTIC)
        )
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.TextNode("a", Point(0f, 0f), "a"),
                SceneNode.TextNode("b", Point(1f, 1f), "b"),
                SceneNode.TextNode("c", Point(2f, 2f), "c")
            ),
            edges = edges
        )
        assertEquals(2, graph.edgesFor("a").size)
        assertEquals(2, graph.edgesFor("b").size)
        assertEquals(2, graph.edgesFor("c").size)
    }

    @Test
    fun neighborsReturnsAdjacentNodes() {
        val edges = listOf(
            SceneEdge("a", "b", EdgeType.SPATIAL),
            SceneEdge("a", "c", EdgeType.SEMANTIC)
        )
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.TextNode("a", Point(0f, 0f), "a"),
                SceneNode.TextNode("b", Point(1f, 1f), "b"),
                SceneNode.TextNode("c", Point(2f, 2f), "c")
            ),
            edges = edges
        )
        val neighborsA = graph.neighbors("a").toSet()
        assertEquals(setOf("b", "c"), neighborsA)
    }
}
