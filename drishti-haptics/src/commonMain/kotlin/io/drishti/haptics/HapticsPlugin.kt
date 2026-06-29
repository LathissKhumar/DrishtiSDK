package io.drishti.haptics

import io.drishti.core.*

/**
 * Haptic renderer plugin facade.
 *
 * Provides both ContentItem-based rendering (via [HapticsRenderer] interface)
 * and SceneGraph-based rendering for direct pipeline integration.
 */
class HapticsPlugin : HapticsRenderer {
    private val renderer = HapticRenderer()
    private val encoder = HapticEncoder()

    override val name = "haptics"

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
    fun renderSceneGraphHaptic(graph: SceneGraph): HapticOutput {
        return renderer.renderFromSceneGraph(graph)
    }

    /**
     * Render a [SceneGraph] into rich [HapticPatternDefinition]s.
     *
     * Returns one pattern per edge and one per node, each with
     * [HapticEventSpec] data mapping to VibrationEffect.Composition parameters.
     */
    fun encodeSceneGraphPatterns(graph: SceneGraph): List<HapticPatternDefinition> {
        return renderer.renderSceneGraphPatterns(graph)
    }

    // ── Encoding ─────────────────────────────────────────────────────────

    /**
     * Encode haptic output to platform format (timing/amplitude arrays).
     */
    fun encode(output: HapticOutput): EncodedPattern {
        return encoder.encode(output.pulses)
    }

    /**
     * Encode for VibrationEffect.Composition (API 30+).
     */
    fun encodeComposition(output: HapticOutput): List<CompositionPrimitive> {
        return encoder.encodeComposition(output.pulses)
    }
}
