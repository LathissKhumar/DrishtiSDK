---
title: "I Built an SDK That Lets Blind Users Touch Graphs"
published: false
description: "DrishtiSDK converts visual STEM content into haptic feedback, spatial audio, and voice guidance. Fully offline. Plugin-driven. Open source."
tags: accessibility, android, kotlin, opensource
canonical_url: null
---

# I Built an SDK That Lets Blind Users Touch Graphs

Last year, a visually-impaired student told me something I couldn't stop thinking about. She was studying chemistry, and her textbook had a benzene ring diagram. Her screen reader said "image." That was it. One word. No description of the bonds, the carbon atoms, the molecular structure. Just... "image."

She wasn't asking for much. She wanted to understand what the diagram showed.

I started asking around. STEM education is full of these invisible walls. Graphs, chemical structures, mathematical formulas, circuit diagrams. All visual. All silent to screen readers. Teachers send PDFs with embedded charts. Students can't read them. Nobody seems to think this is a solvable problem.

I thought it was.

## What DrishtiSDK Does

DrishtiSDK takes any visual content and converts it into three things a person can feel and hear:

- **Haptic patterns**: vibrations that map positions on the phone's surface. Run your finger across the screen and feel where data points sit.
- **Spatial audio**: elements placed in 3D space. Close your eyes and hear the structure around you.
- **Voice descriptions**: natural language explanations of what's there. "Line chart with three data points showing an upward trend."

All of it runs on-device. No cloud. No data leaves the user's hands.

```
Visual content (camera, screenshot, bitmap)
         |
    Drishti Pipeline
         |
+--------+--------+---------+
|        |        |         |
Haptics  Audio   Voice     Text
```

The SDK ships as nine modules on Maven Central. One dependency and you're in.

## The Plugin Architecture

Here's what makes this different from a single-purpose tool: Drishti is plugin-driven. The core knows nothing about graphs, molecules, or formulas. You tell it what to detect and how to render it.

Currently three plugins exist:

| Plugin | Module | Detects |
|--------|--------|---------|
| **GraphPlugin** | `drishti-graph` | Line charts, scatter plots, bar charts, function plots |
| **FormulaPlugin** | `drishti-formula` | LaTeX formulas, handwritten math, printed equations |
| **MoleculePlugin** | `drishti-molecule` | Chemical structures, bond diagrams, SMILES strings |

Each plugin implements all three renderer interfaces. Add `GraphPlugin()` and you immediately get haptic, audio, and voice output for any graph you point the camera at.

Want to add circuit diagrams? Build a plugin. Maps? Plugin. UI elements? Plugin. The architecture doesn't care what the content is.

## Quick Start

Add three lines to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.lathisskhumar:drishti-core:1.0.0")
    implementation("io.github.lathisskhumar:drishti-graph:1.0.0")
    implementation("io.github.lathisskhumar:drishti-android:1.0.0")
}
```

Then wire it up:

```kotlin
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsPlugin())
    .addRenderer(AudioPlugin())
    .build()

val diagram = drishti.readAsync(frame)

// Access outputs
diagram.haptics().getOrNull()?.pulses     // List<HapticPulse> — vibration patterns
diagram.audio().getOrNull()?.sources      // List<AudioSource>  — spatial audio data
diagram.voice().getOrNull()?.speech?.text  // "Line chart with 3 data points..."
diagram.summary().text                     // always available as fallback
```

That's it. Point a camera at a graph. Feel it vibrate. Hear it in 3D space. The `summary()` always returns text, so you have a fallback even if no renderer handles the content.

Note that `haptics()`, `audio()`, and `voice()` return `Result<T>` types. If no renderer can handle the content, you get a failure instead of a crash. `summary()` always works, no wrapping needed.

## Building Your Own Plugin

Say you want to make the SDK understand circuit diagrams. Two steps.

First, define your content type:

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

Then implement the plugin. One class, detector plus all three renderers:

```kotlin
class CircuitPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {
    override val contentType = ContentType.Custom("circuit")
    override val confidence = 0.75f
    override val name = "CircuitPlugin"

    override suspend fun detect(frame: Frame): ContentItem? {
        val result = analyzeCircuit(frame) ?: return null
        return CircuitContent(
            result.components, result.connections,
            result.bounds, result.confidence
        )
    }

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput { /* ... */ }
    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput { /* ... */ }
    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput { /* ... */ }
    override fun renderExplorationHaptic(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): HapticOutput { /* ... */ }
    override fun renderExplorationAudio(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): AudioOutput { /* ... */ }
    override fun renderExplorationVoice(
        item: ContentItem, direction: ExplorationDirection, elementIndex: Int
    ): VoiceOutput { /* ... */ }
}
```

Register it:

```kotlin
val drishti = Drishti.Builder()
    .addDetector(CircuitPlugin())
    .addRenderer(HapticsPlugin())
    .build()
```

Your plugin handles detection. The SDK handles everything else. No core changes needed.

## How the Pipeline Works Under the Hood

The pipeline runs in three stages:

**1. Detection.** All registered detectors run in parallel via Kotlin coroutines. Each one receives the same camera frame and returns content items or null.

**2. Scene Graph.** Detected items get mapped into a unified graph. Nodes represent elements. Edges connect related pieces based on spatial proximity, semantic relationships, and temporal order. Every content type becomes a node in the same structure.

**3. Rendering.** All registered renderers run in parallel. Each renderer reads the scene graph and produces its output. Haptics generate vibration patterns. Audio generates spatial sound sources. Voice generates natural language descriptions.

The whole thing is a single suspend function call. `readAsync(frame)` returns a `DrishtiDiagram` with all outputs ready.

## Why This Matters

The World Health Organization estimates around 2.2 billion people have near or distance vision impairment. In STEM education specifically, the problem is acute. Most visual content in textbooks, lectures, and research papers has no non-visual alternative.

Screen readers read text. They don't read charts. They can't describe the shape of a molecule. They won't tell you that a graph's curve peaks at x=3.

Drishti doesn't replace screen readers. It fills the gap they leave. Visual content gets converted to something you can feel, hear, and understand without looking at it.

Everything runs offline. A student in a classroom with no internet connection gets the same experience as someone in a lab with fiber. No cloud processing. No data collection. No accounts. Just the SDK and the device.

## The Engineering

A few things I'm proud of:

**1,203 test assertions** across 9 modules. Every edge case. Every error path. Every concurrency scenario.

**Binary Compatibility Validator** tracks the public API surface. Breaking changes get caught before they ship.

**Kotlin Multiplatform** with `commonMain` for shared logic and `androidMain` for platform APIs. iOS and Desktop targets are structurally possible without a redesign.

**Oboe** for low-latency spatial audio. HRTF spatialization places elements in 3D space. You hear where things are, not just that they exist.

**Sherpa-ONNX** for fully offline TTS and STT. No internet dependency for voice.

**OpenChemLib** for molecule parsing. PubChem enrichment with rate limiting, retry, and caching.

The full stack: Kotlin 2.1.20, CameraX 1.5, Oboe 1.9.3, OpenChemLib, mXparser, JUnit 5, MockK, Turbine. All Apache 2.0.

## Get Involved

DrishtiSDK is published to Maven Central.

```kotlin
implementation("io.github.lathisskhumar:drishti-core:1.0.0")
```

[![Maven Central](https://img.shields.io/badge/Maven_Central-1.0.0-brightgreen)](https://central.sonatype.com/artifact/io.github.lathisskhumar/drishti-core/1.0.0)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Tests](https://img.shields.io/badge/Tests-1203-blue)](https://github.com/LathissKhumar/DrishtiSDK)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-purple?logo=kotlin&logoColor=white)](https://kotlinlang.org)

**GitHub:** [github.com/LathissKhumar/DrishtiSDK](https://github.com/LathissKhumar/DrishtiSDK)

If you're building something accessibility-related, this SDK might save you months of work. If you want to contribute a plugin for a new content type, there's a plugin template in `CONTRIBUTING.md`. If you just think this matters, a star on the repo helps others find it.

STEM content shouldn't be invisible to anyone. Let's make sure it isn't.

---

*Built with Kotlin, powered by stubbornness, and shipped because somebody has to.*
