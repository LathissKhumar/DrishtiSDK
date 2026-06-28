package io.drishti.core

/**
 * Interface for content detection plugins.
 *
 * Implement this to add support for new content types.
 */
interface DetectorPlugin {
    /**
     * The type of content this plugin detects.
     */
    val contentType: ContentType

    /**
     * Confidence threshold for this detector.
     */
    val confidence: Float

    /**
     * Detect content in the given frame.
     *
     * @param frame The input image frame
     * @return Detected content item, or null if nothing detected
     */
    suspend fun detect(frame: Frame): ContentItem?
}
