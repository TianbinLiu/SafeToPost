# Can Safe to Post plug into the phone keyboard?

Short answer: **a keyboard can check the caption text, but it cannot see the photo the user
picked in another app.** The image check has to happen in the app or through the share
sheet; a keyboard-only integration would be a caption guard, not a full pre-post check.

## What an input method can and cannot see

An Android IME is an app that draws a keyboard and talks to the focused text field through
an `InputConnection` ([Android IME guide](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)).
That connection gives it:

- the text around the cursor (`getTextBeforeCursor`, `getExtractedText`),
- the editor's action/label (for example "Search", "Send"),
- the ability to commit text, delete, or send rich content **to** the app.

It does **not** give the keyboard access to the host app's view hierarchy, its image
attachments, or its camera. The Android "image keyboard" API only works in one direction:
the keyboard sends an image to the app through `commitContent()`
([Image keyboard support](https://developer.android.com/develop/ui/views/touch-and-input/image-keyboard)).
There is no API for "read the image this Instagram editor is about to post".

Reading a text field is also not unconditional: password fields and some editors block
extraction, and the keyboard only sees the field it is attached to.

## Three possible integration points

| Approach | What it can check | Platform support | Limits |
|---|---|---|---|
| **Caption-guard IME** | Caption text in the focused field | Standard `InputConnection` API; a small `InputMethodService` can show a warning strip above the keys | No image access; sees only the active text field; the keyboard becomes a high-trust app (it sees everything typed) |
| **Share-sheet check** (already in v0.2.0) | The image file and, if the app passes it, the caption | `ACTION_SEND` with `EXTRA_STREAM`; platform-sanctioned | Only fires when the user shares; an in-app camera flow does not hit it |
| **Accessibility companion** | The whole screen, including the editor and the photo | `AccessibilityService.takeScreenshot()` (Android 11+) with `canTakeScreenshot`; overlay via `TYPE_ACCESSIBILITY_OVERLAY` | User must enable accessibility; Google Play restricts the Accessibility API to accessibility purposes; `FLAG_SECURE` windows return black; battery and privacy cost; fragile across app versions |

Accessibility screenshot details: `takeScreenshot` was added in API 30 and requires the
service to declare the capability; the user grants it through the accessibility settings
([AccessibilityService reference](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)).
Google Play's [Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491)
says the API may only be used for accessibility purposes and that apps must disclose and
justify its use; using it as a general screen scraper is a policy risk, not a technical
one.

On iOS the situation is similar: a keyboard extension can read the photo library only when
the user grants **Full Access**, but it still cannot read the image the host app has
selected (the keyboard has no access to the host's document model). Apple's sanctioned
hook is the share extension, exactly like Android's share sheet
([Stack Overflow discussion](https://stackoverflow.com/questions/36231848)).

There is precedent for the text half: TypeSafe Keyboard watches typed text for patterns
that look like card numbers or SSNs and alerts a caregiver
([App Store listing](https://apps.apple.com/us/app/advoteck/id6569254742)). No mainstream
keyboard audits photos.

## What this means for the project

1. **A keyboard cannot be the image checker.** The photo is inside another app's process;
   neither `InputConnection` nor the rich-content API exposes it. Anything that claims
   otherwise on Android is using accessibility or screen capture, not the IME API.
2. **A caption-guard IME is realistic and small.** `CaptionScanner` already returns
   `CaptionFinding` objects (kind, range, weight). An IME service can run it on
   `onUpdateSelection` / text changes and show "2 personal details in this caption" above
   the keyboard, with a button that opens Safe to Post with the text prefilled for the
   photo check. This is a 1–2 day prototype, not a rewrite.
3. **The share sheet is the right place for the image check.** It is the only
   platform-sanctioned hook that hands us the actual file before it leaves the device. The
   Android photo picker's new location toggle shows the platform is moving in this
   direction, but it still does not let a third-party app inspect another app's editor.
4. **The accessibility route is a research prototype, not a product.** It could capture the
   screen when a known social app is in the foreground, run ML Kit OCR/face detection
   on-device, and draw an overlay warning. It would need an explicit user opt-in, a clear
   Play policy justification, and handling for `FLAG_SECURE`. It is also the most likely
   path to break silently after an app update.

## If we build the IME prototype

- `SafeToPostImeService : InputMethodService`, a compact QWERTY (or a "privacy strip"
  above a minimal keyboard), with `onStartInput`/`onUpdateSelection` calling
  `CaptionScanner`.
- A banner: "phone + address in this caption" with a **Review** button that launches
  `MainActivity` with `EXTRA_TEXT` and the caption prefilled.
- No network permission, no logging of typed text, and a clear settings screen: the
  keyboard is a high-trust component and must say what it reads.
- Test matrix: Instagram/TikTok caption fields, X composer, the system Messages app; check
  what `getExtractedText` returns in each, and what happens in password fields.

## Prototype in this repository (v0.4.0)

`app/src/main/java/app/safetopost/ime/SafeToPostImeService.kt` is the caption-guard
prototype: a compact QWERTY with a digit row, shift, space, period, enter and backspace,
plus a strip above the keys. The strip runs `CaptionScanner` on the focused field and
shows either "caption looks clean" or "phone + address in this caption · tap to review".
Tapping it opens `MainActivity` with the caption attached; the empty state shows a
"FROM THE KEYBOARD" card and keeps the text when a photo is picked.

Enable it on a device: Settings → System → Languages & input → On-screen keyboard → Manage
keyboards → enable **Safe to Post keyboard**, then switch with the keyboard switcher.
Command-line equivalent for testing:

```bash
adb shell ime enable app.safetopost/.ime.SafeToPostImeService
adb shell ime set app.safetopost/.ime.SafeToPostImeService
# back to the system keyboard afterwards:
adb shell ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME
```

Verified on an Android 16 emulator: typing "Call 4085551234 from 123 Main Street" in the
Settings search box turns the strip into "phone + address"; tapping it opens Safe to Post
with the caption in the "FROM THE KEYBOARD" card.

Known limits of the prototype: English letters and digits only, no prediction or
autocorrect, no emoji, no voice input; it reads only the focused field and sends nothing
off the device. It is a demonstration of the caption half, not a replacement for the
image check.

## Recommendation

Ship the share-sheet path (done) and keep the keyboard as an optional **caption guard**
prototype if the demo needs a "keyboard warns you" moment. Do not try to make the keyboard
inspect photos: the platform does not allow it, and the accessibility workaround is a
different product with a different risk profile.
