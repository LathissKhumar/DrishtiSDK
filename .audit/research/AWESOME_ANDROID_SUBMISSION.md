# Awesome-Android Submission Package for DrishtiSDK

## Target

- Repo: **[JStumpp/awesome-android](https://github.com/JStumpp/awesome-android)** (12k+ stars)
- File: `readme.md`
- Section: `### Other` under `## Libraries`

---

## Entry (copy-paste ready)

```markdown
- [DrishtiSDK](https://github.com/LathissKhumar/DrishtiSDK) - General-purpose accessibility SDK for Android that converts visual content into haptic feedback, spatial audio, and voice guidance. Plugin architecture for extensible content types, published on Maven Central.
```

### Where to Insert

The "Other" section is roughly alphabetical. Insert after `Caffeine` entries, before `Drekkar`:

```markdown
- [Caffeine](https://github.com/percolate/caffeine) - A collection of utility classes that help make Android development faster.
- [DrishtiSDK](https://github.com/LathissKhumar/DrishtiSDK) - General-purpose accessibility SDK for Android that converts visual content into haptic feedback, spatial audio, and voice guidance. Plugin architecture for extensible content types, published on Maven Central.
- [Drekkar](https://github.com/coshx/drekkar) - An Android event bus for WebView and JS.
```

---

## PR Title

```
Add DrishtiSDK to Other
```

## PR Description

> ## What this PR does
>
> Adds [DrishtiSDK](https://github.com/LathissKhumar/DrishtiSDK) to the list.
>
> ## Why it belongs here
>
> DrishtiSDK is an open-source accessibility SDK for Android that converts visual content (charts, graphs, formulas, molecules, images) into three non-visual modalities:
>
> - **Haptic feedback** for spatial relationships and shapes
> - **Spatial audio** for position, distance, and direction
> - **Voice guidance** for content description and navigation
>
> Plugin architecture (`DetectorPlugin` / `RendererPlugin`) lets developers extend it to any visual content type. Kotlin Multiplatform, 1203 tests, Maven Central (`io.github.lathisskhumar:drishti-core:1.0.0`), Apache 2.0.
>
> ## Checklist
> - [x] Link points to GitHub repo with README
> - [x] Repository has a license (Apache 2.0)
> - [x] Project is actively maintained
> - [x] Entry follows format `- [Name](URL) - Description.`

---

## Step-by-Step

1. Fork `JStumpp/awesome-android`
2. Clone your fork
3. `git checkout add-drishti-sdk`
4. Edit `readme.md`, find `### Other`
5. Insert the entry in alphabetical order (D comes after C, before Drekkar)
6. `git commit -m "Add DrishtiSDK to Other"`
7. Push and open PR

## Notes

- **No "Accessibility" section exists.** Don't propose a new section unless maintainers ask. Adding to "Other" is the fastest path to merge.
- **Description ends with period.** Required by their format.
- **One line per entry.** No multi-line descriptions.
