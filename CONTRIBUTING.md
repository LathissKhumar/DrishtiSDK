# Contributing to Drishti SDK

Thanks for your interest in making STEM content accessible. This guide covers everything you need to contribute.

## Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/<your-username>/DrishtiSDK.git
   cd DrishtiSDK
   ```
3. Create a branch:
   ```bash
   git checkout -b feature/my-feature
   ```

## Prerequisites

- JDK 21
- Android SDK with API 35
- Android Studio Ladybug or later

## Building

```bash
ANDROID_HOME=$ANDROID_SDK JAVA_HOME=$JAVA_HOME ./gradlew build
```

Or on Linux with Android Studio:

```bash
ANDROID_HOME=/home/user/android-sdk JAVA_HOME=/opt/android-studio/jbr ./gradlew build
```

## Running Tests

```bash
ANDROID_HOME=$ANDROID_SDK JAVA_HOME=$JAVA_HOME ./gradlew test
```

All tests must pass before submitting a PR.

## Project Structure

```
drishti-core/          Core interfaces and pipeline
drishti-vision/        Image preprocessing
drishti-graph/         Graph detection plugin
drishti-formula/       Formula detection plugin
drishti-molecule/      Molecule detection plugin
drishti-haptics/       Haptic rendering
drishti-audio/         Spatial audio rendering
drishti-voice/         Voice output
drishti-android/       Android platform integration
drishti-demo/          Demo application
```

## Writing a Plugin

Plugins implement detection and rendering for a content type. Here is a complete template:

```kotlin
package io.drishti.myplugin

import io.drishti.core.*

// 1. Define your content type
data class MyContent(
    val data: List<String>,
    override val confidence: Float
) : ContentItem {
    override val contentType = ContentType.Custom("my-type")
}

// 2. Implement detection + rendering
class MyPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    override val contentType = ContentType.Custom("my-type")
    override val confidence = 0.8f
    override val name = "MyPlugin"

    // Gives your content first-class SceneGraph positioning (optional but recommended)
    override val sceneNodeFactory = SceneNodeFactory { item, index, nodes ->
        nodes.add(SceneNode.TextNode(
            id = "my-$index",
            position = orderPosition(index),
            text = item.contentType.name
        ))
    }

    override suspend fun detect(frame: Frame): ContentItem? {
        val result = analyze(frame) ?: return null
        return MyContent(result, confidence = 0.8f)
    }

    override fun renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput {
        return HapticOutput(pulses = listOf(
            HapticPulse(intensity = 0.5f, duration = 100L)
        ))
    }

    override fun renderAudio(items: List<ContentItem>, focusIndex: Int): AudioOutput {
        return AudioOutput(sources = listOf(
            AudioSource(frequency = 440f, amplitude = 0.5f)
        ))
    }

    override fun renderVoice(items: List<ContentItem>, focusIndex: Int): VoiceOutput {
        return VoiceOutput(speech = SpeechSegment(text = "Content description"))
    }

    override fun renderExplorationHaptic(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): HapticOutput {
        return HapticOutput(pulses = listOf(HapticPulse(intensity = 0.3f, duration = 50L)))
    }

    override fun renderExplorationAudio(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): AudioOutput {
        return AudioOutput(sources = listOf(AudioSource(frequency = 220f, amplitude = 0.3f)))
    }

    override fun renderExplorationVoice(item: ContentItem, direction: ExplorationDirection, elementIndex: Int): VoiceOutput {
        return VoiceOutput(speech = SpeechSegment(text = "Navigating element $elementIndex"))
    }
}
```

Register it:

```kotlin
val drishti = Drishti.Builder()
    .addDetector(MyPlugin())
    .build()
```

### Output constraints

| Field | Range | Throws on violation |
|:---|:---|:---|
| `HapticPulse.intensity` | 0.0–1.0 | `IllegalArgumentException` |
| `HapticPulse.duration` | > 0 ms | `IllegalArgumentException` |
| `AudioSource.frequency` | 20–20,000 Hz | `IllegalArgumentException` |
| `AudioSource.amplitude` | 0.0–1.0 | `IllegalArgumentException` |
| `SpeechSegment.rate` | 0.1–3.0 | `IllegalArgumentException` |
| `SpeechSegment.pitch` | 0.1–3.0 | `IllegalArgumentException` |

## Code Style

- Kotlin coding conventions
- 4-space indentation
- KDoc on all public APIs
- No `as any`, no `@Suppress("UNCHECKED_CAST")`
- Explicit types on public API
- No wildcard imports

## Commit Messages

Follow conventional commits:

```
feat(graph): add scatter plot detection
fix(haptics): correct intensity mapping for small screens
docs(formula): add plugin development guide
test(molecule): add SMILES parsing edge cases
```

## Pull Request Process

1. Write tests for new features
2. Ensure all tests pass
3. Update documentation if needed
4. One focused change per PR
5. Request a review from a maintainer

## Reporting Issues

Use the GitHub issue tracker. Include:

- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
