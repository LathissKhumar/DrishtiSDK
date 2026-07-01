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

public fun buildGraphNode(item: GraphContent, index: Int, nodes: MutableList<SceneNode>) {
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

public fun buildFormulaNode(item: FormulaContent, index: Int, nodes: MutableList<SceneNode>) {
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

public fun buildMoleculeNode(item: MoleculeContent, index: Int, nodes: MutableList<SceneNode>) {
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
        SceneNode.ShapeNode(
            id = "molecule-$index",
            position = position,
            shapeType = ShapeType.POLYGON
        )
    )
}

public fun buildShapeNode(item: ShapeContent, index: Int, nodes: MutableList<SceneNode>) {
    val hasBounds = item.width > 0f && item.height > 0f
    val position = if (hasBounds) {
        Point(item.x + item.width / 2f, item.y + item.height / 2f)
    } else {
        orderPosition(index)
    }
    nodes.add(
        SceneNode.ShapeNode(
            id = "shape-$index",
            position = position,
            shapeType = item.shapeType
        )
    )
}

public fun buildTableNode(item: TableContent, index: Int, nodes: MutableList<SceneNode>) {
    nodes.add(
        SceneNode.TextNode(
            id = "table-$index",
            position = orderPosition(index),
            text = "Table ${item.rows}x${item.columns}"
        )
    )
}

public fun buildGenericNode(item: ContentItem, index: Int, nodes: MutableList<SceneNode>) {
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
public fun orderPosition(index: Int): Point {
    val spacing = 0.15f
    val startX = 0.1f
    val y = 0.1f
    val x = (startX + index * spacing).coerceAtMost(0.9f)
    return Point(x, y)
}
