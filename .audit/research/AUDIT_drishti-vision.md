# drishti-vision Per-Line Audit

Generated: 20260705T05:00:00Z
Module: `drishti-vision`
File count: 8 commonMain files
LOC: ~1700 across commonMain

---

## Files Reviewed

| File | LOC | Surface Quality |
|------|-----|-----------------|
| FeatureExtractor.kt | 840 | GOOD — real algorithms (Sobel, RDP, convex hull, shoelace) |
| ImagePreprocessor.kt | 338 | GOOD — Otsu, histogram EQ, Sobel, box blur |
| VisionRenderer.kt | 331 | GOOD — multi-modal rendering, exploration support |
| VisionPlugin.kt | 91 | GOOD — clean delegation |
| VisionDetector.kt | 71 | GOOD — simple, focused |
| Expect.kt | 56 | GOOD — expect/actual pattern |
| FrameBuffer.kt | 104 | GOOD — Mutex-protected ring buffer |
| VisionData.kt | 81 | GOOD — clean data classes |

---

## Real Per-Line Issues Found

### VIS-1: FeatureExtractor.findLines — dead IntArray allocation

**File:** `FeatureExtractor.kt:297`

**Issue:** `val gray = IntArray(width * height)` is allocated but never read. Comment says "already available if needed; recompute minimal" — this is dead code that wastes `width * height * 4` bytes.

**Fix:** Remove the dead allocation.

**Severity:** LOW (dead code, memory waste)

### VIS-2: FeatureExtractor.bytesPerPixel duplicated 5 times

**File:** `FeatureExtractor.kt:67-71, 90-94, 115-119, 140-144, 542-546`

**Issue:** The `bytesPerPixel` computation (YUV→1, GRAYSCALE→1, else→3) + size validation is copy-pasted across `extractContours`, `extractLines`, `extractTextRegions`, `extractROIs`, and `extractAll`.

**Fix:** Extract into a private `fun validateFrame(frame: Frame): ByteArray?` helper.

**Severity:** LOW (DRY, not correctness)

### VIS-3: ImagePreprocessor.toGrayscaleBytes duplicates FeatureExtractor.toGrayscale

**File:** `ImagePreprocessor.kt:32-56` vs `FeatureExtractor.kt:158-181`

**Issue:** Both implement the same ITU-R BT.601 grayscale conversion. Two independent implementations risk diverging over time.

**Status:** OK — different classes serve different purposes. Shared extraction would create coupling. Accept as-is.

**Severity:** INFO (design note, not a fix)

### VIS-4: VisionRenderer.shapeExplorationHaptic — elementIndex unused for shapes

**File:** `VisionRenderer.kt:261-330`

**Issue:** `shapeExplorationHaptic/Audio/Voice` accept `elementIndex` but for shapes, the parameter only gates PREVIOUS (returns empty if index==0). The shape is a single entity — `elementIndex` is semantically misleading here.

**Status:** OK — interface contract requires it. The behavior (returning empty for PREVIOUS at index 0) is correct for the navigation model.

**Severity:** INFO (interface alignment, not a bug)

---

## Summary for drishti-vision

| Severity | Count | Status |
|----------|-------|--------|
| MEDIUM | 0 | — |
| LOW | 2 | VIS-1 (dead code), VIS-2 (DRY) |
| INFO | 2 | VIS-3, VIS-4 |

**Verdict:** drishti-vision is production-quality. Real algorithms (Sobel, Otsu, RDP, convex hull, shoelace formula) are correctly implemented. The only actionable fixes are removing dead code and reducing duplication.
