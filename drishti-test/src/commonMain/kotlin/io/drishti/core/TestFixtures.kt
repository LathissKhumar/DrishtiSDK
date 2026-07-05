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

/**
 * Shared test fixtures for Drishti SDK tests.
 */
public object TestFixtures {

    public fun frame(
        width: Int = 640,
        height: Int = 480,
        format: FrameFormat = FrameFormat.RGB_888,
        data: ByteArray? = ByteArray(1)
    ): Frame = Frame(width = width, height = height, format = format, data = data)

    public fun graphContent(
        graphType: GraphType = GraphType.LINE_CHART,
        title: String = "Test Graph",
        axes: Axes = Axes(
            x = Axis(label = "X", range = 0f..100f),
            y = Axis(label = "Y", range = 0f..100f)
        ),
        dataPoints: List<DataPoint> = listOf(
            DataPoint(10f, 20f),
            DataPoint(30f, 50f),
            DataPoint(60f, 80f)
        ),
        labels: List<String> = emptyList()
    ): GraphContent = GraphContent(
        graphType = graphType,
        title = title,
        axes = axes,
        dataPoints = dataPoints,
        labels = labels,
        confidence = 0.85f
    )

    public fun formulaContent(
        formulaType: FormulaType = FormulaType.ALGEBRAIC,
        expression: String = "x + y = z",
        symbols: List<FormulaSymbol> = listOf(
            FormulaSymbol(
                type = SymbolType.VARIABLE,
                position = Point(10f, 10f),
                boundingBox = BoundingBox(5f, 5f, 20f, 20f),
                value = "x"
            ),
            FormulaSymbol(
                type = SymbolType.OPERATOR,
                position = Point(40f, 10f),
                boundingBox = BoundingBox(35f, 5f, 10f, 20f),
                value = "+"
            ),
            FormulaSymbol(
                type = SymbolType.VARIABLE,
                position = Point(60f, 10f),
                boundingBox = BoundingBox(55f, 5f, 20f, 20f),
                value = "y"
            )
        ),
        geometry: Geometry? = null,
        confidence: Float = 0.88f
    ): FormulaContent = FormulaContent(
        formulaType = formulaType,
        expression = expression,
        symbols = symbols,
        geometry = geometry,
        confidence = confidence
    )

    public fun moleculeContent(
        moleculeType: MoleculeType = MoleculeType.ORGANIC,
        atoms: List<Atom> = listOf(
            Atom(id = 0, element = "C", position = Point(50f, 50f), charge = 0, label = "C"),
            Atom(id = 1, element = "H", position = Point(30f, 30f), charge = 0, label = "H"),
            Atom(id = 2, element = "O", position = Point(70f, 30f), charge = 0, label = "O")
        ),
        bonds: List<Bond> = listOf(
            Bond(from = 0, to = 1, type = BondType.SINGLE, strength = 1.0f),
            Bond(from = 0, to = 2, type = BondType.DOUBLE, strength = 1.0f)
        ),
        name: String = "Methanol",
        geometry: Geometry? = null,
        confidence: Float = 0.92f
    ): MoleculeContent = MoleculeContent(
        moleculeType = moleculeType,
        atoms = atoms,
        bonds = bonds,
        name = name,
        geometry = geometry,
        confidence = confidence
    )

    public fun hapticOutput(
        pulses: List<HapticPulse> = listOf(
            HapticPulse(intensity = 0.5f, duration = 50L, x = 0.5f, y = 0.5f)
        ),
        pattern: String = "test_pattern"
    ): HapticOutput = HapticOutput(pulses = pulses, pattern = pattern)

    public fun audioOutput(
        sources: List<AudioSource> = listOf(
            AudioSource(frequency = 440f, amplitude = 0.5f, spatialX = 0.5f, spatialY = 0.5f, spatialZ = 0.5f)
        ),
        spatial: Boolean = true
    ): AudioOutput = AudioOutput(sources = sources, spatial = spatial)

    public fun voiceOutput(
        text: String = "Test speech",
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
        language: String = "en-US"
    ): VoiceOutput = VoiceOutput(
        speech = SpeechSegment(text = text, rate = rate, pitch = pitch),
        language = language
    )
}

// ---- Shared test stubs ----

public class StubDetector(
    override val contentType: ContentType,
    private val createItem: ContentItem? = null,
    confidence: Float = 0.8f
) : DetectorPlugin {
    override val confidence: Float = confidence
    override suspend fun detect(frame: Frame): ContentItem? = createItem
}

public class StubHapticsRenderer : HapticsRenderer {
    override val name: String = "stub-haptics"
    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput = HapticOutput(pulses = emptyList(), pattern = "stub")
    override fun renderExplorationHaptic(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): HapticOutput = HapticOutput(pulses = emptyList(), pattern = "stub-explore")
}

public class StubAudioRenderer : AudioRenderer {
    override val name: String = "stub-audio"
    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput = AudioOutput(sources = emptyList(), spatial = true)
    override fun renderExplorationAudio(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): AudioOutput = AudioOutput(sources = emptyList(), spatial = true)
}

public class StubVoiceRenderer : VoiceOutputRenderer {
    override val name: String = "stub-voice"
    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput = VoiceOutput(speech = SpeechSegment(text = "stub", rate = 1.0f, pitch = 1.0f), language = "en-US")
    override fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): VoiceOutput = VoiceOutput(speech = SpeechSegment(text = "stub", rate = 1.0f, pitch = 1.0f), language = "en-US")
}
