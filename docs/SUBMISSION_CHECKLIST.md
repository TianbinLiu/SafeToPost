# Submission checklist

Course: CSCI 180 Computer Security · Fall 2026
Project: *Behind the Default Settings: How Social Networks Collect, Track, and Expose User Data*
Implementation: Safe to Post v0.9.0

---

## 1. Deliverables

| Submission item | File / URL | Where it lives | Status |
|---|---|---|---|
| Report — PDF | `SafeToPost_Code_Documentation.pdf` | course folder + `docs/` in the repo | ✅ 10 pages, 12 pt, 1.15 spacing |
| Report — editable source | `SafeToPost_Code_Documentation.docx` | course folder + `docs/` in the repo | ✅ |
| Source code | https://github.com/TianbinLiu/SafeToPost | public GitHub repository | ✅ v0.9.0 |
| Test APK | https://github.com/TianbinLiu/SafeToPost/releases/tag/v0.9.0 | GitHub Releases (`SafeToPost-0.9.0.apk`) | ✅ 63 MB, debug-signed |
| Presentation | Google Slides deck in the group folder | Drive → `CSCI 180 Group` | ✅ 22 slides, shared with Connor/Jason as editors |
| Implementation notes | `docs/CODE_DOCUMENTATION.md` (outline) + PDF/DOCX | repo | ✅ |
| Market / feasibility studies | `docs/MARKET_ANALYSIS.md`, `docs/IME_FEASIBILITY.md` | repo | ✅ |
| Demo script | `docs/DEMO_SCRIPT.md` | repo | ✅ |
| Defense notes | `docs/DEFENSE_NOTES.md` | repo | ✅ |

Note on the report: the **PDF/DOCX in `docs/` and in the course folder are the same final 0.9.0
version**. The Markdown file is an earlier outline kept for editing; the PDF is canonical.

---

## 2. Format requirements (from the project description)

- [x] Report starts with the project description and background, then code documentation.
- [x] Body text **12 pt**, line spacing **1.15**, 1-inch margins.
- [x] Explains what each part of the code does, how the components work together, and how to build,
      run and reproduce it.
- [x] At least one peer-reviewed reference: Liu et al. 2011, Gross & Acquisti 2005, Ebert et al.
      2023, plus the FTC 2024 report and Android/ML Kit documentation.
- [x] No page limit; the report is 10 pages.
- [x] Group/individual names and course on the first page.

If the instructor requires a different first-page format, edit the DOCX first page; do not edit the
PDF alone.

---

## 3. Link and access checks

- [ ] GitHub repository opens while logged out: README, `app/`, `docs/`.
- [ ] Release page opens while logged out and the APK downloads.
- [ ] Google Slides deck opens with the instructor account (or “anyone with the link can view”).
- [ ] Group folder contains the final PDF/DOCX (not an older 0.8 version).
- [ ] The APK installs on a clean Android 10+ device: first launch, grant media-location, pick a
      photo, export, open the exported file.
- [ ] Keyboard (optional): enable it, switch, type a caption, see the strip, Clean, Undo.

---

## 4. Demo kit (bring to class)

**Phone / emulator**

- APK installed and opened once (permissions granted).
- Two sample photos in the gallery: Nikon landscape (GPS) and the portrait.
- Theme set to Forest (Aa → Forest).
- Caption pre-typed on the Nikon photo, screen left on the Caption check card.
- Settings search pre-typed with `Call 4085551234 from 123 Main Street`, then switched to the
  Safe to Post keyboard.
- Do Not Disturb on, brightness up, rotation locked.
- Charger / USB cable.

**Backup**

- `docs/images/` open in a second window: `ui-before-after.png`, `clean-caption.png`,
  `keyboard-risk.png`, `keyboard-handoff.png`, `batch.png`, `batch-face-blur.png`.
- The deck itself has the screenshot evidence on slides 16–18.
- A short screen recording of the 60-second flow, if you have one.

---

## 5. Ten-minute pre-class checklist

1. Deck opens on slide 1; presenter notes visible if needed.
2. Phone mirrored; app at the prepared state; Settings in recents.
3. Keyboard is still the selected input method? (Switching IMEs is the most fragile step.)
4. Photos still in the gallery; caption still in the field; theme still Forest.
5. Do Not Disturb; notifications off; auto-rotate off.
6. Backup screenshots open in a second window.
7. Water; timer (15 min) visible to the speakers.
8. Handoff lines rehearsed: Connor → Jason → Tianbin.

---

## 6. File locations on this machine

| File | Path |
|---|---|
| Report PDF | `C:\Users\tliu7\Desktop\Lecture\CSCI180 Computer Security\SafeToPost_Code_Documentation.pdf` |
| Report DOCX | `C:\Users\tliu7\Desktop\Lecture\CSCI180 Computer Security\SafeToPost_Code_Documentation.docx` |
| APK | `C:\Users\tliu7\Downloads\SafeToPost-0.9.0.apk` |
| Screenshots / demo assets | `C:\Users\tliu7\Downloads\SafeToPost_test\` |
| Repo screenshots | `C:\Users\tliu7\SafeToPost\docs\images\` |
| Slides (Drive) | `CSCI 180 Group` → `CSCI 180 - Group Presentation - Behind the Default Settings` |

---

## 7. After submission

- Keep the release tag and repo unchanged until grades are out; do not force-push.
- If a link is broken, re-upload the APK to the same release rather than creating a new tag.
- Record the questions asked in class and add them to `DEFENSE_NOTES.md` while fresh.
