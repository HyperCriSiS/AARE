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

## Upstream automation

| Upstream | Kind | Intake | Release |
|---|---|---|---|
| `prototux/AARE` | fork source | `.github/workflows/update-agent.yml`, daily, one PR per upstream change into `main` | manual |
| Gradle and GitHub Actions dependencies | dependency | Dependabot weekly, merged by the update agent after a week | manual |

- Protected features: the fork's car screen and phone app parts. A change that touches any of
  these paths is held for a person, whatever its checks say; the JVM unit tests prove the logic
  in them, the rest only a test in the car:
  - `app/src/main/java/net/prototux/aare/ui/car/` (rail, split screen, shortcuts, dashboard)
  - `app/src/main/java/net/prototux/aare/host/CarRuntime.kt`, `host/CarAppScreen.kt`,
    `host/AndroidAutoApps.kt`, `host/templates/`
  - `app/src/main/java/net/prototux/aare/mirror/`, `launch/` (phone screen, hosted video)
  - `app/src/main/java/net/prototux/aare/media/` (media apps, video mode)
  - `app/src/main/java/net/prototux/aare/ui/MainActivity.kt` (Apps page, backup)
  - `app/src/main/java/net/prototux/aare/service/ProjectionService.kt` (certificate choice)
  - `app/src/main/java/net/prototux/aare/Settings.kt`, `SettingsBackup.kt`, `UpdateChecker.kt`
- Agent run: `update-agent.yml`, daily at 07:17 UTC; a first job checks whether anything waits,
  and only then the agent starts. It merges upstream changes that touch no protected path and
  pass CI, and due dependency updates; it holds the rest with the label `:hand: hold`.
  `.github/upstream-reviewed` records the last upstream commit it looked at.
- Recovery: run `update-agent` by hand (workflow_dispatch) once the cause is fixed.
- Pause: repository variable `UPDATE_AGENT=false`, or the open issue "Update needs a manual fix"
  that the automation opens.
- Releases are always made by hand after the owner's approval; the automation never tags.

## Branches and releases

- Default branch: `main`. Pull requests are squash merged.
- `test/car-build` collects open branches for testing in the car; it is never merged.
- A release is a `v<version>` tag on `main`; `.github/workflows/release.yml` builds and signs it
  with the fork's key from the repository secrets. Releases need the owner's approval first.
