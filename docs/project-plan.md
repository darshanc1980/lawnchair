# Custom Android Launcher — Living Project Plan

**Owner:** Darshan C (PM: Grok Bot)  
**Updated:** 2026-11-04 ~23:03 Asia/Calcutta  
**Status:** Fork live; Velo rebrand done; hybrid arch sketched; **Phase 0 stub (`HomeLayoutMode` + FavoritesRailView) on `feat/home-layout-mode-stub`**

---

## Goal

Build a home-screen launcher that beats Niagara, Lawnchair, Smart Launcher, and Nova in 2026 by combining:

- **Hybrid UX:** Niagara-style one-handed vertical favorites + alphabet scroller by default; optional classic grid
- **Excellent free tier** (no aggressive paywalls)
- **Privacy-first:** zero trackers, open-source
- **Strong performance** and best-possible gesture navigation on non-rooted devices
- **Generous customization** without bloat
- **Material 3 Expressive** theming

## Technical foundation

| Item | Choice |
|------|--------|
| Base | Lawnchair (`16-dev`), AOSP Launcher3 / Android 16 lineage |
| Clone | `git clone --recursive https://github.com/LawnchairLauncher/lawnchair.git` |
| New code preference | `lawnchair/` module |
| Build variant | `lawnWithQuickstepGithubDebug` |
| Assemble task | `./gradlew assembleLawnWithQuickstepGithubDebug --no-configuration-cache` |
| Local path | `/workspace/lawnchair` |
| JDK | `/home/box/jdk21` (Temurin 21) |
| SDK | `/home/box/android-sdk` |

### Verified build environment (this run)

- **JDK:** Temurin 21.0.13 (`/home/box/jdk21`) — required (`jdkRelease = 21` in catalog)
- **SDK:** `/home/box/android-sdk` — platforms `android-37.2`, build-tools `37.0.0` (also had `android-34` / `34.0.0`)
- **AGP / Gradle:** AGP 9.5.0, Gradle 9.8.2 (wrapper)
- **Note:** `./gradlew tasks --all` can fail on Compose Preview task wiring; assemble still works. Prefer `--no-configuration-cache` if cache issues appear.

## MVP priorities

1. Hybrid home screen (vertical list default + optional grid) — **architecture sketched** → see design doc
2. Strong alphabet fast-scroller in app drawer
3. Full free customization (icon packs, gestures, grids, folders)
4. Clean Material 3 theming (Expressive)
5. Solid local search
6. Backup / restore
7. Zero trackers, open-source

## Later phases

- Smart organization (local only)
- Flexible widgets / stacking
- Usage-based suggestions (on-device)
- Better foldable / tablet support
- Magisk / QuickSwitch module for rooted users

## Risks

| Risk | Notes |
|------|--------|
| OEM gesture lag | Samsung, Xiaomi, and other OEMs limit third-party launcher gesture quality; Quickstep helps but non-root ceiling remains |
| Launcher3 surface area | Large AOSP + Quickstep codebase; prefer additive work in `lawnchair/` to ease upstream merges |
| Upstream churn | `16-dev` moves fast (SDK 37.2, AGP 9.x, JDK 21) |
| Compose Preview / Gradle quirks | `tasks --all` may fail creating Compose Preview runfiles; does not block assemble |
| Branding / package ID | applicationId already `app.velo.launcher` on github channel; Kotlin package still `app.lawnchair.*` |
| Lawndeck confusion | Existing `enable_lawn_deck` is **not** Niagara-style; hybrid rail must stay separate |

## Design docs

| Doc | Path | Status |
|-----|------|--------|
| **Hybrid home architecture** | `docs/hybrid-home-architecture.md` (in repo) | **Sketched** on branch `docs/hybrid-home-arch` |
| Living plan (mirror) | `docs/project-plan.md` | Synced from `/workspace/launcher-project-plan.md` |

## Repo remotes (2026-11-04)

| Remote | URL |
|--------|-----|
| `origin` | https://github.com/darshanc1980/lawnchair (our fork) |
| `upstream` | https://github.com/LawnchairLauncher/lawnchair |

Default branch on fork: `16-dev`.

## Branding (working)

| Item | Value |
|------|--------|
| Working name | **Velo** |
| applicationId (github channel) | `app.velo.launcher` |
| Debug package | `app.velo.launcher.debug` (suffix `.debug`) |
| Visible label | `Velo` / `Velo (Debug)` |
| Icon | Indigo `#4F46E5` adaptive + white letter **V** (spike art) |
| Branch | `feat/velo-rebrand` (pushed to origin) |
| APK | `/workspace/lawnchair/build/outputs/apk/lawnWithQuickstepGithub/debug/Velo.16.Dev.(85493b9).github.debug.apk` |
| Status | Rebrand spike complete — Kotlin/Java package still `app.lawnchair.*` (applicationId only) |

### Rebrand key files

- `build.gradle` — github `applicationId`, `derived_app_name`, APK `outputFileName`
- `lawnchair/res/drawable/ic_velo_launcher_{background,foreground}.xml`
- `lawnchair/res/drawable/ic_launcher_home.xml` + monochrome override
- `lawnchair/res/mipmap-anydpi-v26/ic_launcher_home{,_round}.xml`

## Immediate next steps

1. ~~Fork under darshanc1980~~ **done** — https://github.com/darshanc1980/lawnchair
2. ~~Rename / rebrand spike~~ **done** — Velo / `app.velo.launcher` on `feat/velo-rebrand`
3. ~~Architecture sketch for hybrid home~~ **done** — `docs/hybrid-home-architecture.md` on `docs/hybrid-home-arch`
4. **First code spike PR:** `HomeLayoutMode` pref + stub `FavoritesRailView` (see architecture §11)
5. Optional: merge rebrand to `16-dev`, polish icon, decide nightly/play applicationIds later
6. **Install debug APK** on a test device and baseline gesture / drawer performance vs Lawnchair stock
7. **Privacy inventory:** confirm no analytics / crash trackers in dependency tree for the free build
8. Resolve open PM questions in architecture §9 (default mode, widgets, persistence Option A, dock-in-rail)

## Clone / build status (2026-11-04)

| Field | Value |
|-------|--------|
| Path | `/workspace/lawnchair` |
| Branch (base) | `16-dev` @ `85493b9776` |
| Active feature branches | `feat/velo-rebrand`, `docs/hybrid-home-arch` |
| Remotes | `origin` = darshanc1980 fork; `upstream` = LawnchairLauncher |
| Submodule | `platform_frameworks_libs_systemui` @ `24bfa2e83b174c36ae1cb9ff50d90883c3b87597` |
| Gradle command | `./gradlew assembleLawnWithQuickstepGithubDebug --no-configuration-cache` |
| Last assemble | **SUCCESS** (Velo rebrand APK) |
| Fork | https://github.com/darshanc1980/lawnchair |
