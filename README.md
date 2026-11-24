# Safe to Post

An Android app that checks a photo before you post it — and helps you remove what it would give away.

Built as an optional demo for our CSCI 180 Computer Security group project, *Behind the Default Settings: How Social Networks Collect, Track, and Expose User Data*. Everything runs on the phone: no account, no upload step for the analysis.

> **Status:** v0.1.0 test build (debug-signed, not on Google Play). Android 10 or newer.

## What it does

**1. Shows the hidden data inside a photo — and removes it.**
Picks apart the JPEG segments and the EXIF block: GPS coordinates, capture time, device make/model, software tags, thumbnail, and anything else the camera wrote in. Each field is listed with its source and risk level; one tap writes a clean copy with the metadata stripped.

**2. Finds faces and blurs the ones you pick.**
Runs ML Kit face detection on the decoded bitmap, overlays the detected regions, and lets you choose which faces to blur before exporting. The blurred result is re-encoded without the original metadata.

**3. Checks the caption for personal details and scores the post.**
Scans the caption text for leak categories (addresses, phone numbers, dates, school/workplace, location names, and similar patterns), highlights the matches, and combines them with the photo findings into a **privacy risk score from 0 to 100** with a per-factor breakdown.

## Install (test build)

1. On an Android phone (Android 10 / API 29 or newer), open the APK from the [Releases](../../releases) page.
2. Allow install from the browser when Android asks.
3. Play Protect may warn first — that is expected for a debug build that is not distributed through Play.

The same APK was shared with the group as `SafeToPost-test-build.apk`.

## How it works

```
photo picker ──► MetadataReader ──► PhotoMetadata ──┐
      │              (EXIF + JPEG segments)         │
      │                                             ├──► RiskScorer ──► RiskScore (0–100)
      └──────────► FaceFinder (ML Kit) ──► FaceRegion┤
                                                    │
caption text ──► CaptionScanner ──► CaptionFinding ─┘
      │
      └──► MetadataStripper / FaceBlur ──► CleanPhoto ──► GalleryExport
```

| Package | Responsibility |
|---|---|
| `metadata` | JPEG segment walking, EXIF parsing, field catalog (`ExifCatalog`), risk tagging per field |
| `photo` | Face detection (`FaceFinder`), bitmap blur (`FaceBlur`), metadata stripping, gallery export |
| `caption` | Rule-based caption scanner and finding categories |
| `risk` | Factor model and 0–100 aggregation (`RiskScorer`, `RiskFactor`, `RiskLevel`) |
| `ui` | Jetpack Compose scan screen, caption highlighting, leak map, theme |
| `ScanViewModel` | Holds `ScanState` and drives the pipeline off the main thread |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the module map and [docs/PRIVACY.md](docs/PRIVACY.md) for the data-flow notes.

## Tech

- Kotlin, Jetpack Compose (Material 3), coroutines
- AndroidX ExifInterface for EXIF, custom JPEG segment reader for the rest
- Google ML Kit face detection (bundled, on-device)
- `minSdk 29`, `targetSdk 36`, `versionName 0.1.0`

## Repository contents

- `docs/` — architecture and privacy notes
- Releases — signed test APK (`v0.1.0`)

The Android Studio project can be added to this repository as a follow-up; the module map in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) documents the shipped build.

## Team

CSCI 180 · Computer Security · Fall 2026
Connor Toreson · Tianbin Liu · Jason Zhou
