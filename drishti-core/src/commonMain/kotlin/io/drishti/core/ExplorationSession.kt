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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interactive exploration session for a diagram.
 *
 * Thread-safe navigation through content items using [Mutex] to prevent
 * race conditions when multiple coroutines explore simultaneously.
 *
 * @property contentItems The list of content items to explore.
 * @property renderers Available renderers for haptic/audio/voice output.
 */
public class ExplorationSession(
    private val contentItems: List<ContentItem>,
    private val renderers: List<RendererPlugin>
) {
    private val mutex = Mutex()
    private var currentItemIndex = -1
    private var currentElementIndex = -1

    /**
     * Move to the next element.
     *
     * @return [ExplorationResult.Item] with the next item and description,
     *         or [ExplorationResult.End] if already at the end.
     */
    public suspend fun next(): ExplorationResult = mutex.withLock {
        if (contentItems.isEmpty()) {
            return@withLock ExplorationResult.End
        }
        if (currentItemIndex >= contentItems.size - 1) {
            currentItemIndex = contentItems.size
            return@withLock ExplorationResult.End
        }
        currentItemIndex++
        currentElementIndex = -1
        val item = contentItems[currentItemIndex]
        ExplorationResult.Item(item, describeItem(item))
    }

    /**
     * Move to the previous element.
     *
     * @return [ExplorationResult.Item] with the previous item and description,
     *         or [ExplorationResult.Beginning] if already at the start.
     */
    public suspend fun previous(): ExplorationResult = mutex.withLock {
        if (contentItems.isEmpty()) {
            return@withLock ExplorationResult.Beginning
        }
        if (currentItemIndex <= 0) {
            currentItemIndex = -1
            return@withLock ExplorationResult.Beginning
        }
        currentItemIndex--
        currentElementIndex = -1
        val item = contentItems[currentItemIndex]
        ExplorationResult.Item(item, describeItem(item))
    }

    /**
     * Move to the next element within the current content item.
     */
    public suspend fun nextElement(): ExplorationResult = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock ExplorationResult.End
        }
        val item = contentItems[currentItemIndex]
        val maxElements = getElementCount(item)
        if (maxElements == 0 || currentElementIndex >= maxElements - 1) {
            currentElementIndex = maxElements
            return@withLock ExplorationResult.End
        }
        currentElementIndex++
        ExplorationResult.Item(item, describeElement(item, currentElementIndex))
    }

    /**
     * Move to the previous element within the current content item.
     */
    public suspend fun previousElement(): ExplorationResult = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock ExplorationResult.Beginning
        }
        val item = contentItems[currentItemIndex]
        if (currentElementIndex <= 0) {
            currentElementIndex = -1
            return@withLock ExplorationResult.Beginning
        }
        currentElementIndex--
        ExplorationResult.Item(item, describeElement(item, currentElementIndex))
    }

    /**
     * Get the position of the element within the current item.
     */
    public suspend fun elementPosition(): ExplorationPosition = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock ExplorationPosition(0, 0)
        }
        val item = contentItems[currentItemIndex]
        val maxElements = getElementCount(item)
        ExplorationPosition(
            current = (currentElementIndex + 1).coerceIn(0, maxElements),
            total = maxElements
        )
    }

    /**
     * Get current position info.
     *
     * @return [ExplorationPosition] with current index and total count.
     */
    public suspend fun position(): ExplorationPosition = mutex.withLock {
        ExplorationPosition(
            current = (currentItemIndex + 1).coerceIn(0, contentItems.size),
            total = contentItems.size
        )
    }

    /**
     * Render current item with haptic feedback.
     *
     * @return [HapticOutput] if a renderer is available and item exists, null otherwise.
     */
    public suspend fun haptic(): HapticOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock null
        }
        val renderer = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderHaptic(listOf(contentItems[currentItemIndex]))
    }

    /**
     * Render current item with audio.
     *
     * @return [AudioOutput] if a renderer is available and item exists, null otherwise.
     */
    public suspend fun audio(): AudioOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock null
        }
        val renderer = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderAudio(listOf(contentItems[currentItemIndex]))
    }

    /**
     * Render current item with voice.
     *
     * @return [VoiceOutput] if a renderer is available and item exists, null otherwise.
     */
    public suspend fun voice(): VoiceOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock null
        }
        val renderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderVoice(listOf(contentItems[currentItemIndex]))
    }

    /**
     * Render exploration feedback using haptics.
     */
    public suspend fun exploreHaptic(direction: ExplorationDirection): HapticOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) return@withLock null
        val renderer = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderExplorationHaptic(contentItems[currentItemIndex], direction, currentElementIndex)
    }

    /**
     * Render exploration feedback using audio.
     */
    public suspend fun exploreAudio(direction: ExplorationDirection): AudioOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) return@withLock null
        val renderer = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderExplorationAudio(contentItems[currentItemIndex], direction, currentElementIndex)
    }

    /**
     * Render exploration feedback using voice.
     */
    public suspend fun exploreVoice(direction: ExplorationDirection): VoiceOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) return@withLock null
        val renderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderExplorationVoice(contentItems[currentItemIndex], direction, currentElementIndex)
    }

    private fun getElementCount(item: ContentItem): Int = when (item) {
        is GraphContent -> item.dataPoints.size
        is FormulaContentItem -> item.symbols.size
        is MoleculeContent -> item.atoms.size
        else -> 0
    }

    private fun describeElement(item: ContentItem, index: Int): String = when (item) {
        is GraphContent -> {
            val point = item.dataPoints.getOrNull(index)
            if (point != null) {
                "Point ${index + 1} of ${item.dataPoints.size}: x = ${point.x}, y = ${point.y}${point.label?.let { ", label = $it" } ?: ""}"
            } else ""
        }
        is FormulaContentItem -> {
            val symbol = item.symbols.getOrNull(index)
            if (symbol != null) {
                "Symbol ${index + 1} of ${item.symbols.size}: ${symbol.value}"
            } else ""
        }
        is MoleculeContent -> {
            val atom = item.atoms.getOrNull(index)
            if (atom != null) {
                "Atom ${index + 1} of ${item.atoms.size}: ${atom.element} at x = ${atom.position.x}, y = ${atom.position.y}"
            } else ""
        }
        else -> ""
    }

    private fun describeItem(item: ContentItem): String {
        return when (item) {
            is GraphContent -> "${item.graphType.name.lowercase().replaceFirstChar { it.uppercase() }} with ${item.dataPoints.size} points"
            is FormulaContentItem -> "${item.formulaType.name.lowercase().replaceFirstChar { it.uppercase() }} formula: ${item.expression}"
            is MoleculeContent -> "Molecule: ${item.name.ifEmpty { "Unknown" }} with ${item.atoms.size} atoms"
            else -> item.contentType.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

public sealed class ExplorationResult {
    public data class Item(val item: ContentItem, val description: String) : ExplorationResult()
    public data object End : ExplorationResult()
    public data object Beginning : ExplorationResult()
}

public data class ExplorationPosition(
    val current: Int,
    val total: Int
)
