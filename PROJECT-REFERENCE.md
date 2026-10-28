# Rubi-Scan OBDII — project reference

Written 2026-10-26 so a later session can pick this project up without relearning it. The user-facing feature list is in `README.md`. This file is the working notes: where things live, what must not change, and what was still local on this date.

## What it is

Android OBD-II scanner for a paired Bluetooth Classic adapter (OBDLink or another ELM327-style adapter). Package `com.example.obdscanner`. App label "Rubi-Scan OBDII". One Jetpack Compose activity. It reads SAE J1979 data and stores vehicle profiles on the phone.

The app stays generic until a VIN identifies the manufacturer, or the user names the vehicle. It does not assume a Jeep, a Mustang, or any other model.

## Where it lives

- Project: `C:\Users\aecii\Desktop\obd2\ObdLinkScanner-starter\ObdLinkScanner`
- GitHub: `https://github.com/aecii/Rubi-Scan-OBD2` (public, branch `main`, login `aecii`)
- Release: `https://github.com/aecii/Rubi-Scan-OBD2/releases/tag/v0.3.0` tagged at `7aae561`. Phones still update from the kindfox `version.json`, not from that release.
- Download page: `https://aecii.kindfox.org/OBDII/`
- APK: `https://aecii.kindfox.org/OBDII/RubiScan-OBDII.apk`
- Version file: `https://aecii.kindfox.org/OBDII/version.json`
- Server files: `/var/www/html/OBDII/` on `aecii.kindfox.org` (SSH `aecii@aecii.kindfox.org`). That folder is writable by `aecii`. `/var/www/html` itself is not.
- Page source in the repo: `web/index.html`
- License: Apache License 2.0. The text is `LICENSE`. Copyright 2026 aecii_3d. Source files carry the Apache header.

Version on `main` when this note was written: 0.3.0, version code 3. The `v0.3.0` tag is `7aae561`. The About link and download-page footer came after that tag and did not bump the version.

## Version and updates

`app/build.gradle.kts` and `app/version.json` are version code **3**, version name **0.3.0**.

On startup the app reads `version.json`. It shows "Update available." only when the server `versionCode` is strictly higher than the installed app. The dialog offers the download link. It does not install the APK itself. There is no `REQUEST_INSTALL_PACKAGES` and no `AutoUpdateManager`. Do not restore those.

`assembleDebug` is `finalizedBy("publishDebugUpdate")`. A debug build rewrites `version.json` from Gradle (it keeps the existing `releaseNotes` text) and uploads the APK and `version.json`. Bump `versionCode` only when the user wants phones to be notified. Rebuilding at the same code publishes a new APK but does not prompt.

Phones off the home network often cannot reach `aecii.kindfox.org` on port 443. The check fails closed and shows no prompt. Forwarding public TCP 443 is a router task and was still undone.

## Build

JDK 17 or 21. On this PC, Gradle 8.10.2 cannot parse the Android Studio Java 25 runtime. Use:

```
JAVA_HOME=C:\Users\aecii\.jdks\graalvm-jdk-21.0.7
```

Android SDK: `C:\Users\aecii\AppData\Local\Android\Sdk`. Compile and target SDK 35. minSdk 26. Root Android Gradle Plugin is 8.7.3. Do not pin a newer plugin in the app module.

```
cd C:\Users\aecii\Desktop\obd2\ObdLinkScanner-starter\ObdLinkScanner
gradlew.bat :app:assembleDebug --offline
```

The debug APK is `app\build\outputs\apk\debug\ObdLinkScanner-debug.apk`. The archive name is `ObdLinkScanner` on purpose.

`gh` is not on `PATH`. The full path used before is:

`C:\Users\aecii\AppData\Local\Microsoft\WinGet\Packages\GitHub.cli_Microsoft.Winget.Source_8wekyb3d8bbwe\bin\gh.exe`

PowerShell 5.1 has no `&&`. Git commands must `Set-Location` into the project first. Do not change git config. Commit messages on this machine were written to a temp file and passed with `git commit -F`.

## How the scanner works

Bluetooth Classic SPP only, already paired. ELM/STN init, then `ATSP0`. If `0100` fails, the fallback is `ATSP6` (ISO 15765-4 CAN, 11-bit, 500k). That fallback is for most 2008+ cars. It is not Jeep-specific.

One OBD command at a time. Modes in use: 01 live data, 02 freeze frame, 03/07/0A trouble codes, 04 clear codes, 09 VIN, calibration ID, and CVN. Values are stored in metric. The screen converts them.

Tabs: 0 Dashboard, 1 Sensors, 2 Codes, 3 Track, 4 Settings. The Demo chip is in the top bar.

## Vehicle identity

Manufacturer comes only from the VIN world-manufacturer code, in `VinDecoder`. Model and engine stay blank unless the user typed them. Shared Chrysler-group codes such as `1C4` are not labeled Jeep. `1J4` / `1J8` can be Jeep. Unknown GM `1G` is "General Motors". Ford is named from a `1F` / `2F` / `3F` prefix with no Mustang or EcoBoost guess.

Display name is "year make", or "year vehicle", or "Generic OBD2 Vehicle". A saved profile matched by VIN is kept. A weaker decode does not overwrite it. If the engine computer returns no VIN, the session stays generic until the user picks a saved vehicle.

Shop notes in `DtcDatabase` run only for an exact non-generic make. Pentastar text needs make Jeep and an engine string containing Pentastar or 3.6. The EcoBoost sentence needs make Ford and an engine string containing EcoBoost. ESIM notes are for Jeep, Chrysler, Dodge, or RAM. Demo Mode looks codes up with no profile, so those notes stay generic.

## Demo Mode

No adapter and no car. Each time it is turned on it rolls a new 17-character VIN and one to four stored trouble codes, plus a freeze frame for the first code. Gauges, the O2 waveform, and fuel trim keep moving. Turning it off and on rolls a new set. Reading codes again while it stays on keeps the same set. While it is on, the Demo chip and the status line are yellow with black text.

## Profiles, units, and the fault banner

Profiles store name, VIN, year, make, model, and engine. Plate number and phone number were removed. More than one vehicle is allowed.

The app does not ask to save during the session. On leaving, if the detected vehicle is not saved, it asks. Save stores it and closes. Not now closes and will ask next time. Never ask again is remembered. The user can still add a vehicle in Settings.

Speed (km/h or mph) and temperature (°C or °F) are chosen separately. psi, miles, gal/h, and lb-ft follow the speed choice. The choice is in memory only, same as the theme. The acceleration timer is 0–60 mph or 0–100 km/h to match.

The orange "FAULT CODE DETECTED" banner opens the Codes tab. The "check engine light is off" banner does not.

Settings → About has a Website link to `https://aecii.kindfox.org/OBDII/` and the direct APK link. A debug APK with that link was uploaded as version code 3, so installed copies are not prompted.

## Website

`web/index.html` is the download page. It has `noindex, nofollow`. The site root `robots.txt` disallows `/OBDII`. Do not use a robots.txt `noindex` line.

Footer links, as updated on the live page on 2026-10-26:

- `https://aecii.kindfox.org/fsm_mustang/` labeled "2007 Ford Mustang FSM s197"
- `https://aecii.kindfox.org/fsm_wrangler/` labeled "2015 Jeep Wrangler FSM"

Those manuals are the owner's reference only. They are not a permanent part of the product. Do not link the app to them, do not attach them to vehicle profiles, and do not copy chart images into the app. The footer in `web/index.html` matches the live page.

## Do not do unless asked

- Do not bump `versionCode` except to make phones see an update.
- Do not assume the connected car is a Jeep or a Mustang.
- Do not restore in-app APK install or the old auto-update manager.
- Do not treat the hosted service manuals as an app feature.
- An emulator was tried and abandoned. Bluetooth Classic does not work there, and the emulator's Bluetooth support dropped the PC's mouse. Do not set the emulator up again unless asked.
- Ideas that were discussed and not built: readiness monitors, scan history, more live graphs, remembering the theme and last adapter, NHTSA VIN lookup, Mode 06, recorded video, and a quarter-mile timer. P / B / C / U labels, and a Ford-only P1000 "monitors not complete" note, were suggested and not built.

## After the v0.3.0 tag

- `AdapterInfoScreen.kt` — Website link in About. Already in the uploaded debug APK, still version code 3.
- `web/index.html` — footer labels above. Already copied to `/var/www/html/OBDII/index.html`.
