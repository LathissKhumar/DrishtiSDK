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

public interface RendererPlugin {
    public val name: String
}

public interface HapticsRenderer : RendererPlugin {
    public fun renderHaptic(items: List<ContentItem>, focusIndex: Int = 0): HapticOutput

    public fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): HapticOutput
}

public interface AudioRenderer : RendererPlugin {
    public fun renderAudio(items: List<ContentItem>, focusIndex: Int = 0): AudioOutput
    public fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): AudioOutput
}

public interface VoiceOutputRenderer : RendererPlugin {
    public fun renderVoice(items: List<ContentItem>, focusIndex: Int = 0): VoiceOutput
    public fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int = -1
    ): VoiceOutput
}
