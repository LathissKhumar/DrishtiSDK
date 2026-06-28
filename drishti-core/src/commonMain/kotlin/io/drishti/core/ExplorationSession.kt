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
class ExplorationSession(
    private val contentItems: List<ContentItem>,
    private val renderers: List<RendererPlugin>
) {
    private val mutex = Mutex()
    private var currentItemIndex = -1

    /**
     * Move to the next element.
     *
     * @return [ExplorationResult.Item] with the next item and description,
     *         or [ExplorationResult.End] if already at the end.
     */
    suspend fun next(): ExplorationResult = mutex.withLock {
        if (contentItems.isEmpty()) {
            return@withLock ExplorationResult.End
        }
        if (currentItemIndex >= contentItems.size - 1) {
            currentItemIndex = contentItems.size
            return@withLock ExplorationResult.End
        }
        currentItemIndex++
        val item = contentItems[currentItemIndex]
        ExplorationResult.Item(item, describeItem(item))
    }

    /**
     * Move to the previous element.
     *
     * @return [ExplorationResult.Item] with the previous item and description,
     *         or [ExplorationResult.Beginning] if already at the start.
     */
    suspend fun previous(): ExplorationResult = mutex.withLock {
        if (contentItems.isEmpty()) {
            return@withLock ExplorationResult.Beginning
        }
        if (currentItemIndex <= 0) {
            currentItemIndex = -1
            return@withLock ExplorationResult.Beginning
        }
        currentItemIndex--
        val item = contentItems[currentItemIndex]
        ExplorationResult.Item(item, describeItem(item))
    }

    /**
     * Get current position info.
     *
     * @return [ExplorationPosition] with current index and total count.
     */
    suspend fun position(): ExplorationPosition = mutex.withLock {
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
    suspend fun haptic(): HapticOutput? = mutex.withLock {
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
    suspend fun audio(): AudioOutput? = mutex.withLock {
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
    suspend fun voice(): VoiceOutput? = mutex.withLock {
        if (currentItemIndex !in contentItems.indices) {
            return@withLock null
        }
        val renderer = renderers.filterIsInstance<VoiceOutputRenderer>().firstOrNull()
            ?: return@withLock null
        renderer.renderVoice(listOf(contentItems[currentItemIndex]))
    }

    private fun describeItem(item: ContentItem): String {
        return when (item) {
            is GraphContent -> "${item.graphType.name.lowercase().replaceFirstChar { it.uppercase() }} with ${item.dataPoints.size} points"
            is FormulaContent -> "${item.formulaType.name.lowercase().replaceFirstChar { it.uppercase() }} formula: ${item.expression}"
            is MoleculeContent -> "Molecule: ${item.name.ifEmpty { "Unknown" }} with ${item.atoms.size} atoms"
            else -> item.contentType.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

sealed class ExplorationResult {
    data class Item(val item: ContentItem, val description: String) : ExplorationResult()
    data object End : ExplorationResult()
    data object Beginning : ExplorationResult()
}

data class ExplorationPosition(
    val current: Int,
    val total: Int
)
