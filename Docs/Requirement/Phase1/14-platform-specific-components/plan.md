# Plan — 14-platform-specific-components

Inputs: `grill-notes.md` (D1–D10), `domain-model.md` (A1–A5).

## Step 1 — iOS dependencies and data
- `libs.versions.toml`/`shared/build.gradle.kts`: `sqldelight-nativeDriver`, `ktor-client-darwin` in `iosMain`.
- `IosAppDirectory`, `IosFileStorage`, `createIosSqlDriver`.

## Step 2 — iOS audio and permission
- `IosAudioSession`, `IosAudioRecorder`, `IosAudioPlayer`, `IosMicrophonePermission`.

## Step 3 — iOS wiring
- `iosPlatformModule`; `MainViewController` (Koin once, sweep, background stop, mic permission local, key from Info.plist).
- `Info.plist` (mic usage, `OpenAIApiKey`), `Config.xcconfig` (`Secrets.xcconfig` include, `-lsqlite3`), `Secrets.xcconfig.example`; README.

## Step 4 — Android audio focus
- `AndroidAudioPlayer(fileStorage, audioManager)` with transient focus.

## Step 5 — Tests
- `IosFileStorageTest` mirroring Android's.

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease`; `xcodebuild … -sdk iphonesimulator build`; launch in the simulator.

## Acceptance criteria
1. Every interface has Android and iOS implementations behind it.
2. The iOS app builds and opens the Home screen on the simulator.
3. Storage contracts match on both platforms.
4. Playback respects audio focus on Android.
