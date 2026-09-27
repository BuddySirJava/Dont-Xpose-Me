# Security policy

Dont Xpose Me can destroy device encryption keys. Treat reports about
false triggers, arming bypasses, or hook bypasses as high severity.

## Reporting

Open a private security advisory on GitHub once the repository is public,
or contact me via https://github.com/BuddySirJava. Please include:

- Device / ROM / Android version
- Magisk and LSPosed versions
- Whether the module was armed or in test mode
- `adb logcat -s DontXpose` around the event (redact nothing except the PIN)

Do not attach the duress PIN or `config.properties`.

## Scope

In scope: accidental wipe, failure to wipe when armed, status checks that
allow arming when `deleteAllKeys` cannot run, credential-hook bypasses on
AOSP SystemUI.

Out of scope: OEM skins that never call `LockSettingsService.checkCredential`
the way AOSP does; physical extraction after keys are already deleted.
