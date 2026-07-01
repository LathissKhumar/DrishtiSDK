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

package io.drishti.vision

import io.drishti.core.AudioOutput
import io.drishti.core.ContentType
import io.drishti.core.ContentItem
import io.drishti.core.DetectorPlugin
import io.drishti.core.ExplorationDirection
import io.drishti.core.Frame
import io.drishti.core.HapticOutput
import io.drishti.core.HapticsRenderer
import io.drishti.core.AudioRenderer
import io.drishti.core.VoiceOutput
import io.drishti.core.VoiceOutputRenderer

/** Plugin combining vision detection with multi-modal rendering. */
public class VisionPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    private val detector = VisionDetector()
    private val renderer = VisionRenderer()

    override val name: String = "vision"
    override val contentType: ContentType = ContentType.SHAPE
    override val confidence: Float = detector.confidence

    override suspend fun detect(frame: Frame): ContentItem? {
        return detector.detect(frame)
    }

    public fun detectAll(frame: Frame): List<ContentItem> {
        return detector.detectAll(frame)
    }

    public fun extractFeatures(frame: Frame): VisionFeatures {
        return detector.extractFeatures(frame)
    }

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        val filtered = if (focusIndex in items.indices) listOf(items[focusIndex]) else items
        return renderer.renderHaptic(filtered)
    }

    override fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        return renderer.renderExplorationHaptic(item, direction, elementIndex)
    }

    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        val filtered = if (focusIndex in items.indices) listOf(items[focusIndex]) else items
        return renderer.renderAudio(filtered)
    }

    override fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        return renderer.renderExplorationAudio(item, direction, elementIndex)
    }

    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        val filtered = if (focusIndex in items.indices) listOf(items[focusIndex]) else items
        return renderer.renderVoice(filtered)
    }

    override fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput {
        return renderer.renderExplorationVoice(item, direction, elementIndex)
    }
}
