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
 * Represents a processed visual diagram with accessible outputs.
 */
public class DrishtiDiagram(
    public val contentItems: List<ContentItem>,
    public val sceneGraph: SceneGraph,
    private val renderers: List<RendererPlugin>,
    private val pipeline: Pipeline
) {
    /**
     * Render haptic feedback for the diagram.
     *
     * Produces vibration patterns that represent the spatial layout and structure
     * of the detected content items, allowing users to feel the diagram through
     * touch on supported devices.
     *
     * @return [Result.success] with [HapticOutput] containing vibration patterns,
     *         or [Result.failure] if no haptic renderer is registered or rendering fails
     */
    public fun haptics(): Result<HapticOutput> {
        val hapticRenderer = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No haptic renderer registered. Add HapticsPlugin to Drishti.Builder."))
        return try {
            Result.success(hapticRenderer.renderHaptic(contentItems))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Render spatial audio representation of the diagram.
     *
     * Produces a spatial audio mix where detected content items are positioned
     * in the stereo field according to their spatial coordinates, enabling users
     * to hear the layout of the diagram.
     *
     * @return [Result.success] with [AudioOutput] containing spatial audio data,
     *         or [Result.failure] if no audio renderer is registered or rendering fails
     */
    public fun audio(): Result<AudioOutput> {
        val audioRenderer = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No audio renderer registered. Add AudioPlugin to Drishti.Builder."))
        return try {
            Result.success(audioRenderer.renderAudio(contentItems))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate spoken description of the diagram content.
     *
     * Produces natural language text that describes the diagram's content,
     * structure, and key relationships, suitable for text-to-speech output.
     *
     * @return [Result.success] with [VoiceOutput] containing speech text,
     *         or [Result.failure] if no voice renderer is registered or rendering fails
     */
    public fun voice(): Result<VoiceOutput> {
        val voiceRenderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No voice renderer registered. Add VoicePlugin to Drishti.Builder."))
        return try {
            Result.success(voiceRenderer.renderVoice(contentItems))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a text summary of the diagram.
     */
    public fun summary(): TextOutput {
        return TextOutput(sceneGraph.describe())
    }

    /**
     * Interactive exploration mode.
     */
    public fun explore(): ExplorationSession {
        return ExplorationSession(contentItems, renderers)
    }
}
