# Architecture Compliance & OSS Standards Gap Analysis

Generated: 2026-07-01T05:26:00Z
Source: Direct codebase analysis + Kotlin OSS benchmark research

---

## Executive Summary

DrishtiSDK is **architecturally sound** — the plugin system, pipeline, SceneGraph, and module structure all match the README description. However, it has **significant OSS quality gaps** when measured against production Kotlin libraries like kotlinx.coroutines, Ktor, and kotlinx.serialization. The most critical gaps are: missing binary compatibility validation, weak error handling (broad catches, no sealed error hierarchy), below-1:1 test ratio, and incomplete KDoc coverage.

**Verdict: Architecture = PASS. OSS Standards = NEEDS WORK.**

---

## 1. Architecture Compliance (README vs Implementation)

### 1.1 Plugin System

| Aspect | README Says | Implementation | Status |
|:---|:---|:---|:---|
| Plugin interfaces | `DetectorPlugin`, `HapticsRenderer`, `AudioRenderer`, `VoiceOutputRenderer` | All 4 interfaces exist in `drishti-core` | ✅ MATCH |
| ContentType extensibility | `ContentType.CUSTOM("my-type")` | `ContentType` is an enum with `CUSTOM` entry (no string parameter) | ⚠️ DISCREPANCY |
| Plugin registration | `Drishti.Builder().addDetector().addRenderer().build()` | Exact match in `Drishti.kt` | ✅ MATCH |
| Plugin detection method | `detect(frame: Frame): List<ContentItem>` | `detect(frame: Frame): ContentItem?` (returns single nullable, not list) | ⚠️ DISCREPANCY |
| Renderer methods | `renderHaptics(item: ContentItem): HapticOutput` | `renderHaptic(items: List<ContentItem>, focusIndex: Int): HapticOutput` (list + index) | ⚠️ DISCREPANCY |

**Analysis:** The README plugin example shows simplified API signatures that don't match the actual implementation. The real API is more sophisticated (list-based with focus index), which is better for production — but the README misleads plugin authors.

### 1.2 Pipeline Architecture

| Aspect | README Says | Implementation | Status |
|:---|:---|:---|:---|
| Pipeline flow | Vision → Detector Registry → Scene Graph → Renderer Registry → Drishti Diagram | `Pipeline.detect()` → `Pipeline.buildSceneGraph()` → `DrishtiDiagram` | ✅ MATCH |
| Parallel execution | "Parallel execution of all registered detectors" | `coroutineScope { detectors.map { async { ... } }.awaitAll() }` | ✅ MATCH |
| SceneGraph | "Unified semantic representation" | `SceneGraph(nodes, edges, bounds)` with adjacency index | ✅ MATCH |
| Output accessors | `.haptics() .audio() .voice() .explore()` | All 4 methods exist on `DrishtiDiagram` | ✅ MATCH |

### 1.3 Module Structure

| Module | README Listed | Exists in Repo | Has Tests | Status |
|:---|:---|:---|:---|:---|
| drishti-core | ✅ | ✅ | ✅ (8 test files) | ✅ MATCH |
| drishti-vision | ✅ | ✅ | ✅ (4 test files) | ✅ MATCH |
| drishti-graph | ✅ | ✅ | ✅ (1 test file) | ✅ MATCH |
| drishti-formula | ✅ | ✅ | ✅ (2 test files) | ✅ MATCH |
| drishti-molecule | ✅ | ✅ | ✅ (4 test files) | ✅ MATCH |
| drishti-haptics | ✅ | ✅ | ✅ (6 test files) | ✅ MATCH |
| drishti-audio | ✅ | ✅ | ✅ (4 test files) | ✅ MATCH |
| drishti-voice | ✅ | ✅ | ✅ (1 test file) | ✅ MATCH |
| drishti-android | ✅ | ✅ | ❌ (0 test files) | ⚠️ MISSING TESTS |
| drishti-demo | ✅ | ✅ | N/A (demo app) | ✅ MATCH |
| drishti-test | ❌ (not listed) | ✅ | N/A (test fixtures) | ℹ️ UNDOCUMENTED |

### 1.4 Tech Stack

| Layer | README Says | Actual Dependency | Status |
|:---|:---|:---|:---|
| Language | Kotlin 2.1 + C++ (NDK) | Kotlin 2.1.20, no native source files visible | ⚠️ C++ CLAIM UNVERIFIED |
| Build | Gradle 8.11 + KMP | Gradle with KMP, version not in toml | ✅ MATCH |
| Vision | OpenCV 4.13 + CameraX 1.5 | `opencv = "4.13.0"`, `camerax = "1.5.0"` | ✅ MATCH |
| ML | LiteRT + ONNX Runtime | In `libs.versions.toml` but **NOT in any build.gradle.kts** | ❌ DECLARED BUT UNUSED |
| Haptics | VibrationEffect.Composition (API 30+) | `minSdk = 30`, `HapticHAL.kt` uses vibration | ✅ MATCH |
| Audio | Oboe 1.9.3 + Android Spatializer | `oboe = "1.9.3"` in drishti-audio | ✅ MATCH |
| Voice | Sherpa-ONNX (offline STT/TTS) | **No sherpa-onnx dependency anywhere** | ❌ NOT IMPLEMENTED |
| Serialization | (implied kotlinx.serialization) | `serialization = "1.8.1"` | ✅ MATCH |
| HTTP | (not listed) | Ktor 3.1.0 in drishti-molecule | ℹ️ UNLISTED |

**Critical Discrepancies:**
1. **LiteRT + ONNX** declared in README tech stack table but not in any `build.gradle.kts` — phantom dependencies
2. **Sherpa-ONNX** listed as the voice engine but no dependency exists — voice module is likely stubbed
3. **drishti-test** module exists but is not documented in README

---

## 2. OSS Quality Benchmarks (DrishtiSDK vs Kotlin OSS Libraries)

### 2.1 Explicit API Mode

| Library | Uses `explicitApi()`? | Scope |
|:---|:---|:---|
| kotlinx.coroutines | Yes | All modules |
| Ktor | Yes | All modules |
| kotlinx.serialization | Yes | All modules |
| **DrishtiSDK** | **Yes** | **All 10 library modules** |

**Verdict: ✅ PASS** — All library modules (not demo) have `explicitApi()` and `allWarningsAsErrors = true`.

### 2.2 Error Handling

| Library | Pattern | Example |
|:---|:---|:---|
| Ktor | Sealed class hierarchy | `HttpResponse` with status-based errors |
| kotlinx.coroutines | CancellationException re-throw discipline | Always re-throw `CancellationException` |
| Arrow (FP library) | `Either<L, R>` for typed errors | `sealed class AppError` |
| **DrishtiSDK** | **Mixed: `Result<T>` + broad catches + single exception class** | See below |

**DrishtiSDK Error Handling Issues:**

| Issue | Count | Location | Severity |
|:---|:---|:---|:---|
| `catch (e: Exception)` broad catch | 17 | 6 files | HIGH |
| `catch (_: Exception)` silently swallowed | 2 | Pipeline.kt, DrishtiDiagram.kt | HIGH |
| Single exception class (`FormulaParseException`) | 1 | drishti-formula | MEDIUM |
| No sealed error hierarchy | — | Global | MEDIUM |
| `Result<T>` usage (good) | 9 calls | DrishtiDiagram.kt | ✅ GOOD |
| `CancellationException` re-throw | 1 | Pipeline.kt L52-53 | ✅ GOOD |
| Missing `CancellationException` re-throw | 15 | Other catch blocks | HIGH |

**What's Missing vs OSS Standard:**
- No `sealed class DrishtiError` with subtypes like `NoRendererError`, `DetectionError`, `NetworkError`
- `catch (e: Exception)` in `PubChemClient.kt` (6 places) catches everything — should catch specific `IOException`, `CancellationException` should re-throw
- `DrishtiDiagram.haptics()` catches `Exception` generically — should use structured error

**Specific Broad Catch Locations:**
```
drishti-android/CameraCapture.kt:110          — catch (e: Exception)
drishti-core/DrishtiDiagram.kt:33,43,53       — catch (e: Exception) x3
drishti-graph/DataExtractor.kt:150            — catch (e: Exception)
drishti-graph/GraphDataParser.kt:126          — catch (e: Exception)
drishti-molecule/PubChemClient.kt:175,218,265,310,333,351 — catch (e: Exception) x6
drishti-molecule/PubChemModels.kt:75,99,122,146,165 — catch (e: Exception) x5
```

### 2.3 Test Ratio

| Metric | DrishtiSDK | OSS Benchmark | Status |
|:---|:---|:---|:---|
| Source files (non-test, non-demo) | 79 | — | — |
| Test files | 29 | — | — |
| **File ratio** | **0.37:1** | ≥1:1 expected | ❌ FAIL |
| Source LOC | 14,727 | — | — |
| Test LOC | 7,724 | — | — |
| **LOC ratio** | **0.52:1** | ≥1:1 expected | ❌ FAIL |
| Total test count | ~665+ tests | — | ✅ Good raw count |

**Module-Level Test Coverage:**

| Module | Source Files | Test Files | Ratio | Status |
|:---|:---|:---|:---|:---|
| drishti-core | 18 | 8 | 0.44 | ⚠️ LOW |
| drishti-vision | 8 | 4 | 0.50 | ⚠️ LOW |
| drishti-graph | 7 | 1 | 0.14 | ❌ VERY LOW |
| drishti-formula | 10 | 2 | 0.20 | ❌ VERY LOW |
| drishti-molecule | 7 | 4 | 0.57 | ⚠️ LOW |
| drishti-haptics | 7 | 6 | 0.86 | ⚠️ NEAR |
| drishti-audio | 6 | 4 | 0.67 | ⚠️ LOW |
| drishti-voice | 5 | 1 | 0.20 | ❌ VERY LOW |
| drishti-android | 5 | 0 | 0.00 | ❌ NO TESTS |

**Note:** While raw test count (665+) is respectable, many tests use assertion-per-line style inflating the count. The file ratio is the honest metric — only drishti-haptics approaches parity.

### 2.4 KDoc / Documentation Coverage

| Metric | Count | Status |
|:---|:---|:---|
| KDoc blocks (`/**`) | 514 | — |
| Total declarations (fun/class/interface/val/var) | ~4,227 | — |
| **KDoc coverage** | **~12%** | ❌ LOW |
| Dokka configured | Yes (multi-module) | ✅ GOOD |
| `.api` dump files | None | ❌ MISSING |

**What's Documented Well:**
- Core interfaces (`DetectorPlugin`, `RendererPlugin`, `ContentItem`) — full KDoc with @param, @return
- `Pipeline.kt` — comprehensive KDoc with edge-case documentation
- `SceneGraph` — thorough documentation
- `ExplorationSession` — full method-level KDoc

**What's Missing KDoc:**
- Most data classes (GraphContent, MoleculeContent, etc. have KDoc but many inner fields don't)
- Plugin implementations (GraphPlugin, FormulaPlugin — minimal)
- Utility classes (BoundingBoxUtils, Geometry, NodeBuilders)
- Test fixtures — none documented (acceptable)

### 2.5 Thread Safety

| Pattern | Usage | Status |
|:---|:---|:---|
| `kotlinx.coroutines.sync.Mutex` | FrameBuffer, ExplorationSession, PubChemClient | ✅ GOOD |
| `expect/actual Lock` (ReentrantLock) | PluginRegistry | ✅ GOOD |
| `synchronized` blocks | None visible | ✅ N/A |
| `@Volatile` annotations | None visible | ⚠️ May be needed |
| `atomicfu` | Not used | ℹ️ Not required at this scale |

**Analysis:** Thread safety is consistently applied where mutable state exists. The `Lock` expect/actual pattern is clean for KMP. Mutex usage in suspend contexts is correct.

### 2.6 Anti-Pattern Detection

| Anti-Pattern | Count | Severity | Location |
|:---|:---|:---|:---|
| `!!` bang operators | 16 | LOW (all in tests) | Test files only |
| Unsafe `as` casts (without null check) | ~24 | MEDIUM | LatexParser.kt |
| `@Suppress("UNCHECKED_CAST")` | 1 | LOW | Registry.kt:145 |
| `@Suppress("DEPRECATION")` | 3 | LOW | HapticHAL.kt |
| `@Suppress("UNUSED_PARAMETER")` | 1 | LOW | FeatureExtractor.kt:425 |
| TODO/FIXME/HACK | 0 | ✅ CLEAN | — |
| Comments restating code | ~5 | LOW | Various |

**LatexParser.kt Unsafe Casts:** The file contains 24+ `(peek() as Token.Command)` and `(peek() as Token.Letter)` casts without null-safety. These are "safe" because the parser controls the token stream, but they violate Kotlin best practices. A `sealed class` with smart-cast patterns would be safer.

### 2.7 Binary Compatibility

| Feature | Status |
|:---|:---|
| `kotlinx.binary-compatibility-validator` plugin | ❌ NOT CONFIGURED |
| `.api` dump files | ❌ NONE |
| API tracking across versions | ❌ NONE |

**Impact:** Without binary compatibility validation, any public API change can silently break consumers. This is a hard requirement for production OSS libraries.

### 2.8 Dependency Management

| Feature | Status |
|:---|:---|
| Version catalog (`libs.versions.toml`) | ✅ YES |
| Centralized versions | ✅ YES |
| Unused dependencies in TOML | ⚠️ tensorflow-lite, onnx-runtime declared but unused |
| `api` vs `implementation` | Mostly correct; `drishti-test` uses `api` for core (good) |
| Dependency locking (`gradle.lockfile`) | ❌ NOT CONFIGURED |

---

## 3. Summary Scorecard

| Category | Score | Benchmark |
|:---|:---|:---|
| Architecture Compliance | **8/10** | README vs implementation match |
| Explicit API Mode | **10/10** | All modules |
| Error Handling | **4/10** | No sealed hierarchy, 17 broad catches |
| Test Ratio | **5/10** | 0.37:1 file ratio (target ≥1:1) |
| KDoc Coverage | **4/10** | ~12% of declarations |
| Thread Safety | **9/10** | Mutex + Lock consistently applied |
| Anti-Patterns | **8/10** | Zero TODOs, bangs only in tests |
| Binary Compatibility | **0/10** | Not configured |
| Dependency Management | **7/10** | Good catalog, phantom deps in TOML |

**Overall OSS Readiness: 55/90 (61%) — NOT PRODUCTION-READY**

---

## 4. Priority Remediation Plan

### P0 — Blocking Release
1. **Add `kotlinx.binary-compatibility-validator`** plugin and generate `.api` files
2. **Seal the error hierarchy** — create `sealed class DrishtiError` with subtypes
3. **Eliminate broad catches** — catch specific exceptions, re-throw `CancellationException`

### P1 — Before 1.0
4. **Increase test ratio to ≥1:1** — prioritize drishti-android (0 tests), drishti-voice (1 test), drishti-graph (1 test)
5. **Fix README discrepancies** — update plugin example signatures, remove phantom tech stack claims (LiteRT, ONNX, Sherpa-ONNX)
6. **Add KDoc to all public APIs** — target ≥80% coverage on public declarations

### P2 — Post 1.0
7. **Clean LatexParser unsafe casts** — use smart-cast patterns or sealed class matching
8. **Remove unused TOML entries** — tensorflow-lite, onnx-runtime (or implement their usage)
9. **Document drishti-test module** in README
