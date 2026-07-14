# DrishtiSDK Social Media Launch Kit

Ready-to-post content for open-source launch. Copy, paste, tweak as needed.

---

## 1. Reddit r/Kotlin

**Title:** Show /r/Kotlin: DrishtiSDK — a KMP accessibility SDK that converts visual content into haptic, spatial audio, and voice output

**Body:**

Hey r/Kotlin. I've been building something I think you'll find interesting.

**DrishtiSDK** is an open-source accessibility SDK for Android. It takes visual content (camera frames, screenshots, bitmaps) and converts it into three non-visual output channels: haptic feedback, spatial audio, and voice descriptions. The whole thing runs offline, on-device.

The core idea: you point it at any visual content, it figures out what's there, and renders it through touch, sound, or speech. No screen required.

**What makes it interesting from a Kotlin perspective:**

- **KMP architecture** — `commonMain` holds shared interfaces and pipeline logic, `androidMain` has platform-specific implementations (CameraX, VibrationEffect, Oboe). The separation is clean and intentional.
- **Plugin system** — detectors and renderers are completely decoupled. Add a new content type (circuit diagrams, maps, whatever) without touching core. One interface, zero coupling.
- **Coroutines throughout** — the pipeline runs all detectors in parallel via coroutines. SceneGraph unifies their output, then renderers run in parallel across output channels.
- **Typed errors** — sealed `DrishtiException` hierarchy with `ErrorCode` enum. No string-based error handling anywhere.
- **expect/actual** — used for platform-specific behavior. commonMain is functional (not just interfaces), androidMain is where the fast native stuff lives.

**The module breakdown (9 modules, all on Maven Central):**

```
drishti-core       Plugin interfaces, pipeline, scene graph
drishti-vision     Frame preprocessing, feature extraction
drishti-graph      Bar/line/scatter/function plot detection
drishti-formula    LaTeX parsing, math evaluation
drishti-molecule   SMILES parsing, PubChem enrichment (OpenChemLib)
drishti-haptics    VibrationEffect.Composition rendering
drishti-audio      Oboe spatial audio + waveform synthesis
drishti-voice      Content description, formula verbalization
drishti-android    CameraX integration, HAL bridge
```

Each detector plugin (graph, formula, molecule) implements all three renderer interfaces. One plugin, full accessibility output.

**Stats:** 1,203 test assertions across 9 modules. Apache 2.0. Published to Maven Central under `io.github.lathisskhumar`.

**Quick start:**

```kotlin
dependencies {
    implementation("io.github.lathisskhumar:drishti-core:1.0.0")
    implementation("io.github.lathisskhumar:drishti-graph:1.0.0")
    implementation("io.github.lathisskhumar:drishti-android:1.0.0")
}

val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsPlugin())
    .addRenderer(AudioPlugin())
    .build()

val diagram = drishti.readAsync(frame)
diagram.haptics().getOrNull()?.pulses    // vibration patterns
diagram.audio().getOrNull()?.sources     // spatial audio data
diagram.voice().getOrNull()?.speech?.text // "Line chart with 3 data points..."
```

GitHub: https://github.com/LathissKhumar/DrishtiSDK
Maven Central: https://central.sonatype.com/artifact/io.github.lathisskhumar/drishti-core/1.0.0
Contributing: https://github.com/LathissKhumar/DrishtiSDK/blob/main/CONTRIBUTING.md

Would love feedback on the plugin architecture and the KMP layering. If anyone's building accessibility tooling, I'd especially like to hear what content types you'd want supported.

---

## 2. Reddit r/androiddev

**Title:** Show r/androiddev: An accessibility SDK for Android — converts visual content into haptic, spatial audio, and voice output

**Body:**

I built an SDK I wish existed when I started working on accessibility features.

**DrishtiSDK** takes visual content (camera, screenshots, bitmaps) and outputs three non-visual channels: haptic feedback patterns, spatial audio placement, and voice descriptions. Runs 100% offline. No cloud, no data leaving the device.

**Why this exists:**

Most accessibility tools focus on one content type (OCR for text, or screen readers for UI). Drishti is content-agnostic. The plugin architecture means you can add support for anything visual — graphs, chemical structures, circuit diagrams, maps — without touching core code. Right now it ships with three detector plugins: graphs (line, bar, scatter, function plots), formulas (LaTeX + handwritten math), and molecules (SMILES + PubChem).

**What you get:**

- **Haptics**: vibration patterns that map spatial positions to the phone's surface. Touch-based navigation through visual content.
- **Spatial audio**: elements placed in 3D space using Oboe. Hear the structure of a graph.
- **Voice**: natural language descriptions of what's detected. "Line chart with 3 data points, upward trend."

Each plugin implements all three output types. Drop in one dependency, get all three channels.

**Tech details:**

- Kotlin 2.1.20, KMP (commonMain + androidMain)
- CameraX 1.5 integration
- Oboe for low-latency spatial audio
- VibrationEffect.Composition (API 30+) with Waveform fallback
- Plugin architecture: `DetectorPlugin` + `HapticsRenderer` + `AudioRenderer` + `VoiceOutputRenderer`
- 1,203 tests, Apache 2.0

```kotlin
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsPlugin())
    .addRenderer(AudioPlugin())
    .build()

val diagram = drishti.readAsync(frame)
```

**Maven Central:**

```kotlin
implementation("io.github.lathisskhumar:drishti-core:1.0.0")
implementation("io.github.lathisskhumar:drishti-graph:1.0.0")
implementation("io.github.lathisskhumar:drishti-android:1.0.0")
```

GitHub: https://github.com/LathissKhumar/DrishtiSDK
Maven Central: https://central.sonatype.com/artifact/io.github.lathisskhumar/drishti-core/1.0.0

If you're building apps that need to serve visually impaired users, this is meant to be the infrastructure layer you don't have to build yourself. PRs welcome, plugin guide in CONTRIBUTING.md.

---

## 3. Reddit r/accessibility

**Title:** I built an SDK that helps blind users explore visual content through touch, spatial audio, and voice

**Body:**

I want to share something I've been working on. It's called DrishtiSDK, and it's designed to make visual content accessible through three sensory channels: touch, sound, and speech.

**What it does:**

Point it at visual content (a camera frame, a screenshot, an image file). It detects what's there and renders it non-visually:

- **Haptic feedback** — vibration patterns that map spatial positions to the phone's surface. You can feel where elements are relative to each other.
- **Spatial audio** — elements placed in 3D space using sound. A chart's data points have different positions in audio space, so you hear the structure.
- **Voice descriptions** — natural language output like "Line chart with 3 data points, trending upward" or "Chemical structure: benzene ring with hydroxyl group."

The whole thing runs on-device. Nothing leaves the phone. No cloud dependency.

**Who it's for:**

Drishti is a developer tool, not an end-user app. It's the infrastructure that app developers can embed to add non-visual access to their content. If you're building an educational app, a scientific tool, or anything with visual data, Drishti gives you the accessibility layer.

The plugin architecture means it's not limited to STEM content. Right now it supports graphs, math formulas, and chemical structures. But anyone can write a plugin for circuit diagrams, maps, UI elements, whatever visual content their app needs to make accessible.

**Why I built this:**

Most accessibility tools focus on one domain — screen readers for UI, OCR for text, etc. Drishti is designed to be general-purpose. The same SDK that makes a bar chart accessible through haptics can make a molecular structure accessible through spatial audio. Three output channels, one API, any visual content.

It's Apache 2.0 and published to Maven Central. The code is on GitHub with 1,203 tests, a contributing guide, and good-first-issue labels.

GitHub: https://github.com/LathissKhumar/DrishtiSDK

I'd love to hear from people in the accessibility community about what content types matter most, what the output modalities should prioritize, and what I'm missing. The contributing guide is at https://github.com/LathissKhumar/DrishtiSDK/blob/main/CONTRIBUTING.md.

---

## 4. Hacker News

**Title:** Show HN: DrishtiSDK — open-source accessibility SDK converting visual content to haptic/audio/voice

**First comment (post this immediately after submitting):**

Hi HN, I'm Lathiss. DrishtiSDK is an open-source accessibility SDK for Android that converts visual content into three non-visual output channels: haptic feedback, spatial audio, and voice descriptions.

The core idea: any visual content (camera frames, screenshots, bitmaps) goes in, and touch/sound/speech comes out. Everything runs on-device, no cloud.

Key design decisions:

- **Plugin architecture.** Detectors and renderers are completely decoupled. Adding a new content type (circuit diagrams, maps, etc.) means implementing one interface. No core changes needed. Each detector plugin automatically supports all three output modalities.
- **KMP-ready.** commonMain holds shared logic, androidMain has platform implementations. The separation is intentional and clean — not just an abstraction layer.
- **Typed errors throughout.** Sealed DrishtiException hierarchy. No string-based error handling.
- **Parallel pipeline.** All detectors run concurrently via coroutines, output unified through a SceneGraph, then renderers run in parallel across channels.

The SDK ships with three detector plugins (graphs, formulas, molecules) across 9 modules. 1,203 tests. Apache 2.0. Published to Maven Central.

Tech stack: Kotlin 2.1.20, CameraX, Oboe (spatial audio), OpenChemLib (chemistry), mXparser (math evaluation).

Repo: https://github.com/LathissKhumar/DrishtiSDK
Maven Central: `io.github.lathisskhumar:drishti-core:1.0.0`

I'm particularly interested in feedback on the plugin interface design and whether the output constraint model (validated ranges for haptic intensity, audio frequency, spatial position) is the right approach for developer ergonomics.

---

## 5. Twitter/X Thread

**Tweet 1 (hook):**

I built an SDK that converts visual content into touch, spatial audio, and voice.

Point it at a graph. Feel the data points through vibration. Hear their positions in 3D audio space. Get a natural language description.

Runs fully offline. No cloud.

https://github.com/LathissKhumar/DrishtiSDK

**Tweet 2 (the problem):**

Most accessibility tools focus on one thing — screen readers for UI, OCR for text, etc.

But what about visual STEM content? Charts, chemical structures, math formulas?

If you can't see them, you're locked out. Drishti is the infrastructure layer that changes that.

**Tweet 3 (the architecture):**

Drishti has a plugin architecture. Three layers:

1. Detector plugins (what's in the image?)
2. SceneGraph (unified representation)
3. Renderer plugins (output through haptics/audio/voice)

Add a new content type by implementing one interface. No core changes.

**Tweet 4 (what ships):**

Nine modules, all on Maven Central:

- drishti-core: pipeline + plugin interfaces
- drishti-graph: line/bar/scatter plots
- drishti-formula: LaTeX + handwritten math
- drishti-molecule: SMILES + PubChem
- drishti-haptics: vibration rendering
- drishti-audio: Oboe spatial audio
- drishti-voice: natural language output
- drishti-android: CameraX integration

**Tweet 5 (the details):**

What makes it work:

- Kotlin 2.1.20, KMP (commonMain + androidMain)
- Parallel detector execution via coroutines
- Typed errors (sealed classes, no strings)
- VibrationEffect.Composition (API 30+)
- Oboe for low-latency spatial audio
- 1,203 tests across all modules

**Tweet 6 (the why):**

I built this because I got tired of seeing accessibility treated as an afterthought.

Every educational app, every scientific tool, every data visualization — they all assume you can see the screen.

Drishti makes it possible to ship accessibility as a first-class feature, not a bolt-on.

**Tweet 7 (CTA):**

DrishtiSDK is Apache 2.0. Published to Maven Central.

If you're building apps that need to serve visually impaired users, try it:
io.github.lathisskhumar:drishti-core:1.0.0

Contributions welcome. Plugin guide in CONTRIBUTING.md.

https://github.com/LathissKhumar/DrishtiSDK

---

## Posting Notes

- **Reddit self-promotion:** Keep engagement high in the days before and after posting. Comment on other threads, answer questions, be present.
- **HN timing:** Post Tuesday-Thursday morning US time for best visibility.
- **Twitter:** Space the thread out. Don't dump all 7 tweets at once. Post the hook first, then add the rest over 10-15 minutes.
- **All platforms:** Reply to every comment in the first 24 hours. Engagement matters more than the post itself.
- **Cross-linking:** Don't cross-reference posts on different platforms. Each should stand alone.
- **Avatar/profile:** Make sure your GitHub profile and Twitter bio mention DrishtiSDK before posting.
