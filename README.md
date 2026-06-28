# Drishti SDK

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![API Level](https://img.shields.io/badge/API-30%2B-brightgreen)](https://developer.android.com/about/versions/11)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-purple?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-orange)](CONTRIBUTING.md)

**Accessibility infrastructure for visual STEM content.** Convert graphs, formulas, molecules, and diagrams into haptic feedback, spatial audio, and voice guidance. Plugin-based. Fully offline. Developer-first.

```
Visual Content (Image/PDF/Camera)
        |
   Drishti SDK (Plugin Pipeline)
        |
+-------+-------+---------+----------+
|               |         |          |
Haptics     Spatial Audio  Voice    Text
```

## Install

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    // Core SDK
    implementation("io.drishti:drishti-core:1.0.0")

    // Plugins (pick what you need)
    implementation("io.drishti:drishti-graph:1.0.0")
    implementation("io.drishti:drishti-formula:1.0.0")
    implementation("io.drishti:drishti-molecule:1.0.0")

    // Renderers
    implementation("io.drishti:drishti-haptics:1.0.0")
    implementation("io.drishti:drishti-audio:1.0.0")
    implementation("io.drishti:drishti-voice:1.0.0")

    // Android integration
    implementation("io.drishti:drishti-android:1.0.0")
}
```

## Quick Start

```kotlin
import io.drishti.core.Drishti
import io.drishti.graph.GraphPlugin
import io.drishti.haptics.HapticsPlugin
import io.drishti.audio.AudioPlugin

// 1. Initialize with plugins
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsPlugin())
    .addRenderer(AudioPlugin())
    .build()

// 2. Read any visual content
val diagram = drishti.read(frame)

// 3. Make it accessible
diagram.haptics()   // Feel the structure
diagram.audio()     // Hear the spatial layout
diagram.voice()     // Get spoken description
diagram.explore()   // Interactive exploration mode
```

## Plugins

| Plugin | Module | Detects | Output |
|:---|:---|:---|:---|
| **Graph** | `drishti-graph` | Line charts, scatter plots, bar charts, function plots | Axes, data points, trends, intersections |
| **Formula** | `drishti-formula` | LaTeX formulas, handwritten math, printed equations | Parsed AST, LaTeX string, evaluated values |
| **Molecule** | `drishti-molecule` | Chemical structures, bond diagrams, SMILES | Atom/bond graph, 3D coordinates, PubChem data |

### Write Your Own Plugin

```kotlin
class MyPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
    override val contentType = ContentType.CUSTOM("my-type")

    override fun detect(frame: Frame): List<ContentItem> {
        // Your detection logic
        return listOf(...)
    }

    override fun renderHaptics(item: ContentItem): HapticOutput {
        // Your haptic rendering
    }

    // ... implement other renderers as needed
}

// Register
Drishti.Builder()
    .addDetector(MyPlugin())
    .build()
```

## Architecture

```
Input (Camera/Bitmap/File)
        |
        v
+-------------------+
|  Vision Pipeline  |  drishti-vision (OpenCV, CameraX, preprocessing)
+--------+----------+
         | Frame + Features
         v
+-------------------+
|  Detector Registry|  Parallel execution of all registered detectors
+--------+----------+
         | ContentItems
         v
+-------------------+
|  Scene Graph      |  Unified semantic representation
+--------+----------+
         | SceneGraph
         v
+-------------------+
|  Renderer Registry|  Parallel rendering to all outputs
+--------+----------+
         | MultimodalOutput
         v
+-------------------+
|  Drishti Diagram  |  .haptics()  .audio()  .voice()  .explore()
+-------------------+
```

Every content type is a plugin. Core knows nothing about graphs, formulas, or molecules.

## Module Structure

```
drishti-core/          Plugin interfaces, registry, pipeline, scene graph
drishti-vision/        Shared vision pipeline (OpenCV, CameraX, preprocessing)
drishti-graph/         Graph detection plugin
drishti-formula/       Formula OCR plugin
drishti-molecule/      Molecule detection + PubChem plugin
drishti-haptics/       Haptic rendering engine
drishti-audio/         Spatial audio engine
drishti-voice/         Voice assistant (Sherpa-ONNX)
drishti-android/       Android HAL + CameraX integration
drishti-demo/          Demo app
```

## Tech Stack

| Layer | Technology |
|:---|:---|
| Language | Kotlin 2.1 + C++ (NDK for audio) |
| Build | Gradle 8.11 + KMP (commonMain + androidMain) |
| Vision | OpenCV 4.13 + CameraX 1.5 |
| ML | LiteRT + ONNX Runtime |
| Haptics | VibrationEffect.Composition (API 30+) + Waveform fallback |
| Audio | Oboe 1.9.3 + Android Spatializer |
| Voice | Sherpa-ONNX (offline STT/TTS) |

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
