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

package io.drishti.audio

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlin.test.*

class SpatialRendererTest {

    private val renderer = SpatialRenderer()

    // ── Spatial positioning from SceneGraphs ────────────────────────

    @Test
    fun nodeAtCenterMapsToZeroAzimuthZeroElevation() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "center",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(1, scene.sources.size)

        val source = scene.sources.first()
        // Center of 100x100 scene → azimuth = (50/100)*360 - 180 = 0°
        assertEquals(0f, source.position.azimuth, 0.1f)
        // Center → elevation = (50/100)*180 - 90 = 0°
        assertEquals(0f, source.position.elevation, 0.1f)
        // Depth 0 → MIN_DISTANCE
        assertEquals(MIN_DISTANCE, source.position.distance, 0.01f)
    }

    @Test
    fun nodeAtLeftEdgeMapsToNegativeAzimuth() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "left",
                    position = Point(0f, 50f),
                    x = 0f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val source = scene.sources.first()
        // Left edge → azimuth = (0/100)*360 - 180 = -180°
        assertEquals(-180f, source.position.azimuth, 0.1f)
    }

    @Test
    fun nodeAtRightEdgeMapsToPositiveAzimuth() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "right",
                    position = Point(100f, 50f),
                    x = 100f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val source = scene.sources.first()
        // Right edge → azimuth = (100/100)*360 - 180 = 180°
        assertEquals(180f, source.position.azimuth, 0.1f)
    }

    @Test
    fun nodeAtTopMapsToNegativeElevation() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "top",
                    position = Point(50f, 0f),
                    x = 50f, y = 0f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val source = scene.sources.first()
        // Top → elevation = (0/100)*180 - 90 = -90°
        assertEquals(-90f, source.position.elevation, 0.1f)
    }

    @Test
    fun nodeAtBottomMapsToPositiveElevation() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "bottom",
                    position = Point(50f, 100f),
                    x = 50f, y = 100f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val source = scene.sources.first()
        // Bottom → elevation = (100/100)*180 - 90 = 90°
        assertEquals(90f, source.position.elevation, 0.1f)
    }

    @Test
    fun deeperNodeIsFartherFromListener() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "shallow",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f,
                    depth = 0
                ),
                SceneNode.DataPointNode(
                    id = "deep",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f,
                    depth = 3
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(2, scene.sources.size)

        val shallow = scene.sources.first { it.nodeId == "shallow" }
        val deep = scene.sources.first { it.nodeId == "deep" }

        assertTrue(deep.position.distance > shallow.position.distance,
            "Deep node (${deep.position.distance}) should be farther than shallow (${shallow.position.distance})")
    }

    @Test
    fun edgeWeightAffectsVolume() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "a",
                    position = Point(30f, 50f),
                    x = 30f, y = 50f
                ),
                SceneNode.DataPointNode(
                    id = "b",
                    position = Point(70f, 50f),
                    x = 70f, y = 50f
                )
            ),
            edges = listOf(
                SceneEdge(
                    sourceId = "a",
                    targetId = "b",
                    edgeType = EdgeType.CONNECTS,
                    weight = 0.3f
                )
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val sourceA = scene.sources.first { it.nodeId == "a" }
        val sourceB = scene.sources.first { it.nodeId == "b" }

        // Both nodes have one edge with weight 0.3, so volume should be 0.3
        assertEquals(0.3f, sourceA.volume, 0.01f)
        assertEquals(0.3f, sourceB.volume, 0.01f)
    }

    @Test
    fun isolatedNodeHasFullVolume() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "solo",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        val source = scene.sources.first()
        assertEquals(1.0f, source.volume, 0.01f)
    }

    @Test
    fun focusNodeGetsVolumeBoost() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "target",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph, focusNodeId = "target")
        val source = scene.sources.first()
        // Focus boost: 1.0 * 1.2 = 1.2, clamped to 1.0
        assertEquals(1.0f, source.volume, 0.01f)
    }

    @Test
    fun dataPointNodeIsMusicalTone() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "dp",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(SoundType.MUSICAL_TONE, scene.sources.first().soundType)
    }

    @Test
    fun textNodeIsSpeech() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.TextNode(
                    id = "label",
                    position = Point(50f, 50f),
                    text = "Hello"
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(SoundType.SPEECH, scene.sources.first().soundType)
    }

    @Test
    fun shapeNodeIsAmbient() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.ShapeNode(
                    id = "box",
                    position = Point(50f, 50f),
                    shapeType = ShapeType.RECTANGLE
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(SoundType.AMBIENT, scene.sources.first().soundType)
    }

    @Test
    fun speechDescriptionsGeneratedForTextNodes() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.TextNode(
                    id = "title",
                    position = Point(50f, 10f),
                    text = "Chart Title"
                ),
                SceneNode.DataPointNode(
                    id = "dp1",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(2, scene.speechDescriptions.size)
        val titleDesc = scene.speechDescriptions.first { it.sourceNodeId == "title" }
        assertEquals("Chart Title", titleDesc.text)
        val dpDesc = scene.speechDescriptions.first { it.sourceNodeId == "dp1" }
        assertTrue(dpDesc.text.contains("50"))
    }

    @Test
    fun speechDescriptionsGeneratedForDataPoints() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "dp1",
                    position = Point(30f, 60f),
                    x = 30f, y = 60f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val scene = renderer.renderScene(graph)
        assertEquals(1, scene.speechDescriptions.size)
        assertTrue(scene.speechDescriptions.first().text.contains("30"))
        assertTrue(scene.speechDescriptions.first().text.contains("60"))
    }

    @Test
    fun renderSceneToAudioOutput() {
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "dp1",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.render(graph)
        assertTrue(output.spatial)
        assertEquals(1, output.sources.size)
    }

    @Test
    fun renderFromContentItems() {
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent()
        )

        val output = renderer.render(items)
        assertTrue(output.sources.isNotEmpty())
        assertTrue(output.spatial)
    }

    @Test
    fun buildSceneGraphFromItemsCreatesNodes() {
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent()
        )

        val graph = renderer.buildSceneGraphFromItems(items)
        assertTrue(graph.nodes.isNotEmpty())
        assertTrue(graph.edges.isNotEmpty())
        assertTrue(graph.bounds.width > 0f)
        assertTrue(graph.bounds.height > 0f)
    }
}
