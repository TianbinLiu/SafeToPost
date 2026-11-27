# Defense notes — likely questions and short answers

Use these as talking points, not a script. Each answer has the evidence you can point to.
**Bold** = the number or fact worth memorising.

---

## A. Concept and novelty

**1. What is the project about, in one sentence?**
Social platforms ship defaults that expose users, and a photo carries hidden data; we measured the
defaults and built an Android tool that checks a photo before posting and explains the risk.

**2. What exactly did you build?**
An Android app (Kotlin/Compose, v0.9.0) with five capabilities: metadata/GPS inspection and removal,
face detection + mosaic, caption scanning + a 0–100 explained score, caption cleaning with undo,
batch cleaning, and a minimal keyboard that checks the caption. Code and APK are public.

**3. Isn’t this just another EXIF remover?**
EXIF removers exist (Redact, Exif Eraser), but they stop at the file. We combine **file metadata,
faces and caption text** in one flow, on Android, with an explained score. In our market check the
closest all-in-one apps were **iOS-only** (ScrubPix, B4U Post, Social Media Privacy Checker).

**4. What is genuinely new?**
The combination and the explanation: every point of the score is traceable to a listed factor, and
the caption is checked as text, not just OCR inside the image. The keyboard is a deliberate probe
into where a pre-post check can live.

---

## B. Privacy and security correctness

**5. Does any data leave the phone?**
No app code sends photos, metadata or captions anywhere. Face detection uses the **bundled** ML Kit
model; analysis and export are local. The app has no account and stores no scan history.

**6. Why does the app need `ACCESS_MEDIA_LOCATION`?**
Android’s photo picker hands apps a **redacted copy** with GPS zeroed unless that permission is
granted. With it, the app requests the original bytes (`MediaStore.setRequireOriginal`) and the GPS
rows are real; without it, the rows are absent. We verified the difference by comparing MD5s of the
picked bytes.

**7. Why write your own GPS parser instead of using `ExifInterface`?**
On the 2008 Nikon sample, `ExifInterface` returned `0/1,0/1,0/1` for GPS. `ExifGpsParser` walks the
APP1/TIFF structure itself: byte order, IFD0, the `0x8825` pointer, latitude/longitude rationals and
N/S/E/W refs, handling inline values and offsets relative to the TIFF header. That is why the app
shows **43°28'02.81" N, 11°53'06.46" E**.

**8. How do you know the export has no metadata?**
After export we pull the file and check with an independent parser (PIL): **no `Exif` marker, zero
EXIF entries**, dimensions preserved. Tested for both single and batch exports.

**9. Is the blur irreversible?**
It is a mosaic over a padded rectangle and the export is re-encoded, so the original pixels are not
recoverable from that file. It is not a cryptographic guarantee: a very high-resolution source and a
small blur could still leak. The limitation is documented.

**10. Could the app leak the original by accident?**
The original is never modified; the export is a new file through MediaStore. No copy of the original
is kept by the app.

---

## C. Implementation questions

**11. How is the risk score calculated?**
Each factor has a weight: GPS and a phone/address in the caption **25**, e-mail 15, school/work 10,
date/handle 6, link 4; metadata fields are weighted by risk (high 25, medium 10, low 3). The sum is
capped at **100**. The UI lists the factors, so the number is explainable.

**12. Is the score scientific?**
No — it is a transparent prioritisation heuristic, not a calibrated probability. We say so in the
UI and the report. A user study would be needed to calibrate it.

**13. Why placeholders instead of deleting the details?**
Deleting can break the sentence. `[phone]` / `[address]` keeps it readable, and the app offers Undo;
the keyboard has a 30-snapshot undo stack. If the editor refuses the batch edit, the keyboard falls
back to copying the cleaned text.

**14. Why a keyboard if it cannot see the photo?**
Because the image is not visible to an IME: `InputConnection` exposes the focused text field, and the
image-keyboard API only lets a keyboard **send** images to an app. The keyboard covers the caption
half; the app and the share sheet cover the image. We documented the limits and the accessibility
alternative (screen capture) rather than pretending it works.

**15. Why not use accessibility to capture the screen?**
It is technically possible (`AccessibilityService.takeScreenshot`, API 30+) but requires the user to
enable accessibility, is restricted by Play policy to accessibility purposes, can be blocked by
`FLAG_SECURE`, and breaks when apps change. It is a different product with a higher risk profile.

**16. Why batch only 12 photos?**
Sequential decode to keep memory bounded; each photo is processed one at a time. The cap is a
deliberate safety limit, not a platform limit.

**17. What about HEIC/PNG/video?**
The reader is JPEG/EXIF-focused. PNG text chunks and vendor blocks fall into “other”; re-encoding
removes them but does not explain them field by field. Video is out of scope. This is in the report.

**18. Why Android only?**
The implementation targets the team’s devices and the Android metadata/face APIs. iOS would need a
separate build, a different face stack and TestFlight; it was deliberately dropped from scope.

---

## D. Evaluation and evidence

**19. How did you evaluate it?**
Technical verification on an Android 16 emulator with real camera files: GPS parsed, metadata list
matches the file, face detection and mosaic verified visually, caption findings verified, exports
checked for zero EXIF. No user study; that is a stated limitation.

**20. What is the strongest evidence for the research part?**
The audit table: **11 settings, 10 start exposed**, only website embeds off; plus Liu et al.’s **37 %**
mismatch and Gross & Acquisti’s 2005 baseline, and the FTC’s 2024 finding that practices are
“dangerously opaque”.

**21. What would you do with one more month?**
On-device NER for captions (names, destinations, plans), ID-card/licence-plate redaction, the new
Android picker location toggle, a proper user study to calibrate the score, and iOS if the scope
allowed.

---

## E. Market and product

**22. Who are the competitors?**
Android metadata tools: Redact, Exif Eraser, MetaClean. iOS all-in-one: ScrubPix, B4U Post, Social
Media Privacy Checker. Single-purpose: Signal’s blur, exif-remove. See `MARKET_ANALYSIS.md`.

**23. Why would anyone use this instead of the built-in controls?**
Built-in controls are per-app and per-setting; the system picker redacts location but not the
caption, and it does not explain what is left. The app is a single pre-post check with an explanation
and a clean copy.

**24. Is this a product or a course demo?**
A course demo with product-shaped decisions: offline, no account, undo, batch, share sheet, keyboard.
It is debug-signed and not on Play.

---

## F. Ethics and policy

**25. A keyboard sees everything typed. Isn’t that dangerous?**
Yes — that is why the prototype reads only the focused field, has no network calls, stores nothing,
and has no logging. A shipping keyboard would need a clear disclosure and a privacy policy.

**26. Doesn’t removing metadata help bad actors?**
The same capability is in every camera and every OS. The tool makes the exposure visible to the
person who is about to post; it does not add a capability that does not already exist.

---

## Key numbers to memorise

| Number | Meaning |
|---|---|
| **11 / 10** | Privacy settings checked on a fresh account / that start exposed |
| **37 %** | Liu et al.: settings matched user expectations |
| **2005** | Gross & Acquisti baseline |
| **91** | Nikon sample score (GPS, time, device, software) |
| **43°28'02.81" N, 11°53'06.46" E** | GPS read by the custom parser |
| **25 / 15 / 10 / 6 / 4** | Caption weights (GPS/phone/address, e-mail, school, date/handle, link) |
| **12** | Batch limit |
| **30** | Keyboard undo snapshots |
| **0** | EXIF entries in exported files |
| **10 pages** | Code documentation (12 pt, 1.15 spacing) |

---

## If you don’t know

- “I don’t have that number in front of me; the report section is … and I can follow up.”
- “That is a limitation we documented — here is the boundary we chose and why.”
- “We tested it on X; we did not test Y, so I would not claim that.”

Never improvise a statistic. Point to the table, the report, or the repo.

---

## Questions to ask the audience (if Q&A stalls)

- “Would you trust a score you cannot inspect, or a list of factors?”
- “Should the platform remove metadata by default, or should the user choose?”
- “Would you use a keyboard that warns you about the caption?”
