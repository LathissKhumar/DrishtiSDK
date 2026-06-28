package io.drishti.core

import kotlin.test.*

class DrishtiTest {

    @Test
    fun builderWithNoPluginsThrows() {
        assertFailsWith<IllegalArgumentException> {
            Drishti.Builder().build()
        }
    }

    @Test
    fun builderWithDetectorAndRenderer() {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.GRAPH))
            .addRenderer(StubHapticsRenderer())
            .build()
        assertNotNull(drishti)
    }

    @Test
    fun readAsyncReturnsDiagram() = kotlinx.coroutines.test.runTest {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.GRAPH, TestFixtures.graphContent()))
            .addRenderer(StubHapticsRenderer())
            .build()
        val frame = TestFixtures.frame()
        val diagram = drishti.readAsync(frame)
        assertNotNull(diagram)
        assertEquals(1, diagram.contentItems.size)
    }

    @Test
    fun readAsyncWithNoMatchingDetectors() = kotlinx.coroutines.test.runTest {
        val drishti = Drishti.Builder()
            .addDetector(StubDetector(ContentType.FORMULA))
            .build()
        val frame = TestFixtures.frame()
        val diagram = drishti.readAsync(frame)
        assertNotNull(diagram)
        assertTrue(diagram.contentItems.isEmpty())
    }
}
