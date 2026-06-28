package io.drishti.vision

import io.drishti.core.Frame

/**
 * Ring buffer for real-time camera frames.
 *
 * Maintains a fixed-capacity circular buffer that overwrites the oldest
 * frame when capacity is reached. Frames are accessible newest-first.
 *
 * @param capacity Maximum number of frames to hold.
 */
class FrameBuffer(private val capacity: Int = 30) {
    private val buffer = mutableListOf<Frame>()
    private var head = 0

    /**
     * Add a frame to the buffer.
     *
     * If the buffer is not yet at capacity, the frame is appended.
     * Otherwise, the frame overwrites the oldest entry and the head advances.
     */
    fun add(frame: Frame) {
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
    fun latest(): Frame? = buffer.lastOrNull()

    /**
     * Get frame at index (0 = newest).
     *
     * @param index Zero-based offset from the most recent frame.
     * @return The frame at the given index, or null if out of bounds.
     */
    fun get(index: Int): Frame? {
        if (index < 0 || index >= buffer.size) return null
        val actualIndex = (head + index) % buffer.size
        return buffer.getOrNull(actualIndex)
    }

    /**
     * Get all frames in order (newest first).
     */
    fun getAll(): List<Frame> {
        return buffer.reversed()
    }

    /**
     * Clear the buffer and reset the head pointer.
     */
    fun clear() {
        buffer.clear()
        head = 0
    }

    /**
     * Current number of frames in the buffer.
     */
    fun size(): Int = buffer.size

    /**
     * Whether the buffer has reached its maximum capacity.
     */
    fun isFull(): Boolean = buffer.size >= capacity
}
