# Custom Android Launcher — Living Project Plan

**Owner:** Darshan C (PM: Grok Bot)  
**Updated:** 2026-11-05 ~00:20 Asia/Calcutta  
**Status:** MVP completion branch `feat/velo-mvp-complete` — rail add/remove, alphabet polish, privacy, M3 rail, backup verified

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
| Fork | https://github.com/darshanc1980/lawnchair |

## MVP priorities — status

| # | Item | Status |
|---|------|--------|
| 1 | Hybrid home (vertical rail default + classic grid) | **Done** (mode + rail overlay + reorder + add/remove) |
| 2 | Strong alphabet fast-scroller | **Done** (`letterFastScroller` enabled; scrollbar default on) |
| 3 | Full free customization | **Inherited** from Lawnchair (icon packs, gestures, grids, folders) — no paywall added |
| 4 | Material 3 Expressive | **Partial** — settings already M3 expressive; rail row/scrim pass landed |
| 5 | Solid local search | **Inherited** — no rail regressions expected; smoke on device |
| 6 | Backup / restore | **Done** — DataStore prefs already in `LawnchairBackup`; documented in `docs/privacy.md` |
| 7 | Zero trackers | **Done for MVP** — inventory in `docs/privacy.md`; Live Information off by default |

## Merged / in-flight

| PR | Topic | State |
|----|-------|-------|
| #1 | Rebrand + architecture + HomeLayoutMode stub | Merged |
| #2 | Rail drag-reorder + order persistence | Merged |
| (this) | MVP complete: add/remove, alphabet, privacy, M3 rail, docs | `feat/velo-mvp-complete` |

## Later phases

- Smart organization (local only)
- Flexible widgets / stacking
- Usage-based suggestions (on-device)
- Better foldable / tablet support
- Magisk / QuickSwitch module for rooted users
- Favorites DB Option A/B (rail-owned pins)
- Device gesture baseline matrix (Pixel / Samsung / Xiaomi)

## Risks

| Risk | Notes |
|------|--------|
| OEM gesture lag | Non-root ceiling remains |
| Launcher3 surface area | Prefer `lawnchair/` additive work |
| Option C membership | Rail membership is Prefs ids; classic grid icons may remain after rail remove |
| FeatureFlagsImpl churn | `letterFastScroller=true` may need re-apply on upstream flag regen |

## Branding

| Item | Value |
|------|--------|
| Name | **Velo** |
| applicationId | `app.velo.launcher` |
| Debug package | `app.velo.launcher.debug` |

## Docs

| Doc | Path |
|-----|------|
| Hybrid home architecture | `docs/hybrid-home-architecture.md` |
| Privacy inventory | `docs/privacy.md` |
| This plan | `docs/project-plan.md` |
