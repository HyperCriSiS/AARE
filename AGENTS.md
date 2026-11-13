# Agent notes

- Fork of `prototux/AARE` for Mercedes-Benz MBUX head units, with its own releases signed with
  its own key. Upstream changes come in as single pull requests, never as a wholesale merge of
  the upstream branch.
- Language for code, comments, UI texts, commits and docs: American English. UI strings also have
  German translations in `values-de`.
- Keep upstream code recognizable: extend existing patterns, avoid reformatting or renaming
  upstream code, so later upstream changes still apply.
- Decision logic goes into plain functions the JVM unit tests cover (for example `splitWith`,
  `sideRailFit`, `swipeSide`, `shortcutColumns`, `hostedDpi`, `SettingsBackup.keeps`); Compose
  code only wires them.

## README style

- Title, bold tagline, one badge row (at most six).
- Every `##` heading in the fork's part starts with an emoji. Fixed ones: ✨ Features,
  📦 Installation, 🚀 Usage, ⚙️ Configuration, 🖼️ Screenshots, 🩺 Troubleshooting,
  💬 Support, 🙏 Credits, 📄 License. Other sections get a fitting emoji of their own, never one
  twice per file.
- The fork's sections lead; the technical part below them stays close to upstream's README so
  upstream edits still apply.
- Install path first, features as bullets, screenshots from an MBUX-sized display, GitHub alerts
  for warnings.

## Verified for real

A change counts as checked only when:

1. `./gradlew :app:testDebugUnitTest :aap:test :headunit:test` passes.
2. The car screen was exercised in the emulator: the car screen preview at 1748x798 and 240 dpi
   (the MBUX visible area), or Google's Desktop Head Unit connected to the emulator through the
   head unit server with an MBUX-like config (1920x1080, 240 dpi, margins 172x360). Screenshots
   were looked at, not only taken.
3. Anything needing Shizuku, root, a real head unit or real audio is tested on a phone in the
   car, or reported as untested.

## Branches and releases

- Default branch: `main`. Pull requests are squash merged.
- `test/car-build` collects open branches for testing in the car; it is never merged.
- A release is a `v<version>` tag on `main`; `.github/workflows/release.yml` builds and signs it
  with the fork's key from the repository secrets. Releases need the owner's approval first.
