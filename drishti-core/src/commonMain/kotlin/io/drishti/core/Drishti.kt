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
 * Main entry point for the Drishti SDK.
 *
 * Usage:
 * ```
 * val drishti = Drishti.Builder()
 *     .addDetector(GraphPlugin())
 *     .addRenderer(HapticsPlugin())
 *     .build()
 *
 * val diagram = drishti.readAsync(frame)
 * diagram.haptics()
 * ```
 */
public class Drishti private constructor(
    private val detectors: List<DetectorPlugin>,
    private val renderers: List<RendererPlugin>,
    private val pipeline: Pipeline
) {
    /**
     * Read visual content from a frame (suspend).
     */
    public suspend fun readAsync(frame: Frame): DrishtiDiagram {
        val contentItems = pipeline.detect(frame, detectors)
        val sceneGraph = pipeline.buildSceneGraph(contentItems)
        return DrishtiDiagram(contentItems, sceneGraph, renderers, pipeline)
    }

    public class Builder {
        private val detectors = mutableListOf<DetectorPlugin>()
        private val renderers = mutableListOf<RendererPlugin>()

        public fun addDetector(plugin: DetectorPlugin): Builder = apply { detectors.add(plugin) }
        public fun addRenderer(plugin: RendererPlugin): Builder = apply { renderers.add(plugin) }

        public fun build(): Drishti {
            require(detectors.isNotEmpty()) { "At least one DetectorPlugin is required" }
            val factories = detectors
                .filter { it.sceneNodeFactory != null }
                .associate { it.contentType to it.sceneNodeFactory!! }
            val pipeline = Pipeline(nodeFactories = factories)
            return Drishti(detectors.toList(), renderers.toList(), pipeline)
        }
    }
}
