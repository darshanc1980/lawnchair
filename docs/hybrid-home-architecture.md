# Velo Hybrid Home — Architecture Sketch

**Product:** Velo (`app.velo.launcher`)  
**Base:** Lawnchair `16-dev` / AOSP Launcher3 + Quickstep  
**Status:** Phase 1 MVP in progress — rail drag-reorder + Prefs order persistence  
**Updated:** 2026-11-05 Asia/Calcutta  
**Branch:** `feat/favorites-rail-mvp`

---

## 1. Product intent

Default home experience is **Niagara-style one-handed UX**:

- A **vertical favorites rail** (ordered list of pinned apps / folders) dominating the home surface
- App drawer opened with a **strong alphabet fast-scroller** (letter rail)
- **Classic multi-page grid** remains available as an explicit preference (parity with Lawnchair users)

Non-negotiables for the hybrid track: free-tier complete, zero trackers, prefer new code in `lawnchair/`, keep upstream merges tractable.

---

## 2. What exists today (code map)

### 2.1 Home / workspace / hotseat

| Piece | Path | Role |
|-------|------|------|
| Root layout | `lawnchair/res/layout/launcher.xml` (overrides `res/layout/launcher.xml`) | `DragLayer` hosts `@id/workspace`, `@id/hotseat`, `@id/apps_view`, overview, scrim |
| Workspace | `src/com/android/launcher3/Workspace.java` | Paged `CellLayout` screens; implements `WorkspaceLayoutManager`, drag/drop, state transitions |
| Cell grid | `src/com/android/launcher3/CellLayout.java` | Occupancy grid for icons/widgets/folders |
| Layout contract | `src/com/android/launcher3/WorkspaceLayoutManager.java` | Bind/add/remove item API used by model → UI |
| Hotseat / dock | `src/com/android/launcher3/Hotseat.java` | Bottom dock; Lawnchair already wires `HotseatMode` + `HotseatPagedView` |
| Hotseat modes | `lawnchair/src/app/lawnchair/hotseat/HotseatMode.kt` | `LawnchairHotseat` / `GoogleSearchHotseat` / `DisabledHotseat` |
| Paged dock | `lawnchair/src/app/lawnchair/hotseat/HotseatPagedView.kt` | Multi-page dock icons |
| Bind path | `src/com/android/launcher3/ModelCallbacks.kt` → `Workspace` / `Launcher` | `bindItems` / `bindItemsAdded` / `bindItemsUpdated` |
| Model DB | `src/com/android/launcher3/LauncherSettings.java` (`Favorites`) | `CONTAINER_DESKTOP (-100)`, `CONTAINER_HOTSEAT (-101)`; cellX/cellY/screen |
| Writer / loader | `src/com/android/launcher3/model/ModelWriter.java`, `LoaderTask.java`, `BgDataModel.kt` | Persist + cold-start bind |
| Device profile | `lawnchair/src/app/lawnchair/DeviceProfileOverrides.kt` | Applies `pref_workspaceColumns` / rows into IDP |

**Implication:** Classic home is a **paged cell grid** backed by Favorites rows with `(container, screen, cellX, cellY)`. There is **no** first-class ordered vertical favorites list today.

### 2.2 All-apps drawer + alphabet scroller

| Piece | Path | Role |
|-------|------|------|
| Container | `src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java` | Tabs, search, fast scroller host |
| Launcher subclass | `src/com/android/launcher3/allapps/LauncherAllAppsContainerView.java` | Launcher-specific callbacks |
| Sorted model | `src/com/android/launcher3/allapps/AlphabeticalAppsList.java` | Builds adapter items + `FastScrollSectionInfo` |
| Lawnchair list | `lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt` | Hidden apps, drawer folders / categorized list when `pref_drawerList=false` |
| Recycler | `src/com/android/launcher3/allapps/AllAppsRecyclerView.java` | Binds letter list via `setLettersToScrollLayout` |
| Fast-scroll UX | `src/com/android/launcher3/views/RecyclerViewFastScroller.java` | Thumb + letter highlighting |
| Section jump | `src/com/android/launcher3/allapps/AllAppsFastScrollHelper.java` | Smooth-scroll to section |
| Letter views | `src/com/android/launcher3/allapps/LetterListTextView.java` | Per-letter row in letter rail |
| Transition | `src/com/android/launcher3/allapps/AllAppsTransitionController.java` + `touch/AllAppsSwipeController.java` | Normal ↔ AllApps |

**Implication:** Alphabet infrastructure is **already solid**. Velo’s drawer work is mostly **defaults, density, and polish** (scrollbar / letter rail always on, optional single-column “list” drawer), not a greenfield scroller.

### 2.3 Preferences / settings hooks

| Layer | Path | Notes |
|-------|------|-------|
| Legacy SharedPreferences | `lawnchair/.../preferences/PreferenceManager.kt` | `pref_workspaceColumns/Rows`, `pref_hotseat*`, `pref_drawerList`, `pref_drawerOpacity`, … |
| DataStore prefs | `lawnchair/.../preferences2/PreferenceManager2.kt` | `hotseat_mode`, `all_apps_show_scrollbar`, `enable_lawn_deck`, `show_deck_layout`, lock home, labels, gestures, … |
| Home UI | `ui/preferences/destinations/HomeScreenPreferences.kt` | Home settings; optionally shows deck toggle |
| Grid UI | `HomeScreenGridPreferences.kt` | Columns/rows |
| Layout toggle UI | `ui/preferences/components/HomeLayoutPreferences.kt` | Lawn**deck** enable/disable with DB backup |
| Drawer UI | `AppDrawerPreferences.kt` | Columns, opacity, scrollbar, drawer list vs categorized |
| Dock UI | `DockPreferences.kt` | Hotseat mode + columns |
| Navigation | `ui/preferences/navigation/PreferenceRoutes.kt` | `HomeScreen`, `HomeScreenGrid`, … |

### 2.4 Lawnchair override surface (prefer extending here)

| Piece | Path |
|-------|------|
| Activity | `lawnchair/.../LawnchairLauncher.kt` (`QuickstepLauncher` subclass) — touch controllers, gestures, state |
| Layout factory | `LawnchairLayoutFactory.kt` |
| Gestures | `lawnchair/.../gestures/` (`VerticalSwipeTouchController`, `GestureController`, handlers) |
| Views | `lawnchair/.../views/` (scrim, edit strip, overlays) |
| Search / all-apps extras | `lawnchair/.../allapps/` |
| Backup | `lawnchair/.../backup/` |

### 2.5 Lawndeck ≠ hybrid rail (do not confuse)

`lawnchair/.../deck/LawndeckManager.kt` + pref `enable_lawn_deck` is an **experimental categorized-folder dump onto the classic grid**, with DB backup/restore. It is **not** Niagara-style vertical favorites.

**Phase-1 decision:** treat Lawndeck as **orthogonal / later**. Hybrid mode must not depend on `enable_lawn_deck`. Keep `show_deck_layout` gated experimental until product decides overlap.

---

## 3. UX defaults (Velo)

| Surface | Default | Optional |
|---------|---------|----------|
| Home | **Vertical favorites rail** (thumb-zone friendly, single column, labels on) | Classic **grid** via Home settings |
| Dock / hotseat | Keep Lawnchair hotseat available; in rail mode default toward **compact / fewer icons** (product polish) | Disabled / Google QSB modes unchanged |
| App drawer | Alphabetical grid **or** list with **letter fast-scroller visible** (`all_apps_show_scrollbar=true`) | Categorized drawer (`pref_drawerList=false`) remains |
| Widgets | **Not** first-class on rail home in phase 1 | Full widget host when user switches to classic grid |
| Wallpaper / Smartspace | Wallpaper always; Smartspace optional above rail (phase 1.5) | — |

Interaction sketch (rail default):

1. Home shows ordered vertical list of favorites (icon + label).
2. Swipe up / gesture → All Apps with alphabet letter rail.
3. Long-press favorite → reorder / remove / shortcuts (reuse Launcher popup where possible).
4. Settings → Home → **Home layout: Vertical list | Classic grid**.

---

## 4. Proposed component map

### 4.1 New types (prefer `lawnchair/` module)

| New type | Package suggestion | Responsibility |
|----------|--------------------|----------------|
| `HomeLayoutMode` | `app.lawnchair.home` | Enum: `VERTICAL_RAIL`, `CLASSIC_GRID` |
| `FavoritesRailController` | `app.lawnchair.home` | Owns rail visibility, sync from model, mode switch |
| `FavoritesRailView` | `app.lawnchair.home` | Vertical `RecyclerView` (+ item decorations, drag-reorder) |
| `FavoritesRailAdapter` | `app.lawnchair.home` | Binds `WorkspaceItemInfo` / folder rows |
| `FavoritesRailItemAnimator` | `app.lawnchair.home` | Lightweight add/move/remove anims |
| `HomeLayoutPreference` UI | `app.lawnchair.ui.preferences…` | Segmented control under Home settings |
| `RailFavoritesRepository` (optional) | `app.lawnchair.home` | Ordered list read/write; wraps `ModelWriter` / Favorites |

### 4.2 Extend / reuse (avoid forking AOSP cores early)

| Existing | How we use it |
|----------|----------------|
| `LawnchairLauncher` | Mode-aware: hide/show workspace pages vs rail; keep Quickstep overview |
| `Workspace` / `CellLayout` | Remain authoritative for **CLASSIC_GRID**; in rail mode keep inflated but `INVISIBLE`/`GONE` (or zero alpha) so bind path stays warm for toggle |
| `Hotseat` + `HotseatMode` | Unchanged API; rail mode may bias defaults |
| `ModelCallbacks` / `ModelWriter` / Favorites DB | Source of truth for pinned items |
| `AlphabeticalAppsList` + `RecyclerViewFastScroller` + letter list | Drawer alphabet — polish only |
| `LawnchairAlphabeticalAppsList` | Keep hidden-apps / folders |
| `PreferenceManager2` | New `home_layout_mode` key |
| `DeviceProfileOverrides` | Grid specs still apply in classic mode; rail uses its own metrics |
| `AllAppsSwipeController` / `VerticalSwipeTouchController` | Preserve swipe-to-drawer; ensure rail scroll doesn’t steal vertical swipe incorrectly |
| `HomeScreenPreferences` / `HomeLayoutSettings` | Add Velo layout mode; keep Lawndeck behind experimental flag |

### 4.3 Layout integration options

**Recommended (phase 1):** Overlay approach

- Add `FavoritesRailView` as a sibling under `DragLayer` in `lawnchair/res/layout/launcher.xml` (e.g. `@id/favorites_rail`).
- When `VERTICAL_RAIL`: show rail, hide workspace paging chrome / page indicator; optionally shrink or keep hotseat.
- When `CLASSIC_GRID`: hide rail, show workspace as today.

**Avoid early:** Subclassing/replacing `Workspace.java` with a list — high merge conflict cost with upstream Launcher3.

---

## 5. Data model

### 5.1 Conceptual

| Mode | Model |
|------|-------|
| Classic grid | Existing Favorites: `CONTAINER_DESKTOP` + `(screen, cellX, cellY)` occupancy; widgets + folders |
| Vertical rail | **Ordered list** of pin entries (apps, folders, deep shortcuts). Position = list index. No cell occupancy. |

### 5.2 Persistence strategies (decision needed)

**Option A — Reuse Favorites with a convention (recommended for spike)**

- Store rail items as `CONTAINER_DESKTOP`, `screen = RAIL_SCREEN_ID` (dedicated constant, e.g. a high reserved id or `FIRST_SCREEN_ID` with `cellX=0`, `cellY=order`).
- Pros: works with `ModelWriter`, backup/restore, existing loaders.
- Cons: must fence classic grid migration carefully; widgets on rail screen need policy.

**Option B — New container**

- e.g. `CONTAINER_FAVORITES_RAIL = -110` in Favorites (requires schema/provider awareness).
- Pros: clean separation from grid.
- Cons: touches more AOSP model code; migration + backup converters.

**Option C — Side table / DataStore ordered ComponentKeys**

- Pros: simple for apps-only MVP.
- Cons: duplicates Launcher model (badges, shortcuts, folders, uninstall sync); **not recommended** beyond throwaway spike.

**MVP decision (2026-11-05):** **Option C for order only** — DataStore key `favorites_rail_order` stores comma-separated Favorites row ids. Icons / titles / launch / uninstall sync still come from `BgDataModel` + Favorites DB (desktop + hotseat pins). We deliberately **did not** pick Option A for MVP because rewriting `cellY` / moving items onto a reserved screen would corrupt classic-grid coordinates on mode toggle. Option A/B remains the Phase-2 target once the rail owns its own pin set. Folders allowed; **widgets ignored on rail**. See `FavoritesRailOrderStore`.

### 5.3 Migration

| From → To | Behavior |
|-----------|----------|
| Fresh install | Empty rail or seed from default workspace XML / hotseat pins (product choice) |
| Classic → Rail | Flatten: take `CONTAINER_HOTSEAT` left-to-right, then desktop icons **row-major on first screen**, skip widgets; cap list length (e.g. 30) with “rest in drawer” |
| Rail → Classic | Place rail items onto first screen in a single column (or refill hotseat N + first-screen column); leave leftover screens empty; widgets restored from still-present Favorites rows if we never deleted them |
| Lawndeck DB backups | Do **not** auto-convert; if both flags ever coexist, prefer explicit user choice |

Backup/restore: ensure Lawnchair backup path (`lawnchair/.../backup/`) serializes whatever container/screen convention we pick; Nova converter can stay grid-oriented initially.

---

## 6. Preference keys / flags

| Key | Store | Type | Default (Velo) | Notes |
|-----|-------|------|----------------|-------|
| `home_layout_mode` | PreferenceManager2 | `string` (`vertical_rail` / `classic_grid`) | `vertical_rail` | Primary switch |
| `rail_show_labels` | PreferenceManager2 | `boolean` | `true` | Can alias `showIconLabelsOnHomeScreen` initially |
| `rail_max_items` | PreferenceManager2 | `int` | `30` (or unbounded) | Optional |
| `all_apps_show_scrollbar` | PreferenceManager2 (existing) | `boolean` | **force `true` for Velo defaults** | Letter rail visibility |
| `pref_drawerList` | PreferenceManager (existing) | `boolean` | `true` (alpha sections) | Keep |
| `pref_workspaceColumns/Rows` | PreferenceManager (existing) | `int` | unchanged | Classic mode only |
| `enable_lawn_deck` | PreferenceManager2 (existing) | `boolean` | `false` | **Out of hybrid scope** |
| Feature flag (debug) | `FeatureFlags` / experimental prefs | `boolean` | on for debug builds | Gate unfinished rail behind flag if needed |

Reload policy: changing `home_layout_mode` → recreate or targeted show/hide (prefer recreate for spike simplicity via `ReloadHelper.recreate()`).

---

## 7. Phased implementation plan

### Phase 0 — Spike (smallest PR)

- Add `HomeLayoutMode` + `home_layout_mode` pref (default still **classic** for spike safety, or rail behind experimental flag).
- Inflate stub `FavoritesRailView` listing **already-bound** desktop+hotseat apps (read from `BgDataModel` / launcher model callbacks) **without** changing DB schema.
- Toggle in Home settings; verify Quickstep overview + All Apps still work.
- **Exit criteria:** install debug APK, switch modes without crash; no Favorites migration yet.

### Phase 1 — MVP vertical rail

- [x] Persist order (**Option C MVP**: Prefs-backed Favorites ids via `FavoritesRailOrderStore`)
- [x] Long-press drag-reorder (`ItemTouchHelper` on `FavoritesRailView`)
- [ ] Add from All Apps / popup “Add to home”; remove from rail
- [ ] One-handed metrics polish (bottom-weighted padding, large touch targets)
- [x] Hide page indicator while rail is home (workspace kept inflated/invisible)
- [ ] Full migration classic → rail (hotseat + first screen) beyond seed-from-model
- **Exit criteria:** daily-driver usable as Niagara-like home without widgets.
- **Landed on:** `feat/favorites-rail-mvp`

### Phase 2 — Alphabet drawer polish

- Default scrollbar + letter list on; tune letter hit targets; optional single-column drawer density.
- Search remains local (`lawnchair` search stack).
- **Exit criteria:** letter jump feels as good as or better than stock Lawnchair on mid-range devices.

### Phase 3 — Grid toggle parity

- Round-trip migration rail ↔ grid without data loss for apps/folders.
- Widgets only in classic mode; document clearly in UI.
- Hotseat / Smartspace / wallpaper scrolling parity checks.
- **Exit criteria:** Lawnchair users can switch back to classic without restoring a backup.

### Later (explicitly after MVP)

- Smartspace above rail, usage-based suggestions (on-device), foldables, Lawndeck reconciliation, Magisk/QuickSwitch.

---

## 8. Gesture / Quickstep notes & OEM risks

| Topic | Notes |
|-------|-------|
| Quickstep build | Stick to `lawnWithQuickstepGithubDebug` — overview + gesture path lives in Quickstep launcher (`LawnchairLauncher` → `QuickstepLauncher`). |
| Swipe to All Apps | `AllAppsSwipeController` + Lawnchair `VerticalSwipeTouchController` — rail’s vertical `RecyclerView` must **not** consume edge/nav gestures; use nested scroll / gesture exclusion / bottom overscroll carefully. |
| Home → Overview | Keep system gesture contract (`handleGestureContract` in `LawnchairLauncher`); mode switch must not break recents. |
| OEM lag | Samsung / Xiaomi / etc. still throttle third-party gesture quality; hybrid UX cannot fully fix this without root/QuickSwitch. Document as known ceiling. |
| Lawndeck gesture filter | `GestureHandlerOptions.filterGestureHandlerOptions(deckLayoutEnabled)` disables some handlers in deck mode — **do not** reuse that gate for rail; define rail-specific gesture policy separately. |
| Risk mitigation | Early device matrix: Pixel (baseline), one Samsung, one Xiaomi/HyperOS; measure All Apps open latency & gesture miss rate vs stock Lawnchair. |

---

## 9. Open questions / PM decisions

1. **Default for first public build:** rail on by default, or classic default + “Try vertical home” onboarding?
2. **Widgets on rail home:** none (recommended phase 1), single Smartspace row, or free-form later?
3. **Persistence:** MVP = Option C (Prefs ids). Confirm Phase-2 move to Option A or B.
4. **Seed favorites:** empty vs copy hotseat vs copy first-screen icons on first rail enable.
5. **Folders on rail:** yes (recommended) vs apps-only MVP.
6. **Dock in rail mode:** keep / auto-disable / merge dock icons into rail.
7. **Package rename:** keep Kotlin package `app.lawnchair.*` while applicationId is `app.velo.launcher`, or gradually introduce `app.velo.*` for new home code?
8. **Lawndeck:** hide entirely in Velo UI, or leave as advanced experimental mutually exclusive with rail?
9. **Max rail length** and overflow UX.
10. **Tablet / fold:** single rail centered vs dual-pane — defer, but avoid painting into a corner in `DeviceProfile`.

---

## 10. Non-goals (phase 1)

- Replacing or heavily rewriting `Workspace.java` / `CellLayout.java`
- Depending on Lawndeck or Flowerpot categorization for the default home
- Cloud sync, accounts, or any tracker/analytics
- Paywalled icon packs / layout features
- Magisk / QuickSwitch module
- Pixel-perfect Niagara clone (inspiration only; Velo keeps Lawnchair DNA)
- Full widget stacking on the rail
- Renaming entire `app.lawnchair` Java/Kotlin package tree
- Play Store listing / signing pipeline (separate track)

---

## 11. Code spikes

**Phase 0:** Implemented / merged — `HomeLayoutMode` + stub `FavoritesRailView` (PR #1).

**Phase 1 MVP (this branch):** `feat/favorites-rail-mvp` — drag-reorder + `favorites_rail_order` persistence.


**Title:** `feat(home): add HomeLayoutMode pref + stub FavoritesRailView`

**Scope:**

1. `HomeLayoutMode` + `home_layout_mode` in `PreferenceManager2` (default `classic_grid` behind the scenes **or** experimental flag so debug stays safe).
2. Preference row under Home settings.
3. Stub `FavoritesRailView` in `lawnchair/` inflated from `lawnchair/res/layout/launcher.xml`.
4. Populate from currently bound workspace/hotseat apps (no DB migration).
5. Show/hide vs `Workspace` / page indicator based on mode; smoke-test All Apps + Overview.

**Out of scope for that PR:** reorder persistence, migration, drawer polish, Lawndeck changes.

**Build:**  
`JAVA_HOME=/home/box/jdk21 ANDROID_HOME=/home/box/android-sdk ./gradlew assembleLawnWithQuickstepGithubDebug --no-configuration-cache`

---

## 12. References (primary files)

- `src/com/android/launcher3/Workspace.java`
- `src/com/android/launcher3/Hotseat.java`
- `src/com/android/launcher3/WorkspaceLayoutManager.java`
- `src/com/android/launcher3/LauncherSettings.java` (`Favorites`)
- `src/com/android/launcher3/ModelCallbacks.kt`
- `src/com/android/launcher3/allapps/AlphabeticalAppsList.java`
- `src/com/android/launcher3/allapps/ActivityAllAppsContainerView.java`
- `src/com/android/launcher3/allapps/AllAppsRecyclerView.java`
- `src/com/android/launcher3/views/RecyclerViewFastScroller.java`
- `lawnchair/src/app/lawnchair/LawnchairLauncher.kt`
- `lawnchair/src/app/lawnchair/allapps/LawnchairAlphabeticalAppsList.kt`
- `lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt`
- `lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt`
- `lawnchair/src/app/lawnchair/ui/preferences/destinations/HomeScreenPreferences.kt`
- `lawnchair/src/app/lawnchair/ui/preferences/components/HomeLayoutPreferences.kt`
- `lawnchair/src/app/lawnchair/deck/LawndeckManager.kt`
- `lawnchair/res/layout/launcher.xml`
