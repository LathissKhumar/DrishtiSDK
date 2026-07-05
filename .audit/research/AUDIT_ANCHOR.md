# DrishtiSDK — Per-Line Production Audit Anchor

**Initiated:** 20260702-045218Z  
**Session:** Ralph-Loop / Ultrawork  
**Goal:** Make DrishtiSDK truly production-grade (OSS standard). Audit EVERY line of code, fix every non-production-ready piece.  

---

## User's Mandate (verbatim)

> "fire research agents for each and every line of code and analyse them if they are correct or is there anything better available to implement them"
> "some code is like demo piece which is capable of handling only some thing like only some cases"
> "The code quality must be Open source software standard and should not look like ai generated code"
> "verify if everything here is according to the architecture and according to the project plan. if anything left our or not implemented implement them too"
> "First do a audit on this and store it in the .audit/ directory so that when you run out of credits and then when you are back , you will not lose content"

---

## Critical Pin: Working Directory

**ALWAYS operate from `/home/lathiss/Projects/DrishtiSDK/` (NOT `../DrishtiSTEM/`).**  
DrishtiSTEM is empty docs-only directory. DrishtiSDK contains all 10 modules.

---

## Progress Anchor (UPDATE THIS EACH ITERATION)

### Wave State (cumulative across sessions)

| Wave | Description | Status | Issues Fixed | Audit Section |
|------|-------------|--------|--------------|----------------|
| Wave 0 | Error infrastructure (DrishtiErrors.kt, NonFatal.kt) | ✅ DONE | C2/utility | See PRODUCTION_AUDIT_FINDINGS.md |
| Wave 1 | CRITICAL/HIGH fixes (9 critical, 77 high) | ✅ DONE | 86 | See PRODUCTION_AUDIT_FINDINGS.md |
| Wave 2 | MEDIUM structural fixes (8 modules) | ✅ DONE | 90 | See PRODUCTION_AUDIT_FINDINGS.md |
| Wave 3 | Broad catch replacement + CancellationException | ✅ DONE | included in Wave 2 | See PRODUCTION_AUDIT_FINDINGS.md |
| Wave 4 | BCV plugin setup + .api generation | ✅ DONE | infra | 9 .api files generated |
| Wave 5 | Wildcard imports cleanup | ✅ DONE | infra | All modules clean |

### Current Verification State

- **assembleDebug**: GREEN (456 tasks, 0 failures)
- **testDebugUnitTest**: GREEN (665+ tests, 0 failures)
- **apiCheck**: GREEN
- **LSP diagnostics**: clean on changed files

### Remaining Open Issues (DEFERRED PREVIOUS SESSIONS)

| Module | Open Issues |
|--------|------------|
| drishti-vision | C4 OCR implementation (DEFERRED — requires native Tesseract/ML Kit) |
| drishti-android | HAL/CameraX integration testing (EMULATOR AVAILABLE) |
| drishti-demo | Demo app missing/empty? |
| ~20 cosmetic LOW | Not blocking |

---

## Per-Line Audit Methodology (THIS SESSION'S WORK)

### Why this audit differs

Previous Wave 1-5 addressed overall structure. This per-line audit goes DEEPER:
1. **For every .kt file in commonMain**: examine each public function for:
   - Correctness (does it produce the documented result?)
   - Edge cases (null, empty, extreme values, concurrent access)
   - Concurrency safety (mutable state, locks, suspension points)
   - Algorithm correctness (CS109-grade review)
   - Documentation accuracy (KDoc matches impl)
   - API design (idiomatic Kotlin, sealed Result-style, no leaks)
2. **For every Android bridge file**: verify JNI signatures, lifecycle correctness, ANR risk
3. **For every test**: verify it actually tests the specified behavior (not just compiles)
4. **For every mock/stub**: flag for production replacement if reached by non-test code

### Output

For each module: write `.audit/research/MODULE_AUDIT_<module>.md` with:
- File:line evidence
- "What it does vs. what it should do" diff
- Recommended OSS-grade replacement
- Verification gate plan (RED→GREEN test)

---

## Files In Scope (estimated by module)

| Module | commonMain files | androidMain files | Test files |
|--------|-----------------|-------------------|------------|
| drishti-core | ~30 | 0 | ~15 |
| drishti-vision | ~15 | ~20 (CameraX) | ~10 |
| drishti-graph | ~10 | 0 | ~6 |
| drishti-formula | ~12 | 0 | ~8 |
| drishti-molecule | ~8 | 0 | ~6 |
| drishti-haptics | ~6 | ~5 (VibrationEffect JNI) | ~5 |
| drishti-audio | ~8 | ~15 (Oboe JNI) | ~6 |
| drishti-voice | ~8 | 0 | ~4 |
| drishti-android | ~4 | ~25 | ~5 |
| drishti-test | shared test stubs | — | — |
| drishti-demo | app module (untouched?) | — | — |

Total ~250 .kt files for deep review.

---

## Parallelization Plan

Spawn 11 `explore` agents (one per module) in parallel — each does per-line audit of its module. Each writes `MODULE_AUDIT_<name>.md` to `.audit/research/`. Updates this anchor on completion.

After all 11 modules audited: synthesize cross-cutting findings into a prioritized fix plan, dispatch 11 fix-agents in parallel.

---

## Build/Test Verification Gate

Before declaring ANY issue "fixed":
1. RED: Write failing test that demonstrates the bug
2. GREEN: Apply fix, watch test go green
3. SURFACE: If applicable, exercise via emulator or curl
4. REGRESSION: Full test suite still green
5. LSP: diagnostics clean

---

## Findings (live running list - APPEND as discovered)

[Updated as we find things in this session]

---

## Learnings (patterns / pitfalls - APPEND as discovered)

- `explicitApi()` is required on all modules — BCV checks this
- Kotlin 2.1.20 standalone BCV plugin (not built-in)
- `TextNode` vs `DataPointNode` vs `ShapeNode` are 4 distinct subclasses — assertion wrongness = test regression
- `orderPosition` uses normalized coords [0,1], not pixels
- CancellationException re-throw guard pattern mandatory
