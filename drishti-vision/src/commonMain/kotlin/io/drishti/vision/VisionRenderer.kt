package io.drishti.vision

import io.drishti.core.AudioOutput
import io.drishti.core.AudioSource
import io.drishti.core.ContentItem
import io.drishti.core.HapticOutput
import io.drishti.core.HapticPulse
import io.drishti.core.ShapeContent
import io.drishti.core.VoiceOutput
import io.drishti.core.SpeechSegment

class VisionRenderer {

    fun renderHaptic(items: List<ContentItem>): HapticOutput {
        val pulses = items.flatMap { item ->
            when (item) {
                is ShapeContent -> shapeToHapticPulses(item)
                else -> emptyList()
            }
        }
        return HapticOutput(pulses = pulses, pattern = "vision_haptic")
    }

    fun renderVoice(items: List<ContentItem>): VoiceOutput {
        val text = describeItems(items)
        return VoiceOutput(
            speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    fun renderAudio(items: List<ContentItem>): AudioOutput {
        val sources = items.flatMap { item ->
            when (item) {
                is ShapeContent -> shapeToAudioSources(item)
                else -> emptyList()
            }
        }
        return AudioOutput(sources = sources, spatial = true)
    }

    fun renderHaptic(features: VisionFeatures): HapticOutput {
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

    fun renderVoice(features: VisionFeatures): VoiceOutput {
        val text = describeFeatures(features)
        return VoiceOutput(
            speech = SpeechSegment(text = text, rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }

    fun renderAudio(features: VisionFeatures): AudioOutput {
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
        return listOf(
            HapticPulse(
                intensity = 0.7f,
                duration = 100L,
                x = 0.5f,
                y = 0.5f
            )
        )
    }

    private fun shapeToAudioSources(shape: ShapeContent): List<AudioSource> {
        return listOf(
            AudioSource(
                frequency = 440f,
                amplitude = 0.5f,
                spatialX = 0.5f,
                spatialY = 0.5f,
                spatialZ = 0.5f
            )
        )
    }

    private fun describeItems(items: List<ContentItem>): String {
        if (items.isEmpty()) return "No visual content detected."

        val shapes = items.filterIsInstance<ShapeContent>()
        return buildString {
            if (shapes.isNotEmpty()) {
                append("${shapes.size} shape(s) detected: ")
                append(shapes.joinToString(", ") { shape ->
                    "${shape.shapeType.name.lowercase()} with area ${formatArea(shape.area)}"
                })
                append(".")
            }
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
}
