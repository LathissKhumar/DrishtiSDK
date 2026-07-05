# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-05

Initial stable release of Drishti SDK — the accessibility infrastructure for visual STEM content.

### Core (`drishti-core`)
- Plugin architecture with detector and renderer registries
- Pipeline orchestrator with parallel detector execution
- Scene graph with semantic representation and adjacency index
- ContentItem interface and ContentType system
- Sealed `DrishtiException` error hierarchy with `ErrorCode` enum
- `NonFatal(t)` expect/actual + `safeCatch` utility
- Serialization support (JSON) for scene graph persistence

### Vision (`drishti-vision`)
- OpenCV image preprocessing pipeline (grayscale, threshold, blur, edge detection)
- CameraX integration for live camera frame capture
- Image conversion utilities (YUV, Bitmap, Mat)

### Graph Plugin (`drishti-graph`)
- Line chart, scatter plot, bar chart, and function plot detection
- Axis extraction and coordinate mapping
- Data point identification with trend analysis
- Intersection detection between data series

### Formula Plugin (`drishti-formula`)
- LaTeX formula parsing with recursive descent parser
- Math expression evaluation (mXparser integration)
- Symbolic computation support (Symja CAS)
- Depth-limited parsing to prevent stack overflow

### Molecule Plugin (`drishti-molecule`)
- SMILES string parsing (OpenChemLib integration)
- 3D coordinate generation from 2D structures
- PubChem API enrichment (with rate limiting, retry, caching, request coalescing)
- Atom/bond graph representation

### Haptics (`drishti-haptics`)
- VibrationEffect.Composition API (API 30+) with Waveform fallback
- Haptic pattern generation from scene graph elements
- Exploration mode with directional haptic navigation

### Audio (`drishti-audio`)
- Oboe low-latency spatial audio engine
- HRTF (Head-Related Transfer Function) spatialization
- Graph sonification (data-to-audio mapping)
- FrameBuffer with Mutex for thread-safe audio processing

### Voice (`drishti-voice`)
- Sherpa-ONNX offline TTS (text-to-speech)
- Sherpa-ONNX offline STT (speech-to-text)
- Spoken description generation from scene graph
- Exploration mode voice guidance

### Android (`drishti-android`)
- Hardware Abstraction Layer for device capability detection
- CameraCapture with CameraX frame processing
- AudioHAL for audio device enumeration
- Narrow exception handling throughout

### Test Infrastructure (`drishti-test`)
- Shared test fixtures across all modules
- Common test utilities and mock helpers

### Build & Quality
- Kotlin Multiplatform (commonMain + androidMain)
- Binary Compatibility Validator (BCV) 0.18.1 for API stability
- Dokka API documentation generation
- 665+ unit tests across all modules
- Per-module production audit: 0 critical issues
- CI/CD pipeline with build, test, and apiCheck
- JitPack distribution support

### Documentation
- Architecture guide with plugin development instructions
- API reference documentation
- Android integration guide
- Building from source guide
- Contributing guidelines and code of conduct
