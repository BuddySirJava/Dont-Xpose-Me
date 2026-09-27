<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="80" alt="Dont Xpose Me Icon" />
</p>

<h1 align="center">Dont Xpose Me</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Android-12%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 12+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square" alt="License" />
</p>

<p align="center">
  Personal <strong>duress PIN</strong> for Magisk + LSPosed on AOSP-like lock screens.
</p>

<p align="center">
  <a href="#overview">Overview</a> &bull;
  <a href="#features">Features</a> &bull;
  <a href="#requirements">Requirements</a> &bull;
  <a href="#install">Install</a> &bull;
  <a href="#build">Build</a> &bull;
  <a href="#how-it-works">How it works</a> &bull;
  <a href="#safety">Safety</a> &bull;
  <a href="#releases">Releases</a>
</p>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="280" alt="Setup screen showing status checks, the duress PIN fields, and test mode" />
</p> 

> [!WARNING]
> Armed mode is irreversible. A matching PIN calls `AndroidKeyStoreMaintenance.deleteAllKeys()` (KeyMint — File-Based Encryption wrapping keys are destroyed), then attempts a recovery userdata wipe, then shuts down if still running. There is no file shred. Stay in **test mode** until a match shows under **Test attempts** in the app.



## Overview

Dont Xpose Me adds a second PIN beside the real lock PIN. The real PIN still unlocks the phone. Once the module is armed, the duress PIN destroys encryption keys, requests a recovery userdata wipe, and powers off if needed.

The wipe is: `deleteAllKeys()` (one retry on failure), then `RecoverySystem.rebootWipeUserData` (or a recovery `--wipe_data` command), then shutdown. v1 matches numeric PINs only. Patterns and passwords are ignored.[M](https://github.com/BuddySirJava)

## Features

- **Test mode by default.** A matching PIN is recorded under Test attempts in the app and the wipe is suppressed until you explicitly arm.
- **Fail-closed arming.** The app refuses to arm unless Magisk `su` works, the LSPosed hook has a recent heartbeat, a scope is present, `deleteAllKeys` exists, and a 4-digit-or-longer PIN is set.
- **Salted hash only.** The PIN is stored as SHA-256 over a random salt followed by the PIN, under `/data/system/dontxpose`. The plaintext PIN is never written.
- **Two hook points.** `LockSettingsService.checkCredential` / `verifyCredential` in `system_server`, plus a `LockPatternUtils.checkCredential` fallback in SystemUI.
- **Confirm before arm or disarm.** Changing the armed state asks for a biometric or the real device credential.



## Requirements

- Magisk (root)
- LSPosed (Zygisk), Xposed API 93+
- An AOSP-like SystemUI (Pixel, Lineage, and similar). OEM skins that never call `LockSettingsService.checkCredential` the way AOSP does will not trigger the hook.
- Android 12+ (`minSdk` 31)

To build from source: JDK 17 or 21 (a full JDK, not a JRE) and an Android SDK with `ANDROID_HOME` set.

## Install

1. Build the release APK, or install a [release](#releases) build. See [Build](#build).
2. In LSPosed, enable **Dont Xpose Me** and set the scope to `android` (`system_server`) and `com.android.systemui`.
3. Soft reboot, or reboot.
4. Open the app, grant Magisk `su`, and set a duress PIN of at least 4 digits. It must not be your real lock PIN.
5. Confirm the status card: hook heartbeat is fresh and `deleteAllKeys` is available.
6. Stay in **test mode**. Enter the duress PIN at the lock screen, open the app, and refresh. A match should appear under **Test attempts** (wipe suppressed). The PIN itself is never stored in that list.
7. **Arm** only when those checks pass. Armed means the next match destroys keys and shuts down.

> [!IMPORTANT]
> If lock-screen auto-confirm is on and your real PIN is shorter than the duress PIN, the lock screen can submit before you finish typing. Disable auto-confirm, or keep the duress PIN no longer than the real one. Saving a PIN always asks you to acknowledge this.

Disarming turns test mode back on. The only way to leave test mode is to arm.

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64   # 17 or 21
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

The unsigned release APK (what F-Droid builds) is:

`app/build/outputs/apk/release/app-release-unsigned.apk`

To sign locally, set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. The same names work as Gradle properties or environment variables.

The Xposed API is a compile-only stub in `:xposed-api`. It is not packaged in the APK; LSPosed provides the implementation at runtime.

Gradle resolves dependencies from Google Maven and Maven Central first (for F-Droid / CI). An [Aliyun mirror](https://maven.aliyun.com/repository/google) is listed afterward when `dl.google.com` is unreachable.

## How it works


| Piece       | Where               | What it does                                                                               |
| ----------- | ------------------- | ------------------------------------------------------------------------------------------ |
| Setup UI    | `:app`              | Compose screen. Writes config with Magisk `su`.                                            |
| Hook        | `me.dontxpose.hook` | Loaded by LSPosed into `android` and SystemUI. Compares the credential to the stored hash. |
| Wipe        | `me.dontxpose.wipe` | `deleteAllKeys()` (retry once), recovery userdata wipe, then shutdown / `reboot -p`.       |
| Xposed stub | `:xposed-api`       | Compile-only. Not shipped in the APK.                                                      |


Config and hook status live in `/data/system/dontxpose/` (owner `system`). Config is mode `644` (salted hash only — readable by SystemUI). Hook status is mode `666` so both `system_server` and SystemUI can update scope/heartbeat. The app writes config as root. A hook heartbeat older than 7 days is treated as not loaded, so arming stays closed.

## Safety

- Verify an armed wipe on a **spare device** before relying on it. Once keys are gone, the data is gone.
- Do not reuse your real lock PIN.
- Arming is refused when the hook is missing or `deleteAllKeys` cannot be resolved.
- OEM / non-AOSP lock screens may never call `LockSettingsService` the way this module expects — the hook will not fire.
- This is not GrapheneOS Weaver / secure-element duress. Magisk and LSPosed sit in the trusted computing base and weaken the locked-device story versus a stock hardened ROM.

Report accidental wipes, arming bypasses, or hook bypasses privately. See [SECURITY.md](SECURITY.md). Do not include the duress PIN or `config.properties`.

## Releases

Current version is `1.0.0` (`versionCode` 1) in `app/build.gradle.kts`.

For F-Droid and GitHub Releases, tag a commit with the same `versionName`, for example `v1.0.0`. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters max) for that `versionCode`. Store listing files use the Fastlane layout under `fastlane/metadata/android/en-US/`; Fastlane itself is not required.

F-Droid packaging notes:

- Application ID: `me.dontxpose`
- License: Apache-2.0
- Anti-Feature: **RequiresRoot** (Magisk + LSPosed)
- Categories: Security
- Build: `./gradlew assembleRelease` → unsigned APK at `app/build/outputs/apk/release/`
- FOSS-only dependencies (Xposed API is compile-only stubs; LSPosed provides the runtime)

GitHub Actions:

- `[.github/workflows/ci.yml](.github/workflows/ci.yml)` — wrapper check, lint, unit tests, unsigned release APK, Fastlane metadata check
- `[.github/workflows/release.yml](.github/workflows/release.yml)` — on `v*` tags, attach the APK to a GitHub Release. Optional signing uses repository secrets `RELEASE_STORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`

