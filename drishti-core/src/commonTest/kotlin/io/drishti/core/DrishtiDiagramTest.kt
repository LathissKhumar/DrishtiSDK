package io.drishti.core

import kotlin.test.*

class DrishtiDiagramTest {

    @Test
    fun hapticsReturnsOutputWhenRendererRegistered() {
        val renderer = StubHapticsRenderer()
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = listOf(renderer),
            pipeline = Pipeline()
        )
        val output = diagram.haptics()
        assertNotNull(output)
    }

    @Test
    fun hapticsReturnsFailureWhenNoRendererRegistered() {
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        val result = diagram.haptics()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun audioReturnsOutputWhenRendererRegistered() {
        val renderer = StubAudioRenderer()
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = listOf(renderer),
            pipeline = Pipeline()
        )
        val output = diagram.audio()
        assertNotNull(output)
    }

    @Test
    fun audioReturnsFailureWhenNoRendererRegistered() {
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        val result = diagram.audio()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun voiceReturnsOutputWhenRendererRegistered() {
        val renderer = StubVoiceRenderer()
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = listOf(renderer),
            pipeline = Pipeline()
        )
        val output = diagram.voice()
        assertNotNull(output)
    }

    @Test
    fun voiceReturnsFailureWhenNoRendererRegistered() {
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        val result = diagram.voice()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun summaryReturnsTextOutput() {
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        val output = diagram.summary()
        assertNotNull(output.text)
        assertTrue(output.text.contains("0 nodes"))
    }

    @Test
    fun exploreReturnsExplorationSession() {
        val diagram = DrishtiDiagram(
            contentItems = listOf(TestFixtures.graphContent()),
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        val session = diagram.explore()
        assertNotNull(session)
    }

    @Test
    fun contentItemsAreAccessible() {
        val items = listOf(TestFixtures.graphContent(), TestFixtures.formulaContent())
        val diagram = DrishtiDiagram(
            contentItems = items,
            sceneGraph = SceneGraph(nodes = emptyList(), edges = emptyList()),
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        assertEquals(2, diagram.contentItems.size)
    }

    @Test
    fun sceneGraphIsAccessible() {
        val nodes = listOf(SceneNode.DataPointNode("n1", Point(0f, 0f), 1f, 2f))
        val graph = SceneGraph(nodes = nodes, edges = emptyList())
        val diagram = DrishtiDiagram(
            contentItems = emptyList(),
            sceneGraph = graph,
            renderers = emptyList(),
            pipeline = Pipeline()
        )
        assertEquals(1, diagram.sceneGraph.nodes.size)
    }
}
