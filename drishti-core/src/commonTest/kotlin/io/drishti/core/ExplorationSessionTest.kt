package io.drishti.core

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ExplorationSessionTest {

    @Test
    fun nextReturnsItemWhenAvailable() = runTest {
        val items = listOf(TestFixtures.graphContent(), TestFixtures.formulaContent())
        val session = ExplorationSession(items, emptyList())
        val result = session.next()
        assertTrue(result is ExplorationResult.Item)
        assertEquals(ContentType.GRAPH, (result as ExplorationResult.Item).item.contentType)
    }

    @Test
    fun nextAdvancesPosition() = runTest {
        val items = listOf(TestFixtures.graphContent(), TestFixtures.formulaContent())
        val session = ExplorationSession(items, emptyList())
        session.next()
        val position = session.position()
        assertEquals(1, position.current)
        assertEquals(2, position.total)
    }

    @Test
    fun nextReturnsEndWhenExhausted() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        session.next()
        val result = session.next()
        assertTrue(result is ExplorationResult.End)
    }

    @Test
    fun previousReturnsBeginningWhenAtStart() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        val result = session.previous()
        assertTrue(result is ExplorationResult.Beginning)
    }

    @Test
    fun previousReturnsItemWhenAvailable() = runTest {
        val items = listOf(TestFixtures.graphContent(), TestFixtures.formulaContent())
        val session = ExplorationSession(items, emptyList())
        session.next()
        session.next()
        val result = session.previous()
        assertTrue(result is ExplorationResult.Item)
    }

    @Test
    fun positionInitiallyZero() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        val position = session.position()
        assertEquals(0, position.current)
        assertEquals(1, position.total)
    }

    @Test
    fun positionWithEmptyItems() = runTest {
        val session = ExplorationSession(emptyList(), emptyList())
        val position = session.position()
        assertEquals(0, position.current)
        assertEquals(0, position.total)
    }

    @Test
    fun hapticReturnsNullWhenNoRenderer() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        assertNull(session.haptic())
    }

    @Test
    fun hapticReturnsOutputWhenRendererAvailable() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val renderer = StubHapticsRenderer()
        val session = ExplorationSession(items, listOf(renderer))
        session.next()
        assertNotNull(session.haptic())
    }

    @Test
    fun hapticReturnsNullWhenBeyondEnd() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        session.next()
        session.next()
        assertNull(session.haptic())
    }

    @Test
    fun audioReturnsNullWhenNoRenderer() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        assertNull(session.audio())
    }

    @Test
    fun audioReturnsOutputWhenRendererAvailable() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val renderer = StubAudioRenderer()
        val session = ExplorationSession(items, listOf(renderer))
        session.next()
        assertNotNull(session.audio())
    }

    @Test
    fun voiceReturnsNullWhenNoRenderer() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val session = ExplorationSession(items, emptyList())
        assertNull(session.voice())
    }

    @Test
    fun voiceReturnsOutputWhenRendererAvailable() = runTest {
        val items = listOf(TestFixtures.graphContent())
        val renderer = StubVoiceRenderer()
        val session = ExplorationSession(items, listOf(renderer))
        session.next()
        assertNotNull(session.voice())
    }

    @Test
    fun descriptionForGraph() = runTest {
        val items = listOf(TestFixtures.graphContent(dataPoints = listOf(DataPoint(1f, 2f), DataPoint(3f, 4f))))
        val session = ExplorationSession(items, emptyList())
        val result = session.next() as ExplorationResult.Item
        assertTrue(result.description.contains("Line_chart"))
        assertTrue(result.description.contains("2 points"))
    }

    @Test
    fun descriptionForFormula() = runTest {
        val items = listOf(TestFixtures.formulaContent(expression = "x + y"))
        val session = ExplorationSession(items, emptyList())
        val result = session.next() as ExplorationResult.Item
        assertTrue(result.description.contains("Algebraic"))
        assertTrue(result.description.contains("x + y"))
    }

    @Test
    fun descriptionForMolecule() = runTest {
        val items = listOf(TestFixtures.moleculeContent(name = "Water", atoms = listOf(Atom(0, "O", Point(0f, 0f)), Atom(1, "H", Point(1f, 1f)), Atom(2, "H", Point(2f, 2f)))))
        val session = ExplorationSession(items, emptyList())
        val result = session.next() as ExplorationResult.Item
        assertTrue(result.description.contains("Water"))
        assertTrue(result.description.contains("3 atoms"))
    }

    @Test
    fun descriptionForMoleculeEmptyName() = runTest {
        val items = listOf(TestFixtures.moleculeContent(name = "", atoms = listOf(Atom(0, "C", Point(0f, 0f)))))
        val session = ExplorationSession(items, emptyList())
        val result = session.next() as ExplorationResult.Item
        assertTrue(result.description.contains("Unknown"))
        assertTrue(result.description.contains("1 atoms"))
    }

    @Test
    fun descriptionForGenericContent() = runTest {
        val items = listOf(ShapeContent(ShapeType.CIRCLE, 100f, 35f))
        val session = ExplorationSession(items, emptyList())
        val result = session.next() as ExplorationResult.Item
        assertEquals("Shape", result.description)
    }
}

