# DrishtiSTEM Learnings

## drishti-audio Spatial Audio Rewrite (2026-06-28)

### Architecture
- `drishti-audio` is a Kotlin Multiplatform module targeting only Android (no JVM metadata target)
- `compileKotlinMetadata` task is SKIPPED because there's no common metadata compilation target — use `compileDebugSources` instead
- Core types (`SceneGraph`, `SceneNode`, `SceneEdge`) live in `drishti-core` `commonMain`
- `AudioRenderer` interface in core expects `renderAudio(items: List<ContentItem>, focusIndex: Int)` returning `AudioOutput`

### Spatial Mapping
- Node X position → azimuth (-180° to 180°): center = 0°, left = -180°, right = +180°
- Node Y position → elevation (-90° to 90°): center = 0°, top = -90° (above), bottom = +90° (below)
- Node depth → distance: depth 0 = 0.1m (MIN_DISTANCE), each depth level adds 1.5m
- Edge weight → volume: average weight of incident edges, default 1.0 for isolated nodes
- Node type → sound type: DataPointNode=MUSICAL_TONE, TextNode=SPEECH, ShapeNode/AxisNode=AMBIENT

### Key Files
- `AudioData.kt`: SpatialPosition, SpatialAudioSource, SpeechDescription, SpatialAudioScene, SoundType enum
- `SpatialRenderer.kt`: SceneGraph→spatial audio, with spherical-to-Cartesian conversion
- `AudioPlugin.kt`: Facade bridging ContentItem lists to SpatialAudioScene

### Gotchas
- `Point` in core only has (x, y) — need custom `Cartesian3D` for (x, y, z) spatial coordinates
- Speech descriptions should be generated for ALL nodes with non-empty text (not just SPEECH sound type)
- The actual Android Spatializer API (API 32+) and Oboe playback are consumed by drishti-android HAL, not this module
- This module produces structured descriptions (azimuth, elevation, distance) that the HAL layer feeds to Spatializer
