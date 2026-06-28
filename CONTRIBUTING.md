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

Plugins implement both detection and rendering. Here is the minimal structure:

```kotlin
package io.drishti.myplugin

import io.drishti.core.*

class MyPlugin : DetectorPlugin, HapticsRenderer, AudioRenderer, VoiceOutputRenderer {

    override val contentType = ContentType.CUSTOM("my-content-type")

    override fun detect(frame: Frame): List<ContentItem> {
        // Analyze the frame and return detected content
        return listOf(...)
    }

    override fun renderHaptics(item: ContentItem): HapticOutput {
        // Convert content to haptic pulses
        return HapticOutput(pulses = listOf(...), pattern = "my-pattern")
    }

    override fun renderAudio(item: ContentItem): AudioOutput {
        // Convert content to spatial audio
        return AudioOutput(sources = listOf(...), spatial = true)
    }

    override fun renderVoice(item: ContentItem): VoiceOutput {
        // Convert content to spoken description
        return VoiceOutput(
            speech = SpeechSegment(text = "Description of content", rate = 1.0f, pitch = 1.0f),
            language = "en-US"
        )
    }
}
```

Register it:

```kotlin
val drishti = Drishti.Builder()
    .addDetector(MyPlugin())
    .build()
```

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
