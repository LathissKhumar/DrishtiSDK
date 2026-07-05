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

/**
 * Default scale factor for proximity-based edge generation.
 * Override to customize sensitivity.
 */
public object EdgeGeneratorDefaults {
    public var proximityScale: Float = 200f
}

public fun generateEdges(
    items: List<ContentItem>, 
    nodes: List<SceneNode>,
    config: PipelineConfig
): List<SceneEdge> {
    val edges = mutableListOf<SceneEdge>()

    // 1. Spatial proximity edges
    edges.addAll(generateSpatialEdges(nodes, config.spatialThreshold))

    // 2. Containment edges (bounding box overlap)
    edges.addAll(generateContainmentEdges(items, nodes, config.containmentOverlapRatio))

    // 3. Semantic complement edges (formula ↔ graph, etc.)
    edges.addAll(generateSemanticEdges(items, nodes))

    // 4. Temporal / detection-order edges
    edges.addAll(generateTemporalEdges(nodes))

    // Deduplicate: if the same (source, target) pair has multiple edge types,
    // keep the strongest one.
    return deduplicateEdges(edges)
}

/**
 * Create [EdgeType.SPATIAL] edges between nodes whose centers are within
 * [spatialThreshold] of each other.
 */
public fun generateSpatialEdges(nodes: List<SceneNode>, spatialThreshold: Float): List<SceneEdge> {
    val edges = mutableListOf<SceneEdge>()
    for (i in nodes.indices) {
        for (j in i + 1 until nodes.size) {
            val dist = distance(nodes[i].position, nodes[j].position)
            if (dist <= spatialThreshold) {
                val weight = 1f - (dist / spatialThreshold)
                edges.add(
                    SceneEdge(
                        sourceId = nodes[i].id,
                        targetId = nodes[j].id,
                        edgeType = EdgeType.SPATIAL,
                        weight = weight
                    )
                )
            }
        }
    }
    return edges
}

/**
 * Create [EdgeType.CONTAINS] edges when one item's bounding box
 * significantly overlaps another item's bounding box.
 */
public fun generateContainmentEdges(
    items: List<ContentItem>, 
    nodes: List<SceneNode>, 
    containmentOverlapRatio: Float
): List<SceneEdge> {
    val edges = mutableListOf<SceneEdge>()
    val boundsList = items.map { estimateBoundingBox(it) }

    for (i in items.indices) {
        for (j in i + 1 until items.size) {
            val bbI = boundsList[i] ?: continue
            val bbJ = boundsList[j] ?: continue
            val overlap = computeOverlapArea(bbI, bbJ)
            val smallerArea = minOf(bbI.width * bbI.height, bbJ.width * bbJ.height)
            if (smallerArea > 0f && overlap / smallerArea >= containmentOverlapRatio) {
                val weight = (overlap / smallerArea).coerceIn(0f, 1f)
                val (contained, container) = if (bbI.width * bbI.height <= bbJ.width * bbJ.height) {
                    nodes[i].id to nodes[j].id
                } else {
                    nodes[j].id to nodes[i].id
                }
                edges.add(
                    SceneEdge(
                        sourceId = contained,
                        targetId = container,
                        edgeType = EdgeType.CONTAINS,
                        weight = weight
                    )
                )
            }
        }
    }
    return edges
}

/**
 * Create [EdgeType.SEMANTIC] edges between complementary content types.
 *
 * Complementary pairs:
 * - FORMULA ↔ GRAPH (formula describes graph data)
 * - FORMULA ↔ MOLECULE (formula names molecular structure)
 * - GRAPH ↔ TABLE (table provides graph data)
 * - MOLECULE ↔ SHAPE (molecular geometry)
 */
public fun generateSemanticEdges(items: List<ContentItem>, nodes: List<SceneNode>): List<SceneEdge> {
    val edges = mutableListOf<SceneEdge>()
    val complementaryPairs = setOf(
        ContentType.FORMULA to ContentType.GRAPH,
        ContentType.FORMULA to ContentType.MOLECULE,
        ContentType.GRAPH to ContentType.TABLE,
        ContentType.MOLECULE to ContentType.SHAPE
    )

    for (i in items.indices) {
        for (j in i + 1 until items.size) {
            val pair = items[i].contentType to items[j].contentType
            val reversePair = items[j].contentType to items[i].contentType
            if (pair in complementaryPairs || reversePair in complementaryPairs) {
                val dist = distance(nodes[i].position, nodes[j].position)
                val proximity = 1f / (1f + dist / EdgeGeneratorDefaults.proximityScale)
                val weight = minOf(items[i].confidence, items[j].confidence) * proximity
                edges.add(
                    SceneEdge(
                        sourceId = nodes[i].id,
                        targetId = nodes[j].id,
                        edgeType = EdgeType.SEMANTIC,
                        weight = weight
                    )
                )
            }
        }
    }
    return edges
}

/**
 * Create [EdgeType.TEMPORAL] edges connecting nodes in detection order.
 *
 * Each node is connected to the next one detected, forming a chain.
 */
public fun generateTemporalEdges(nodes: List<SceneNode>): List<SceneEdge> {
    val edges = mutableListOf<SceneEdge>()
    for (i in 0 until nodes.size - 1) {
        val dist = distance(nodes[i].position, nodes[i + 1].position)
        val proximity = 1f / (1f + dist / EdgeGeneratorDefaults.proximityScale)
        val weight = (0.5f + 0.5f * proximity).coerceIn(0.1f, 1.0f)
        edges.add(
            SceneEdge(
                sourceId = nodes[i].id,
                targetId = nodes[i + 1].id,
                edgeType = EdgeType.TEMPORAL,
                weight = weight
            )
        )
    }
    return edges
}

/**
 * Deduplicate edges, keeping one edge per (source, target, edgeType).
 *
 * When multiple edges of the *same* type exist between a pair, the
 * one with the highest weight is kept. Different edge types between
 * the same pair are all preserved (e.g., a pair can have both a
 * SPATIAL and a TEMPORAL edge).
 */
public fun deduplicateEdges(edges: List<SceneEdge>): List<SceneEdge> {
    val best = mutableMapOf<String, SceneEdge>()
    for (edge in edges) {
        val key = "${edge.sourceId}-${edge.targetId}-${edge.edgeType}"
        val existing = best[key]
        if (existing == null || edge.weight > existing.weight) {
            best[key] = edge
        }
    }
    return best.values.toList()
}
