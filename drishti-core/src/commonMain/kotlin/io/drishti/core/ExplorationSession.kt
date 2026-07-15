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
 * **Thread safety:** The [Mutex] is non-reentrant — if a coroutine already
 * holds the lock, a second `withLock` call from the same coroutine will
 * suspend rather than deadlock. This is safe because each suspend point
 * releases the coroutine, allowing the lock holder to complete.
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
    public suspend fun haptic(): HapticOutput? {
        val (renderer, item) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
                ?: return null
            r to contentItems[currentItemIndex]
        }
        return renderer.renderHaptic(listOf(item))
    }

    /**
     * Render current item with audio.
     *
     * @return [AudioOutput] if a renderer is available and item exists, null otherwise.
     */
    public suspend fun audio(): AudioOutput? {
        val (renderer, item) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
                ?: return null
            r to contentItems[currentItemIndex]
        }
        return renderer.renderAudio(listOf(item))
    }

    /**
     * Render current item with voice.
     *
     * @return [VoiceOutput] if a renderer is available and item exists, null otherwise.
     */
    public suspend fun voice(): VoiceOutput? {
        val (renderer, item) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
                ?: return null
            r to contentItems[currentItemIndex]
        }
        return renderer.renderVoice(listOf(item))
    }

    /**
     * Render exploration feedback using haptics.
     */
    public suspend fun exploreHaptic(direction: ExplorationDirection): HapticOutput? {
        val (renderer, item, elementIndex) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<HapticsRenderer>().firstOrNull()
                ?: return null
            Triple(r, contentItems[currentItemIndex], currentElementIndex)
        }
        return renderer.renderExplorationHaptic(item, direction, elementIndex)
    }

    /**
     * Render exploration feedback using audio.
     */
    public suspend fun exploreAudio(direction: ExplorationDirection): AudioOutput? {
        val (renderer, item, elementIndex) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<AudioRenderer>().firstOrNull()
                ?: return null
            Triple(r, contentItems[currentItemIndex], currentElementIndex)
        }
        return renderer.renderExplorationAudio(item, direction, elementIndex)
    }

    /**
     * Render exploration feedback using voice.
     */
    public suspend fun exploreVoice(direction: ExplorationDirection): VoiceOutput? {
        val (renderer, item, elementIndex) = mutex.withLock {
            if (currentItemIndex !in contentItems.indices) return null
            val r = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
                ?: return null
            Triple(r, contentItems[currentItemIndex], currentElementIndex)
        }
        return renderer.renderExplorationVoice(item, direction, elementIndex)
    }

    private fun getElementCount(item: ContentItem): Int = when (item) {
        is GraphContent -> item.dataPoints.size
        is FormulaContentItem -> item.symbols.size
        is MoleculeContent -> item.atoms.size
        is ShapeContent -> 1
        is TableContent -> item.rows * item.columns
        else -> 1
    }

    internal fun describeElement(item: ContentItem, index: Int): String = when (item) {
        is GraphContent -> {
            require(index in item.dataPoints.indices) {
                "Element index $index out of range for graph with ${item.dataPoints.size} points"
            }
            val point = item.dataPoints[index]
            "Point ${index + 1} of ${item.dataPoints.size}: x = ${point.x}, y = ${point.y}${point.label?.let { ", label = $it" } ?: ""}"
        }
        is FormulaContentItem -> {
            require(index in item.symbols.indices) {
                "Element index $index out of range for formula with ${item.symbols.size} symbols"
            }
            val symbol = item.symbols[index]
            "Symbol ${index + 1} of ${item.symbols.size}: ${symbol.value}"
        }
        is MoleculeContent -> {
            require(index in item.atoms.indices) {
                "Element index $index out of range for molecule with ${item.atoms.size} atoms"
            }
            val atom = item.atoms[index]
            "Atom ${index + 1} of ${item.atoms.size}: ${atom.element} at x = ${atom.position.x}, y = ${atom.position.y}"
        }
        else -> "${item.contentType.name} element ${index + 1}"
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
