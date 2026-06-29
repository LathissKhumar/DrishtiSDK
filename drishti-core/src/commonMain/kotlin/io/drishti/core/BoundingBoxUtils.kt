package io.drishti.core

import kotlin.math.hypot

/** Euclidean distance between two points. */
fun distance(a: Point, b: Point): Float {
    return hypot(a.x - b.x, a.y - b.y)
}

/**
 * Estimate the bounding box of a [ContentItem].
 *
 * Uses actual geometric data when available (graph data points,
 * formula symbols, molecule atoms), falling back to a default
 * size for items without position data.
 */
fun estimateBoundingBox(item: ContentItem): BoundingBox? {
    return when (item) {
        is GraphContent -> graphBounds(item)
        is FormulaContent -> formulaBounds(item)
        is MoleculeContent -> moleculeBounds(item)
        else -> null
    }
}

fun graphBounds(item: GraphContent): BoundingBox? {
    if (item.dataPoints.isEmpty()) return null
    val minX = item.dataPoints.minOf { it.x }
    val maxX = item.dataPoints.maxOf { it.x }
    val minY = item.dataPoints.minOf { it.y }
    val maxY = item.dataPoints.maxOf { it.y }
    return BoundingBox(minX, minY, maxX - minX, maxY - minY)
}

fun formulaBounds(item: FormulaContent): BoundingBox? {
    if (item.symbols.isEmpty()) return null
    val minX = item.symbols.minOf { it.boundingBox.x }
    val maxX = item.symbols.maxOf { it.boundingBox.x + it.boundingBox.width }
    val minY = item.symbols.minOf { it.boundingBox.y }
    val maxY = item.symbols.maxOf { it.boundingBox.y + it.boundingBox.height }
    return BoundingBox(minX, minY, maxX - minX, maxY - minY)
}

fun moleculeBounds(item: MoleculeContent): BoundingBox? {
    if (item.atoms.isEmpty()) return null
    val minX = item.atoms.minOf { it.position.x }
    val maxX = item.atoms.maxOf { it.position.x }
    val minY = item.atoms.minOf { it.position.y }
    val maxY = item.atoms.maxOf { it.position.y }
    val padding = 30f
    return BoundingBox(minX - padding, minY - padding, maxX - minX + 2 * padding, maxY - minY + 2 * padding)
}

/**
 * Compute the intersection area of two [BoundingBox] rectangles.
 */
fun computeOverlapArea(a: BoundingBox, b: BoundingBox): Float {
    val overlapX = maxOf(0f, minOf(a.x + a.width, b.x + b.width) - maxOf(a.x, b.x))
    val overlapY = maxOf(0f, minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y))
    return overlapX * overlapY
}

/**
 * Compute [SceneBounds] from the extent of all node positions.
 */
fun computeBounds(nodes: List<SceneNode>): SceneBounds {
    if (nodes.isEmpty()) return SceneBounds(0f, 0f)
    val minX = nodes.minOf { it.position.x }
    val maxX = nodes.maxOf { it.position.x }
    val minY = nodes.minOf { it.position.y }
    val maxY = nodes.maxOf { it.position.y }
    return if (nodes.size == 1) {
        SceneBounds(
            width = maxX + 100f,
            height = maxY + 100f
        )
    } else {
        SceneBounds(
            width = maxX - minX + 100f,
            height = maxY - minY + 100f
        )
    }
}
