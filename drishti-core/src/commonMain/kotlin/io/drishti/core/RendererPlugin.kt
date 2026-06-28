package io.drishti.core

interface RendererPlugin {
    val name: String
}

interface HapticsRenderer : RendererPlugin {
    fun renderHaptic(items: List<ContentItem>, focusIndex: Int = 0): HapticOutput

    fun renderExplorationHaptic(item: ContentItem, direction: ExplorationDirection): HapticOutput
}

interface AudioRenderer : RendererPlugin {
    fun renderAudio(items: List<ContentItem>, focusIndex: Int = 0): AudioOutput
    fun renderExplorationAudio(item: ContentItem, direction: ExplorationDirection): AudioOutput
}

interface VoiceOutputRenderer : RendererPlugin {
    fun renderVoice(items: List<ContentItem>, focusIndex: Int = 0): VoiceOutput
    fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection): VoiceOutput
}
