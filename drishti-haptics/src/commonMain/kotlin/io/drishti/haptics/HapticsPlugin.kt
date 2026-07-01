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

package io.drishti.haptics

import io.drishti.core.*

/**
 * Haptic renderer plugin facade.
 *
 * Provides both ContentItem-based rendering (via [HapticsRenderer] interface)
 * and SceneGraph-based rendering for direct pipeline integration.
 */
public class HapticsPlugin : HapticsRenderer {
    private val renderer = HapticRenderer()
    private val encoder = HapticEncoder()

    override val name: String = "haptics"

    // ── ContentItem facade (HapticsRenderer interface) ───────────────────

    /**
     * Render content items as haptic output.
     */
    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        return renderer.render(items, focusIndex)
    }

    /**
     * Render exploration sequence.
     */
    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        return renderer.renderExploration(item, direction, elementIndex)
    }

    // ── SceneGraph facade ────────────────────────────────────────────────

    /**
     * Render a [SceneGraph] directly into [HapticOutput].
     *
     * Each edge becomes a haptic pulse with intensity from weight and
     * waveform from edge type. Each node becomes a spatial anchor.
     */
    public fun renderSceneGraphHaptic(graph: SceneGraph): HapticOutput {
        return renderer.renderFromSceneGraph(graph)
    }

    /**
     * Render a [SceneGraph] into rich [HapticPatternDefinition]s.
     *
     * Returns one pattern per edge and one per node, each with
     * [HapticEventSpec] data mapping to VibrationEffect.Composition parameters.
     */
    public fun encodeSceneGraphPatterns(graph: SceneGraph): List<HapticPatternDefinition> {
        return renderer.renderSceneGraphPatterns(graph)
    }

    // ── Encoding ─────────────────────────────────────────────────────────

    /**
     * Encode haptic output to platform format (timing/amplitude arrays).
     */
    public fun encode(output: HapticOutput): EncodedPattern {
        return encoder.encode(output.pulses)
    }

    /**
     * Encode for VibrationEffect.Composition (API 30+).
     */
    public fun encodeComposition(output: HapticOutput): List<CompositionPrimitive> {
        return encoder.encodeComposition(output.pulses)
    }
}
