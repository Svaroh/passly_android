# Passly Android Notes

This checkout now uses the `Svaroh/passly_android` fork as `origin`. The
previous `66Ton99/mobile-passbolt-android` remote is `legacy-66ton99`. The
primary local and GitHub default branch is `main`.

On `2026-06-02`, the former local `rebrand/passly` branch was renamed to
`main`, pushed to `origin/main`, and `Svaroh/passly_android` default branch was
set to `main`. The migrated `main` head is `0d5ecbb3f`
(`Replace Android branding with Passly Team Shield`). Old `master` and
`rebrand/passly` refs may still exist for history; use `main` for new local and
remote work unless the user explicitly asks for a legacy branch.

Also on `2026-06-02`, the Android commit after `347410833` was rewritten to fix
author/committer metadata from `45160296+66Ton99@users.noreply.github.com` to
`github@66ton99.org.ua`. The corrected commit is `0d5ecbb3f`; both
`origin/main` and legacy `origin/rebrand/passly` were force-updated to the
corrected history.

## Architecture & Modules

- Stack: Kotlin / Android Gradle multi-module project.
- Main module groups:
  - `app`: Application entry point, dependency injection, and activity navigation.
  - `core/*`: Common utilities, cryptographic helpers, and `core/sync` synchronization engine.
  - `features/*`: Feature-specific UI flows and Jetpack Compose / View binding components.
  - `domain/*`: Domain models, use cases, and business logic.
  - `services/*`: Background services and autofill provider.
- Key capabilities:
  - Room + SQLCipher local encrypted replica.
  - Android Keystore hardware-backed cryptographic key protection.
  - Non-blocking offline vault unlock.
  - Background synchronization (`core/sync`).

## Versioning

- For Android app changes or installable Android builds, always bump the app
  version first in `gradle/versions.gradle`.
- Increment the patch component of `projectVersionName` by `1`.
- Do not routinely increment `projectVersionCode`; it is Android's internal
  install/build ordering code and is not shown in the normal app UI. Change it
  only when the user explicitly asks for a build-code bump or when preparing a
  store/release build that requires a higher internal code.

## File Headers

- For new files that are fully Passly/Svaroh-authored, use the Passly/Svaroh
  copyright header, including `@link https://passly.svaroh.net Passly`. Do not
  copy the Passbolt SA copyright header into new Passly-owned files.
- Keep Passbolt SA headers only in files that are derived from or modified from
  upstream Passbolt code.

## Builds And Installs

- Use project-local Gradle commands from `passly_android/`.
- Common commands:

```bash
./gradlew assembleDebug
./gradlew detekt ktlint lintDebug unitTest licenseeRelease dependencyUpdates buildHealth
./gradlew connectedAndroidTest
```

- Android instrumented tests require environment variables for the test user and
  keys. Do not ask the user to paste secret values in chat.
- When installing a build on a connected phone, verify the device is available
  with `adb devices` first.
- On `2026-06-02`, `rebrand/passly` built successfully with
  `./gradlew assembleDebug`. The APK was generated at
  `app/build/outputs/apk/debug/app-debug.apk`.
- On `2026-06-02`, the Team Shield Passly logo/icon update bumped the app to
  `3.0.12` / version code `64`; `./gradlew assembleDebug` completed
  successfully and regenerated `app/build/outputs/apk/debug/app-debug.apk`.
- On `2026-06-02`, `adb devices` returned no connected devices, so no Android
  install was attempted.

## Mobile First Login / Browser Pairing

- The Android app is expected to transfer the encrypted private key payload to
  the browser for first login. Do not design the browser flow around manually
  moving the key from the phone to the browser.
- Preserve the existing legacy browser setup/login paths unless the user
  explicitly asks to remove them.
