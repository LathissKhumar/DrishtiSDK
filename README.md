# Drishti SDK

The accessibility infrastructure for visual STEM content.

## Install

```kotlin
implementation("io.drishti:drishti-core:1.0.0")
implementation("io.drishti:drishti-graph:1.0.0")
```

## Quick Start

```kotlin
val drishti = Drishti.Builder()
    .addDetector(GraphPlugin())
    .addRenderer(HapticsRenderer())
    .build()

val diagram = drishti.read(image)
diagram.haptics()
diagram.audio()
```

## License

Apache 2.0
