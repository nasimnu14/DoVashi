# Progress — 15 OpenAI API Security

> /execute-plan · Plan: Docs/Requirement/Phase1/15-openai-api-security/plan.md · Started: 2026-10-11 03:00
> Status: ✅ Complete · Last updated: 2026-10-11 03:25
> Baseline: `e64b2bd` + the uncommitted debug-logging work; `:shared:testAndroidHostTest :androidApp:assembleDebug` green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Land debug HTTP logging | ✅ Done | `libs.versions.toml`, `shared/build.gradle.kts`, `HttpClientFactory.kt`, `SharedModule.kt`, `AndroidPlatformModule.kt`, `LogcatHttpLogger.kt`, `DoVashiApplication.kt`, `HttpClientFactoryTest.kt` | host + iOS-sim tests ✅ |
| 2 | Hard-coded-key guard | ✅ Done | `androidHostTest/.../security/NoHardcodedSecretsTest.kt` | 2 tests ✅; mutation (planted `sk-…` file) fails the scan ✅, file removed |
| 3 | iOS secrets ignore + docs | ✅ Done | `.gitignore`, `README.md` | read-through |

## Step Log
- Step 1: the in-tree work was reviewed as-is: Ktor `Logging` is installed only when a `Logger` is bound; Android binds `LogcatHttpLogger` only when `FLAG_DEBUGGABLE`; `Authorization` sanitised; `LoggingFormat.OkHttp` omits binary bodies.
- Step 2: scan roots `shared/src`, `androidApp/src`, `iosApp` (skips `build`, `xcuserdata`); the `.gitignore` check requires `local.properties`, `*.key`, `Secrets.xcconfig`.
- Step 3: README "OpenAI API key" section documents setup, the embedded-key trade-off and the proxy path.
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease` → BUILD SUCCESSFUL.

## Review Cycles
Self-review (user asked for no approval waits); each cycle re-read the full diff.

### Cycle 1 — Plan conformance · 2026-10-11 03:12
| # | Severity | Finding | File:line | Action | Result |
|---|---|---|---|---|---|
| 1 | Low | The guard test would silently scan nothing if the working directory changed | `NoHardcodedSecretsTest.kt` | Fixed — asserts the scan roots exist | ✅ |
- Summary: 1 found · 1 fixed · 0 open.

### Cycle 2 — Edge cases · 2026-10-11 03:16
| # | Severity | Finding | File:line | Action | Result |
|---|---|---|---|---|---|
| 1 | Low | A debuggable build outside Android Studio (`android:debuggable` release) would log bodies | `DoVashiApplication.kt` | Accepted — debuggable means a developer build by definition | accepted |
| 2 | Low | Logger bound via `getOrNull` — a missing binding means no logging (safe default) | `SharedModule.kt` | None | ok |
- Summary: 0 fixed · 0 open (both accepted).

### Cycle 3 — Security & quality · 2026-10-11 03:20
| # | Severity | Finding | File:line | Action | Result |
|---|---|---|---|---|---|
| 1 | Low | Key still compiled into the APK and Gradle caches | `androidApp/build.gradle.kts` | Documented as the MVP trade-off with the proxy path (D6) | open (by design) |
| 2 | — | `OpenAiConfig.toString` redacts the key; the header is masked in logs (tests) | — | Verified | ok |
- Summary: no Critical/High.

## Final Report
**Outcome:** ✅ Done with accepted trade-off (key embedded in APK, doc 15 allows direct calls for the MVP). Not run on a device.

### Verification Run
| Command | Result |
|---|---|
| `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease` | BUILD SUCCESSFUL |
| Mutation: planted `sk-proj-…` in a source file | `noSourceFileContainsAnApiKey` FAILED as intended; file removed |

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected | Why critical |
|---|---|---|---|---|
| 1 | Release is silent | Install a release build, record a message, `adb logcat \| grep DoVashi-HTTP` | No lines | Bodies contain what users said |
| 2 | Debug masks the key | Debug build, same, `adb logcat -s DoVashi-HTTP` | `Authorization` value masked | Key leak via logcat |
| 3 | Key never reaches Git | `git log -p -S'sk-' --all` | no matches | Public-repo leak |
