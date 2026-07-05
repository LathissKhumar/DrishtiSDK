# drishti-graph Per-Line Audit

Generated: 20260705T05:15:00Z
Module: `drishti-graph`
File count: 6 commonMain files
LOC: ~1880 across commonMain

---

## Files Reviewed

| File | LOC | Surface Quality |
|------|-----|-----------------|
| VegaLiteSpec.kt | 437 | GOOD — clean Vega-Lite v5 spec generation |
| GraphDataParser.kt | 466 | GOOD — JSON/CSV parsing with type inference |
| GraphRenderer.kt | 347 | GOOD — real haptic/audio/voice rendering |
| GraphPlugin.kt | 351 | GOOD — clean facade with all 3 renderer interfaces |
| GraphDetector.kt | 238 | GOOD — data-first detection with OCR fallback |
| DataExtractor.kt | 237 | GOOD — unified extraction with auto-detect |

---

## Real Per-Line Issues Found

### GR-1: GraphPlugin.renderHaptic — wasteful list allocation

**File:** `GraphPlugin.kt:172-187`

**Issue:** `val graphItems = items.filterIsInstance<GraphContent>()` creates a filtered list and checks `isEmpty()`, but then `items.mapIndexedNotNull` iterates the ORIGINAL unfiltered list, re-doing the `filterIsInstance` check per-item. Same pattern in `renderAudio` (line 194) and `renderVoice` (line 212).

**Fix:** Use the filtered `graphItems` list instead of re-iterating `items`.

**Severity:** MEDIUM (wasteful allocation + redundant type checks)

### GR-2: VegaLiteSpec.buildMark — dead local variable

**File:** `VegaLiteSpec.kt:104`

**Issue:** `val markType = markTypeForGraph(graph.graphType)` is computed but never referenced. The `when` block on line 106 rebuilds the mark from scratch using `graph.graphType` directly.

**Fix:** Remove dead variable.

**Severity:** LOW (dead code)

### GR-3: GraphDataParser.parseCsv — variable shadows parameter

**File:** `GraphDataParser.kt:209`

**Issue:** `val input = GraphDataInput(...)` shadows the function parameter `input: String` from line 163.

**Fix:** Rename to `graphInput`.

**Severity:** LOW (shadowing, not a bug)

### GR-4: GraphDataParser.parseJson — CancellationException catch on wrong type

**File:** `GraphDataParser.kt:137`

**Issue:** `catch (e: Exception)` at line 137 covers `CancellationException`. Line is missing `if (e is CancellationException) throw e` guard. The JSON parsing at line 136 is `json.parseToJsonElement(trimmed)` which is synchronous — CancellationException won't trigger here. Low risk but inconsistent with other catch blocks in the codebase.

**Fix:** Add CancellationException guard for consistency.

**Severity:** LOW (defensive, not a current bug)

---

## Summary for drishti-graph

| Severity | Count | Status |
|----------|-------|--------|
| MEDIUM | 1 | GR-1 (wasteful iteration) |
| LOW | 3 | GR-2 (dead var), GR-3 (shadow), GR-4 (missing guard) |

**Verdict:** drishti-graph is production-quality. Real algorithms (data parsing, type inference, Vega-Lite generation, multi-modal rendering). The only meaningful fix is GR-1 (wasteful filtered list in GraphPlugin renderers).
