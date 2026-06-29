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

class VisionPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    private val detector = VisionDetector()
    private val renderer = VisionRenderer()

    override val name = "vision"
    override val contentType = ContentType.SHAPE
    override val confidence = detector.confidence

    override suspend fun detect(frame: Frame): ContentItem? {
        return detector.detect(frame)
    }

    fun detectAll(frame: Frame): List<ContentItem> {
        return detector.detectAll(frame)
    }

    fun extractFeatures(frame: Frame): VisionFeatures {
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
        return renderer.renderHaptic(listOf(item))
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
        return renderer.renderAudio(listOf(item))
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
        return renderer.renderVoice(listOf(item))
    }
}
