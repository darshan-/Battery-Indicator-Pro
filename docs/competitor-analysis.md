# Competitor feature analysis: AccuBattery and Battery One

Analysed 2026-10-20 from the APKs installed on the vivo V2435 (`adb pull`, then `aapt2 dump badging / xmltree / resources`).
Sources are each app's manifest (permissions, components) and its English string table, which names every screen, setting and help text. No code was decompiled or copied.

| | AccuBattery | Battery One |
|---|---|---|
| Package / version | `com.digibites.accubattery` 2.1.8 (201008) | `com.oneapps.batteryone` 2.6.7 (488) |
| min / target SDK | 24 / 35 | 25 / 36 |
| UI stack | Views + Material; tabs Charging / Discharging / Health / History | Jetpack Compose + Glance widgets; tabs Charge / Discharge / Health / History + Settings |
| Monetisation | Ads (AppLovin, AdMob, Meta) after a grace period; one-time "buy us a treat" Pro | Ads; subscription (monthly / annual) or lifetime "Advanced Access"; watch a video to remove ads for 24 h |
| Notable permissions | `DUMP`, `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `INTERNET`, ad IDs | `DUMP`, `BATTERY_STATS`, `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, `ACCESS_NOTIFICATION_POLICY`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, Shizuku API, `INTERNET`, ad IDs |

## 1. Feature inventory

### Shared by both apps (table stakes for this category)

- **Measured health:** estimated capacity in mAh from charge sessions, shown against an auto-detected (editable) design capacity, as a health %.
- **Calibration / diagnostics on first run:** unplug the charger, detect current sign (polarity) and unit (mA vs µA), and flag devices without a fuel gauge.
- **Dual-cell fixes:** "current is reported halved" correction and "voltage is reported doubled (>7 V)" correction.
- **Charge alarm** at a chosen % ("unplug now"), with its own DND window, sound and vibration.
- **Wear in cycles:** "charging from X to Y costs Z wear cycles", charted per day; education on charging to 80%.
- **Separate screen-on / screen-off / combined** drain rates and remaining-time estimates, plus "full battery lasts…" estimates.
- **Deep sleep vs awake** split of screen-off time.
- **Per-app power usage** (needs Usage Access): mAh attributed to the foreground app.
- **Session history:** every charge / discharge session saved, with duration, % range, mAh, screen time, average current.
- **Persistent notification** with live "Now / Average" current, %/h, and mAh; configurable priority.
- **Battery-optimisation / "don't kill my app" onboarding** linking to dontkillmyapp.com, and detection of gaps in recorded data.
- **Floating overlay** over other apps (`SYSTEM_ALERT_WINDOW`).
- **Themes:** Light / Dark / AMOLED.
- **Temperature unit** choice.

### AccuBattery only

- **Charge efficiency score:** % charged divided by wear cycles, explaining why partial top-ups near 100% cost more.
- **Health trend chart:** 5-session moving average plus a linear-regression trendline, "capacity loss per year" and "predicted health on date"; tap a point to open that session.
- **Excluded sessions:** sessions too short to be valid are shown with the reason they were excluded.
- **"Last full charge N days ago"** reminder to recalibrate the OS percentage by doing a full charge monthly.
- **"Test your charger"** guidance: compare screen-off charge speed between chargers and cables.
- **Task-killer detection:** "a session of X was split into N segments" warning.
- **Android-upgrade recalibration** prompt.
- **Exportable notification sounds** for the system sound picker.

### Battery One only

- **Two independent capacity methods with a confidence label:**
  - *From charging:* each charge is one measurement; a pool of up to 40 recent charges votes on the value, weighted, with older ones aged out. It shows the pool and notes the extra mAh that goes in after the indicator reads 100%.
  - *From discharging:* the screen-on mAh cost of each 1% step is mapped across 0–100%. A "map %" fill shows coverage, and the value appears after 40 levels are measured.
  - Also a "singular" method (one full charge from under 15% to 100%) and a "cumulative" method (from several partial charges of at least 15%).
- **Charging-pause detection:** if charging stops (adaptive or limit charging), the session is trimmed at that point.
- **Wear prediction:** a slider picks a target health % and the app forecasts the date from the measured wear rate.
- **Wakelock inspector** (via Shizuku): per-app wakelocks split by tag, kernel vs userspace, count, max hold and background time, with an option to reset on unplug.
- **Full-screen alarm-style alerts** (lock screen, repeat, "stop / stop no repeat / disable" buttons).
- **Alert set:** charge-level reached, full charge, low battery, overheating (above normal and critical), plugged-in and unplugged notifications.
- **Graphs:** % over 2 / 4 / 8 / 16 / 24 / 48 h, temperature, daily screen-on time with gaps for missing data, and a per-session amperage curve with an optional temperature overlay.
- **History summary:** total sessions, full cycles, Ah throughput, and wear "since" the first record. Filters for charges, discharges, or only sessions with measured capacity; a per-session "use in capacity calculation" toggle.
- **Manual session reset**, for running your own tests (premium).
- **Database export / import / clear**, for backup after a reinstall or factory reset (premium).
- **Edit UI:** reorder and hide blocks on the main screens (premium).
- **Status-bar icon modes:** %, % in a circle, temperature, temperature in a circle; live voltage, amperage or temperature in the status bar (premium).
- **Highly customisable widgets:** level and temperature widgets with ring or line colours, widths, font, corner rounding and padding, a different style when charging, hot or low, and a choice of tap action.
- **Overlay customisation:** power, amperage, voltage or temperature; snap to edge; lock position.
- **Show system cycle count** ("Charge cycles (system data)"), reading `EXTRA_CYCLE_COUNT` the same way we do.
- **App behaviour options:** close on minimise, auto-close after 5 minutes, exclude from Recents, and a HyperOS temperature-freeze warning.

## 2. Where BatteryBot Pro stands

| Feature | AccuBattery | Battery One | BatteryBot Pro now |
|---|---|---|---|
| Live %, temp, voltage, current | ✅ | ✅ | ✅ |
| Persistent notification, configurable lines | ✅ | ✅ | ✅ (strongest: two configurable lines and icon sets) |
| Charge / discharge time prediction | ✅ (screen on / off) | ✅ (on / off / combined) | ✅ one predictor; usage card splits screen on / off mAh |
| Alarms: full, level up / down, temp up / down, health | partial (charge alarm only) | ✅ + full-screen | ✅ more types, notification only |
| Capacity / health estimate | ✅ | ✅✅ (two methods + confidence) | 🔄 single charge-session estimate |
| Cycle count (system) | – | ✅ | ✅ |
| Wear-in-cycles / efficiency | ✅ | ✅ | ❌ |
| Deep sleep vs awake | ✅ | ✅ | ❌ |
| Per-app usage | ✅ | ✅ | ❌ (pending) |
| Session history (charge / discharge sessions) | ✅ | ✅ | ⚠ status-change log rows, not sessions |
| Graphs | health trend, wear | %, temp, screen time, per-session current | 24 h level chart |
| Calibration (polarity, unit) | ✅ | ✅ | ⚠ implicit (`UsageTracker` unit heuristic; vivo sign shown as magnitude) |
| Dual-cell correction | ✅ | ✅ | ❌ (`current_hack_multiplier` exists, not surfaced as such) |
| Backup / export | – | DB export / import | CSV export |
| Widgets | – | ✅ highly customisable | ✅ circle + full |
| Overlay | ✅ | ✅ | ❌ |
| Themes | Light / Dark / AMOLED | 4 themes + inverted + accent | follows system day / night |
| Ads / network | ads, Internet | ads, Internet, subscription | **none, no Internet permission, GPLv3** |

The clear differentiator is the last row. Both competitors bundle 3 or more ad SDKs, ad-ID permissions and paywalls. BatteryBot Pro can offer their measurement features with no network access and nothing locked.

## 3. Recommended features to add (ranked by value / effort)

1. **Charge / discharge sessions.** Group log rows into sessions (plug to unplug) with start/end %, duration, mAh in/out, average current, screen-on time and max temperature. Almost everything else on this list builds on it.
   *Where:* new `sessions` table in `LogDatabase` (DB v6, handled in `onUpgrade`); the service closes a session on plug-state change in `BatteryInfoService.update`.
2. **Capacity estimate with a confidence label and history.** Keep a per-session capacity measurement and show a weighted median of the last N sessions (Battery One's pool idea) with Low / Medium / High confidence by number of sessions and % covered. Exclude charges under 15–20% and show why. Detect charging pauses (current near zero while plugged, or a charge-limit level) and stop counting there.
   *Where:* `UsageTracker` (already samples the charge counter) and the details dialog in `CurrentInfoFragment`.
3. **Wear in cycles + charge-alarm education.** Use AccuBattery's model, where cycles of wear grow with the top level. Show "Charging 20→100% costs X cycles, 20→80% costs Y". A cheap add-on to the existing "fully charged"/"charge rises" alarms: a one-tap "Unplug at 80%" preset.
4. **Deep sleep vs awake while screen off.** Compare `SystemClock.elapsedRealtime()` and `uptimeMillis()` deltas across screen-off periods; no extra permission needed. It fits the existing usage card next to the screen-on/off bar.
5. **Screen-on / screen-off / combined remaining time.** We already split mAh by screen state. Divide the remaining mAh by each rate to give three estimates, shown as a small row under the main time-remaining figure.
6. **First-run calibration step.** Ask the user to unplug, then detect current sign and unit automatically. This would replace the vivo "negative while charging" special case and the `readChargeCounter` unit heuristic with a stored, user-visible result. Include a dual-cell toggle (×2 current, ÷2 voltage).
7. **More graphs from data we already log:** temperature over time and the per-session current curve (with an optional temperature overlay), plus 2 / 8 / 24 / 48 h ranges on `LevelChartView`.
8. **Health trend.** Plot the session capacity estimates over time with a moving average and a linear trend; "capacity loss per year" and "health on date" follow directly from it.
9. **Backup / restore of the databases** using the Storage Access Framework, with no permissions needed, to complement CSV export.
10. **Background-reliability helper.** Detect gaps in the log (service killed) and point to the battery-optimisation setting and dontkillmyapp.com. We already have the foreground service; this addresses the main cause of "missing data" complaints in both competitors.

### Worth considering later

- **Per-app usage** (Usage Access): attribute mAh to the foreground app. It is already a Pending Item. Both competitors have it, so it's expected, but it adds a sensitive permission. Make it opt-in, as AccuBattery does.
- **Full-screen / assertive alarms.** Already in the maintainer's `notes`. Battery One shows the UX: lock-screen display, repeat, and three stop buttons.
- **Low-battery and overheating presets** within the existing alarm types, plus plugged/unplugged notifications if they're not already covered.
- **AMOLED (true black) theme option.** Already pending as "light/dark theme option".
- **Widget customisation** (colours, ring width, style on charge or temperature).

### Not recommended

- **Wakelock inspector:** needs Shizuku or `DUMP`/`BATTERY_STATS` and setup through adb. That's a niche feature with a high support cost.
- **Overlay over other apps:** it needs `SYSTEM_ALERT_WINDOW` and adds little beyond the notification and status-bar icon.
- **Ads, subscriptions, sign-in, remote config:** they conflict with the no-Internet, GPLv3 positioning, which is BatteryBot Pro's main advantage.
