# Architecture

Safe to Post v0.9.0 is a single-activity Jetpack Compose app plus an optional input method. `ScanViewModel`
owns the single-photo pipeline (`ScanState`) and the batch flow (`BatchState`); `MainActivity` renders
`SafeToPostApp`, which applies the selected palette and the wavy theme transition. Heavy work runs off
the main thread.

## Single-photo pipeline

```
Photo picker / share sheet
        │
        ▼
PhotoFiles ──► LoadedPhoto (bitmap + original bytes + name)
        │
        ├──► MetadataReader ──► PhotoMetadata ──► MetadataEntry[]
        │        (JpegInspector → JpegSegment; ExifInterface; ExifGpsParser for GPS)
        │
        ├──► FaceFinder (ML Kit) ──► FaceRegion[] (PixelRect)
        │
        └──► CaptionScanner ──► CaptionFinding[] (kind + range)
                                   │
                                   ▼
                              RiskScorer ──► RiskScore (0–100, RiskFactor[])
                                   │
                                   ▼
              FaceBlur (selected) ──► MetadataStripper ──► GalleryExport
```

## Batch pipeline

```
Batch picker (≤ 12 URIs)
        │
        ▼
loadBatch: bytes → JpegInspector → MetadataReader → RiskScore + thumbnail
        │
        ▼
BatchScreen: checkbox (skip), per-row Clean, optional Blur all faces
        │
        ▼
cleanItem: decode → [FaceFinder + FaceBlur] → MetadataStripper → GalleryExport
```

## Modules

### `metadata`

| Class | Role |
|---|---|
| `JpegInspector`, `JpegSegment`, `SegmentKind` | Walk the JPEG marker structure without decoding pixels. |
| `MetadataReader` | Combines EXIF and segment facts into `PhotoMetadata`. |
| `ExifGpsParser` | Reads the APP1/TIFF GPS IFD directly (the platform reader returns zeros on some camera files), including inline values and offsets relative to the TIFF header. |
| `ExifCatalog`, `TagSpec` | Which tags the app can explain, with label, `LeakCategory` and `Risk`. |
| `CoordinateFormat`, `ValueFormat` | DMS formatting and value trimming. |

### `photo`

| Class | Role |
|---|---|
| `PhotoFiles` | Original bytes (with `MediaStore.setRequireOriginal` when permitted), bounded bitmap decode, thumbnail, display name. |
| `FaceFinder` | Bundled ML Kit face detection → `FaceRegion`. |
| `FaceBlur` | Padded mosaic over selected faces. |
| `MetadataStripper` | Re-encodes the bitmap so the output contains no APP1/EXIF. |
| `GalleryExport` | Writes the cleaned copy through MediaStore into `Pictures/Safe to Post`. |

### `caption`

`CaptionScanner` runs ordered regex rules and returns `CaptionFinding` (kind + character range).
`CaptionCleaner` replaces findings with `[phone]`, `[address]`, …; it is shared by the app and the
keyboard so both produce the same cleaned text.

### `risk`

`RiskScorer` turns metadata entries and caption findings into weighted `RiskFactor`s, caps the sum at
100, and maps it to a `RiskLevel`. The UI lists the factors, so the score is explainable.

### `ui`

| Class | Role |
|---|---|
| `ScanScreen` | Verdict card, photo overlay, People, metadata, caption, sticky action bar, theme sheet. |
| `BatchScreen` | Batch list, thumbnails, skip checkbox, per-row Clean, blur switch. |
| `SafeToPostApp` | Palette state, wavy theme reveal. |
| `Theme`, `ThemePreference` | `AppPalette` (Ember/Forest/Ocean/Plum), scheme, persisted choice. |
| `BatchState`, `ScanState` | Immutable UI state models. |

### `ime`

`SafeToPostImeService` is a minimal `InputMethodService`: compact QWERTY, caption strip that runs
`CaptionScanner` on the focused field, Clean/Undo (30 snapshots), and handoff of the caption to
`MainActivity`. It has no image access and no network use.

## Build facts

| | |
|---|---|
| Package | `app.safetopost` |
| Version | 0.9.0 (versionCode 9) |
| minSdk / targetSdk | 29 / 36 |
| UI | Jetpack Compose, Material 3 |
| Face detection | ML Kit bundled model (on-device) |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 |
