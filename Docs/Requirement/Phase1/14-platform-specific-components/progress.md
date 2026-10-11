# Progress — 14 Platform-Specific Components

> /execute-plan · Plan: Docs/Requirement/Phase1/14-platform-specific-components/plan.md · Started: 2026-10-11 05:20
> Status: ✅ Complete · Baseline: `c006fee` (ticket 11), green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | iOS deps + data | ✅ Done | `libs.versions.toml`, `shared/build.gradle.kts`, `iosMain/storage/*`, `iosMain/data/database/IosSqlDriver.kt` | iOS compile ✅ |
| 2 | iOS audio + permission | ✅ Done | `iosMain/audio/*`, `iosMain/permissions/IosMicrophonePermission.kt` | iOS compile ✅ |
| 3 | iOS wiring | ✅ Done | `iosMain/di/IosPlatformModule.kt`, `iosMain/MainViewController.kt`, `Info.plist`, `Config.xcconfig`, `Secrets.xcconfig.example`, `README.md` | `xcodebuild` ✅, simulator launch ✅ |
| 4 | Android audio focus | ✅ Done | `AndroidAudioPlayer.kt`, `AndroidPlatformModule.kt` | `assembleDebug/Release` ✅ |
| 5 | Tests | ✅ Done | `iosTest/.../IosFileStorageTest.kt` | iOS-sim tests ✅ |

## Step Log
- `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease` → BUILD SUCCESSFUL.
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17' build` → first link FAILED (`_sqlite3_*` undefined: the static framework needs `-lsqlite3` in the app link). Fixed with `OTHER_LDFLAGS` in `Config.xcconfig`; rebuild ✅.
- Installed on the iPhone 17 simulator and launched: "Conversations / No conversations yet" rendered (Koin, database and Compose all started). Recording and playback were not exercised (no microphone/key on the simulator run).
- Deviations: none from the plan; `Dispatchers.IO` unavailable on Native → `Dispatchers.Default` (D7).

## Review Cycles (self-review; full diff re-read each cycle)
### Cycle 1 — Conformance
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | High | Unit tests can't see the missing `-lsqlite3`; the iOS app would not have linked | Fixed (xcconfig) after the Xcode build |
| 2 | Low | `NoHardcodedSecretsTest` scans `iosApp/`; a developer's real `Secrets.xcconfig` would fail it | Fixed — that one file is excluded (its ignore entry is asserted separately) |
### Cycle 2 — Edge cases
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | Medium | Recorder: our `stop()` must not fire the "stopped automatically" callback | Fixed by design — delegate detached first (A3) |
| 2 | Medium | Recorder/player initialisers can fail for undecodable files | Handled — `null` result → `start`/`play` return false, session released |
| 3 | Low | Phone-call/Siri interruptions during recording are not handled beyond the background hook | Accepted; listed as manual check |
| 4 | Low | `recordPermission` is deprecated from iOS 17 (works) | Accepted |
### Cycle 3 — Integration / quality
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | Low | Two storage implementations share a contract, not code | Accepted (D6); contract tests mirror each other |
| 2 | Low | iOS key is in the app bundle (ticket 15 trade-off) | Documented |
| 3 | — | Android: focus abandoned on pause/stop/release; `AudioManager` injected for testability | ok |

## Final Report
**Outcome:** ✅ Done with open issues: the iOS app builds and launches on the simulator, but recording, playback, permission prompt and OpenAI calls have not been exercised on an iOS device. Android audio focus is unverified on a device.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected | Why critical |
|---|---|---|---|---|
| 1 | iOS record → transcribe → translate | Real iPhone, key in `Secrets.xcconfig`; allow mic; record; stop | Message completes with translation | None of the iOS audio/network path has run end to end |
| 2 | iOS playback | Tap Play on a Message | Plays, returns to Play at the end; other audio resumes | AVAudioSession handling |
| 3 | iOS interruptions | Call or Siri during recording; background the app | Recording stops, Message processed or Failed with Retry | Not handled beyond the background hook |
| 4 | Android focus | Play music, play a Recording; take a call during playback | Music pauses and resumes; call pauses playback | Focus code is not unit-testable |
| 5 | Backup exclusion | Inspect backup / `NSURLIsExcludedFromBackupKey` on the app directory | Excluded | Voice privacy |
