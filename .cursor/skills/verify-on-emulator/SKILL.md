---
name: verify-on-emulator
description: >
  Build, install, and visually verify a Kotlin Android app on an emulator or
  a device, then fix what the screenshot shows. Use after a UI change, when
  the user asks to run the app, check a screen, or confirm edge-to-edge, dark
  mode, and loading, empty, and error states.
metadata:
  author: android-ai-starter-kit
  version: "1.0"
---

> **Nota (PL):** Skill po angielsku, bo agenty czytają go pewniej. Użyj go po zmianie UI. Kompilacja nie jest weryfikacją. Złóż aplikację, zainstaluj, zrób zrzut i oceń go. Gdy nie ma emulatora — napisz to wprost.

# Verify on an emulator

A UI change is finished when a screenshot of the running app has been looked at, or when you report why that was impossible. `assembleDebug` alone is not verification.

Prefer the Mobile MCP server (`mobile-mcp`) when it is connected. Fall back to `adb`. Do not invent a device.

## 1. Find a target

- MCP: `mobile_list_available_devices`.
- Shell: `adb devices`.

Use an emulator or a device that is already authorized. Do not wipe data, do not uninstall other apps, and do not factory-reset anything. If the list is empty, boot an AVD only when the Android SDK is installed and an AVD already exists (`emulator -list-avds`). If none exists, stop and say what the user needs to start. Do not download system images as a surprise.

Write screenshots under `screenshots/` (gitignored). Name them `<screen>-<theme>-<n>.png`.

## 2. Build and install

Detect the application module from `settings.gradle.kts` (the module that applies the Android application plugin). Don't assume `:app` when the name differs. If several application modules exist, use the one the user named.

```bash
./gradlew :app:assembleDebug
```

Install the debug APK that command produced (path is under that module's `build/outputs/apk/debug/`).

- MCP: `mobile_install_app`, then `mobile_launch_app` with the `applicationId` from `defaultConfig`.
- Shell:

```bash
adb install -r path/to/app-debug.apk
adb shell monkey -p <applicationId> -c android.intent.category.LAUNCHER 1
```

`monkey` only launches. Navigate to the screen with MCP taps (`mobile_list_elements_on_screen`, `mobile_click_on_screen_at_coordinates`) or `adb` input when you know the coordinates. Don't tap through a login with real credentials. If the screen is behind auth you cannot pass, screenshot the gate and say the target screen was not reached.

## 3. Capture

- MCP: `mobile_take_screenshot` and, when you need a file, `mobile_save_screenshot`.
- Shell: `adb exec-out screencap -p > screenshots/<screen>-light-1.png`

Dark theme, when the task includes it:

```bash
adb shell cmd uimode night yes
```

Take another screenshot. Restore `adb shell cmd uimode night no` if you changed the emulator.

On a failure, pull the relevant log, not the whole buffer: `adb logcat -d -t 200` or `mobile_get_device_logs`. Read the crash before changing code.

## 4. Judge the image

Look for these, in this order:

1. The screen is the one you meant to open, not a blank activity or a crash dialog.
2. Status bar and navigation bar do not cover a title, a button, or the last list row. Open `edge-to-edge` if they do.
3. Text is not clipped, including a longer Polish string. Buttons meet about 48.dp.
4. Loading, empty, and error are reachable or explicitly unreachable with a reason.
5. Dark theme: text and icons stay legible. System bar icons are visible (`isAppearanceLightStatusBars` follows the theme).
6. The layout matches the Figma or Mobbin reference in structure. Pixel-perfect imitation of another brand is not the goal.

## 5. Fix and repeat

Fix the code, reinstall, and capture again. Three visual passes is the limit. After the third, report the remaining gap and the screenshot it shows. Do not keep tweaking padding by a few dp past that.

## Done

State all of the following:

- device or emulator name from `adb` / MCP
- command or tool that installed the app
- path of each screenshot
- what you checked and what failed
- what you could not reach (no device, login wall, missing state)

Never claim "looks good" without a screenshot you actually inspected.
