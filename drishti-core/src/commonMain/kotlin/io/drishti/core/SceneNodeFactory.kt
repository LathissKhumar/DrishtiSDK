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
 * Factory interface for building [SceneNode]s from custom [ContentItem] types.
 *
 * Implement this interface on your [DetectorPlugin] to give the [Pipeline]
 * first-class SceneGraph support for your content type. Without this,
 * custom content falls through to a generic [SceneNode.TextNode] with
 * only the type name — no spatial positioning, no meaningful node shape.
 *
 * Example implementation for a circuit diagram plugin:
 * ```kotlin
 * class CircuitPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
 *     override val contentType = ContentType.Custom("circuit")
 *     override val confidence = 0.75f
 *     override val name = "CircuitPlugin"
 *
 *     override val sceneNodeFactory = SceneNodeFactory { item, index, nodes ->
 *         val circuit = item as CircuitContent
 *         // Position at the bounding box center
 *         val cx = circuit.bounds.x + circuit.bounds.width / 2f
 *         val cy = circuit.bounds.y + circuit.bounds.height / 2f
 *         nodes.add(
 *             SceneNode.ShapeNode(
 *                 id = "circuit-$index",
 *                 position = Point(cx, cy),
 *                 shapeType = ShapeType.RECTANGLE
 *             )
 *         )
 *     }
 *     // ... detect(), renderHaptic(), etc.
 * }
 * ```
 *
 * @see DetectorPlugin.sceneNodeFactory
 * @see Pipeline.buildSceneGraph
 */
public fun interface SceneNodeFactory {
    /**
     * Build one or more [SceneNode]s from a detected [ContentItem].
     *
     * Implementations should add nodes to the [nodes] list. The [index]
     * is the item's position in the detection-order list (useful for
     * generating unique node IDs and fallback positions via [orderPosition]).
     *
     * @param item The detected content item to build nodes for.
     * @param index Detection-order index (0-based).
     * @param nodes Mutable list to add generated nodes to.
     */
    public fun buildNodes(item: ContentItem, index: Int, nodes: MutableList<SceneNode>)
}
