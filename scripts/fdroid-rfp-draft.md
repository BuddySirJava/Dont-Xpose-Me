# F-Droid RFP / fdroiddata draft (do not paste secrets)

Use this when filing at https://gitlab.com/fdroid/rfp or opening an MR to
https://gitlab.com/fdroid/fdroiddata. Replace SOURCE_URL after you push the
public repo and tag `v1.0.0`.

## Checklist before opening

- [ ] Public git repo with full source (not placeholders)
- [ ] `LICENSE` present (Apache-2.0)
- [ ] Fastlane metadata under `fastlane/metadata/android/en-US/`
- [ ] Tag `v1.0.0` on the commit that has `versionName 1.0.0` / `versionCode 1`
- [ ] You are the author (or author agreed to inclusion)
- [ ] `./gradlew assembleRelease` produces an unsigned APK without proprietary deps

## RFP issue body

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy/).
* [x] The app is not already listed in the repo or issue tracker.
* [x] The app has not already been requested.
* [x] The upstream app source code repo contains Fastlane metadata.
* [x] The original app author has been notified, and does not oppose the inclusion (I am the author).
* [ ] Optionally donated to support maintenance.

#### APPLICATION ID: `me.dontxpose`

```yaml
Categories:
 - Security

License: Apache-2.0

AuthorName: Mahyar Darvishi
AuthorWebSite: https://github.com/BuddySirJava

SourceCode: SOURCE_URL
IssueTracker: SOURCE_URL/issues

AutoName: Dont Xpose Me

RepoType: git
Repo: SOURCE_URL

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1

AntiFeatures:
 - RequiresRoot
```

Why do you want this app added to F-Droid:

> FOSS Magisk/LSPosed duress PIN module. Users who cannot use Play Store sideloading should be able to install a root-required security tool from a trusted repo. Anti-Feature RequiresRoot is declared.

Summary:

> Duress PIN for LSPosed: wipe KeyMint keys and shut down

Description:

> (same content as fastlane/metadata/android/en-US/full_description.txt)

## Preferred path

F-Droid maintainers prefer a merge request to **fdroiddata** over RFP alone when you can supply working metadata. After the public tag exists, open:

https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md
