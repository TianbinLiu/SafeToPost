# Market check: photo-privacy tools before posting

Research snapshot: November 2026. Feature lists come from the Play Store / App Store
listings and the articles linked below; prices and features change, so re-check before
quoting them in a presentation.

## What already exists

| Product | Platform | What it does | What it does not do |
|---|---|---|---|
| [Redact: Photo Metadata Remover](https://play.google.com/store/apps/details?id=com.doubleangels.redact) | Android | Shows what a file reveals, strips GPS, EXIF, XMP, IPTC, comments and video tags; offline; one-time purchase | No face blur, no caption check |
| [Exif Eraser](https://play.google.com/store/apps/details?id=com.none.tom.exiferaser) | Android | Permissionless metadata eraser (EXIF, XMP, ICC) for JPEG/PNG | No preview of what was found, no risk score, no faces/caption |
| [Metadata Remover (MetaClean)](https://play.google.com/store/apps/details?id=com.patzerllc.MetaClean) | Android | Batch clean, share several photos, per-photo result review | Metadata only |
| [ScrubPix](https://apps.apple.com/na/app/scrubpix-exif-privacy-ai/id6785740201) | iOS | Local AI: strip EXIF/GPS, blur faces, IDs and plates, OCR phone/email/ID patterns, album scan, share extension, batch up to 500, face allow/block lists; one-time $2.99 | iOS only; caption *text* is not checked; no cross-signal risk explanation |
| [Social Media Privacy Checker](https://apps.apple.com/na/app/social-media-privacy-checker/id6770195065) | iOS | On-device review of faces, addresses, plates, audio and metadata before posting | iOS only; no caption text check |
| [B4U Post](https://apptopia.com/ios/app/6758669603/about) | iOS | Share-sheet-first: shows what a photo/video reveals, removes metadata in one tap | Metadata only, iOS only |
| [Metapho](https://apps.apple.com/sg/app/metapho/id914457352) | iOS | Metadata viewer/editor | No faces, no caption, no score |
| Exif Detail / LudyExif | Android / iOS | Per-photo "privacy score" from metadata | Metadata only, no caption, no face blur |
| [Signal blur tools](https://signal.org/blog/blur-tools/) | Android/iOS | Blur faces or areas inside Signal's image editor | Only inside Signal; no metadata audit, no caption |
| [exif-remove](https://github.com/jakobkreft/exif-remove) | Android (open source) | Share target, templates, strips metadata offline | Metadata only |

Platform features worth knowing:

- Android's Photo Picker now has a **location metadata API**: the picker asks whether to
  include location, with a toggle inside the picker. Snapseed was the first app to ship it
  ([Android Authority, Aug 2026](https://www.androidauthority.com/android-photo-picker-location-sharing-release-3704689/)).
- Google Photos can remove geolocation from shared links
  ([Google support](https://support.google.com/maps/answer/7009612)).
- iOS asks about location when sharing and Photos can remove it per item
  ([iPhoneliFe](https://www.iphonelife.com/content/how-to-remove-your-location-shared-photos)).
- A hackathon project, [SafePost](https://devpost.com/software/safepost), describes the
  same idea (AI scans images *and* captions for personal details), but it is a concept
  prototype, not a shipping product.

## Where Safe to Post sits

Unique combination in this survey:

1. **Metadata explained, not just stripped** — every field with source and risk, and a
   readable "what this file reveals" summary.
2. **Face selection** with an explicit "which faces to blur" step, like Signal/ScrubPix.
3. **Caption text check** — rare. Only the SafePost concept and ScrubPix's in-image OCR
   come close; neither scores caption text against photo metadata.
4. **One explained 0–100 score** across all three signals, with per-factor breakdown.
5. **Android**, where the closest all-in-one products are iOS-only.

So the gap is less "nobody does this" and more "nobody puts all three checks in one
Android flow, with an explanation".

## Follow-up opportunities (impact / effort)

| Opportunity | Why it matters | Effort |
|---|---|---|
| Share-sheet intake and "clean & share" onward | The winning convenience pattern (B4U Post, exif-remove, ScrubPix). Users already share from the gallery, not from a separate app. **Implemented in v0.2.0** as an intake path; onward sharing from the export snackbar is also in. | Low |
| Batch mode ("clean 20 photos") | MetaClean, Redact and ScrubPix Pro all support it; a camera roll needs it | Medium |
| Use Android's new picker location toggle | Keeps control at the moment of picking; avoids the `ACCESS_MEDIA_LOCATION` prompt and the redacted-copy confusion | Medium |
| License plates, ID cards, screens | ScrubPix covers them; our face-only redaction misses the most common "oops" objects | Medium |
| Caption NER / small on-device model | Catches names, destinations, plans that regex misses; SafePost's LLM idea | High |
| Map thumbnail for GPS + distance from campus/home | Makes "location" tangible in a screenshot for the presentation | Low |
| Before/after "what was removed" sheet and presets (story / post / close friends) | exif-remove templates; a preset is more usable than a checklist | Low |
| Dark theme, Chinese localization, accessibility pass | Broader audience and a more finished demo | Low |
| iOS port | The closest competitors are iOS-only, so the gap is real — but it needs a Mac, a different face stack and TestFlight, which is outside this project's scope | High |

## What to say in the presentation

- The market is split: metadata erasers on Android, all-in-one privacy checkers on iOS,
  and face blur inside messaging apps. The **combination is the differentiator**, not any
  single feature.
- The platform itself is moving toward "privacy at pick time" (Android's picker location
  API), which supports the project's thesis that defaults are design choices.
- The honest limitation: our caption scanner is rule-based, and our face blur is a fixed
  mosaic. ScrubPix already ships OCR-based redaction and face lists; matching that is the
  next step, not a solved problem.
