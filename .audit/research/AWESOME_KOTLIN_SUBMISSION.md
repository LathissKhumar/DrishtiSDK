# Awesome-Kotlin Submission Package for DrishtiSDK

## Important Notes

- Canonical repo: **[Heapy/awesome-kotlin](https://github.com/Heapy/awesome-kotlin)** (11.4k stars), NOT JetBrains/awesome-kotlin
- Entries use `.awesome.kts` Kotlin DSL files, NOT README.md editing
- Target file: `src/main/resources/links/Android.awesome.kts`
- Target section: `subcategory("Libraries")`

---

## The Entry to Add

Add this inside the `subcategory("Libraries") { ... }` block:

```kotlin
link {
  github = "LathissKhumar/DrishtiSDK"
  desc = "General-purpose accessibility SDK for Android that converts visual content into haptic feedback, spatial audio, and voice guidance. Plugin architecture for any visual content type."
  setTags("accessibility", "haptic", "audio", "spatial", "kmp")
  setPlatforms(ANDROID)
}
```

---

## PR Metadata

**Branch name:** `add-drishti-sdk`

**PR title:** `Add DrishtiSDK to Android Libraries`

**PR description:**

> Adds [DrishtiSDK](https://github.com/LathissKhumar/DrishtiSDK) to the Android > Libraries section.
>
> **What it is:** General-purpose accessibility SDK for Android that converts visual content into haptic feedback, spatial audio, and voice guidance.
>
> **Why it fits:** Kotlin Multiplatform library (Kotlin 2.1.20, commonMain + androidMain) with plugin architecture for extensible visual content processing. Available on Maven Central (`io.github.lathisskhumar:drishti-core:1.0.0`). 1,203 tests, Apache 2.0 license.
>
> **Unique differentiator:** Only SDK providing haptic + spatial audio + voice output for visual content accessibility on Android.

---

## Step-by-Step Submission

1. Fork `Heapy/awesome-kotlin` on GitHub
2. Clone: `git clone https://github.com/<YOUR_USERNAME>/awesome-kotlin.git`
3. Branch: `git checkout -b add-drishti-sdk`
4. Edit: `src/main/resources/links/Android.awesome.kts`
5. Find: `subcategory("Libraries") { ... }`
6. Add the link block (alphabetically or at end of Libraries)
7. Commit: `git commit -m "Add DrishtiSDK to Android Libraries"`
8. Push and create PR

## Gotchas

- **No README.md editing.** `.kts` files generate the website automatically via CI
- **Description stays short.** One sentence, like existing entries
- **No badges in entry.** The `awesome()` call is for curated/notable projects only (maintainers add it)
- **`setPlatforms(ANDROID)` is correct** even though it's KMP internally
- **Typical review:** 1-2 weeks
