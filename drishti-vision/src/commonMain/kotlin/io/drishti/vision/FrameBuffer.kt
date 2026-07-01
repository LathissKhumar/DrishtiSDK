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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe ring buffer for real-time camera frames.
 *
 * Maintains a fixed-capacity circular buffer that overwrites the oldest
 * frame when capacity is reached. Frames are accessible newest-first.
 * All public methods are guarded by a [Mutex] for safe concurrent access.
 *
 * @param capacity Maximum number of frames to hold.
 */
public class FrameBuffer(private val capacity: Int = 30) {
    init {
        require(capacity > 0) { "FrameBuffer capacity must be positive, was $capacity" }
    }

    private val buffer = mutableListOf<Frame>()
    private var head = 0
    private val mutex = Mutex()

    /**
     * Add a frame to the buffer.
     *
     * If the buffer is not yet at capacity, the frame is appended.
     * Otherwise, the frame overwrites the oldest entry and the head advances.
     */
    public suspend fun add(frame: Frame): Unit = mutex.withLock {
        if (buffer.size < capacity) {
            buffer.add(frame)
        } else {
            buffer[head] = frame
            head = (head + 1) % capacity
        }
    }

    /**
     * Get the most recent frame.
     */
    public suspend fun latest(): Frame? = mutex.withLock {
        if (buffer.isEmpty()) return@withLock null
        buffer[(head + buffer.size - 1) % buffer.size]
    }

    /**
     * Get frame at index (0 = newest).
     *
     * @param index Zero-based offset from the most recent frame.
     * @return The frame at the given index, or null if out of bounds.
     */
    public suspend fun get(index: Int): Frame? = mutex.withLock {
        if (index < 0 || index >= buffer.size) return@withLock null
        buffer[(head + buffer.size - 1 - index) % buffer.size]
    }

    /**
     * Get all frames in order (newest first).
     */
    public suspend fun getAll(): List<Frame> = mutex.withLock {
        if (buffer.isEmpty()) return@withLock emptyList()
        List(buffer.size) { index -> buffer[(head + buffer.size - 1 - index) % buffer.size] }
    }

    /**
     * Clear the buffer and reset the head pointer.
     */
    public suspend fun clear(): Unit = mutex.withLock {
        buffer.clear()
        head = 0
    }

    /**
     * Current number of frames in the buffer.
     */
    public suspend fun size(): Int = mutex.withLock {
        buffer.size
    }

    /**
     * Whether the buffer has reached its maximum capacity.
     */
    public suspend fun isFull(): Boolean = mutex.withLock {
        buffer.size >= capacity
    }
}
