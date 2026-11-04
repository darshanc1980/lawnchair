# Velo Privacy Inventory

**Updated:** 2026-11-05 Asia/Calcutta  
**Build path reviewed:** `lawnWithQuickstepGithubDebug` / free github channel

## Goal

Zero trackers in the free / github debug path. Open-source; no ads; no crash-reporting SDKs.

## Findings (this session)

| Area | Status | Notes |
|------|--------|-------|
| Firebase / Crashlytics / Sentry / Bugsnag / Amplitude / Mixpanel / AppCenter | **Absent** | Not in `gradle/libs.versions.toml` or lawnchair module deps |
| Ads / mediation SDKs | **Absent** | — |
| Play integrity / ads IDs | **Absent** from launcher github path | — |
| Android `StatsLogManager` / framework stats | Present (AOSP) | System logging hooks only; not a third-party tracker |
| Lawnchair **Live Information** (`lawnchair.app/live-information.json`) | **Disabled by default in Velo** | Was `config_default_live_information_enabled=true` upstream; Velo sets **false**. Optional announcements fetch when user re-enables. |
| Web suggestion provider default | Config may still name `google` for dock/search suggestions | Local app search remains on-device; web suggestions are user-configurable providers. Review before Play release. |
| Icon pack / themed icons | Local | No network |
| Backup / restore | Local zip via SAF | Includes DataStore (`preferences.preferences_pb`) → `home_layout_mode`, `favorites_rail_order`, membership flags travel with layout backups |

## Backup coverage for hybrid prefs

`LawnchairBackup.getFiles()` already packs:

- `launcher.db`
- shared_prefs XML
- Room `preferences` DB
- DataStore `preferences.preferences_pb`

So **`home_layout_mode`**, **`favorites_rail_order`**, **`favorites_rail_membership_initialized`**, and **`favorites_rail_pending_keys`** are included whenever the user backs up “layout and settings”. No extra backup hooks required for MVP.

## Recommendations

1. Keep Live Information **off** by default (done).
2. Before any store release: audit search provider network calls and document which are optional.
3. Do not add crash reporters without an explicit, opt-in privacy toggle.
4. Prefer F-Droid / github APKs without proprietary GMS-only analytics.
