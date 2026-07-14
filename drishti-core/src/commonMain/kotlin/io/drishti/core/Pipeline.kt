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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates the detection and scene-graph construction pipeline.
 *
 * The pipeline runs detectors concurrently on a [Frame], then builds a
 * [SceneGraph] with real spatial positions and meaningful edges between
 * related content items.
 *
 * @param config Pipeline configuration parameters.
 * @param onError Optional callback invoked when a detector throws an exception during detection.
 *   When null (default), exceptions are silently caught and the detector returns no results.
 *   When set, exceptions are reported via this callback before returning empty results.
 *   The callback receives the [DetectorPlugin] that threw and the [Exception] it threw.
 *   Fatal exceptions ([IllegalStateException], [IllegalArgumentException],
 *   [UnsupportedOperationException]) are always re-thrown regardless of this callback.
 */
public class Pipeline(
    private val config: PipelineConfig = PipelineConfig(),
    private val onError: ((DetectorPlugin, Exception) -> Unit)? = null
) {

    /**
     * Run all detectors on the frame concurrently.
     *
     * Each [DetectorPlugin] produces at most one [ContentItem] per frame.
     * Null results (no detection) are filtered out, items below the
     * configured [PipelineConfig.minConfidence] threshold are dropped,
     * and the result is truncated to [PipelineConfig.maxItemsPerFrame].
     *
     * @param frame The input image frame.
     * @param detectors List of detector plugins to run.
     * @return Non-null detected content items passing the confidence threshold,
     *   capped at [PipelineConfig.maxItemsPerFrame].
     */
    public suspend fun detect(frame: Frame, detectors: List<DetectorPlugin>): List<ContentItem> {
        if (frame.data == null || frame.data.isEmpty()) {
            return emptyList()
        }
        val detected = coroutineScope {
            detectors.map { detector ->
                async {
                    try {
                        detector.detect(frame)
                    } catch (_: CancellationException) {
                        throw CancellationException("Pipeline cancelled during detection")
                    } catch (e: Exception) {
                        when (e) {
                            is IllegalStateException,
                            is IllegalArgumentException,
                            is UnsupportedOperationException -> throw e
                            else -> {
                                onError?.invoke(detector, e)
                                null
                            }
                        }
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .filter { it.confidence >= config.minConfidence }
        }
        return detected.take(config.maxItemsPerFrame)
    }

    /**
     * Build a scene graph from detected content items.
     *
     * Each content type is mapped to a positioned [SceneNode]:
     * - [GraphContent] → [SceneNode.DataPointNode] positioned at the centroid of its data points.
     * - [FormulaContent] → [SceneNode.TextNode] positioned at the bounding box center.
     * - [MoleculeContent] → [SceneNode.TextNode] positioned at the centroid of its atoms.
     * - [ShapeContent] → [SceneNode.ShapeNode] positioned by detection order.
     * - [TableContent] → [SceneNode.TextNode] positioned by detection order.
     * - Other types → [SceneNode.TextNode] positioned by detection order.
     *
     * Edges are generated using three heuristics:
     * 1. **Spatial proximity** – items within [spatialThreshold] distance.
     * 2. **Semantic complement** – complementary types (formula ↔ graph, etc.).
     * 3. **Temporal order** – sequential detection-order edges.
     *
     * Scene [SceneBounds] are computed from the extent of all node positions.
     *
     * @param items Detected content items from [detect].
     * @return A fully-connected [SceneGraph] with positions, edges, and bounds.
     */
    public fun buildSceneGraph(items: List<ContentItem>): SceneGraph {
        if (items.isEmpty()) {
            return SceneGraph(
                nodes = emptyList(),
                edges = emptyList(),
                bounds = SceneBounds(0f, 0f)
            )
        }

        val nodes = mutableListOf<SceneNode>()

        items.forEachIndexed { index, item ->
            when (item) {
                is GraphContent -> buildGraphNode(item, index, nodes)
                is FormulaContent -> buildFormulaNode(item, index, nodes)
                is MoleculeContent -> buildMoleculeNode(item, index, nodes)
                is ShapeContent -> buildShapeNode(item, index, nodes)
                is TableContent -> buildTableNode(item, index, nodes)
                else -> buildGenericNode(item, index, nodes)
            }
        }

        val edges = generateEdges(items, nodes, config)
        val bounds = computeBounds(nodes)

        return SceneGraph(
            nodes = nodes,
            edges = edges,
            bounds = bounds
        )
    }
}
