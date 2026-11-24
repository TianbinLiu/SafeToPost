# Architecture

The app is a single-activity Jetpack Compose application. `ScanViewModel` owns the pipeline and exposes an immutable `ScanState`; `MainActivity` renders `ScanScreen`. All heavy work runs off the main thread.

## Pipeline

```
PhotoPicker / DemoPhoto
        │
        ▼
PhotoFiles ──► LoadedPhoto (bitmap + bytes + name)
        │
        ├──► MetadataReader ──► PhotoMetadata ──► MetadataEntry[]
        │        (JpegInspector → JpegSegment, EXIF via ExifInterface)
        │
        ├──► FaceFinder (ML Kit) ──► FaceRegion[] (PixelRect)
        │
        └──► CaptionScanner ──► CaptionFinding[] (kind + range)
                                   │
                                   ▼
                              RiskScorer ──► RiskScore (0–100, RiskFactor[])
                                   │
                                   ▼
              MetadataStripper / FaceBlur ──► CleanPhoto ──► GalleryExport
```

## Packages

### `metadata`

| Class | Role |
|---|---|
| `JpegInspector` | Walks the JPEG marker structure and yields `JpegSegment`s (APPn, SOF, SOS, …) without decoding pixels. |
| `JpegSegment` / `SegmentKind` | One segment and its kind. |
| `MetadataReader` | Turns the segments plus EXIF into a `PhotoMetadata` list. |
| `PhotoMetadata` | Immutable result: entries, GPS, dimensions, capture info. |
| `ExifCatalog` / `TagSpec` | The catalog of fields the UI can explain, with human labels and risk class. |
| `AttributeSource` | Where a value came from (EXIF tag, JPEG segment, inferred). |
| `LeakCategory` | What kind of privacy leak a field represents (location, identity, device, time, …). |
| `GeoLocation`, `CoordinateFormat`, `ValueFormat` | Value normalization and display formatting (e.g., DMS coordinates). |
| `Risk` | Per-field risk annotation used by the scorer. |

### `photo`

| Class | Role |
|---|---|
| `PhotoFiles` | Opens the picked URI, reads bytes, decodes a bounded bitmap. |
| `LoadedPhoto` | Bitmap + original bytes + display name. |
| `DemoPhoto` | Bundled sample so the flow can be shown without picking a photo. |
| `FaceFinder` | Runs ML Kit face detection and maps results to `FaceRegion`s. |
| `FaceBlur` | Applies the chosen blurs to the bitmap. |
| `MetadataStripper` | Re-encodes the JPEG without the original metadata segments. |
| `CleanPhoto` | Result of stripping/blurring, ready to export. |
| `GalleryExport` | Writes the cleaned image through MediaStore. |

### `caption`

`CaptionScanner` runs an ordered set of `Rule`s against the caption and returns `CaptionFinding`s (`CaptionKind` + character range). The ranges are what `CaptionHighlight` uses to mark the text, and the findings are also fed to the scorer.

### `risk`

`RiskScorer` combines the metadata entries and caption findings into weighted `RiskFactor`s, sums them into a 0–100 `RiskScore`, and maps the total to a `RiskLevel` (low / medium / high). Each factor carries a `RiskSource` so the UI can explain *why* the score moved.

### `ui` and state

`ScanScreenKt` is the Compose screen; `LeakMapKt` renders the detected faces/fields; `CaptionHighlight` draws the caption findings; `ThemeKt` and `StatusColors` hold the design tokens. `ScanViewModel` exposes `ScanState` and the actions (pick, scan, toggle face, export).

## Build facts (from the shipped APK)

| | |
|---|---|
| Package | `app.safetopost` |
| Version | 0.1.0 (versionCode 1) |
| minSdk / targetSdk | 29 / 36 |
| ABIs | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Face detection | ML Kit bundled model (`libface_detector_v2_jni.so`) |
| UI | Jetpack Compose |
