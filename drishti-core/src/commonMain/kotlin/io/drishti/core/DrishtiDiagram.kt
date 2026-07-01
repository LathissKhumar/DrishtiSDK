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
    public fun haptics(): Result<HapticOutput> {
        val hapticRenderer = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No haptic renderer registered. Add HapticsPlugin to Drishti.Builder."))
        return try {
            Result.success(hapticRenderer.renderHaptic(contentItems))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    public fun audio(): Result<AudioOutput> {
        val audioRenderer = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No audio renderer registered. Add AudioPlugin to Drishti.Builder."))
        return try {
            Result.success(audioRenderer.renderAudio(contentItems))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    public fun voice(): Result<VoiceOutput> {
        val voiceRenderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return Result.failure(IllegalStateException("No voice renderer registered. Add VoicePlugin to Drishti.Builder."))
        return try {
            Result.success(voiceRenderer.renderVoice(contentItems))
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
