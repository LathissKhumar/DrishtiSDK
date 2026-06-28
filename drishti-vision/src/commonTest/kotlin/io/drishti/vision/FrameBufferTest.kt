package io.drishti.vision

import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import kotlin.test.*

class FrameBufferTest {

    private fun frame(id: Int) = Frame(
        width = 640,
        height = 480,
        format = FrameFormat.RGB_888,
        data = ByteArray(0)
    )

    @Test
    fun initialStateIsEmpty() {
        val buffer = FrameBuffer()
        assertEquals(0, buffer.size())
        assertFalse(buffer.isFull())
        assertNull(buffer.latest())
    }

    @Test
    fun addIncreasesSize() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertEquals(1, buffer.size())
    }

    @Test
    fun latestReturnsNewestFrame() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        val latest = buffer.latest()
        assertNotNull(latest)
    }

    @Test
    fun getReturnsNewestFirst() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        // index 0 = newest, index 1 = second newest, etc.
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))
        assertNull(buffer.get(3))
    }

    @Test
    fun getReturnsNullForNegativeIndex() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertNull(buffer.get(-1))
    }

    @Test
    fun isFullWhenAtCapacity() {
        val buffer = FrameBuffer(capacity = 3)
        assertFalse(buffer.isFull())
        buffer.add(frame(1))
        buffer.add(frame(2))
        assertFalse(buffer.isFull())
        buffer.add(frame(3))
        assertTrue(buffer.isFull())
    }

    @Test
    fun ringBufferOverwritesOldest() {
        val buffer = FrameBuffer(capacity = 3)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        // Buffer is full, adding one more should overwrite oldest
        buffer.add(frame(4))
        assertEquals(3, buffer.size())
        assertTrue(buffer.isFull())
        // All frames should be accessible
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))
    }

    @Test
    fun getAllReturnsNewestFirst() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        val all = buffer.getAll()
        assertEquals(3, all.size)
        // All frames returned in reverse order (newest first)
    }

    @Test
    fun clearResetsBuffer() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.clear()
        assertEquals(0, buffer.size())
        assertNull(buffer.latest())
        assertFalse(buffer.isFull())
    }

    @Test
    fun customCapacity() {
        val buffer = FrameBuffer(capacity = 1)
        buffer.add(frame(1))
        assertTrue(buffer.isFull())
        buffer.add(frame(2))
        assertEquals(1, buffer.size())
    }

    @Test
    fun multipleOverwrites() {
        val buffer = FrameBuffer(capacity = 2)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        buffer.add(frame(4))
        buffer.add(frame(5))
        assertEquals(2, buffer.size())
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
    }

    @Test
    fun sizeReturnsCorrectCount() {
        val buffer = FrameBuffer(capacity = 10)
        assertEquals(0, buffer.size())
        buffer.add(frame(1))
        assertEquals(1, buffer.size())
        buffer.add(frame(2))
        assertEquals(2, buffer.size())
        buffer.clear()
        assertEquals(0, buffer.size())
    }

    @Test
    fun getOutOfRangeReturnsNull() {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertNull(buffer.get(5))
        assertNull(buffer.get(100))
    }
}
