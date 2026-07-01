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

package io.drishti.haptics

import io.drishti.core.*
import kotlin.test.*

class SceneGraphHapticTest {

    @Test
    fun renderFromSceneGraphProducesPulsesForEdgesAndNodes() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.DataPointNode("n2", Point(60f, 80f), 60f, 80f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.CONNECTS, weight = 0.8f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.renderFromSceneGraph(graph)

        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
        // CONNECTS edge: 1 pulse + 2 node pulses = 3
        assertEquals(3, output.pulses.size)
    }

    @Test
    fun edgeWeightMapsToAmplitude() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(50f, 50f), 50f, 50f),
                SceneNode.DataPointNode("n2", Point(50f, 50f), 50f, 50f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.SPATIAL, weight = 0.9f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.renderFromSceneGraph(graph)

        // Edge pulse is first: intensity = edge weight = 0.9
        val edgePulse = output.pulses.first()
        assertEquals(0.9f, edgePulse.intensity)
    }

    @Test
    fun edgeTypeSpatialProducesProximityBuzzWaveform() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.DataPointNode("n2", Point(60f, 80f), 60f, 80f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.SPATIAL, weight = 0.8f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val patterns = renderer.renderSceneGraphPatterns(graph)

        assertTrue(patterns.isNotEmpty())
        val edgePattern = patterns.first { it.sourceNodeId == "n1" }
        assertEquals(HapticWaveform.PROXIMITY_BUZZ, edgePattern.events.first().waveform)
    }

    @Test
    fun edgeTypeContainsProducesDoubleTapWaveform() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.TextNode("txt", Point(50f, 50f), "Label"),
                SceneNode.DataPointNode("dp", Point(50f, 50f), 50f, 50f)
            ),
            edges = listOf(
                SceneEdge("txt", "dp", EdgeType.CONTAINS, weight = 0.7f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val patterns = renderer.renderSceneGraphPatterns(graph)
        val edgePattern = patterns.first { it.sourceNodeId == "txt" }
        assertEquals(HapticWaveform.DOUBLE_TAP, edgePattern.events.first().waveform)
    }

    @Test
    fun edgeTypeSemanticProducesPulseWaveform() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.TextNode("n2", Point(60f, 80f), "Formula")
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.SEMANTIC, weight = 0.6f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val patterns = renderer.renderSceneGraphPatterns(graph)
        val edgePattern = patterns.first { it.sourceNodeId == "n1" }
        assertEquals(HapticWaveform.PULSE, edgePattern.events.first().waveform)
    }

    @Test
    fun nodeDepthModifiesPulseDuration() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("shallow", Point(50f, 50f), 50f, 50f, depth = 0),
                SceneNode.DataPointNode("deep", Point(50f, 50f), 50f, 50f, depth = 2)
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val patterns = renderer.renderSceneGraphPatterns(graph)

        val shallowPattern = patterns.first { it.sourceNodeId == "shallow" }
        val deepPattern = patterns.first { it.sourceNodeId == "deep" }

        // depth=0 → modifier 1.0, depth=2 → modifier 0.5
        // deep node should have shorter duration
        assertTrue(
            deepPattern.totalDuration < shallowPattern.totalDuration,
            "Deep node (${deepPattern.totalDuration}ms) should have shorter duration than shallow (${shallowPattern.totalDuration}ms)"
        )
    }

    @Test
    fun emptyGraphProducesNoOutput() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = emptyList(),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.renderFromSceneGraph(graph)
        assertTrue(output.pulses.isEmpty())

        val patterns = renderer.renderSceneGraphPatterns(graph)
        assertTrue(patterns.isEmpty())
    }

    @Test
    fun nodePositionMapsToSpatialCoordinates() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("left", Point(0f, 50f), 0f, 50f),
                SceneNode.DataPointNode("right", Point(100f, 50f), 100f, 50f)
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.renderFromSceneGraph(graph)
        val pulses = output.pulses

        // First = left node (x≈0), second = right node (x≈1)
        val leftPulse = pulses[0]
        val rightPulse = pulses[1]

        assertTrue(
            leftPulse.x < rightPulse.x,
            "Left node (${leftPulse.x}) should have smaller x than right (${rightPulse.x})"
        )
    }

    @Test
    fun doubleTapEdgeProducesMultiplePulses() {
        val renderer = HapticRenderer()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.DataPointNode("n2", Point(60f, 80f), 60f, 80f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.CONTAINS, weight = 0.8f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val output = renderer.renderFromSceneGraph(graph)

        // CONTAINS → DOUBLE_TAP → 2 edge pulses + 2 node pulses = 4
        assertEquals(4, output.pulses.size)
    }

    @Test
    fun hapticsPluginRenderSceneGraphHapticReturnsOutput() {
        val plugin = HapticsPlugin()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.DataPointNode("n2", Point(60f, 80f), 60f, 80f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.CONNECTS, weight = 0.8f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val output = plugin.renderSceneGraphHaptic(graph)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun hapticsPluginEncodeSceneGraphPatternsReturnsDefinitions() {
        val plugin = HapticsPlugin()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode("n1", Point(20f, 40f), 20f, 40f),
                SceneNode.DataPointNode("n2", Point(60f, 80f), 60f, 80f)
            ),
            edges = listOf(
                SceneEdge("n1", "n2", EdgeType.SPATIAL, weight = 0.9f)
            ),
            bounds = SceneBounds(100f, 100f)
        )

        val patterns = plugin.encodeSceneGraphPatterns(graph)
        assertNotNull(patterns)
        assertTrue(patterns.isNotEmpty())
        // 1 edge pattern + 2 node patterns = 3
        assertEquals(3, patterns.size)
    }

    @Test
    fun amplitudeRangeConversion() {
        assertEquals(1, HapticAmplitudeRange.toAmplitude(0f))
        assertEquals(255, HapticAmplitudeRange.toAmplitude(1f))
        assertEquals(128, HapticAmplitudeRange.toAmplitude(0.5f))
    }
}
