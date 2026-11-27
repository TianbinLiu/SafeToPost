# Code documentation — outline

> **The submission version is the PDF/DOCX in this folder:**
> [SafeToPost_Code_Documentation.pdf](SafeToPost_Code_Documentation.pdf) ·
> [SafeToPost_Code_Documentation.docx](SafeToPost_Code_Documentation.docx)
> (v0.9.0, 10 pages, 12 pt, 1.15 line spacing). This Markdown is a working outline kept for editing;
> the PDF is canonical.

## What the report covers

1. Project description and background (defaults, EXIF, caption leaks).
2. System overview: single-photo pipeline, batch pipeline, keyboard.
3. Code documentation per module: metadata, photo, caption, risk, UI, keyboard, batch.
4. How the components work together, including the original-bytes path
   (`ACCESS_MEDIA_LOCATION` + `MediaStore.setRequireOriginal`) and why a custom GPS parser exists.
5. Building and running (JDK 17, SDK 36, `./gradlew assembleDebug`).
6. Reproduction steps for metadata removal, face blur, caption scanning/cleaning, batch, keyboard.
7. Limitations and future work.
8. References (Liu et al. 2011; Gross & Acquisti 2005; Ebert et al. 2023; FTC 2024; Android and
   ML Kit documentation).

## Source map

| Area | Files |
|---|---|
| Metadata | `app/src/main/java/app/safetopost/metadata/` |
| Photo, faces, export | `app/src/main/java/app/safetopost/photo/` |
| Caption scan + clean | `app/src/main/java/app/safetopost/caption/` |
| Risk score | `app/src/main/java/app/safetopost/risk/` |
| Single-photo UI | `app/src/main/java/app/safetopost/ui/ScanScreen.kt` |
| Batch UI | `app/src/main/java/app/safetopost/ui/BatchScreen.kt` |
| Themes / wave | `app/src/main/java/app/safetopost/ui/Theme.kt`, `SafeToPostApp.kt` |
| Keyboard | `app/src/main/java/app/safetopost/ime/SafeToPostImeService.kt` |
| State | `ScanViewModel.kt`, `ScanState.kt`, `BatchState.kt` |

## Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Quick reproduction

- Metadata/GPS: pick a phone photo with location, grant media-location, confirm GPS and capture time,
  tap Clean & export, check the exported copy has no EXIF.
- Faces: pick a portrait, select a face, export; the exported copy shows a mosaic.
- Caption: type `Call 4085551234 from 123 Main Street`, confirm phone/address findings, tap Clean
  caption and Undo.
- Batch: Batch → two photos → optionally Blur all faces → Clean N selected; rows get checks.
- Keyboard: enable Safe to Post keyboard, type the same caption in any text field, Clean/Undo.

## Limitations

Android only; JPEG/EXIF focus; rule-based captions (no NER); bundled face model can miss profile
views; fixed mosaic; score is a heuristic; keyboard sees only the focused text field.
