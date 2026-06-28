package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Input frame from camera, bitmap, or file.
 */
@Serializable
data class Frame(
    val width: Int,
    val height: Int,
    val format: FrameFormat,
    val data: ByteArray? = null,
    val timestamp: Long = 0L
) {
    init {
        require(width > 0 && height > 0) { "Frame dimensions must be positive" }
    }

    fun isNotEmpty(): Boolean = data != null && data.isNotEmpty()

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
enum class FrameFormat {
    YUV_420_888,
    RGB_888,
    JPEG,
    PNG
}
