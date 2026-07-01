# DrishtiSDK Production Audit Findings
> Generated: 2026-07-01 by Sisyphus (8 parallel audit agents)
> Previous audit: 2026-07-01 (CRITICAL/HIGH fixes applied, now re-auditing)

---

## Executive Summary

**10 modules audited. 100 source files. ~665 tests. 8 research agents.**

| Module | Verdict | Critical | High | Medium | Low |
|--------|---------|----------|------|--------|-----|
| drishti-core | PASS w/ gaps | 0 | 3 | 6 | 12 |
| drishti-vision | **FAIL** | 3 | 9 | 8 | 5 |
| drishti-formula | PASS w/ gaps | 0 | 6 | 8 | 4 |
| drishti-graph | PASS w/ gaps | 2 | 8 | 12 | 5 |
| drishti-molecule | PASS w/ gaps | 0 | 3 | 3 | 3 |
| drishti-haptics | PASS | 0 | 0 | 3 | 3 |
| drishti-audio | PASS | 0 | 0 | 3 | 2 |
| drishti-voice | PASS | 0 | 0 | 1 | 2 |
| drishti-android | **FAIL** | 3 | 8 | 11 | 6 |
| Test suite | **WEAK** | 1 | 38 | 31 | 7 |
| Architecture | 8/10 | 0 | 2 | 3 | 0 |
| OSS Standards | 55/90 (61%) | — | 3 | 3 | — |

**Overall: 9 CRITICAL, 77 HIGH, 88 MEDIUM, ~50 LOW issues found.**
**OSS Readiness: 55/90 (61%) — NOT PRODUCTION READY.**

---

## CRITICAL ISSUES (Must Fix Before Release)

### C1. FrameBuffer.get()/latest()/getAll() return WRONG ORDER after ring buffer wraps
**File**: `drishti-vision/.../FrameBuffer.kt:67`
**Root cause**: `head` points to NEXT write position (oldest frame). Formula `(head + index) % buffer.size` returns oldest-first, not newest-first as documented. After wrapping, `latest()` returns wrong frame, `getAll()` returns wrong order.
**Example**: capacity=3, add frames 1,2,3,4,5,6,7. `get(0)` returns frame 5 instead of frame 7.
**Fix**: Change formula to `(head - 1 - index + buffer.size * 2) % buffer.size`. Fix `latest()` and `getAll()` similarly.

### C2. FrameBuffer allows capacity <= 0 causing ArithmeticException
**File**: `drishti-vision/.../FrameBuffer.kt:32`
**Fix**: Add `init { require(capacity > 0) }`.

### C3. ImagePreprocessor.reduceNoise() crashes with negative kernelSize
**File**: `drishti-vision/.../ImagePreprocessor.kt:135`
**Root cause**: `kernelSize=-1` → half=-1 → range 1..-1 empty → count=0 → division by zero.
**Fix**: Add `require(kernelSize >= 1 && kernelSize % 2 == 1)`.

### C4. GraphDataParser NaN/Infinity values silently corrupt all rendering
**File**: `drishti-graph/.../GraphDataParser.kt:335`
**Root cause**: `"NaN".toFloatOrNull()` returns `Float.NaN` (NOT null). NaN enters DataPoint, corrupts all downstream normalization.
**Fix**: Add `if (yFloat.isNaN() || yFloat.isInfinite())` guard after toFloatOrNull.

### C5. VegaLiteSpec.isNumericAxis() misclassifies real 0..100% data as categorical
**File**: `drishti-graph/.../VegaLiteSpec.kt:236-238`
**Root cause**: Heuristic uses default range `0f..100f` as signal for "no data". Real data with 0..100% range is rendered as discrete categories.
**Fix**: Use explicit flag or check if ALL x values in dataPoints are numeric.

### C6. CameraCapture.imageProxyToFrame extracts only Y-plane, not full YUV_420_888
**File**: `drishti-android/.../CameraCapture.kt:116`
**Root cause**: `planes[0].buffer` extracts only luminance. Frame tagged YUV_420_888 but contains ~50% of data.
**Fix**: Copy all 3 planes (Y+U+V) with correct strides, OR tag as GRAYSCALE.

### C7. CameraCapture.isAnalyzing has thread-safety race condition
**File**: `drishti-android/.../CameraCapture.kt:40`
**Root cause**: Plain `var` read on analysis thread, written from main thread. No @Volatile.
**Fix**: `@Volatile private var isAnalyzing = false`.

### C8. DrishtiClient has no cleanup/disposal — resource leak
**File**: `drishti-android/.../DrishtiClient.kt:69-72`
**Root cause**: `stop()` nulls cameraCapture but not drishti. Re-initialize leaks old Pipeline.
**Fix**: `stop()` should null drishti. `initialize()` should call `stop()` first if already initialized.

### C9. VisionDetectorTest.visionPluginDetectsShapes has VACUOUS assertion
**File**: `drishti-vision/.../VisionDetectorTest.kt:100-106`
**Root cause**: `if (item != null) { assertEquals(...) }` — test passes when item IS null, never asserting anything.
**Fix**: `assertNotNull(item); assertEquals(ContentType.SHAPE, item.contentType)`.

---

## HIGH ISSUES

### drishti-core (3)

| # | File:Line | Issue |
|---|-----------|-------|
| H1 | ContentItem.kt:48,98,172,197,209 | Hardcoded confidence defaults (0.85f-0.92f) — demo pattern |
| H2 | NodeBuilders.kt:122-127 | `orderPosition()` hardcoded pixel constants in normalized 0-1 coordinate space |
| H3 | Pipeline.kt:51-53 | `catch (e: Exception) { null }` swallows all detector exceptions silently |

### drishti-vision (9)

| # | File:Line | Issue |
|---|-----------|-------|
| H4 | FeatureExtractor.kt:248-254 | `groupContours()` is O(n^2) — will fail on real camera images at 30fps |
| H5 | FeatureExtractor.kt:351-367 | `estimateLocalAngle()` is O(n^2) — same performance issue |
| H6 | FeatureExtractor.kt:317-327 | `findLines()` O(k^2) max-distance search per bin |
| H7 | AndroidImagePreprocessor.kt:30-55 | STUB: claims "using OpenCV" but delegates to pure Kotlin — documentation fraud |
| H8 | Expect.kt:38,43 | KDoc claims Gaussian blur and Canny edge detection — implementation uses box blur and Sobel |
| H9 | FeatureExtractor.kt:43,46,49,780,783 | 5 hardcoded thresholds not configurable (edgeThreshold, minLinePoints, etc.) |
| H10 | VisionRenderer.kt:75,123,134,162,189,261,291 | ALL audio/haptic/voice parameters hardcoded — shapes sound identical |
| H11 | VisionDetector.kt:26,38,43 | No KDoc on public API |
| H12 | VisionPlugin.kt:31,44,48 | No KDoc on public API |

### drishti-formula (6)

| # | File:Line | Issue |
|---|-----------|-------|
| H13 | LatexParser.kt:684,487 | `\Leftrightarrow` missing from RELATION_COMMANDS and operator list |
| H14 | FormulaPlugin.kt:112-304 | Exploration pulses from `ast.visit()` don't match `item.symbols` indexing — mismatch causes wrong element highlighted |
| H15 | FormulaRenderer.kt:60-62,96-98 | `catch (_: Exception) { null }` broad catch in haptic/audio rendering |
| H16 | FormulaDetector.kt:53,93,115 | Three `catch (_: Exception) { null }` blocks swallow ALL parsing errors |
| H17 | ParsedFormula.kt:138-162 | Symbol position `offsetX` not accumulated across recursive calls — symbols overlap spatially |
| H18 | FormulaEvaluator.kt:28-35 | Division by zero returns `Infinity` instead of null — `Infinity.isNaN()` is false |

### drishti-graph (8)

| # | File:Line | Issue |
|---|-----------|-------|
| H19 | GraphRenderer.kt:322-331, VegaLiteSpec.kt:396-405 | `graphTypeLabel()` duplicated in two files — DRY violation |
| H20 | GraphRenderer.kt:333-339, VegaLiteSpec.kt:407-413 | `formatNumber()` duplicated in two files — DRY violation |
| H21 | GraphRenderer.kt:341-351 | `normalizeIntensity()` and `normalizePosition()` are identical — DRY violation |
| H22 | GraphDetector.kt:62-65,82-87,104-110,145-151 | All error paths swallow exceptions silently — no logging, no diagnostics |
| H23 | GraphRenderer.kt:171-183 | Pie chart haptic/audio ignores data proportions — all slices identical |
| H24 | VegaLiteSpec.kt:67-68 | Hardcoded 600x400 Vega-Lite dimensions — not configurable |
| H25 | VegaLiteSpec.kt:310-329 | Trend computation uses only first/last data points — ignores intermediate |
| H26 | GraphDataParser.kt:313-316 | `parseDataArray` crashes on non-primitive array elements |

### drishti-molecule (3)

| # | File:Line | Issue |
|---|-----------|-------|
| H27 | MoleculePlugin.kt:189-195,216-218,242-247 | Exploration direction INVERTED: NEXT→size-1, PREVIOUS→0 (opposite of formula fix) |
| H28 | PubChemClient.kt:324-340,360 | Double retry stacking: fetchPropertiesWithRetry wraps fetchProperties = 3x3=9 HTTP requests |
| H29 | MoleculeParser.kt:60-61 | `Ca(OH)2` misclassified as chemical name instead of formula (parentheses not in FORMULA_PATTERN) |

### drishti-android (8)

| # | File:Line | Issue |
|---|-----------|-------|
| H30 | AudioHAL.kt:50 | Unmanaged raw Thread — no lifecycle, no cancellation, no way to abort |
| H31 | AudioHAL.kt:110 | Blocking `Thread.sleep()` on audio thread serves no purpose |
| H32 | AudioHAL.kt:129-135 | Generates identical L/R channels — "spatial audio" with zero spatial differentiation |
| H33 | CameraCapture.kt:68 | `analysisExecutor.shutdown()` prevents restart — can't rebind camera after stop/start |
| H34 | CameraCapture.kt:110-112 | `catch (e: Exception) { e.printStackTrace() }` swallowed camera binding errors |
| H35 | AndroidPlatformDetector.kt:62-67 | SPATIALIZER_BASIC is dead code — never returned by detectAudioLevel() |
| H36 | AndroidPlatformDetector.kt:73-77 | NPU detection uses Build.HARDWARE heuristic — false positives on older Qualcomm, misses Tensor/MediaTek |
| H37 | HapticHAL.kt:61-63 | playPulse silently returns on missing VIBRATE permission — no logging |

### Test Suite (38 — abbreviated, see TEST QUALITY section)

Top severity:
- 1 CRITICAL: Vacuous assertion (C9)
- 38 HIGH: Type-only assertions on core logic, missing error/boundary/concurrency tests, FrameBuffer tests don't verify ordering

---

## MEDIUM ISSUES (88 — abbreviated by module)

### drishti-core (6)
1. EdgeGenerators.kt:19 — PROXIMITY_SCALE=200f not configurable
2. BoundingBoxUtils.kt:66,90 — Hardcoded padding values
3. ExplorationSession.kt:184 — Mutex held during renderer invocation
4. ContentItem.kt:205-212 — TableContent no rows*columns==cells.size validation
5. SceneGraph.kt:141 — SceneEdge.weight no 0..1 validation
6. Drishti.kt:42 — No PipelineConfig through Builder

### drishti-vision (8)
1. ImagePreprocessor.kt:135 — Box blur called "Gaussian blur" in KDoc
2. ImagePreprocessor.kt:54 — Silently passes through unknown frame formats
3. ImagePreprocessor.kt:302-306 — Redundant when-block (always GRAYSCALE)
4. VisionRenderer.kt:265,294 — Tautological `elementIndex >= 0` guard
5. VisionRenderer.kt:209-222 — describeItems ignores non-ShapeContent items
6. FeatureExtractor.kt:539-564 — extractAll efficient but individual methods recompute grayscale+edges
7. build.gradle.kts:27,33 — Potentially unused deps (kotlinx-serialization-json, coroutines-android)
8. VisionData.kt:77-79 — ShapeKind ELLIPSE/LINE_SEGMENT unreachable from classifyShape

### drishti-formula (8)
1. LatexParser — Missing `\oint`, `\mathbf`, `\mathrm`, `\mathcal`, `\text`, `\operatorname`
2. SpeechRuleEngine.kt:191 — Missing `\coth`, `\lg`, `\det`, `\dim`, `\ker` verbalizations
3. FormulaRenderer.kt:45,83,86,111 — Hardcoded spatial coordinates
4. FormulaRenderer.kt:359-361 — normalizePosition hardcodes /200f divisor
5. FormulaEvaluator.kt:129 — Limit evaluation ignores limit, just evaluates body
6. FormulaEvaluator.kt:146-148 — Cases/Matrix/Product return null with no user indication
7. ~190 lines of duplicated exploration logic in FormulaPlugin.kt
8. SpeechRuleEngine.kt:43 — Broad `catch(_: Exception)` catches CancellationException

### drishti-graph (12)
1. GraphDetector.kt:207 — extractNumericPairs regex doesn't handle scientific notation
2. GraphDetector.kt:211-213 — Float parsed before Double (precision loss)
3. GraphDataParser.kt:389-401 — inferChartType() magic thresholds not configurable
4. DataExtractor.kt:147-161 — Broad catch in fromProperties()
5. GraphRenderer.kt:186-195 — Scatter plot ignores y-value for intensity
6. GraphRenderer.kt:353-356 — Hardcoded audio frequency range (130-523Hz)
7. GraphDetector.kt:46 — confidence hardcoded 0.95f
8. GraphDataParser.kt:169-172 — CSV column detection can produce identical xIndex/yIndex
9. GraphDetector.kt:19 — Wildcard imports in all source files
10. GraphRenderer.kt:337, VegaLiteSpec.kt:411 — `"%.2f".format()` may not work in all KMP targets
11. GraphDetector.kt:187-192 — detectFromOcrText always assumes line_chart
12. GraphDataParser.kt:353-363 — Empty range fallback uses same 0..100 as isNumericAxis sentinel

### drishti-molecule (3)
1. MoleculeDetector.kt:103 — Broad `catch (_: Exception)` on decodeToString
2. PubChemClient.kt:70,72 — Hardcoded cache TTL and rate limit
3. PubChemModels.kt:75,99,122,146 — Broad exception catches in parsers

### drishti-haptics (3)
1. HapticRenderer.kt:51,79 — ShapeContent/TableContent silently dropped (else->emptyList)
2. HapticEncoder.kt:79 — Hardcoded sharpness=0.5f for all SDK events
3. PatternBuilder.kt:215 — build() clears state (single-use) but undocumented

### drishti-audio (3)
1. SonificationMapper.kt:65-68 — mapToPanning always returns [0,1], left channel never used
2. ToneGenerator.kt:85-103 — ADSR sustain parameter name misleading (controls time, not amplitude)
3. SpatialRenderer.kt:133 — ShapeContent/TableContent dropped silently

### drishti-voice (1)
1. VoiceData.kt:85-92 — defaultPitchAdjustments all 1.0f (no-op dead config)

---

## LOW ISSUES (~50 — key themes)

- Dead code: ShapeKind unreachable entries, sortByAngleFromCentroid unused, pitch adjustments no-ops
- Missing KDoc on 88% of declarations (~12% coverage)
- Comments restating code in some files
- Minor DRY within files (normalizeIntensity/normalizePosition identical)
- Missing input validation (negative decimalPlaces, zero capacity)
- Bracket speech gap in FormulaSpeech
- Zero test files for drishti-android module
- Wildcard imports in drishti-graph source files

---

## TEST QUALITY ASSESSMENT

### Overall Score: WEAK (55/100)

| Category | Score | Details |
|----------|-------|---------|
| Vacuous assertions | 1 CRITICAL | VisionDetectorTest passes when feature is broken |
| Type-only assertions | 38 HIGH | Tests check format/type but never verify correctness |
| Error-path tests | ABSENT | No tests for null input, corrupt data, network failures across most modules |
| Boundary tests | ABSENT | No tests for zero values, max values, NaN, Infinity |
| Concurrency tests | ABSENT | FrameBuffer Mutex untested, Pipeline concurrency unverified |
| Cancellation tests | ABSENT | No coroutine cancellation test anywhere |
| Test fixtures | WEAK | Trivial fixtures (ByteArray(1) for 640x480 frame, 3-point graph, 5 simple formulas) |
| Stub verification | COMMON | Tests pass because stubs return non-null, not because behavior is correct |

### Module Quality Ranking (test quality):
1. **drishti-formula** — BEST (LatexParser has genuine edge cases)
2. **drishti-molecule** — GOOD (PubChemClient has retry/cache/error tests)
3. **drishti-graph** — MIXED (GraphDataParser good, exploration tests shallow)
4. **drishti-core** — MIXED (SceneGraph/ExplorationSession good, Geometry/ContentItem 100% data-class tests)
5. **drishti-haptics** — ADEQUATE (SceneGraphHapticTest good)
6. **drishti-audio** — WEAK (ToneGenerator tests are non-tests)
7. **drishti-voice** — ADEQUATE
8. **drishti-vision** — WEAKEST (nearly every test is type-only)
9. **drishti-android** — ZERO TESTS (no test files exist)

---

## ARCHITECTURE COMPLIANCE

### Score: 8/10 ✅

The architecture is **solid and matches the README**:
- Plugin system (DetectorPlugin + RendererPlugin) ✅
- Pipeline flow (Vision → Detectors → SceneGraph → Renderers → Diagram) ✅
- Parallel detector execution via `coroutineScope { async { } }` ✅
- Module structure (10 modules + demo) ✅
- SceneGraph adjacency index ✅
- All coordinate positions normalized to 0-1 ✅

### Discrepancies:
1. README shows `ContentType.CUSTOM("my-type")` but implementation is just `ContentType.CUSTOM` (enum, no string param)
2. README shows `detect(frame): List<ContentItem>` but real API is `detect(frame): ContentItem?` (single nullable)
3. **Phantom tech stack**: LiteRT, ONNX Runtime, Sherpa-ONNX declared in README but no dependencies in any build.gradle.kts

---

## OSS STANDARDS GAP

### Score: 55/90 (61%) ❌

| Category | Score | Status |
|----------|-------|--------|
| Explicit API | 10/10 | All 10 modules ✅ |
| Thread Safety | 9/10 | Mutex + Lock consistently used ✅ |
| Anti-Patterns | 8/10 | Zero TODOs, bangs only in tests ✅ |
| Dependency Mgmt | 7/10 | Good catalog, phantom TOML entries ⚠️ |
| Test Ratio | 5/10 | 0.37:1 file ratio (need ≥1:1) ❌ |
| Error Handling | 4/10 | 17+ broad catches, no sealed error hierarchy ❌ |
| KDoc Coverage | 4/10 | ~12% of declarations documented ❌ |
| Binary Compatibility | 0/10 | No validator configured ❌ |

### Top 3 Blocking Issues:
1. **No binary-compatibility-validator** — any API change silently breaks consumers
2. **No sealed error hierarchy** — 17+ `catch (e: Exception)` blocks swallow errors
3. **Test file ratio 0.37:1** — drishti-android has zero tests

---

## PRIORITY FIX ORDER

### Phase 1: CRITICAL (blocks correctness — 9 issues)
1. **FrameBuffer ordering** (C1) — wrong frames served to callers
2. **FrameBuffer capacity validation** (C2) — crash on invalid input
3. **ImagePreprocessor kernelSize validation** (C3) — crash on invalid input
4. **GraphDataParser NaN guard** (C4) — silent data corruption
5. **VegaLiteSpec isNumericAxis** (C5) — wrong chart type for real data
6. **CameraCapture Y-plane only** (C6) — incomplete data for detectors
7. **CameraCapture isAnalyzing race** (C7) — data race on variable
8. **DrishtiClient resource leak** (C8) — orphaned Pipeline
9. **VisionDetectorTest vacuous assertion** (C9) — tests pass when broken

### Phase 2: HIGH (production quality — 37 issues)
- Fix MoleculePlugin exploration direction inversion (H27)
- Fix PubChemClient double retry stacking (H28)
- Fix MoleculeParser parenthesized formulas (H29)
- Extract shared graphTypeLabel/formatNumber (H19-H21)
- Add configurable thresholds to FeatureExtractor (H9)
- Fix FormulaEvaluator division by zero (H18)
- Fix ParsedFormula symbol overlap (H17)
- Add KDoc to all public APIs (H11, H12, + many more)
- Fix CameraCapture executor shutdown (H33)
- Fix AudioHAL thread management (H30-H32)

### Phase 3: MEDIUM (OSS quality — 88 issues)
- Fix DRY violations across modules
- Add missing LaTeX commands (\oint, \mathbf, \text, etc.)
- Fix panning to use full [-1, 1] range
- Add ShapeContent/TableContent support to haptics/audio
- Make hardcoded thresholds configurable
- Add cancellation tests across all suspend functions

### Phase 4: LOW (polish — ~50 issues)
- Remove dead code
- Add KDoc to remaining 88% of declarations
- Clean up wildcard imports
- Add binary-compatibility-validator
- Build sealed error hierarchy
- Add missing test files for drishti-android

---

## OSS BENCHMARK COMPARISON

| Dimension | OSS Benchmark (Ktor, kotlinx) | DrishtiSDK Current | Gap |
|-----------|-------------------------------|--------------------|-----|
| Error handling | Sealed hierarchy + structured metadata | Basic sealed classes + 17 broad catches | Add error codes, paths, remove broad catches |
| Thread safety | Mutex.withLock + atomicfu | Mutex + Lock consistently used | Close to benchmark |
| Explicit API | public on API, internal on impl | Done across all modules | At benchmark |
| Plugin system | AttributeKey + registry + dedup | DetectorPlugin/RendererPlugin | Needs registry validation |
| Test ratio | 1:1 to 4:1 | 0.37:1 (100 src / 29 test files) | Need ~70+ test files |
| AI slop check | allWarningsAsErrors, no !! | Done + flags | At benchmark |
| Binary compat | binary-compatibility-validator | Not configured | **Gap** |
| KDoc | 80%+ public API | ~12% coverage | **Major gap** |
