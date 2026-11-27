# Demo script — 15-minute group presentation

**Project:** Behind the Default Settings: How Social Networks Collect, Track, and Expose User Data
**App:** Safe to Post v0.9.0
**Speakers:** Connor Toreson (platform collection) · Jason Zhou (trackers) · Tianbin Liu (defaults + implementation)
**Deck:** 22 slides (Google Slides, group folder). Slide numbers below refer to that deck.

> 中文提示：这是一份可以直接照着讲的稿子。英文句子是台上说的，括号里的中文是给你的操作提示。
> 演示的核心只有 60–75 秒：分数 + GPS → Clean caption → 键盘提示 → 回到 app。其余功能用截图/一句话带过。

---

## 1. At a glance

| Time | Slides | Who | What |
|---|---|---|---|
| 0:00–0:30 | 1 | Tianbin | Title + the question |
| 0:30–1:15 | 2 | Tianbin | Roadmap + time budget |
| 1:15–2:15 | 3 | Tianbin | Why defaults matter (37 %, 2005, FTC) |
| 2:15–5:15 | 4–12 | Connor | What platforms collect |
| 5:15–8:15 | 13–14 | Jason | Who else is watching (trackers) |
| 8:15–13:30 | 15–20 | Tianbin | Method → finding → cross-platform → why/fix **+ demo** |
| 13:30–14:15 | 19 | Tianbin | Defensive rules / regulation |
| 14:15–14:45 | 20 | Tianbin | Conclusion |
| 14:45–15:00 | 21 | All | Q&A handoff |

Within Tianbin's 5:15:

| Time | Slide | Content |
|---|---|---|
| 8:15–9:15 | 15 | Method: fresh account, 11 settings, 10 of 11 exposed |
| 9:15–10:45 | 16 | Finding + audit table |
| 10:45–11:45 | 17 | Cross-platform: TikTok / X / Facebook |
| 11:45–12:45 | 18 | Why it is hard + 10-minute checklist |
| 12:45–13:30 | 18 | **Live demo (45 s)** + close of section |

If the talk is running late, demo only the caption-clean (20 s) and skip the keyboard.

---

## 2. Thirty minutes before

**Device / emulator**

- Install `SafeToPost-0.9.0.apk` (release page) or `app-debug.apk` from the repo build.
- Grant `ACCESS_MEDIA_LOCATION` (needed for the GPS rows).
- Put the two sample photos in the gallery: the Nikon landscape (GPS) and the portrait. They are in
  `Downloads/SafeToPost_test` on the demo laptop if you need to `adb push` again.
- Set the theme to **Forest** (Aa → Forest) so the screen is not the default orange on the projector.
- Open the app once and leave it on the empty state.

**Prepared state for the 45-second demo**

1. In the app, choose the Nikon landscape, let it scan, then type the caption
   `Call 4085551234 from 123 Main Street` (use Gboard; this is before the demo).
2. Scroll so the **Caption check** card is visible, with the Clean caption button just below the
   field. Leave the app there.
3. In Settings, type the same caption into the search box **with the normal keyboard**, then switch
   the input method to **Safe to Post keyboard** and focus the search box. The strip should read
   `phone + address in this caption`. Leave Settings in the recents.
4. Disable notifications / do-not-disturb, set brightness, lock rotation.

**Backup**

- `docs/images/` has every screen used in the demo. Keep the folder open in a second window.
- If the phone fails, say: “I have a recording of the same flow” and show the screenshots; do not
  debug on stage.

---

## 3. Script

### 0:00–0:30 — Title (slide 1)

> “Every social app ships with a privacy configuration. Most people never open it. We asked what
> that default actually exposes — and then built a tool that checks a photo before you post it.”

### 0:30–1:15 — Roadmap (slide 2)

> “Three layers of one account: what the platform collects, who else is watching, and what the
> defaults expose. Connor takes the first, Jason the second, I take the third — about eleven minutes
> of content, then questions.”

### 1:15–2:15 — Why defaults (slide 3)

> “Two numbers. Liu et al. found that users’ actual Facebook settings matched what they wanted only
> **37 %** of the time, and when they missed, the account was almost always more exposed than
> intended. Gross and Acquisti saw the same thing in 2005: most students never changed the defaults,
> so most profiles were visible network-wide. Defaults are not neutral; they are the policy most
> people live under.”

> “Connor will show what the platform collects when you do nothing.”

### 2:15–5:15 — Connor (slides 4–12)

Hand off; stay quiet. At the end: “Thanks Connor. Jason, who else is watching?”

### 5:15–8:15 — Jason (slides 13–14)

Hand off; at the end: “Thanks Jason. So the defaults are the third layer — that is my part.”

### 8:15–9:15 — Method (slide 15)

> “I made a fresh account, touched nothing, and recorded every privacy-relevant setting I could
> find: audience, activity status, tagging, ad personalisation, contact discovery, and so on.
> Eleven settings that a new user actually encounters. Ten of them start on the exposed option.
> The only one that is off is website embeds.”

### 9:15–10:45 — Finding (slide 16)

> “The table is the evidence. The pattern is not random: the exposed option is the one that keeps
> the network growing — discoverable, taggable, targetable. The private option exists, but it is
> never the default. And this is not only Instagram: TikTok, X and Facebook ship the same shape.”

### 10:45–11:45 — Cross-platform (slide 17)

> “TikTok defaults to a public account for adults, X to public posts, Facebook to a public-ish
> audience for much of the profile. The details differ; the direction does not.”

### 11:45–12:45 — Why it is hard (slide 18, top)

> “Why don’t people just change it? The settings are spread across five menus, the wording changes
> between platforms, and turning one thing off often turns off a feature people want. We wrote a
> ten-minute checklist instead of pretending it is a one-click fix.”

### 12:45–13:30 — Live demo (45–60 s)

Transition: “The checklist is the manual fix. The other half of my implementation is a tool that
does the last check for you — before the photo leaves the phone.”

**Step 1 — score and evidence (15 s).** The app is already on the Nikon photo.

> “This is a photo from a 2008 camera. The app reads the file, not the picture: location, capture
> time, device, software. 91 out of 100, and the card lists exactly why.”

*Action:* tap **Aa** only if the theme is wrong (it should already be Forest). Scroll to the
metadata card; the first rows are `GPS latitude 43°28'02.81" N` and `GPS longitude 11°53'06.46" E`.

> “Ten of eleven settings is the platform; this is the file. Same story: the default is to expose.”

**Step 2 — clean the caption (10 s).** Scroll to Caption check.

> “The caption is the second leak: a phone number and a street address. One tap replaces them with
> placeholders — and it is undoable.”

*Action:* tap **Clean caption** → `Call [phone] from [address]`; pause; tap **Undo** to show the
original returns. Leave it restored, or tap Clean again if you want to end on the clean state.

**Step 3 — keyboard (15 s).** Switch to Settings (recents).

> “A keyboard cannot see the photo inside another app — we checked the platform APIs. But it can
> see the caption as you type it. This is the Safe to Post keyboard: the strip already knows the
> caption is risky.”

*Action:* tap the search box (the strip is visible). Tap **Clean** on the strip → the field becomes
`Call [phone] from [address]` and the strip turns green. Tap **Undo** to restore, then tap the strip
itself to hand the caption to the app.

> “So the image check lives in the app and the share sheet; the keyboard covers the text half.”

**Step 4 — close (5 s).**

> “Everything runs on the phone: no account, no upload.”

### 13:30–14:15 — Defensive rules (slide 19)

> “Three rules: default to the private option, make the exposure visible, and make the safe path
> the easy path. The EU’s privacy-by-default principle and the FTC’s 2024 report point the same way.”

### 14:15–14:45 — Conclusion (slide 20)

> “One account, three layers. The defaults are a design choice, not a user skill. If you take one
> thing: check what the file and the caption say before you post.”

### 14:45–15:00 — Q&A (slide 21)

> “Thank you — questions?”

---

## 4. If the demo fails

| Failure | What to say | What to do |
|---|---|---|
| App crashes / photo won’t load | “The build is debug-signed; let me show the recorded flow.” | Open `docs/images/ui-before-after.png` and `clean-caption.png` |
| Keyboard does not appear | “The keyboard is a separate input method; switching takes a second.” | Try the keyboard switcher; if it still fails, show `keyboard-risk.png` |
| Settings search text was cleared | “The field is empty; here is the strip on the prepared screenshot.” | Show `keyboard-risk.png` / `keyboard-handoff.png` |
| Phone mirroring dies | “I’ll narrate the screenshots.” | Switch to the slide deck; the evidence is on slides 16–18 |
| Time is short | “I’ll leave the demo to the screenshots.” | Skip to slide 19; keep 30 s for the conclusion |

Never debug on stage for more than ~10 seconds. The screenshots are the backup, and the score is
already on the slides.

---

## 5. Extra demo (only if you have 3+ minutes)

| Feature | Steps | Line |
|---|---|---|
| Face blur | Open the portrait → People → Face 1 → Clean & export | “It finds the face; you choose whether to blur it.” |
| Batch | Batch → pick 2 photos → toggle **Blur all faces** → Clean 2 selected | “For a camera roll: score each photo, skip the ones you want, clean the rest.” |
| Share sheet | Gallery → Share → Safe to Post | “The image check also works from the share sheet.” |
| Themes | Aa → Ocean/Forest | “Four palettes; the change sweeps across.” |

---

## 6. After the talk

- Stop the screen mirror; close the app and Settings.
- If asked for the APK, point to the release page, not a file transfer.
- Note the questions you could not answer; add them to `DEFENSE_NOTES.md` the same day.
