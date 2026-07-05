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

package io.drishti.core

import kotlin.test.*

class PipelineTest {

    // --- detect() tests ---

    @Test
    fun detectReturnsNonNullItems() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline()
        val frame = TestFixtures.frame()
        val detector = StubDetector(ContentType.GRAPH, createItem = TestFixtures.graphContent())
        val results = pipeline.detect(frame, listOf(detector))
        assertEquals(1, results.size)
        assertEquals(ContentType.GRAPH, results[0].contentType)
    }

    @Test
    fun detectFiltersNullResults() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline()
        val frame = TestFixtures.frame()
        val detector = StubDetector(ContentType.GRAPH, createItem = null)
        val results = pipeline.detect(frame, listOf(detector))
        assertTrue(results.isEmpty())
    }

    @Test
    fun detectRunsDetectorsConcurrently() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline()
        val frame = TestFixtures.frame()
        val detectors = listOf(
            StubDetector(ContentType.GRAPH, createItem = TestFixtures.graphContent()),
            StubDetector(ContentType.FORMULA, createItem = TestFixtures.formulaContent()),
            StubDetector(ContentType.MOLECULE, createItem = TestFixtures.moleculeContent())
        )
        val results = pipeline.detect(frame, detectors)
        assertEquals(3, results.size)
    }

    @Test
    fun detectWithEmptyDetectors() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline()
        val frame = TestFixtures.frame()
        val results = pipeline.detect(frame, emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun detectFiltersLowConfidenceByConfig() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline(config = PipelineConfig(minConfidence = 0.5f))
        val frame = TestFixtures.frame()
        val lowConfDetector = StubDetector(
            ContentType.GRAPH,
            createItem = GraphContent(
                graphType = GraphType.LINE_CHART,
                dataPoints = listOf(DataPoint(10f, 20f)),
                confidence = 0.3f
            )
        )
        val highConfDetector = StubDetector(
            ContentType.FORMULA,
            createItem = TestFixtures.formulaContent(confidence = 0.9f)
        )
        val results = pipeline.detect(frame, listOf(lowConfDetector, highConfDetector))
        assertEquals(1, results.size, "Should keep only confidence >= 0.5f")
        assertEquals(ContentType.FORMULA, results[0].contentType)
    }

    @Test
    fun detectTruncatesAtMaxItemsPerFrame() = kotlinx.coroutines.test.runTest {
        val pipeline = Pipeline(config = PipelineConfig(maxItemsPerFrame = 2))
        val frame = TestFixtures.frame()
        val detectors = listOf(
            StubDetector(ContentType.GRAPH, createItem = TestFixtures.graphContent()),
            StubDetector(ContentType.FORMULA, createItem = TestFixtures.formulaContent()),
            StubDetector(ContentType.MOLECULE, createItem = TestFixtures.moleculeContent())
        )
        val results = pipeline.detect(frame, detectors)
        assertEquals(2, results.size, "Should truncate to 2 items")
    }

    // --- buildSceneGraph: empty / single item ---

    @Test
    fun buildSceneGraphWithEmptyItems() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(emptyList())
        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
        assertEquals(0f, graph.bounds.width)
        assertEquals(0f, graph.bounds.height)
    }

    @Test
    fun buildSceneGraphSingleGraphItem() {
        val pipeline = Pipeline()
        val items = listOf(TestFixtures.graphContent())
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(1, graph.nodes.size)
        assertTrue(graph.nodes[0] is SceneNode.DataPointNode)
        // Single item → no edges
        assertTrue(graph.edges.isEmpty())
        // Position should reflect data points, not (0, 0)
        val node = graph.nodes[0] as SceneNode.DataPointNode
        assertTrue(node.x > 0f, "Graph node x should be > 0, got ${node.x}")
        assertTrue(node.y > 0f, "Graph node y should be > 0, got ${node.y}")
    }

    @Test
    fun buildSceneGraphSingleFormulaItem() {
        val pipeline = Pipeline()
        val items = listOf(TestFixtures.formulaContent())
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(1, graph.nodes.size)
        assertTrue(graph.nodes[0] is SceneNode.TextNode)
        val node = graph.nodes[0] as SceneNode.TextNode
        assertEquals("x + y = z", node.text)
        // Position from symbol positions: avg of (10,10), (40,10), (60,10) → (36.67, 10)
        assertTrue(node.position.x > 0f, "Formula node x should be > 0, got ${node.position.x}")
        assertTrue(node.position.y > 0f, "Formula node y should be > 0, got ${node.position.y}")
    }

    @Test
    fun buildSceneGraphSingleMoleculeItem() {
        val pipeline = Pipeline()
        val items = listOf(TestFixtures.moleculeContent())
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(1, graph.nodes.size)
        assertTrue(graph.nodes[0] is SceneNode.ShapeNode)
        val node = graph.nodes[0] as SceneNode.ShapeNode
        assertEquals(ShapeType.POLYGON, node.shapeType)
        // Position from atom positions: avg of (50,50), (30,30), (70,30) → (50, 36.67)
        assertTrue(node.position.x > 0f, "Molecule node x should be > 0, got ${node.position.x}")
        assertTrue(node.position.y > 0f, "Molecule node y should be > 0, got ${node.position.y}")
    }

    @Test
    fun buildSceneGraphSingleShapeItem() {
        val pipeline = Pipeline()
        val items = listOf(ShapeContent(ShapeType.CIRCLE, 100f, 35f, confidence = 0.9f))
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(1, graph.nodes.size)
        assertTrue(graph.nodes[0] is SceneNode.ShapeNode)
        val node = graph.nodes[0] as SceneNode.ShapeNode
        assertEquals(ShapeType.CIRCLE, node.shapeType)
        // Position from order-based layout
        assertTrue(node.position.x > 0f, "Shape node x should be > 0, got ${node.position.x}")
    }

    // --- buildSceneGraph: position assignment ---

    @Test
    fun graphNodePositionReflectsDataPoints() {
        val pipeline = Pipeline()
        val content = TestFixtures.graphContent(
            dataPoints = listOf(
                DataPoint(10f, 20f),
                DataPoint(30f, 50f),
                DataPoint(60f, 80f)
            )
        )
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.DataPointNode
        // Centroid: x = (10+30+60)/3 = 33.33, y = (20+50+80)/3 = 50
        assertEquals(33.33f, node.x, 0.5f)
        assertEquals(50f, node.y, 0.5f)
    }

    @Test
    fun graphNodeWithNoDataPointsFallsBackToOrderPosition() {
        val pipeline = Pipeline()
        val content = GraphContent(
            graphType = GraphType.LINE_CHART,
            title = "Empty",
            dataPoints = emptyList(),
            confidence = 0.85f
        )
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.DataPointNode
        // orderPosition(0) returns (0.1, 0.1) in normalized coords
        assertEquals(0.1f, node.position.x, 0.5f)
        assertEquals(0.1f, node.position.y, 0.5f)
    }

    @Test
    fun formulaNodePositionFromSymbols() {
        val pipeline = Pipeline()
        val content = TestFixtures.formulaContent()
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.TextNode
        // Symbols at x: 10, 40, 60 → avg 36.67; y: 10, 10, 10 → avg 10
        assertEquals(36.67f, node.position.x, 0.5f)
        assertEquals(10f, node.position.y, 0.5f)
    }

    @Test
    fun formulaNodeWithGeometryFallsBackToBoundingBox() {
        val pipeline = Pipeline()
        val content = FormulaContent(
            formulaType = FormulaType.CALCULUS,
            expression = "integral",
            symbols = emptyList(),
            geometry = Geometry(
                boundingBox = BoundingBox(50f, 100f, 200f, 40f)
            ),
            confidence = 0.9f
        )
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.TextNode
        assertEquals(150f, node.position.x, 0.5f)  // 50 + 200/2
        assertEquals(120f, node.position.y, 0.5f)  // 100 + 40/2
    }

    @Test
    fun moleculeNodePositionFromAtoms() {
        val pipeline = Pipeline()
        val content = TestFixtures.moleculeContent()
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.ShapeNode
        // Atoms at (50,50), (30,30), (70,30) → avg (50, 36.67)
        assertEquals(50f, node.position.x, 0.5f)
        assertEquals(36.67f, node.position.y, 0.5f)
    }

    @Test
    fun moleculeWithNoAtomsFallsBackToOrderPosition() {
        val pipeline = Pipeline()
        val content = MoleculeContent(
            moleculeType = MoleculeType.SIMPLE,
            atoms = emptyList(),
            name = "Empty",
            confidence = 0.9f
        )
        val graph = pipeline.buildSceneGraph(listOf(content))
        val node = graph.nodes[0] as SceneNode.ShapeNode
        // orderPosition(0) returns (0.1, 0.1) in normalized coords
        assertEquals(0.1f, node.position.x, 0.5f)
        assertEquals(0.1f, node.position.y, 0.5f)
    }

    @Test
    fun moleculeWithEmptyNameDefaultsToMolecule() {
        val pipeline = Pipeline()
        val items = listOf(TestFixtures.moleculeContent(name = ""))
        val graph = pipeline.buildSceneGraph(items)
        val node = graph.nodes[0] as SceneNode.ShapeNode
        assertEquals(ShapeType.POLYGON, node.shapeType)
        assertTrue(node.position.x > 0f, "Molecule node x should be > 0")
    }

    // --- buildSceneGraph: edge generation ---

    @Test
    fun emptySceneHasNoEdges() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(emptyList())
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun singleItemHasNoEdges() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(listOf(TestFixtures.graphContent()))
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun multipleItemsGenerateTemporalEdges() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        // With items at different positions, we should get at least temporal edges
        // (detection order chain: 0→1, 1→2)
        assertTrue(graph.edges.isNotEmpty(), "Multiple items should generate edges")

        // Should have temporal edges connecting the chain
        val temporalEdges = graph.edges.filter { it.edgeType == EdgeType.TEMPORAL }
        assertEquals(2, temporalEdges.size, "Should have 2 temporal edges for 3 items")
    }

    @Test
    fun spatialEdgesGeneratedForNearbyItems() {
        val pipeline = Pipeline()
        // Two graph items with nearby data points → spatial proximity
        val content1 = TestFixtures.graphContent(
            dataPoints = listOf(DataPoint(10f, 10f))
        )
        val content2 = TestFixtures.graphContent(
            dataPoints = listOf(DataPoint(15f, 15f))
        )
        val graph = pipeline.buildSceneGraph(listOf(content1, content2))
        val spatialEdges = graph.edges.filter { it.edgeType == EdgeType.SPATIAL }
        assertTrue(spatialEdges.isNotEmpty(), "Nearby items should have spatial edges")
    }

    @Test
    fun semanticEdgesForComplementaryTypes() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        // FORMULA + GRAPH is a complementary pair → SEMANTIC edge
        val semanticEdges = graph.edges.filter { it.edgeType == EdgeType.SEMANTIC }
        assertTrue(semanticEdges.isNotEmpty(), "Formula + Graph should have semantic edge")
    }

    @Test
    fun semanticEdgesForMoleculeAndShape() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.moleculeContent(),
            ShapeContent(ShapeType.CIRCLE, 100f, 35f, confidence = 0.9f)
        )
        val graph = pipeline.buildSceneGraph(items)
        val semanticEdges = graph.edges.filter { it.edgeType == EdgeType.SEMANTIC }
        assertTrue(semanticEdges.isNotEmpty(), "Molecule + Shape should have semantic edge")
    }

    @Test
    fun edgeWeightsAreBetweenZeroAndOne() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(10f, 10f))),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent(
                atoms = listOf(Atom(0, "C", Point(12f, 12f)))
            )
        )
        val graph = pipeline.buildSceneGraph(items)
        for (edge in graph.edges) {
            assertTrue(edge.weight >= 0f, "Edge weight ${edge.weight} should be >= 0")
            assertTrue(edge.weight <= 1f, "Edge weight ${edge.weight} should be <= 1")
        }
    }

    @Test
    fun noDuplicateEdgeTypeBetweenSameNodePair() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(10f, 10f))),
            TestFixtures.formulaContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        // Each (source, target, type) combination should be unique
        val keys = graph.edges.map {
            val sorted = listOf(it.sourceId, it.targetId).sorted()
            "${sorted[0]}-${sorted[1]}-${it.edgeType}"
        }.toSet()
        assertEquals(keys.size, graph.edges.size, "Should have no duplicate (pair, type) combos")
    }

    // --- buildSceneGraph: bounds ---

    @Test
    fun boundsComputedFromNodePositions() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(10f, 20f))),
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(100f, 80f)))
        )
        val graph = pipeline.buildSceneGraph(items)
        assertTrue(graph.bounds.width > 0f, "Bounds width should be > 0")
        assertTrue(graph.bounds.height > 0f, "Bounds height should be > 0")
    }

    @Test
    fun emptySceneHasZeroBounds() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(emptyList())
        assertEquals(0f, graph.bounds.width)
        assertEquals(0f, graph.bounds.height)
    }

    @Test
    fun singleItemBoundsAreNonZero() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(listOf(TestFixtures.graphContent()))
        assertTrue(graph.bounds.width > 0f)
        assertTrue(graph.bounds.height > 0f)
    }

    // --- buildSceneGraph: mixed content ---

    @Test
    fun buildSceneGraphWithMixedContent() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(3, graph.nodes.size)
        // Mixed content should produce both temporal and semantic edges
        assertTrue(graph.edges.isNotEmpty(), "Mixed content should have edges")
    }

    @Test
    fun genericContentCreatesTextNode() {
        val pipeline = Pipeline()
        val items = listOf(TableContent(3, 4, listOf(listOf("a")), confidence = 0.9f))
        val graph = pipeline.buildSceneGraph(items)
        assertEquals(1, graph.nodes.size)
        assertTrue(graph.nodes[0] is SceneNode.TextNode)
        val node = graph.nodes[0] as SceneNode.TextNode
        assertTrue(node.text.contains("Table"), "Should contain 'Table' in text")
    }

    // --- buildSceneGraph: scene graph helpers ---

    @Test
    fun sceneGraphDescribeIncludesBounds() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(
            listOf(TestFixtures.graphContent(dataPoints = listOf(DataPoint(0f, 0f))))
        )
        val desc = graph.describe()
        assertTrue(desc.contains("bounds="), "Description should include bounds")
    }

    @Test
    fun nodeByIdFindsCorrectNode() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(listOf(TestFixtures.graphContent()))
        val node = graph.nodeById("graph-0")
        assertNotNull(node)
        assertTrue(node is SceneNode.DataPointNode)
    }

    @Test
    fun nodeByIdReturnsNullForMissing() {
        val pipeline = Pipeline()
        val graph = pipeline.buildSceneGraph(listOf(TestFixtures.graphContent()))
        assertNull(graph.nodeById("nonexistent"))
    }

    @Test
    fun edgesForReturnsRelevantEdges() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(10f, 10f))),
            TestFixtures.formulaContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        val edges0 = graph.edgesFor("graph-0")
        val edges1 = graph.edgesFor("formula-1")
        assertTrue(edges0.isNotEmpty() || edges1.isNotEmpty(), "At least one node should have edges")
    }

    @Test
    fun neighborsReturnsConnectedNodes() {
        val pipeline = Pipeline()
        val items = listOf(
            TestFixtures.graphContent(dataPoints = listOf(DataPoint(10f, 10f))),
            TestFixtures.formulaContent()
        )
        val graph = pipeline.buildSceneGraph(items)
        val neighbors0 = graph.neighbors("graph-0")
        // graph-0 and formula-1 are a complementary pair → should be neighbors
        assertTrue(neighbors0.contains("formula-1") || neighbors0.isNotEmpty())
    }
}
