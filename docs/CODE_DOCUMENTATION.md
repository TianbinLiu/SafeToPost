# Safe to Post — Code Documentation

Implementation component of the CSCI 180 group project.
Tianbin Liu · CSCI 180 Computer Security · Fall 2026

---

## 1. Project description

Our group project studies how much a social media account exposes by default. The research part asks two questions: what data does a platform collect (inside the app and through trackers on other sites), and how much can other people see on an account whose owner never changes a setting. **Safe to Post** is the optional Android demo that applies the second question in practice: it surfaces the metadata hidden inside a photo, blurs faces the user chooses, and scores the privacy risk of the caption. Everything is processed on the phone, with no account and no upload step.

The app is deliberately narrow. It does not post for the user, does not sync to a server, and does not store a history of scanned photos. It is a measurement and cleanup tool: show the exposure, then remove it.

- Build: `app.safetopost` 0.1.0, Android 10 (API 29) or newer, target SDK 36
- Repository: <https://github.com/TianbinLiu/SafeToPost>
- Test build: <https://github.com/TianbinLiu/SafeToPost/releases/tag/v0.1.0>

## 2. Background

A photo is more than pixels. A JPEG produced by a phone usually carries an EXIF block with the capture time, device make and model, software version, and often GPS coordinates. Some cameras also embed a thumbnail and vendor tags. Captions add a second channel: a street name, phone number, date, or school name identifies the poster without any image analysis.

The literature on defaults is why this matters. Liu et al. found that Facebook users' actual privacy settings matched what they wanted only about 37% of the time, and the mismatch almost always exposed more than intended [1]. Gross and Acquisti documented the same pattern in 2005 [2]. The FTC's 2024 staff report describes social media data practices as "dangerously opaque" [4]. Ebert et al. shows that even privacy-conscious teenagers work around settings rather than trust them [3].

Safe to Post takes the defensive side: if the default is to expose, the least a tool can do is make the exposure visible and reversible at the moment of posting. The app is intentionally offline.

## 3. System overview

Single-activity Jetpack Compose app, MVVM. `MainActivity` hosts the Compose content and the system photo picker; `ScanViewModel` owns the pipeline and exposes one immutable `ScanState`. Five stages: load, inspect metadata, detect faces, scan the caption, score — then the user chooses faces and exports a cleaned copy.

| Package | Responsibility |
|---|---|
| `metadata` | JPEG segment walking, EXIF parsing, field catalog, per-field risk labels |
| `photo` | Reading/decoding the image, face detection, blurring, metadata stripping, gallery export |
| `caption` | Rule-based caption scanning and findings |
| `risk` | Combining metadata and caption findings into a 0–100 score with factors |
| `ui` | Compose screen, leak map overlay, caption highlighting, theme |

Shared data model: `PhotoMetadata` (parsed fields), `FaceRegion` (detected face + pixel rect), `CaptionFinding` (match + character range), `RiskScore` (total + factors). No stage holds a view reference.

```
Pick photo → Inspect metadata → Detect faces → Scan caption → Score risk → Strip/blur → Export
```

## 4. Code documentation

### 4.1 Application shell and state

`MainActivity` sets the Compose content, registers the photo-picker launcher, and forwards the returned content URI to the view model. It does not parse images itself.

`ScanViewModel` is the coordinator. It holds `ScanState`, starts the pipeline when a photo is picked or the demo image is loaded, and moves work to a background dispatcher. `Busy` prevents a second scan or export from starting while one is running. `UiMessage` carries one-shot feedback (exported, unreadable file) so the screen shows a snackbar without putting transient text into the state.

`ScanState` is the single source of truth: loaded photo, metadata list, detected faces with selection flags, caption findings, current score, and a phase value (idle, scanning, ready, exporting). Immutable state lets the Compose screen recompose from it and lets the pipeline be reasoned about as a transition.

### 4.2 Metadata inspection

`JpegInspector` walks the JPEG marker structure: starting at SOI, it reads each segment length, records the marker kind in `JpegSegment` (`SegmentKind`: APPn, DQT, SOF, SOS, comment, …), and stops at the start of scan. It does not decode pixels. That is enough to find the EXIF APP1 block, XMP, the embedded thumbnail, and comment segments.

`MetadataReader` combines two sources: it parses the EXIF block with AndroidX `ExifInterface` and maps the tags it cares about through `ExifCatalog`. The catalog is a list of `TagSpec` entries — tag, human label, `LeakCategory` (location, identity, device, time, social), and the risk level used by the scorer. Anything outside the catalog is grouped as "other" instead of dumped raw. `AttributeSource` records provenance (EXIF tag, JPEG segment, or inferred) so the UI can be honest about where a value came from.

`PhotoMetadata` is the result: ordered `MetadataEntry` list plus derived facts (GPS present, capture time, dimensions, thumbnail present). `GeoLocation` and `CoordinateFormat` convert rational GPS values to a readable DMS string and a map-ready pair. `ValueFormat` centralizes trimming and redaction. `Risk` is the per-field annotation consumed by the scorer.

### 4.3 Photo handling, face detection and export

`PhotoFiles` opens the content URI, reads bytes once, and decodes a bitmap: first bounds only, then a sample size that keeps the long edge within a working limit to avoid OOM on camera files. `LoadedPhoto` keeps bitmap, original bytes, and display name.

`FaceFinder` runs ML Kit face detection on the bitmap. The detector is the bundled variant, so the model is in the APK and no download or network call is involved. Results become `FaceRegion` objects (id, selected flag, `PixelRect` in bitmap coordinates). `FaceRegionKt` contains the helpers that scale between bitmap and preview coordinates so the overlay and export agree.

`FaceBlur` applies the selection: for each selected region it blurs a padded rectangle (padding covers hair and chin) and blends the patch back. The current build uses a mosaic-style blur strong enough that the face is not recognizable at normal size. `MetadataStripper` produces the cleaned JPEG by re-encoding the bitmap, so the output has no APP1/EXIF, no thumbnail, no comment segments. `CleanPhoto` wraps the final bitmap and a suggested name. `GalleryExport` inserts it through MediaStore into Pictures/Safe to Post and returns a result. `DemoPhoto` is a bundled sample for demos and tests.

### 4.4 Caption scanning

`CaptionScanner` is a rule engine. A `Rule` pairs a `CaptionKind` with a matcher (regex or keyword set) and a label. `scan` applies the rules, collects `CaptionFinding` objects (kind, character range, matched text), and returns them sorted by position. The ranges drive `CaptionHighlight`, so the user sees exactly which words triggered the warning.

The rule set is deliberately conservative: phone numbers, e-mail addresses, street addresses and location phrases, dates/times, school or employer names, handles and links. It does not pretend to be NER; on a phone-sized input a transparent rule set is easier to explain and audit.

### 4.5 Risk scoring

`RiskScorer` turns findings into one number. Each metadata entry and caption finding contributes a `RiskFactor`: source (`RiskSource`), weight, and a one-line explanation. The scorer sums weights, caps at 100, and maps the total to a `RiskLevel` (low/medium/high). Because factors are kept, the UI can show why the score is what it is. GPS and a phone number weigh most; device model and software version weigh less; a clean caption adds nothing.

The score is a heuristic prioritization aid, not a guarantee; the UI copy says so.

### 4.6 User interface

`ScanScreenKt` is the screen: photo preview with `LeakMap` overlay, score header, metadata list grouped by category, face chips with blur toggles, caption with `CaptionHighlight`, and the two primary actions (clean/export). `ThemeKt` and `StatusColors` hold the palette and score colors. `LeakMapKt` draws detected regions over the image and keeps them aligned on resize.

## 5. How the components work together

Picking a photo produces a URI; `PhotoFiles` turns it into a `LoadedPhoto`. `MetadataReader`, `FaceFinder`, and `CaptionScanner` are independent, so metadata parsing and face detection can run off the main thread and be combined. `RiskScorer` is the join point: it reads metadata entries and caption findings and produces the score.

The second half is user-driven: the screen holds detected faces with a selected flag; toggling changes only that flag. On export, `MetadataStripper` re-encodes, `FaceBlur` applies the selected regions, and `GalleryExport` writes the result. The original file is never modified — analysis is deterministic and re-runnable, while export is the only side-effecting step and happens once, on an explicit tap.

Errors leave the previous state intact; `Busy` prevents a slow export from racing a new scan.

## 6. Building and running

1. Install Android Studio (current stable), JDK 17, and the Android SDK for API 36.
2. Open the project folder, let Gradle sync, accept requested SDK components.
3. Connect a device on Android 10 (API 29)+ or start an emulator.
4. Run the `app` configuration, or build with `./gradlew assembleDebug` (`gradlew.bat` on Windows). The APK lands in `app/build/outputs/apk/debug/`.
5. Install with `adb install -r app-debug.apk`, or copy the APK to the phone and open it.

Dependencies: Jetpack Compose + Material 3, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.exifinterface:exifinterface`, `com.google.mlkit:face-detection` (bundled). Versions are managed by the Gradle version catalog. No API key, backend, or network configuration is required; the debug build is the demo build.

The published test APK is the same debug build: <https://github.com/TianbinLiu/SafeToPost/releases/tag/v0.1.0>.

## 7. Reproducing the three features

### 7.1 Metadata inspection and removal

- Pick a phone photo (or the demo photo) and open the metadata list: capture time, device model, GPS and the embedded thumbnail appear with categories.
- Tap clean and export. Open the exported copy with a metadata viewer (`exiftool` on desktop, or a metadata app): EXIF, thumbnail, and GPS are gone; the image is unchanged apart from re-encoding.
- Expected: entries present before export are absent after, and the original is untouched.

### 7.2 Face detection and blurring

- Pick a photo with faces: the overlay marks regions and face chips appear.
- Select one face, export, open the result: only the selected face is blurred; others stay sharp; no metadata.
- Expected: the blurred face is not recognizable at normal size; the rest of the image is unmodified.

### 7.3 Caption scan and risk score

- Type a caption with a phone number and a street address, then one with neither: findings and score change.
- Remove findings one at a time and watch the score and factor list change.
- Expected: every point of the score traces to a factor; no finding is counted twice.

## 8. Limitations and future work

- Caption scanner is rule-based: transparent and fast, but misses paraphrases and can flag harmless dates. On-device NER could be added while keeping rules as fallback.
- Metadata reader focuses on JPEG/EXIF. HEIC, PNG text chunks, and vendor blocks fall into "other" and are removed by re-encoding but not explained field by field. Video is out of scope.
- ML Kit bundled model can miss profile views and heavy occlusion; blur is a fixed-strength mosaic.
- Android only; the iOS port was dropped from scope.
- The risk score is heuristic, not a guarantee.

## 9. References

1. Y. Liu, K. P. Gummadi, B. Krishnamurthy, A. Mislove. "Analyzing Facebook Privacy Settings: User Expectations vs. Reality." IMC 2011.
2. R. Gross, A. Acquisti. "Information Revelation and Privacy in Online Social Networks." WPES 2005.
3. N. Ebert et al. "Creative beyond TikToks: Investigating Adolescents' Social Privacy Management on TikTok." PoPETs 2023(2).
4. U.S. Federal Trade Commission. "A Look Behind the Screens: Examining the Data Practices of Social Media and Video Streaming Services." Staff Report, Sept 2024.
5. Android Developers. Photo picker, MediaStore, ExifInterface documentation.
6. Google ML Kit. Face detection documentation.
