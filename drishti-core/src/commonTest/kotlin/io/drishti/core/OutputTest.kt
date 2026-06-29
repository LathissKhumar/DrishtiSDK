package io.drishti.core

import kotlin.test.*

class OutputTest {

    @Test
    fun hapticOutputCreation() {
        val pulses = listOf(HapticPulse(0.5f, 50L, 0.3f, 0.7f))
        val output = HapticOutput(pulses = pulses, pattern = "test")
        assertEquals(1, output.pulses.size)
        assertEquals("test", output.pattern)
        assertEquals(0.5f, output.pulses[0].intensity)
        assertEquals(50L, output.pulses[0].duration)
    }

    @Test
    fun hapticOutputDefaults() {
        val output = HapticOutput(pulses = emptyList())
        assertEquals("", output.pattern)
    }

    @Test
    fun audioOutputCreation() {
        val sources = listOf(AudioSource(440f, 0.8f, 0.1f, 0.2f, 0.3f))
        val output = AudioOutput(sources = sources, spatial = true)
        assertEquals(1, output.sources.size)
        assertTrue(output.spatial)
    }

    @Test
    fun audioOutputDefaults() {
        val output = AudioOutput(sources = emptyList())
        assertTrue(output.spatial)
    }

    @Test
    fun voiceOutputCreation() {
        val speech = SpeechSegment("Hello", 1.0f, 1.0f)
        val output = VoiceOutput(speech = speech, language = "en-US")
        assertEquals("Hello", output.speech.text)
        assertEquals("en-US", output.language)
    }

    @Test
    fun voiceOutputDefaults() {
        val speech = SpeechSegment("Hi")
        val output = VoiceOutput(speech = speech)
        assertEquals("en-US", output.language)
        assertEquals(1.0f, output.speech.rate)
    }

    @Test
    fun textOutputCreation() {
        val output = TextOutput(text = "Test summary")
        assertEquals("Test summary", output.text)
    }

    @Test
    fun audioSourceRangeValidation() {
        // Valid creation
        assertNotNull(AudioSource(440f, 0.5f, 0.5f, 0.5f, 0.5f))

        // Invalid frequency
        assertFailsWith<IllegalArgumentException> {
            AudioSource(10f, 0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            AudioSource(25000f, 0.5f)
        }

        // Invalid amplitude
        assertFailsWith<IllegalArgumentException> {
            AudioSource(440f, -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            AudioSource(440f, 1.1f)
        }

        // Invalid spatial parameters
        assertFailsWith<IllegalArgumentException> {
            AudioSource(440f, 0.5f, spatialX = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            AudioSource(440f, 0.5f, spatialY = 1.2f)
        }
        assertFailsWith<IllegalArgumentException> {
            AudioSource(440f, 0.5f, spatialZ = 1.05f)
        }
    }

    @Test
    fun speechSegmentRangeValidation() {
        // Valid creation
        assertNotNull(SpeechSegment("ok", 1.5f, 1.5f))

        // Invalid rate
        assertFailsWith<IllegalArgumentException> {
            SpeechSegment("text", rate = 0.05f)
        }
        assertFailsWith<IllegalArgumentException> {
            SpeechSegment("text", rate = 4.0f)
        }

        // Invalid pitch
        assertFailsWith<IllegalArgumentException> {
            SpeechSegment("text", pitch = 0.05f)
        }
        assertFailsWith<IllegalArgumentException> {
            SpeechSegment("text", pitch = 4.0f)
        }
    }
}
