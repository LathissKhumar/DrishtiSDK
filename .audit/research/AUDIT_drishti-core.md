# drishti-core Per-Line Audit

Generated: 20260702-045218Z  
Module: `drishti-core`  
File count: 17 commonMain + 1 androidMain + 9 test files = 27 files  
LOC: ~2000 across commonMain

---

## Files Reviewed

| File | LOC | Surface Quality |
|------|-----|-----------------|
| Drishti.kt | 60 | GOOD — clean Builder API |
| DrishtiDiagram.kt | 71 | GOOD — proper separator |
| Pipeline.kt | 119 | GOOD — concurrent, validated |
| PipelineConfig.kt | 31 | GOOD — @Serializable, defaults |
| Registry.kt | 238 | GOOD — Mutex-protected lookup |
| ExplorationSession.kt | 268 | MIXED |
| ContentItem.kt | 248 | GOOD — sealed hierarchy clear |
| ContentType.kt | 33 | GOOD — @Serializable enum |
| DetectorPlugin.kt | 42 | GOOD — interface minimal |
| RendererPlugin.kt | 49 | GOOD — interface minimal |
| SceneGraph.kt | 164 | GOOD — indexes lazy-built |
| Frame.kt | 60 | GOOD — with init validation |
| Geometry.kt | 67 | GOOD — init validation |
| BoundingBoxUtils.kt | 103 | GOOD — pure math |
| EdgeGenerators.kt | 196 | GOOD — heuristics isolated |
| NodeBuilders.kt | 132 | GOOD — pure functions |
| Lock.kt (commonMain) | 31 | GOOD — expect/actual pattern |
| Lock.kt (androidMain) | 31 | OK — ReentrantLock impl |
| Output.kt | 88 | GOOD — init validation |

---

## Real Per-Line Issues Found

### CORE-1: ExplorationSession — empty `Running`/`muted` race risk

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/ExplorationSession.kt:99-110`

**Issue:** `previousElement()` decrements `currentElementIndex` then calls `describeElement(item, currentElementIndex)`. If `items[currentItemIndex]` is a `FormulaContentItem` and the index is at -1, `describeElement` returns "". The test `continuousNextPreviousCallsDoNotCrash` (ExplorationSessionTest:225) covers happy-path — but doesn't cover concurrent `next()` interleaved with `nextElement()` from different coroutines.

**Why it matters:** Coroutine cancellation during `mutex.withLock` is auto-handled by Mutex.suspendable locks — but `renderers.filterIsInstance<X>().firstOrNull()` is iterating a snapshot list (renderers is `List<RendererPlugin>` — immutable from constructor). Confirm `renderers` is captured immutably. Looked at line 33-34: `private val renderers: List<RendererPlugin>` — yes, immutable. OK.

**Real issue:** `Mutex()` from kotlinx.coroutines lacks reentrance. If `exploreVoice()` from a coroutine suspends while `another suspend fun` is awaiting the same mutex — kotlinx Mutex is **fair/non-reentrant**, so reentrant calls deadlock. Verified by reading `kotlinx.coroutines.sync.withLock` doc.

**Fix:** Either document "non-reentrant" explicitly in KDoc, OR switch to `synchronized` for reentrance. For coroutine code, non-reentrant is correct behavior — current code is fine, just document.

**Severity:** LOW (doc gap)

### CORE-2: ExplorationSession.describeElement returns "" silently

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/ExplorationSession.kt:227-247`

**Issue:** When `getOrNull(index)` returns null, returns "". Caller can't distinguish "no element" from "empty description".

**Why it matters:** Test `descriptionForGenericContent` (line 181) likely passes for CONTENT cases, but `elementNavigationAcrossElements` (line 189) may not exercise the edge case where an atom at a given index doesn't exist (out-of-bounds).

**Fix:** Throw `IllegalStateException("Element index $index out of range")` instead of returning empty string. Caller `nextElement()` already checks bounds at line 88 — so `getOrNull` should never return null there. For `describeElement`, return `null` and let caller decide. Current code: silently empty string = bad.

**Severity:** MEDIUM (silent failure of accessibility-critical string)

### CORE-3: ExplorationSession callbacks acquire renderer inside mutex

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/ExplorationSession.kt:144-179`

**Issue:** `haptic()`, `audio()`, `voice()` callbacks hold the mutex while calling `renderers.filterIsInstance<X>().firstOrNull()`. Even though renderers is immutable, this couples renderer lookup with state lookup under lock. The `exploreHaptic`, `exploreAudio`, `exploreVoice` (line 184+) correctly use the snapshot-release-call pattern. The simpler `haptic()` doesn't.

**Why it matters:** Renderer lookup happens under mutex; if filter allocation becomes expensive (N renderers), this holds the lock longer than necessary.

**Fix:** Match the pattern from `exploreHaptic`. Snap the renderer and item under lock, release, then call.

**Severity:** LOW (perf, not correctness)

### CORE-4: Pipeline.detect fails to filter out detectors by confidence threshold

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/Pipeline.kt:43-65`

**Issue:** `PipelineConfig.minConfidence = 0.3f` is defined but **NEVER USED**. Detectors always run regardless of their own confidence threshold.

**Why it matters:** A `DetectorPlugin` declares its `confidence` threshold (content type + minimum confidence), but Pipeline doesn't apply `minConfidence` filter. This silently ignores user config and breaks the "configurable" claim.

**Fix:** Add filter: `results.filter { it.confidence >= config.minConfidence }` after `filterNotNull()`.

**Severity:** HIGH — silently broken config

### CORE-5: Pipeline.detect does not enforce maxItemsPerFrame / explorationElementLimit

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/PipelineConfig.kt:25-30`

**Issue:** `maxItemsPerFrame = 50` and `explorationElementLimit = 100` declared but never consulted. A 200-element frame will be processed fully.

**Why it matters:** These limits exist for resource protection. Without enforcement, a malicious or pathological detector output could exhaust memory.

**Fix:** Truncate at `.take(config.maxItemsPerFrame)` after `filterNotNull()`.

**Severity:** MEDIUM

### CORE-6: DrishtiDiagram.haptics/audio/voice — illegal-state exception lost

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/DrishtiDiagram.kt:28-56`

**Issue:** All three use `Result.failure(IllegalStateException(...))` — but the `IllegalStateException` carries the cause as a `String` ("No haptic renderer..."). The exception type is wrong — it should be a domain-sealed error.

**Why it matters:** Caller doing `result.exceptionOrNull()!!::class.simpleName` sees `IllegalStateException` — not informative. Better to use a sealed error type or at minimum a typed exception.

**Fix:** Either:
- Use `DrishtiException` sealed type (already exists from Wave 0)
- OR keep IllegalStateException but provide better message including the missing renderer role name in a structured way

**Severity:** LOW (works, but typed errors would be better)

### CORE-7: Output.kt — HapticPulse/AudioSource validation throws on invalid input

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/Output.kt:22-65`

**Issue:** `init { require(...) }` clamps invalid inputs via exceptions. Correct behavior. BUT the validation is duplicated and string-coded; no named error type. Minor.

**Status:** OK

### CORE-8: Drishti.Builder.build requires detectors but never validates renderers

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/Drishti.kt:54-58`

**Issue:** `require(detectors.isNotEmpty())` only ensures at least one detector. A user with only a graph detector but no renderer will silently get `Result.failure(IllegalStateException(...))` later. Better: warn at build time using PluginRegistry.validate().

**Why it matters:** Fail-fast principle.

**Fix:** After builders finalize, validate that each renderer's required content types have detectors, warn or fail early.

**Severity:** LOW

### CORE-9: DetectionPlugin — PluginRegistry doesn't validate DetectorPlugin.confidence invariant

**File:** `drishti-core/src/commonMain/kotlin/io/drishti/core/DetectorPlugin.kt:33`

**Issue:** `DetectorPlugin.confidence` is declared as the threshold (0.0-1.0). Nothing enforces it's in range. A plugin declaring `confidence = 1.5f` would silently never produce items (because of CORE-4 fix).

**Fix:** Add validation in registerDetector: `plugin.confidence.let { require(it in 0f..1f) { "Detector confidence must be 0.0-1.0" } }`.

**Severity:** MEDIUM (silent acceptance of invalid plugin)

---

## Summary for drishti-core

| Severity | Count | Status |
|----------|-------|--------|
| HIGH | 1 | CORE-4 (minConfidence unused) |
| MEDIUM | 4 | CORE-2, CORE-5, CORE-9 + others |
| LOW | 4 | CORE-1, CORE-3, CORE-6, CORE-8 |

**Real fixes to apply:**
1. CORE-4: USE `config.minConfidence` in `Pipeline.detect`
2. CORE-5: USE `config.maxItemsPerFrame` truncation
3. CORE-9: Validate `DetectorPlugin.confidence` range on registration
4. CORE-2: Replace empty-string fallback with explicit null/throw

---

## Test Coverage Analysis

**Existing tests cover:** happy paths for detect, buildSceneGraph, registry CRUD, exploration linear traversal.

**Missing test coverage (real gaps):**
- Concurrent exploration (multiple coroutines navigating same session)
- Pipeline respects `config.minConfidence` filter
- Pipeline truncates at `config.maxItemsPerFrame`
- DetectorPlugin.confidence range validation in registerDetector
- describeElement behavior for out-of-bounds index

**Tests that test wrong things:**
- `moleculeWithNoAtomsFallsBackToOrderPosition`: asserts `(0.1, 0.1)` which is the position-result, not "no atoms → fallback" — semantically weak test

---

## Architectural Verdict

Drishti-core is structurally sound. All systems use:
- `kotlinx.coroutines.sync.Mutex` (correct for coroutine concurrency)
- `init { require(...) }` for input validation
- `expect/actual` for platform-specific code
- `@Serializable` for data classes
- `companion object` for constants (mostly)

**Issues are integration gaps (config not wired) + edge-case handling (empty strings, validation gaps), NOT structural.**

After 4 targeted fixes, this module is OSS-grade.
