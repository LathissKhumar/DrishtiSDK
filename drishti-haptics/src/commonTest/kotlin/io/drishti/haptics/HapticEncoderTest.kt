package io.drishti.haptics

import io.drishti.core.*
import kotlin.test.*

class HapticEncoderTest {

    @Test
    fun encodePulses() {
        val encoder = HapticEncoder()
        val pulses = listOf(
            HapticPulse(intensity = 0.5f, duration = 50L, x = 0.5f, y = 0.5f),
            HapticPulse(intensity = 1.0f, duration = 100L, x = 0.3f, y = 0.7f)
        )
        val result = encoder.encode(pulses)
        assertEquals(2, result.timings.size)
        assertEquals(50L, result.timings[0])
        assertEquals(100L, result.timings[1])
        assertEquals(127, result.amplitudes[0])
        assertEquals(255, result.amplitudes[1])
        assertEquals(-1, result.repeat)
    }

    @Test
    fun encodeEmptyPulses() {
        val encoder = HapticEncoder()
        val result = encoder.encode(emptyList())
        assertEquals(0, result.timings.size)
        assertEquals(0, result.amplitudes.size)
    }

    @Test
    fun encodeComposition() {
        val encoder = HapticEncoder()
        val pulses = listOf(
            HapticPulse(intensity = 0.9f, duration = 50L, x = 0.5f, y = 0.5f),
            HapticPulse(intensity = 0.3f, duration = 200L, x = 0.5f, y = 0.5f)
        )
        val result = encoder.encodeComposition(pulses)
        assertEquals(2, result.size)
        assertEquals("CLICK", result[0].primitiveType)
        assertEquals("LOW_TICK", result[1].primitiveType)
    }

    @Test
    fun encodeSDK() {
        val encoder = HapticEncoder()
        val pulses = listOf(
            HapticPulse(intensity = 0.5f, duration = 50L, x = 0.5f, y = 0.5f)
        )
        val result = encoder.encodeSDK(pulses)
        assertEquals(1, result.version)
        assertEquals(50L, result.duration)
        assertEquals(1, result.channels.size)
    }
}

class PatternBuilderTest {

    @Test
    fun buildPattern() {
        val builder = PatternBuilder()
        val pattern = builder.buildPattern(3, 1)
        assertEquals("content_3_focus_1", pattern)
    }

    @Test
    fun buildExplorationPattern() {
        val builder = PatternBuilder()
        val pattern = builder.buildExplorationPattern("next")
        assertEquals("exploration_next", pattern)
    }

    @Test
    fun buildNotificationPattern() {
        val builder = PatternBuilder()
        val pattern = builder.buildNotificationPattern("alert")
        assertEquals("notification_alert", pattern)
    }
}

class SpatialMapperTest {

    @Test
    fun mapToHaptic() {
        val mapper = SpatialMapper()
        val coord = mapper.mapToHaptic(Point(50f, 75f), 100f, 100f)
        assertEquals(0.5f, coord.x)
        assertEquals(0.75f, coord.y)
        assertEquals(0.5f, coord.z)
    }

    @Test
    fun mapToHapticClamped() {
        val mapper = SpatialMapper()
        val coord = mapper.mapToHaptic(Point(150f, 200f), 100f, 100f)
        assertEquals(1.0f, coord.x)
        assertEquals(1.0f, coord.y)
    }

    @Test
    fun mapRegion() {
        val mapper = SpatialMapper()
        val region = mapper.mapRegion(10f, 20f, 50f, 40f)
        assertEquals(0.2f, region.left)
        assertEquals(0.5f, region.top)
    }
}
