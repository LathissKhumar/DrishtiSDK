package io.drishti.core

/**
 * Represents a processed visual diagram with accessible outputs.
 */
class DrishtiDiagram(
    val contentItems: List<ContentItem>,
    val sceneGraph: SceneGraph,
    private val renderers: List<RendererPlugin>,
    private val pipeline: Pipeline
) {
    fun haptics(): Result<HapticOutput> {
        val hapticRenderer = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No haptic renderer registered. Add HapticsPlugin to Drishti.Builder."))
        return Result.success(hapticRenderer.renderHaptic(contentItems))
    }

    fun audio(): Result<AudioOutput> {
        val audioRenderer = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No audio renderer registered. Add AudioPlugin to Drishti.Builder."))
        return Result.success(audioRenderer.renderAudio(contentItems))
    }

    fun voice(): Result<VoiceOutput> {
        val voiceRenderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No voice renderer registered. Add VoicePlugin to Drishti.Builder."))
        return Result.success(voiceRenderer.renderVoice(contentItems))
    }

    /**
     * Get a text summary of the diagram.
     */
    fun summary(): TextOutput {
        return TextOutput(sceneGraph.describe())
    }

    /**
     * Interactive exploration mode.
     */
    fun explore(): ExplorationSession {
        return ExplorationSession(contentItems, renderers)
    }
}
