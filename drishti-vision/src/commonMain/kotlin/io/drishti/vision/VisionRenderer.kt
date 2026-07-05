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
import io.drishti.core.AudioSource
import io.drishti.core.ContentItem
import io.drishti.core.ExplorationDirection
import io.drishti.core.HapticOutput
import io.drishti.core.HapticPulse
import io.drishti.core.FormulaContent
import io.drishti.core.GraphContent
import io.drishti.core.MoleculeContent
import io.drishti.core.ShapeContent
import io.drishti.core.TableContent
import io.drishti.core.VoiceOutput
import io.drishti.core.SpeechSegment

public class VisionRenderer {

    public fun renderHaptic(items: List<ContentItem>): HapticOutput {
        val pulses = items.flatMap { item ->
            when (item) {
                is ShapeContent -> shapeToHapticPulses(item)
                else -> emptyList()
            }
        }
        return HapticOutput(pulses = pulses, pattern = "vision_haptic")
    }

    public fun renderExplorationHaptic(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): HapticOutput {
        val pulses = when (item) {
            is ShapeContent -> shapeExplorationHaptic(item, direction, elementIndex)
            else -> emptyList()
        }
        return HapticOutput(pulses = pulses, pattern = "vision_exploration_haptic")
    }

    public fun renderExplorationAudio(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): AudioOutput {
        val sources = when (item) {
            is ShapeContent -> shapeExplorationAudio(item, direction, elementIndex)
            else -> emptyList()
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    public fun renderExplorationVoice(
        item: ContentItem,
        direction: ExplorationDirection,
        elementIndex: Int
    ): VoiceOutput {
        val text = when (item) {
            is ShapeContent -> shapeExplorationVoice(item, direction, elementIndex)
            else -> "No visual content to explore."
        }
        return VoiceOutput(
            speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    public fun renderVoice(items: List<ContentItem>): VoiceOutput {
        val text = describeItems(items)
        return VoiceOutput(
            speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    public fun renderAudio(items: List<ContentItem>): AudioOutput {
        val sources = items.flatMap { item ->
            when (item) {
                is ShapeContent -> shapeToAudioSources(item)
                else -> emptyList()
            }
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    public fun renderHaptic(features: VisionFeatures): HapticOutput {
        val pulses = mutableListOf<HapticPulse>()

        val allCenters = mutableListOf<io.drishti.core.Point>()
        for (shape in features.shapes) {
            val center = shape.boundingBox.let {
                io.drishti.core.Point(it.x + it.width / 2, it.y + it.height / 2)
            }
            allCenters.add(center)
        }
        for (region in features.textRegions) {
            allCenters.add(io.drishti.core.Point(
                region.boundingBox.x + region.boundingBox.width / 2,
                region.boundingBox.y + region.boundingBox.height / 2
            ))
        }

        val maxX = allCenters.maxOfOrNull { it.x }?.coerceAtLeast(1f) ?: 1f
        val maxY = allCenters.maxOfOrNull { it.y }?.coerceAtLeast(1f) ?: 1f

        for (shape in features.shapes) {
            val center = shape.boundingBox.let {
                io.drishti.core.Point(it.x + it.width / 2, it.y + it.height / 2)
            }
            pulses.add(
                HapticPulse(
                    intensity = 0.7f,
                    duration = 100L,
                    x = (center.x / maxX).coerceIn(0.05f, 0.95f),
                    y = (center.y / maxY).coerceIn(0.05f, 0.95f)
                )
            )
        }
        for (region in features.textRegions) {
            val cx = region.boundingBox.x + region.boundingBox.width / 2
            val cy = region.boundingBox.y + region.boundingBox.height / 2
            pulses.add(
                HapticPulse(
                    intensity = 0.5f,
                    duration = 50L,
                    x = (cx / maxX).coerceIn(0.05f, 0.95f),
                    y = (cy / maxY).coerceIn(0.05f, 0.95f)
                )
            )
        }
        return HapticOutput(pulses = pulses, pattern = "vision_haptic")
    }

    public fun renderVoice(features: VisionFeatures): VoiceOutput {
        val text = describeFeatures(features)
        return VoiceOutput(
            speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    public fun renderAudio(features: VisionFeatures): AudioOutput {
        val maxX = features.shapes.maxOfOrNull { it.boundingBox.x + it.boundingBox.width / 2 }
            ?.coerceAtLeast(1f) ?: 1f
        val maxY = features.shapes.maxOfOrNull { it.boundingBox.y + it.boundingBox.height / 2 }
            ?.coerceAtLeast(1f) ?: 1f

        val sources = features.shapes.map { shape ->
            AudioSource(
                frequency = 440f,
                amplitude = 0.5f,
                spatialX = ((shape.boundingBox.x + shape.boundingBox.width / 2) / maxX).coerceIn(0.05f, 0.95f),
                spatialY = ((shape.boundingBox.y + shape.boundingBox.height / 2) / maxY).coerceIn(0.05f, 0.95f),
                spatialZ = 0.5f
            )
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    private fun shapeToHapticPulses(shape: ShapeContent): List<HapticPulse> {
        val (sx, sy) = shapeCenter(shape)
        return listOf(
            HapticPulse(
                intensity = 0.7f,
                duration = 100L,
                x = sx,
                y = sy
            )
        )
    }

    private fun shapeToAudioSources(shape: ShapeContent): List<AudioSource> {
        val (sx, sy) = shapeCenter(shape)
        return listOf(
            AudioSource(
                frequency = 440f,
                amplitude = 0.5f,
                spatialX = sx,
                spatialY = sy,
                spatialZ = 0.5f
            )
        )
    }

    /** Falls back to center (0.5, 0.5) when boundingBox dimensions are zero. */
    private fun shapeCenter(shape: ShapeContent): Pair<Float, Float> {
        val hasBounds = shape.width > 0f && shape.height > 0f
        return if (hasBounds) {
            val cx = shape.x + shape.width / 2f
            val cy = shape.y + shape.height / 2f
            Pair(cx.coerceIn(0.05f, 0.95f), cy.coerceIn(0.05f, 0.95f))
        } else {
            Pair(0.5f, 0.5f)
        }
    }

    private fun describeItems(items: List<ContentItem>): String {
        if (items.isEmpty()) return "No visual content detected."
        return buildString {
            items.forEach { item ->
                when (item) {
                    is ShapeContent -> append("${item.shapeType.name.lowercase()} shape with area ${formatArea(item.area)}, ")
                    is TableContent -> append("table with ${item.rows} rows and ${item.columns} columns, ")
                    is GraphContent -> append("${item.graphType.name.lowercase()} graph, ")
                    is FormulaContent -> append("formula '${item.expression.take(50)}', ")
                    is MoleculeContent -> append("molecule ${item.name.ifEmpty { item.canonicalSmiles }}, ")
                    else -> append("content, ")
                }
            }
            if (length > 2) deleteCharAt(length - 2)
        }
    }

    private fun describeFeatures(features: VisionFeatures): String {
        if (!features.isNotEmpty()) return "No visual content detected."

        return buildString {
            if (features.shapes.isNotEmpty()) {
                append("${features.shapes.size} shape(s) detected: ")
                append(features.shapes.joinToString(", ") {
                    "${it.type.name.lowercase()} with area ${formatArea(it.area)}"
                })
                append(". ")
            }
            if (features.lines.isNotEmpty()) {
                append("${features.lines.size} line(s) detected. ")
            }
            if (features.textRegions.isNotEmpty()) {
                append("${features.textRegions.size} text region(s) detected. ")
            }
            if (features.regionsOfInterest.isNotEmpty()) {
                append("${features.regionsOfInterest.size} region(s) of interest detected. ")
            }
        }
    }

    private fun formatArea(area: Float): String {
        return if (area == area.toLong().toFloat()) {
            "${area.toLong()} sq px"
        } else {
            "%.1f sq px".format(area)
        }
    }

    private fun shapeExplorationHaptic(
        shape: ShapeContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): List<HapticPulse> {
        val (sx, sy) = shapeCenter(shape)
        val intensity = if (direction == ExplorationDirection.POSITION) 1.0f else 0.7f
        val duration = if (direction == ExplorationDirection.POSITION) 150L else 100L
        return when (direction) {
            ExplorationDirection.NEXT -> {
                listOf(HapticPulse(intensity = intensity, duration = duration, x = sx, y = sy))
            }
            ExplorationDirection.PREVIOUS -> {
                if (elementIndex > 0) {
                    listOf(HapticPulse(intensity = intensity, duration = duration, x = sx, y = sy))
                } else {
                    emptyList()
                }
            }
            ExplorationDirection.POSITION -> {
                listOf(HapticPulse(intensity = intensity, duration = duration, x = sx, y = sy))
            }
        }
    }

    private fun shapeExplorationAudio(
        shape: ShapeContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): List<AudioSource> {
        val (sx, sy) = shapeCenter(shape)
        val amplitude = if (direction == ExplorationDirection.POSITION) 1.0f else 0.5f
        val freq = if (direction == ExplorationDirection.POSITION) 800f else 440f
        return when (direction) {
            ExplorationDirection.NEXT -> {
                listOf(AudioSource(frequency = freq, amplitude = amplitude, spatialX = sx, spatialY = sy, spatialZ = 0.5f))
            }
            ExplorationDirection.PREVIOUS -> {
                if (elementIndex > 0) {
                    listOf(AudioSource(frequency = freq, amplitude = amplitude, spatialX = sx, spatialY = sy, spatialZ = 0.5f))
                } else {
                    emptyList()
                }
            }
            ExplorationDirection.POSITION -> {
                listOf(AudioSource(frequency = freq, amplitude = amplitude, spatialX = sx, spatialY = sy, spatialZ = 0.5f))
            }
        }
    }

    private fun shapeExplorationVoice(
        shape: ShapeContent,
        direction: ExplorationDirection,
        elementIndex: Int
    ): String {
        val label = shape.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }
        val description = "$label shape with area ${formatArea(shape.area)} " +
            "and perimeter ${"%.1f".format(shape.perimeter)}."
        return when (direction) {
            ExplorationDirection.NEXT -> {
                description
            }
            ExplorationDirection.PREVIOUS -> {
                if (elementIndex > 0) description else "At the first shape."
            }
            ExplorationDirection.POSITION -> {
                "Showing $label shape."
            }
        }
    }
}
