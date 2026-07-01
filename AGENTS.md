# AGENTS.md — DrishtiSDK Operational Playbook

> This file is the LIVING MEMORY for all AI agent sessions working on DrishtiSDK.
> Read this FIRST at the start of every session. Update it whenever you discover new patterns.
> Last updated: 2026-06-29 by Sisyphus during ULTRAWORK audit session.

---

## 1. SESSION STARTUP PROTOCOL

Every new session MUST execute this in order:

```
1. Read AGENTS.md (this file) — operational rules
2. Read /home/lathiss/Projects/DrishtiSTEM/.audit/research/SESSION_HANDOFF.md — current state & next steps
3. Read /home/lathiss/Projects/DrishtiSTEM/.audit/PRODUCTION_AUDIT_FINDINGS.md — detailed issue registry
4. Read /home/lathiss/Projects/DrishtiSTEM/.audit/research/*.md — domain-specific research
5. Run build verification: ./gradlew assembleDebug testDebugUnitTest --offline
6. Resume work from SESSION_HANDOFF.md "WORK PLAN" section
```

---

## 2. RESEARCH STYLE

### How We Research (Parallel Agent Swarms)

We NEVER research serially. Every research task fires multiple background agents simultaneously:

```
Phase 1: Fire 2-5 background agents in parallel
  - explore agents: grep codebase for patterns, find issues, map architecture
  - librarian agents: search docs, OSS examples, production patterns
  - oracle agent: deep code review, architecture decisions

Phase 2: Collect results as they complete (system-reminder notification)

Phase 3: Synthesize findings into .audit/research/ documents

Phase 4: Update AGENTS.md with any new patterns discovered
```

### Agent Types & When To Use

| Agent | Use When | Cost | Output |
|-------|----------|------|--------|
| `explore` | Find code patterns, grep for bugs, map module structure | FREE | File paths + descriptions |
| `librarian` | Find docs, OSS examples, library usage patterns | CHEAP | Code snippets + links |
| `oracle` | Deep code review, architecture decisions, hard debugging | EXPENSIVE | Detailed analysis |
| `metis` | Pre-planning, ambiguity analysis, failure point identification | EXPENSIVE | Risk assessment |
| `momus` | Plan review, quality gates, completeness checks | EXPENSIVE | Go/no-go verdict |

### Parallelization Rules
- Fire ALL independent agents simultaneously — never sequentially
- Use `run_in_background=true` for explore/librarian
- Use `run_in_background=false` for oracle (need results before proceeding)
- NEVER re-search what an agent was tasked with (anti-duplication rule)
- End response and wait for `system-reminder` notification — never poll

### Research Output Format
Every research cycle produces files in `.audit/research/`:
```markdown
# [Topic] Research
Generated: [timestamp]
Source: [agent IDs]

## Executive Summary
[One paragraph recommendation]

## Detailed Findings
[Organized by subtopic]

## Action Items for DrishtiSDK
[Specific files to change, code to write]
```

---

## 3. AUDIT PATTERNS

### Production Code Audit Checklist
Check EVERY file against this:

```
□ No stubs (methods that pass data through unchanged)
□ No hardcoded values (confidence scores, spatial coordinates, frequencies)
□ No demo data (placeholder text, mock responses)
□ No TODO/FIXME/HACK comments without owner+date
□ No unused dependencies in build.gradle.kts
□ No unused imports
□ No dead code (unreachable branches, unused private methods)
□ Every public API has KDoc with @param, @return, @throws
□ Every public declaration has explicit visibility modifier
□ Every public function has explicit return type
□ No `!!` bang operators — use require()/check()/nullable
□ No broad try/catch (catching Exception/Throwable)
□ No comments restating code ("// increment counter")
□ Error handling uses sealed classes, not strings
□ Thread safety for mutable shared state
□ expect/actual pattern: minimal commonMain surface, real platform impl in androidMain
```

### Anti-AI-Slop Detection
AI-generated code has specific hallmarks. Check for:

```
□ Comments that restate code instead of explaining WHY
□ Generic names: result, data, handler, manager, output, process
□ try { ... } catch (e: Exception) { return null } patterns
□ !! bang operators or ?: return null chains
□ Over-abstraction: AbstractBaseFactoryProvider patterns
□ Only happy-path tests (no edge cases, error paths, cancellation)
□ Linear textbook flow with no domain vocabulary
□ Sealed classes with "impossible" states that don't map to business rules
□ Missing @Suppress annotations where Kotlin requires them
```

### Module Audit Process
For each module, fire a dedicated explore agent:
```
task(subagent_type="explore", run_in_background=true,
  description="Audit [module] production quality",
  prompt="[CONTEXT] ... [GOAL] ... [DOWNSTREAM] ... [REQUEST] Read every .kt file in
  drishti-[module]/src/, check for stubs/demo/hardcoded/unused/missing-error-handling,
  return file:line:issue format")
```

---

## 4. FIX PATTERNS

### Bug Fix Protocol
1. Read the file thoroughly (full file, not grep snippets)
2. Understand the root cause (not just the symptom)
3. Read all callers and consumers (grep for function name)
4. Fix minimally — one bug per commit, no refactoring while fixing
5. Add test that would have caught the bug
6. Run affected module tests
7. Run full test suite

### Exploration Navigation Bug Pattern (Fixed in Voice/Haptic, pending in Vision/Spatial)
The pattern: `renderExplorationXxx()` accepts `elementIndex` parameter but ignores it.
Fix: Use `elementIndex` to compute current position, then `list.getOrNull(idx +/- 1)` for navigation.

### Stub Replacement Protocol
When replacing a stub:
1. Read the interface/expect declaration
2. Research the production pattern (librarian agent)
3. Check existing deps — use what's already declared
4. Implement with proper error handling
5. Add unit tests with edge cases
6. Verify the module compiles and tests pass

---

## 5. BUILD & TEST

### Build Command (ALWAYS use this exact command)
```bash
cd /home/lathiss/Projects/DrishtiSDK && \
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew assembleDebug --offline \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1
```

### Test Command
```bash
cd /home/lathiss/Projects/DrishtiSDK && \
JAVA_HOME=/opt/android-studio/jbr \
ANDROID_HOME=/home/lathiss/android-sdk \
PATH=/opt/android-studio/jbr/bin:$PATH \
./gradlew testDebugUnitTest --offline \
  -Dorg.gradle.jvmargs="-Xmx1g" \
  -Dorg.gradle.workers.max=1
```

### Build Rules
- ALWAYS use `--offline` flag (no network in CI environment)
- ALWAYS set JAVA_HOME and ANDROID_HOME explicitly
- `-Dorg.gradle.workers.max=1` prevents OOM on limited machines
- `-Dorg.gradle.jvmargs="-Xmx1g"` caps memory
- Current baseline: 199 tasks, 50 tests, 0 failures

---

## 6. DOCUMENTATION STANDARDS

### File Locations
```
.audit/
├── PROJECT_CONSTITUTION.md        — Project rules, architecture decisions
├── ROADMAP.md                     — Phase plan (0-3)
├── IMPLEMENTATION_PLAN.md         — Detailed implementation steps
├── ACTIVE_MILESTONE.md            — Current milestone tracking
├── TASK_QUEUE.md                  — Task tracking with status
├── PRODUCTION_AUDIT_FINDINGS.md   — Master issue registry
├── prompts/
│   └── opencode.md               — Senior Engineer prompt
└── research/
    ├── SESSION_HANDOFF.md          — Session context for next time
    ├── OCR_ENGINE_COMPARISON.md    — OCR research
    ├── IMAGE_PREPROCESSING_PATTERNS.md — OpenCV/YUV patterns
    ├── KOTLIN_OSS_STANDARDS.md     — Quality standards reference
    └── ORACLE_REVIEW_EXPLORATION.md — Code review findings
```

### Research File Naming
`[TOPIC]_[TYPE].md` where TYPE is:
- `COMPARISON` — evaluating options
- `PATTERNS` — implementation patterns
- `STANDARDS` — quality/reference
- `REVIEW` — code review findings
- `HANDOFF` — session transition

---

## 7. KOTLIN KMP QUALITY STANDARDS

### Build System
```kotlin
// build.gradle.kts — must have
kotlin {
    explicitApi()  // Forces public/internal on every declaration
    compilerOptions {
        allWarningsAsErrors = true
    }
}
```

### API Design Rules
- Interface-first: contracts separate from implementations
- Extension functions for convenience behavior
- Lambda parameters last for DSL-style APIs
- `out` covariance on return types, `in` contravariance on parameters
- Never use `kotlin.Result` for domain errors — use sealed classes
- Provide both throwing and Result-returning variants
- `@Throws` for iOS interop

### Test Naming
```
GIVEN [setup] WHEN [action] THEN [expected result]
```
Or shorter:
```
WHEN [action] THEN [expected result]
```

### Test Coverage Requirements
- Happy path
- Error path
- Edge cases (empty input, boundary values, null)
- Cancellation/unsubscribe paths
- Thread safety (if concurrent)

### Sealed Class Pattern
```kotlin
// TWO type parameters — separates success from error
sealed class ResourceState<out T, out E> {
    data class Success<out T, out E>(val data: T) : ResourceState<T, E>()
    data class Failed<out T, out E>(val error: E) : ResourceState<T, E>()
    class Loading<out T, out E> : ResourceState<T, E>()
    class Empty<out T, out E> : ResourceState<T, E>()

    fun isLoading(): Boolean = this is Loading
    fun isSuccess(): Boolean = this is Success
    fun dataValue(): T? = (this as? Success)?.data
    fun errorValue(): E? = (this as? Failed)?.error
}
```

---

## 8. KMP EXPECT/ACTUAL PATTERNS

### Architecture
```
commonMain/  — Interface + pure Kotlin fallback (functional but slow)
androidMain/ — Real platform impl (OpenCV, ML Kit, CameraX)
```

### Rules
- expect API is minimal — only what's needed cross-platform
- actual wraps platform APIs — never exposes platform types in commonMain
- Internal expect/actual for hidden platform details
- commonMain implementation should be FUNCTIONAL even if slow
- androidMain implementation should be FAST (native libs, JNI)

---

## 9. PROJECT CONVENTIONS

### Commit Messages
```
feat(scope): description
fix(scope): description
refactor(scope): description
test(scope): description
docs(scope): description
```

### License
Apache-2.0 — every `.kt` file must have license header

### Min SDK
API 30 (Android 11)

### Package Structure
```
io.drishti.core     — Core types, Pipeline, SceneGraph
io.drishti.vision   — Vision module (OCR, detection, rendering)
io.drishti.formula  — LaTeX parsing, formula detection
io.drishti.graph    — Graph/chart detection and rendering
io.drishti.molecule — Chemical structure detection
io.drishti.haptics  — Haptic feedback rendering
io.drishti.audio    — Audio/spatial rendering
io.drishti.voice    — Speech/TTS rendering
io.drishti.android  — Android platform integration
```

---

## 10. LESSONS LEARNED

### What We Got Wrong (Prior Sessions)
1. **Source Audit Was Wrong**: 7 of 8 "critical bugs" were already fixed or not bugs. ALWAYS verify before trusting audit results.
2. **TestFixtures Location**: Memory said `drishti-core` but it's actually `drishti-test`. ALWAYS check filesystem, not memory.
3. **CameraCapture Y-Plane-Only**: Flagged as bug but is intentional design choice. UNDERSTAND design context before fixing.

### What We Got Right
1. **Plugin Architecture**: DetectorPlugin + RendererPlugin pattern scales well
2. **SceneGraph with adjacencyIndex**: Proper graph navigation
3. **PubChemClient**: Production-quality with rate limiting, retry, caching, coalescing
4. **LatexParser**: Recursive descent with depth limits prevents StackOverflow

### Critical Insight
> "What you have created now is child's play as per OSS standards. They are not production code and does not work for every case."
— User, 2026-06-29

This means: Every stub, every hardcoded value, every missing error handler must be fixed before this is release-worthy.

---

## 11. FUTURE SESSION INSTRUCTIONS

When you start a new session on this project:
1. DO NOT trust memory blocks blindly — verify against filesystem
2. DO fire research agents in parallel — never serially
3. DO update this AGENTS.md with new patterns discovered
4. DO save research to `.audit/research/` — persistent across sessions
5. DO update `SESSION_HANDOFF.md` at end of every session
6. DO check `.audit/PRODUCTION_AUDIT_FINDINGS.md` before starting work
7. NEVER start implementation without reading governance docs first
8. NEVER skip build verification after changes
9. NEVER claim completion without running full test suite
10. ALWAYS challenge prior audit findings — they may be wrong
