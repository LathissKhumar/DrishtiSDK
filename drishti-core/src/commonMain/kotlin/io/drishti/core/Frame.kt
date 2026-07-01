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

package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Input frame from camera, bitmap, or file.
 */
@Serializable
public data class Frame(
    val width: Int,
    val height: Int,
    val format: FrameFormat,
    val data: ByteArray? = null,
    val timestamp: Long = 0L
) {
    init {
        require(width > 0 && height > 0) { "Frame dimensions must be positive" }
    }

    public fun isNotEmpty(): Boolean = data != null && data.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Frame) return false
        return width == other.width && height == other.height && format == other.format &&
            data.contentEquals(other.data) && timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = 31 * (31 * width + height) + format.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

@Serializable
public enum class FrameFormat {
    YUV_420_888,
    RGB_888,
    GRAYSCALE,
    JPEG,
    PNG
}
