package io.drishti.core

import kotlinx.serialization.Serializable

/**
 * Types of content that can be detected.
 */
@Serializable
enum class ContentType {
    GRAPH,
    FORMULA,
    MOLECULE,
    SHAPE,
    TABLE,
    CUSTOM
}
