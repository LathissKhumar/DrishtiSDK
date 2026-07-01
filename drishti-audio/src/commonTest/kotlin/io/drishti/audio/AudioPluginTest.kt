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

class AudioPluginTest {

    @Test
    fun nameIsAudio() {
        val plugin = AudioPlugin()
        assertEquals("audio", plugin.name)
    }

    @Test
    fun renderAudioWithGraphContent() {
        val plugin = AudioPlugin()
        val items = listOf(TestFixtures.graphContent())
        val output = plugin.renderAudio(items)
        assertNotNull(output)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderAudioWithEmptyItems() {
        val plugin = AudioPlugin()
        val output = plugin.renderAudio(emptyList())
        assertNotNull(output)
        assertTrue(output.sources.isEmpty())
    }

    @Test
    fun renderExplorationAudioReturnsOutput() {
        val plugin = AudioPlugin()
        val item = TestFixtures.graphContent()
        val output = plugin.renderExplorationAudio(item, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun generateTone() {
        val plugin = AudioPlugin()
        val samples = plugin.generateTone(440f, 100)
        assertNotNull(samples)
        assertTrue(samples.isNotEmpty())
    }

    @Test
    fun applyEnvelope() {
        val plugin = AudioPlugin()
        val samples = FloatArray(100) { 1.0f }
        val enveloped = plugin.applyEnvelope(samples)
        assertNotNull(enveloped)
        assertEquals(100, enveloped.size)
    }

    @Test
    fun renderSpatialSceneReturnsSpatialAudioScene() {
        val plugin = AudioPlugin()
        val graph = SceneGraph(
            nodes = listOf(
                SceneNode.DataPointNode(
                    id = "n1",
                    position = Point(50f, 50f),
                    x = 50f, y = 50f
                )
            ),
            edges = emptyList(),
            bounds = SceneBounds(100f, 100f)
        )
        val scene = plugin.renderSpatialScene(graph)
        assertNotNull(scene)
        assertEquals(1, scene.sources.size)
        assertTrue(scene.sceneBounds.width > 0f)
    }

    @Test
    fun renderSpatialFromItemsProducesScene() {
        val plugin = AudioPlugin()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent()
        )
        val scene = plugin.renderSpatialFromItems(items)
        assertNotNull(scene)
        assertTrue(scene.sources.isNotEmpty())
    }

    @Test
    fun describeContentReturnsSpeechTexts() {
        val plugin = AudioPlugin()
        val items = listOf(
            TestFixtures.graphContent(),
            TestFixtures.formulaContent(),
            TestFixtures.moleculeContent()
        )
        val descriptions = plugin.describeContent(items)
        assertEquals(3, descriptions.size)
        assertTrue(descriptions[0].contains("graph"))
        assertTrue(descriptions[1].contains("formula"))
        assertTrue(descriptions[2].contains("molecule") || descriptions[2].contains("Methanol"))
    }
}
