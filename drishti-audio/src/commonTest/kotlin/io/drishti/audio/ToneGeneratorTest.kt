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

package io.drishti.audio

import io.drishti.core.*
import kotlin.test.*

class ToneGeneratorTest {

    @Test
    fun generateSineWave() {
        val gen = ToneGenerator()
        val samples = gen.generateSineWave(440f, 100)
        assertTrue(samples.isNotEmpty())
        assertTrue(samples.all { it in -1f..1f })
    }

    @Test
    fun generateSquareWave() {
        val gen = ToneGenerator()
        val samples = gen.generateSquareWave(440f, 100)
        assertTrue(samples.isNotEmpty())
        assertTrue(samples.all { it == -1f || it == 1f })
    }

    @Test
    fun generateSawtoothWave() {
        val gen = ToneGenerator()
        val samples = gen.generateSawtoothWave(440f, 100)
        assertTrue(samples.isNotEmpty())
    }

    @Test
    fun generateTriangleWave() {
        val gen = ToneGenerator()
        val samples = gen.generateTriangleWave(440f, 100)
        assertTrue(samples.isNotEmpty())
    }

    @Test
    fun applyEnvelopeModifiesAmplitude() {
        val gen = ToneGenerator()
        val samples = FloatArray(4410) { 1.0f }
        val result = gen.applyEnvelope(samples)
        assertEquals(4410, result.size)
    }
}

class SonificationMapperTest {

    @Test
    fun mapToFrequency() {
        val mapper = SonificationMapper()
        val freq = mapper.mapToFrequency(50f, 0f, 100f)
        assertEquals(600f, freq, 0.01f)
    }

    @Test
    fun mapToFrequencyMin() {
        val mapper = SonificationMapper()
        val freq = mapper.mapToFrequency(0f, 0f, 100f)
        assertEquals(200f, freq, 0.01f)
    }

    @Test
    fun mapToFrequencyMax() {
        val mapper = SonificationMapper()
        val freq = mapper.mapToFrequency(100f, 0f, 100f)
        assertEquals(1000f, freq, 0.01f)
    }

    @Test
    fun mapToAmplitude() {
        val mapper = SonificationMapper()
        val amp = mapper.mapToAmplitude(50f, 0f, 100f)
        assertEquals(0.5f, amp, 0.01f)
    }

    @Test
    fun mapToDuration() {
        val mapper = SonificationMapper()
        val dur = mapper.mapToDuration(50f, 0f, 100f)
        assertEquals(125L, dur)
    }
}

class AudioSpatialMapperTest {

    @Test
    fun mapToAudio() {
        val mapper = AudioSpatialMapper()
        val coord = mapper.mapToAudio(Point(50f, 50f), 100f, 100f)
        assertEquals(0.5f, coord.x, 0.01f)
        assertEquals(0.5f, coord.y, 0.01f)
    }

    @Test
    fun distance() {
        val mapper = AudioSpatialMapper()
        val a = AudioCoordinate(0f, 0f, 0f)
        val b = AudioCoordinate(3f, 4f, 0f)
        val dist = mapper.distance(a, b)
        assertEquals(5f, dist, 0.01f)
    }
}
