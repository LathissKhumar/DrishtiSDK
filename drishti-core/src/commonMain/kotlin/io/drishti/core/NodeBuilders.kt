package io.drishti.core

fun buildGraphNode(item: GraphContent, index: Int, nodes: MutableList<SceneNode>) {
    if (item.dataPoints.isNotEmpty()) {
        val centroidX = item.dataPoints.map { it.x }.average().toFloat()
        val centroidY = item.dataPoints.map { it.y }.average().toFloat()
        nodes.add(
            SceneNode.DataPointNode(
                id = "graph-$index",
                position = Point(centroidX, centroidY),
                x = centroidX,
                y = centroidY
            )
        )
    } else {
        // Fallback: use detection-order spacing
        nodes.add(
            SceneNode.DataPointNode(
                id = "graph-$index",
                position = orderPosition(index),
                x = orderPosition(index).x,
                y = orderPosition(index).y
            )
        )
    }
}

fun buildFormulaNode(item: FormulaContent, index: Int, nodes: MutableList<SceneNode>) {
    val position = if (item.symbols.isNotEmpty()) {
        val avgX = item.symbols.map { it.position.x }.average().toFloat()
        val avgY = item.symbols.map { it.position.y }.average().toFloat()
        Point(avgX, avgY)
    } else if (item.geometry?.boundingBox != null) {
        val bb = item.geometry.boundingBox
        Point(bb.x + bb.width / 2f, bb.y + bb.height / 2f)
    } else {
        orderPosition(index)
    }

    nodes.add(
        SceneNode.TextNode(
            id = "formula-$index",
            position = position,
            text = item.expression
        )
    )
}

fun buildMoleculeNode(item: MoleculeContent, index: Int, nodes: MutableList<SceneNode>) {
    val position = if (item.atoms.isNotEmpty()) {
        val avgX = item.atoms.map { it.position.x }.average().toFloat()
        val avgY = item.atoms.map { it.position.y }.average().toFloat()
        Point(avgX, avgY)
    } else if (item.geometry?.boundingBox != null) {
        val bb = item.geometry.boundingBox
        Point(bb.x + bb.width / 2f, bb.y + bb.height / 2f)
    } else {
        orderPosition(index)
    }

    nodes.add(
        SceneNode.TextNode(
            id = "molecule-$index",
            position = position,
            text = item.name.ifEmpty { "Molecule" }
        )
    )
}

fun buildShapeNode(item: ShapeContent, index: Int, nodes: MutableList<SceneNode>) {
    nodes.add(
        SceneNode.ShapeNode(
            id = "shape-$index",
            position = orderPosition(index),
            shapeType = item.shapeType
        )
    )
}

fun buildTableNode(item: TableContent, index: Int, nodes: MutableList<SceneNode>) {
    nodes.add(
        SceneNode.TextNode(
            id = "table-$index",
            position = orderPosition(index),
            text = "Table ${item.rows}x${item.columns}"
        )
    )
}

fun buildGenericNode(item: ContentItem, index: Int, nodes: MutableList<SceneNode>) {
    nodes.add(
        SceneNode.TextNode(
            id = "content-$index",
            position = orderPosition(index),
            text = item.contentType.name
        )
    )
}

/**
 * Compute a position for the nth item in detection order.
 * Items are laid out on a horizontal line with consistent spacing.
 */
fun orderPosition(index: Int): Point {
    val spacing = 150f
    val startX = 100f
    val y = 100f
    return Point(startX + index * spacing, y)
}
