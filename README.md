<p align="center">
  <strong>An accessibility SDK for Android.</strong><br>
  Convert any visual content into haptic feedback, spatial audio, and voice guidance.<br>
  Built for developers shipping accessible apps.
</p>

> [!IMPORTANT]
> **Available on [Maven Central](https://central.sonatype.com/artifact/io.github.lathisskhumar/drishti-core/1.0.0).** All 9 modules published under `io.github.lathisskhumar`. Just add `mavenCentral()` to your repositories — no custom repos needed.

---

## How it works

Drishti sits between your app's visual layer and three output channels: haptics, audio, and voice. Point it at any visual content. It detects what's there and renders it non-visually.

```
Visual content (camera, screenshot, bitmap, file)
         |
    Drishti Pipeline
         |
+--------+--------+---------+
|        |        |         |
Haptics  Audio   Voice     Text
```

**Haptics** vibrate patterns that map spatial positions to the phone's surface. **Audio** places elements in 3D space so the user hears structure. **Voice** describes content in natural language. **Text** is always available as a fallback.

> [!TIP]
> Touch, listen, ask. No screen required.

---

## Why Drishti?

| | |
|:---|:---|
| **General-purpose accessibility** | Not tied to one content type. Add graphs, circuits, maps, UI elements, anything visual. |
| **Three output modalities** | Haptic patterns, spatial audio, voice descriptions. One SDK, three sensory channels. |
| **Plugin architecture** | Add a new content type without touching core. One interface, zero coupling. |
| **Fully offline** | Detection and rendering on-device. No cloud. No data leaves the user's hands. |
| **1,203 test assertions** | Every edge case exercised across 9 modules. |
| **KMP ready** | `commonMain` for shared logic, `androidMain` for platform APIs. iOS/desktop structurally possible. |
| **Apache 2.0** | Published to Maven Central. Fork it, embed it, ship it. |

---

## Quick Start

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.lathisskhumar:drishti-core:1.0.0")
    implementation("io.github.lathisskhumar:drishti-graph:1.0.0")    // or formula, molecule
    implementation("io.github.lathisskhumar:drishti-android:1.0.0")  // CameraX integration
}
```

```kotlin
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsPlugin())
    .addRenderer(AudioPlugin())
    .build()

val diagram = drishti.readAsync(frame)

// Access outputs (Result types for haptics/audio/voice, TextOutput for summary)
diagram.haptics().getOrNull()?.pulses    // List<HapticPulse> — vibration patterns
diagram.audio().getOrNull()?.sources     // List<AudioSource>  — spatial audio data
diagram.voice().getOrNull()?.speech?.text // "Line chart with 3 data points..."
diagram.summary().text                    // always available
```

> [!NOTE]
> Full install options, all 9 modules, and the `settings.gradle.kts` setup are in [Quick Start (full)](#quick-start-full) below.

---

## Available Plugins

| Plugin | Module | Detects | Output |
|:---|:---|:---|:---|
| **GraphPlugin** | `drishti-graph` | Line charts, scatter plots, bar charts, function plots | Axes, data points, trends, intersections |
| **FormulaPlugin** | `drishti-formula` | LaTeX formulas, handwritten math, printed equations | Parsed AST, LaTeX string, evaluated values |
| **MoleculePlugin** | `drishti-molecule` | Chemical structures, bond diagrams, SMILES | Atom/bond graph, 3D coordinates, PubChem data |

Every detector implements **all renderer interfaces** (`HapticsRenderer`, `AudioRenderer`, `VoiceOutputRenderer`). `GraphPlugin()` alone produces haptic, audio, and voice output.

> [!IMPORTANT]
> STEM content (graphs, formulas, molecules) is the first domain. The plugin architecture supports any visual content type: UI elements, maps, circuit diagrams, flowcharts, images, photographs. See [Write Your Own Plugin](#write-your-own-plugin).

---

## Write Your Own Plugin

Two steps: define your content type, then implement the plugin.

**Step 1 — Define your content type** (implements `ContentItem`):

```kotlin
data class CircuitContent(
    val components: List<Component>,
    val connections: List<Wire>,
    val bounds: BoundingBox,
    override val confidence: Float
) : ContentItem {
    override val contentType = ContentType.Custom("circuit")
}
```

**Step 2 — Implement the plugin** (detector + all three renderers):

```kotlin
class CircuitPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
    override val contentType = ContentType.Custom("circuit")
    override val confidence = 0.75f
    override val name = "CircuitPlugin"

    override suspend fun detect(frame: Frame): ContentItem? {
        // Analyze frame, return null if nothing found
        val result = analyzeCircuit(frame) ?: return null
        return CircuitContent(result.components, result.connections, result.bounds, result.confidence)
    }

    // SceneNodeFactory gives your content first-class SceneGraph support
    override val sceneNodeFactory = SceneNodeFactory { item, index, nodes ->
        val circuit = item as CircuitContent
        val cx = circuit.bounds.x + circuit.bounds.width / 2f
        val cy = circuit.bounds.y + circuit.bounds.height / 2f
        nodes.add(SceneNode.ShapeNode(
            id = "circuit-$index",
            position = Point(cx, cy),
            shapeType = ShapeType.RECTANGLE
        ))
    }

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput { ... }
    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput { ... }
    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput { ... }
    override fun renderExplorationHaptic(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): HapticOutput { ... }
    override fun renderExplorationAudio(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): AudioOutput { ... }
    override fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): VoiceOutput { ... }
}

val drishti = Drishti.Builder().addDetector(CircuitPlugin()).build()
```

> [!TIP]
> One detector plugin + all three renderer interfaces = full accessibility output for your content type. No core changes needed. See [CONTRIBUTING.md](CONTRIBUTING.md) for a complete plugin template.

### Output constraints

When building renderers, keep these validated ranges in mind:

| Type | Field | Range | Notes |
|:---|:---|:---|:---|
| `HapticPulse` | `intensity` | 0.0–1.0 | Maps to vibration strength |
| `HapticPulse` | `duration` | > 0 ms | Vibration duration |
| `HapticPulse` | `x`, `y` | 0.0–1.0 | Normalized surface position |
| `AudioSource` | `frequency` | 20–20,000 Hz | Human hearing range |
| `AudioSource` | `amplitude` | 0.0–1.0 | Volume level |
| `AudioSource` | `spatialX/Y/Z` | 0.0–1.0 | 3D position (0.5 = center) |
| `SpeechSegment` | `rate` | 0.1–3.0 | Speech speed (1.0 = normal) |
| `SpeechSegment` | `pitch` | 0.1–3.0 | Voice pitch (1.0 = normal) |

Values outside these ranges throw `IllegalArgumentException` at construction time.

---

## Architecture

```
Input (Camera/Bitmap/File)
         |
         v
+-------------------+
|  Vision Pipeline  |  frame preprocessing, feature extraction
+--------+----------+
         | Frame + Features
         v
+-------------------+
| Detector Registry |  parallel execution of all registered detectors
+--------+----------+
         | ContentItems
         v
+-------------------+
|    Scene Graph     |  unified semantic representation
+--------+----------+
         | SceneGraph
         v
+-------------------+
| Renderer Registry |  parallel rendering to all outputs
+--------+----------+
         | HapticOutput / AudioOutput / VoiceOutput
         v
+-------------------+
|  DrishtiDiagram   |  .haptics()  .audio()  .voice()  .summary()  .explore()
+-------------------+
```

Every content type is a plugin. Core knows nothing about graphs, formulas, or molecules.

> [!NOTE]
> The pipeline runs all detectors in parallel via coroutines. SceneGraph unifies their output into a single semantic representation. Renderers then run in parallel across all output channels.

---

## Module Structure

| Module | Purpose |
|:---|:---|
| `drishti-core` | Plugin interfaces, registry, pipeline, scene graph |
| `drishti-vision` | Shared vision pipeline (frame preprocessing, feature extraction) |
| `drishti-graph` | Graph detection plugin (bar, line, scatter, function plots) |
| `drishti-formula` | Formula detection plugin (LaTeX parsing, math evaluation) |
| `drishti-molecule` | Molecule detection plugin (OpenChemLib, PubChem) |
| `drishti-haptics` | Haptic rendering engine (VibrationEffect API 30+) |
| `drishti-audio` | Spatial audio engine (Oboe + waveform synthesis) |
| `drishti-voice` | Voice guidance engine (content description, formula speech) |
| `drishti-android` | Android HAL + CameraX integration |
| `drishti-demo` | Demo application |

---

## Tech Stack

| Layer | Technology |
|:---|:---|
| Language | Kotlin 2.1.20 (KMP: commonMain + androidMain) |
| Build | Gradle 8.7 + Kotlin Multiplatform + Binary Compatibility Validator |
| Vision | CameraX 1.5 + custom frame preprocessing |
| Haptics | Android VibrationEffect.Composition (API 30+) |
| Audio | Oboe 1.9.3 + waveform synthesis (sine, square, sawtooth, triangle) |
| Voice | Pure Kotlin content description + formula verbalization |
| Chemistry | OpenChemLib (molecule parsing) + Ktor (PubChem API) |
| Math | mXparser (expression evaluation) |
| Testing | JUnit 5 + MockK + Turbine |

---

## Quick Start (full)

**Install:**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// build.gradle.kts
dependencies {
    // Core SDK
    implementation("io.github.lathisskhumar:drishti-core:1.0.0")

    // Detector plugins (pick what you need)
    implementation("io.github.lathisskhumar:drishti-graph:1.0.0")
    implementation("io.github.lathisskhumar:drishti-formula:1.0.0")
    implementation("io.github.lathisskhumar:drishti-molecule:1.0.0")

    // Standalone renderers (optional, detector plugins include their own)
    implementation("io.github.lathisskhumar:drishti-haptics:1.0.0")
    implementation("io.github.lathisskhumar:drishti-audio:1.0.0")
    implementation("io.github.lathisskhumar:drishti-voice:1.0.0")

    // Android integration (CameraX pipeline, HAL)
    implementation("io.github.lathisskhumar:drishti-android:1.0.0")
}
```

**Use it:**

```kotlin
import io.drishti.core.Drishti
import io.drishti.core.Frame
import io.drishti.graph.GraphPlugin
import io.drishti.haptics.HapticsPlugin
import io.drishti.audio.AudioPlugin

// 1. Initialize with plugins
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())   // detects graphs in frames
    .addRenderer(HapticsPlugin()) // renders haptic output
    .addRenderer(AudioPlugin())   // renders spatial audio
    .build()

// 2. Read visual content (suspend function)
val diagram = drishti.readAsync(frame)

// 3. Access outputs
val hapticResult = diagram.haptics()   // Result<HapticOutput>
val audioResult = diagram.audio()      // Result<AudioOutput>
val voiceResult = diagram.voice()      // Result<VoiceOutput>
val summary = diagram.summary()        // TextOutput (always available)
val session = diagram.explore()        // ExplorationSession

// 4. Pattern match on results
hapticResult.getOrNull()?.let { haptic ->
    // use haptic.vibrations, haptic.durations
}
```

> [!TIP]
> Start with the core + one detector plugin + one renderer plugin. Add more as needed. Every detector plugin includes all three renderers built-in.

---

## Contributing

Every new content type is a new plugin. The architecture is designed so you never touch core.

### Build locally

```bash
git clone https://github.com/LathissKhumar/DrishtiSDK.git
cd DrishtiSDK

# Build all modules
./gradlew assembleDebug

# Run all tests (must pass before any PR)
./gradlew testDebugUnitTest
```

> [!NOTE]
> **Prerequisites:** JDK 21, Android SDK with API 35, Android Studio Ladybug or later.

### What to work on

- **Good first issues:** [`good-first-issue`](https://github.com/LathissKhumar/DrishtiSDK/labels/good-first-issue) label. Scoped, well-defined tasks.
- **New plugins:** Circuit diagrams, maps, UI elements. See [Write Your Own Plugin](#write-your-own-plugin).
- **Renderer improvements:** Better haptic patterns, richer spatial audio, more natural voice.
- **Tests:** Edge cases, error paths, concurrency. Every PR should include tests.

### Pull request process

1. Fork and create a feature branch
2. Write tests for new features
3. All tests pass: `./gradlew test`
4. Commit convention: `feat(graph): add scatter plot detection`
5. One change per PR
6. Request review from a maintainer

> [!IMPORTANT]
> See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines, code style, and the plugin development guide.

---

## Community

- **Issues:** [GitHub Issues](https://github.com/LathissKhumar/DrishtiSDK/issues) for bugs, feature requests, questions
- **Discussions:** [GitHub Discussions](https://github.com/LathissKhumar/DrishtiSDK/discussions) for architecture and ideas
- **Pull Requests:** [PRs Welcome](CONTRIBUTING.md)

---

## License

Apache 2.0. See [LICENSE](LICENSE) for details.
