# Drishti SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API Level](https://img.shields.io/badge/API-30%2B-brightgreen)](https://developer.android.com/about/versions/11)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-purple?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Tests](https://img.shields.io/badge/Tests-1203-blue)]()
[![Modules](https://img.shields.io/badge/Modules-9-brightgreen)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-orange)](CONTRIBUTING.md)

**Accessibility infrastructure for visual STEM content.** Convert graphs, formulas, and molecules into haptic feedback, spatial audio, and voice guidance. Plugin-based. Fully offline. Developer-first.

```
Visual Content (Camera/Bitmap/File)
         |
    Drishti Pipeline
         |
+--------+--------+---------+
|        |        |         |
Haptics  Audio   Voice    Text
```

## Install

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
    implementation("io.drishti:drishti-core:1.0.0")

    // Detector plugins (pick what you need)
    implementation("io.drishti:drishti-graph:1.0.0")
    implementation("io.drishti:drishti-formula:1.0.0")
    implementation("io.drishti:drishti-molecule:1.0.0")

    // Standalone renderers (optional — detector plugins include their own renderers)
    implementation("io.drishti:drishti-haptics:1.0.0")
    implementation("io.drishti:drishti-audio:1.0.0")
    implementation("io.drishti:drishti-voice:1.0.0")

    // Android integration (CameraX pipeline, HAL)
    implementation("io.drishti:drishti-android:1.0.0")
}
```

## Quick Start

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

## Plugins

| Plugin | Module | Detects | Output |
|:---|:---|:---|:---|
| **GraphPlugin** | `drishti-graph` | Line charts, scatter plots, bar charts, function plots | Axes, data points, trends, intersections |
| **FormulaPlugin** | `drishti-formula` | LaTeX formulas, handwritten math, printed equations | Parsed AST, LaTeX string, evaluated values |
| **MoleculePlugin** | `drishti-molecule` | Chemical structures, bond diagrams, SMILES | Atom/bond graph, 3D coordinates, PubChem data |

Each detector plugin implements **all renderer interfaces** (`HapticsRenderer`, `AudioRenderer`, `VoiceOutputRenderer`) — so `GraphPlugin()` alone can produce haptic, audio, and voice output. Standalone renderer plugins (`HapticsPlugin`, `AudioPlugin`, `VoicePlugin`) provide additional rendering strategies for custom detector plugins.

### Write Your Own Plugin

```kotlin
class MyPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
    override val contentType: ContentType = ContentType.Custom("my-type")
    override val confidence: Float = 0.8f
    override val name: String = "MyPlugin"

    override suspend fun detect(frame: Frame): ContentItem? {
        // Your detection logic — return null if nothing found
        return MyContent(...)
    }

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        // Your haptic rendering
    }

    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        // Your spatial audio rendering
    }

    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        // Your voice rendering
    }

    override fun renderExplorationHaptic(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): HapticOutput { ... }

    override fun renderExplorationAudio(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): AudioOutput { ... }

    override fun renderExplorationVoice(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): VoiceOutput { ... }
}

// Register
val drishti = Drishti.Builder()
    .addDetector(MyPlugin())
    .build()
```

## Architecture

```
Input (Camera/Bitmap/File)
         |
         v
+-------------------+
|  Vision Pipeline  |  drishti-vision (frame preprocessing, feature extraction)
+--------+----------+
         | Frame + Features
         v
+-------------------+
| Detector Registry |  Parallel execution of all registered detectors
+--------+----------+
         | ContentItems
         v
+-------------------+
|    Scene Graph     |  Unified semantic representation
+--------+----------+
         | SceneGraph
         v
+-------------------+
| Renderer Registry |  Parallel rendering to all outputs
+--------+----------+
         | HapticOutput / AudioOutput / VoiceOutput
         v
+-------------------+
|  DrishtiDiagram   |  .haptics()  .audio()  .voice()  .summary()  .explore()
+-------------------+
```

Every content type is a plugin. Core knows nothing about graphs, formulas, or molecules.

## Module Structure

```
drishti-core/          Plugin interfaces, registry, pipeline, scene graph
drishti-vision/        Shared vision pipeline (frame preprocessing, feature extraction)
drishti-graph/         Graph detection plugin (bar, line, scatter, function plots)
drishti-formula/       Formula detection plugin (LaTeX parsing, math evaluation)
drishti-molecule/      Molecule detection plugin (OpenChemLib, PubChem)
drishti-haptics/       Haptic rendering engine (VibrationEffect API 30+)
drishti-audio/         Spatial audio engine (Oboe + waveform synthesis)
drishti-voice/         Voice guidance engine (content description, formula speech)
drishti-android/       Android HAL + CameraX integration
drishti-demo/          Demo app
```

## Tech Stack

| Layer | Technology |
|:---|:---|
| Language | Kotlin 2.1.20 (KMP: commonMain + androidMain) |
| Build | Gradle 8.7 + Kotlin Multiplatform + Binary Compatibility Validator |
| Vision | CameraX 1.5 (drishti-android) + custom frame preprocessing |
| Haptics | Android VibrationEffect.Composition (API 30+) |
| Audio | Oboe 1.9.3 + waveform synthesis (sine, square, sawtooth, triangle) |
| Voice | Pure Kotlin content description + formula verbalization |
| Chemistry | OpenChemLib (molecule parsing) + Ktor (PubChem API) |
| Math | mXparser (expression evaluation) |
| Serialization | kotlinx.serialization 1.8.1 |
| Concurrency | kotlinx.coroutines 1.10.1 |
| Testing | JUnit 5 + MockK + Turbine |

## Project Stats

| Metric | Value |
|:---|:---|
| Production files | 80 |
| Lines of code | ~15,700 |
| Test files | 29 |
| Test assertions | 1,203 |
| Publishable modules | 9 |
| Min SDK | 30 (Android 11) |
| License | Apache 2.0 |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

1. Each new diagram type is a separate plugin
2. Add a detector without modifying core
3. Every PR should include tests

## License

```
Copyright 2026 Drishti SDK Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
