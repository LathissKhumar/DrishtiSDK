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

package io.drishti.vision

import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FrameBufferTest {

    private fun frame(id: Int) = Frame(
        width = 640,
        height = 480,
        format = FrameFormat.RGB_888,
        data = ByteArray(0)
    )

    @Test
    fun initialStateIsEmpty() = runTest {
        val buffer = FrameBuffer()
        assertEquals(0, buffer.size())
        assertFalse(buffer.isFull())
        assertNull(buffer.latest())
    }

    @Test
    fun addIncreasesSize() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertEquals(1, buffer.size())
    }

    @Test
    fun latestReturnsNewestFrame() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        val latest = buffer.latest()
        assertNotNull(latest)
    }

    @Test
    fun getReturnsNewestFirst() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))
        assertNull(buffer.get(3))
    }

    @Test
    fun getReturnsNullForNegativeIndex() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertNull(buffer.get(-1))
    }

    @Test
    fun isFullWhenAtCapacity() = runTest {
        val buffer = FrameBuffer(capacity = 3)
        assertFalse(buffer.isFull())
        buffer.add(frame(1))
        buffer.add(frame(2))
        assertFalse(buffer.isFull())
        buffer.add(frame(3))
        assertTrue(buffer.isFull())
    }

    @Test
    fun ringBufferOverwritesOldest() = runTest {
        val buffer = FrameBuffer(capacity = 3)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        buffer.add(frame(4))
        assertEquals(3, buffer.size())
        assertTrue(buffer.isFull())
        assertNotNull(buffer.get(0))
        assertNotNull(buffer.get(1))
        assertNotNull(buffer.get(2))
    }

    @Test
    fun getAllReturnsNewestFirst() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.add(frame(3))
        val all = buffer.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun clearResetsBuffer() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        buffer.add(frame(2))
        buffer.clear()
        assertEquals(0, buffer.size())
        assertNull(buffer.latest())
        assertFalse(buffer.isFull())
    }

    @Test
    fun customCapacity() = runTest {
        val buffer = FrameBuffer(capacity = 1)
        buffer.add(frame(1))
        assertTrue(buffer.isFull())
        buffer.add(frame(2))
        assertEquals(1, buffer.size())
    }

    @Test
    fun multipleOverwrites() = runTest {
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
    fun sizeReturnsCorrectCount() = runTest {
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
    fun getOutOfRangeReturnsNull() = runTest {
        val buffer = FrameBuffer(capacity = 5)
        buffer.add(frame(1))
        assertNull(buffer.get(5))
        assertNull(buffer.get(100))
    }
}
