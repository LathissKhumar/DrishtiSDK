# Per-Line Audit: drishti-molecule

Generated: 2026-07-05

## Files Audited (7 files, 1,477 LOC)

| File | LOC | Lines |
|------|-----|-------|
| PubChemClient.kt | 464 | HTTP client with rate limiting, caching, request coalescing |
| MoleculeDetector.kt | 109 | PubChem API-first detector |
| PubChemModels.kt | 239 | JSON response parsers, element mapper |
| MoleculeParser.kt | 251 | Input type detection (SMILES/formula/name/InChI) |
| MoleculeData.kt | 86 | Data class with toMoleculeContent() conversion |
| MoleculePlugin.kt | 266 | Plugin facade (Detector + Haptics + Audio + Voice renderers) |
| MoleculeRenderer.kt | 262 | Haptic/audio/voice rendering with weight-scaled intensities |

## Verdict: PASS

Production-quality module. PubChemClient implements industry-standard patterns:
- Rate limiting (5 req/sec via Mutex + delay)
- LRU caching with TTL eviction
- Request coalescing via CompletableDeferred (cache stampede protection)
- Exponential backoff retry (3 attempts) for 429/503
- Coroutine-level timeout (30s) as defense-in-depth
- CancellationException properly rethrown in ALL catch blocks across ALL files
- Custom exception hierarchy (PubChemException → CompoundNotFound/Network/RateLimit)
- RFC 3986 percent-encoding for URL path segments
- Stack-based formula atom counter supporting parenthesized groups

MoleculeParser handles edge cases: Unicode subscripts (C₆H₁₂O₆), all-uppercase SMILES chains (CCO), ring closures (c1ccccc1), branch parentheses (CC(O)C), and disambiguation between SMILES/formula/name.

## Issues Found

### MOL-1: Wildcard import `io.drishti.core.*` in all 7 files [MEDIUM]
- **Files**: All 7 molecule files
- **Line(s)**: Import section of each file
- **Issue**: All files use `import io.drishti.core.*` wildcard import. Should be expanded to specific imports per project convention (Wave 5 addressed this across other modules).
- **Impact**: Style consistency — wildcard imports were expanded in core/vision/graph/formula modules but molecule was not yet audited.
- **Status**: NOT FIXED (deferred to avoid large diff in this audit pass; flagged for future cleanup)
- **Fix**: Expand `io.drishti.core.*` to explicit imports in all 7 files.

## Notes

- PubChemClient companion constants are properly documented with KDoc
- All public APIs have KDoc with @param/@return/@throws
- ElementMapper covers all 118 elements (periodic table complete)
- bondTypeFor defaults unknown orders to SINGLE (safe fallback)
- MoleculeData.computeBoundingBox correctly guards empty atom list before minOf/maxOf
- MoleculePlugin.renderExploration* methods properly use elementIndex (no navigation bug)
- No stubs, no demo data, no hardcoded confidence scores
- `io.ktor.client.*` wildcard import in MoleculeDetector.kt — minor, acceptable for Ktor client builder pattern
