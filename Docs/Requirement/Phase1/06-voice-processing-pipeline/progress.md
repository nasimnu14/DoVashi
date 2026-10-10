# Progress — 06 Voice Processing Pipeline

> /execute-plan · Plan: Docs/Requirement/Phase1/06-voice-processing-pipeline/plan.md · Started: 2026-10-10 22:56
> Status: ✅ Complete · Last updated: 2026-10-11 02:30
> Baseline: `96ddb5e` (ticket 09); 206 host + 168 iOS-sim tests green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Interfaces + Android impls | ✅ Done | `audio/{AudioRecorder,FileStorage}.kt`, `permissions/MicrophonePermission.kt`; androidMain `audio/AndroidAudioRecorder.kt`, `permissions/AndroidMicrophonePermission.kt`, `storage/AndroidFileStorage.kt`, `di/AndroidPlatformModule.kt`; manifest; `MainActivity.kt`; `shared/build.gradle.kts` | assembleDebug + iOS compile ✅ |
| 2 | Repository additions | ✅ Done | `Message.sq`, `MessageRepository.kt`, `SqlMessageRepository.kt`, `ConversationRepository.kt` | tests ✅ |
| 3 | Pipeline + DI | ✅ Done | `domain/usecase/{ProcessRecording,FailInterruptedMessages}UseCase.kt`, `MessageProcessor.kt`, `VoiceRecordingController.kt`, `di/SharedModule.kt`, androidApp `DoVashiApplication.kt`, `data/debug/DebugSeeder.kt` | assembleDebug ✅ |
| 4 | Chat UI | ✅ Done | `presentation/conversation/{ChatUiState,ChatUiStateMapper,ChatViewModel,ChatScreen}.kt`, `navigation/AppNavHost.kt`, `composeResources/drawable/ic_{mic,stop}.xml` | assembleDebug ✅ |
| 5 | Tests | ✅ Done | commonTest `ProcessRecordingUseCaseTest` (8), `MessageProcessorTest` (2), `VoiceRecordingControllerTest` (9), `ChatViewModelTest` (+5), `TestSupport` (fakes + `PipelineFixture`); androidHostTest `SqlRepositoriesTest` (+3), `AndroidFileStorageTest` (+3) | 236 host + 192 iOS-sim, 0 failures |

## Step Log
### Steps 1–5 · ✅ Done
- Recorder: MediaRecorder (AAC/MPEG-4, mono 16 kHz, 64 kbps, `setMaxDuration`). A max-duration flag keeps the auto-stopped file even if `stop()` then throws. Storage: `newRecordingReference` (`audio/<uuid>.m4a`), `writablePath`, `delete`, all through one canonical root check.
- Permission: `MicrophonePermission` + `LocalMicrophonePermission` (default unavailable). Android `rememberMicrophonePermission()` uses `rememberLauncherForActivityResult`; `MainActivity` provides it. `RECORD_AUDIO` declared.
- Pipeline:
  - `ProcessRecordingUseCase` composes tickets 05/07/08/09; any exception (except our own cancellation) → markFailed.
  - `MessageProcessor` (app scope): the newest call wins; jobs are registered under a lock in call order and started outside it.
  - `VoiceRecordingController`: inserts RECORDING before opening the mic; ≥ 0.5 s → process, else delete row and file; a 2-minute cap; `stopIfRecording` for `onCleared`.
  - The startup sweep runs (`runBlocking`) before seeding. The seed's TRANSLATING sample became FAILED-with-transcript.
- UI: `MicUi` (Idle / Recording(startedAt) / Unavailable) and `micMessage`. Mic bar: 64 dp button with mic/stop icons, "Tap to speak" / "Recording m:ss · tap to stop", error text as a live region. The permission gate is in `AppNavHost`.
- **Bug found and fixed during Step 5:** in `MessageProcessor`, the lambda's `processRecording(...)` resolved to the member function of the same name (not the injected use case), so every job launched another one forever → OOM in 6 tests after 7.5 minutes. Diagnosed with a minimal debug test (removed afterwards). Fixed by renaming the property to `processRecordingUseCase`. While diagnosing, the job start was also moved outside the lock and completed jobs are pruned at the next launch (no suspension in `finally`).
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 236 host + 192 iOS-sim tests, 0 failures.

## Review Cycles
Cycles 1 and 2 ran in parallel (fresh read-only subagents, different lenses); fixes were applied together.

### Cycle 1 — Plan conformance & correctness · 2026-10-11 00:55
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | **High** | State became Recording only at the end of `start()`, so a quick second tap went back into start → "Another conversation is recording" and the Recording kept running (TC-13) | controller, VM | Fixed — `RecordingState.Starting(conversationId, startedAt)` is set before the insert. A tap while Starting/Recording here means stop; the stop waits for the start, then discards a short tap | ✅ `startingIsVisibleAndAStopDuringItWaitsThenDiscardsAShortTap`, VM `aSecondTapWhileStartingStopsInsteadOfComplaining` |
| 2 | Medium | Cancelling the caller could split "insert row" from "recorder started" (orphan row or a hot, untracked MediaRecorder) | controller | Fixed — start/stop/cancel bodies run in `withContext(NonCancellable)` | ✅ |
| 3 | Medium | A double tap on first use launched the permission request twice; the second instant "false" showed the denial and dropped the callback | Android permission | Fixed — a second request is ignored while one is pending | ✅ builds; manual |
| 4 | Medium | Recording continued in the background (Android silences the mic) | AppNavHost / Activity | Fixed — `MainActivity.onStop` (not on configuration change) → `controller.stopAny()`: stops and processes. D8 refined | ✅ `stopAnyStopsWhateverIsRecording`; manual |
| 5 | Low | Chained cancel could cut job2's join on job1 | processor | Fixed — `withContext(NonCancellable) { previous?.cancelAndJoin() }` | ✅ |
| 6 | Low | A replaced attempt can leave TRANSCRIBING (refused first step returns) | pipeline | Not fixed — only reachable when the same Recording is processed twice (Retry, ticket 12, will own that path); the startup sweep recovers it | open → 12 |
| 7 | Low | An accidental tap leaves `Conversation.updatedAt` bumped | repository | Not fixed — I2 (every insert bumps) comes from ticket 02; the effect is only a reorder | accepted |
| 8 | Low | The raw transcript is saved only after language resolution | pipeline | Documented — ticket 05's API saves transcript + codes together; undetermined → FAILED without text | accepted |
| 9 | Low | No exception handler on the app scope; first/last steps unguarded | DI, pipeline | Fixed — handler on the app scope; `markTranscribing`/`markFailed` guarded | ✅ |
| 10 | Low | Weak tests (insert-before-mic order, ...) | tests | Fixed — `theMessageExistsBeforeTheMicrophoneOpens`, more controller cases | ✅ |

### Cycle 2 — Edge cases, lifecycle & concurrency · 2026-10-11 00:55
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | No stop on background, screen off or call; no `OnErrorListener` | | Fixed — Activity `onStop` stop (see C1 #4); recorder `OnErrorListener` → automatic stop (the callback renamed `onStoppedAutomatically`) | ✅ |
| 2 | Medium | Double tap during start (same as C1 #1) | | Fixed | ✅ |
| 3 | Medium | DB exceptions uncaught (insert in viewModelScope, markFailed in app scope) → crash | controller, pipeline | Fixed — start returns FAILED on any exception; discard/cancel guarded; pipeline guarded; scope handler | ✅ `aDatabaseFailureOnStartIsReportedNotThrown` |
| 4 | Medium | A long Recording the recorder failed was silently discarded like a tap | controller, VM | Fixed — `StopResult` (PROCESSING / TOO_SHORT / FAILED / NOT_RECORDING); a user stop that FAILED shows "The recording didn't work. Please try again." | ✅ `aBrokenLongRecordingIsReportedAsFailed` |
| 5 | Med/Low | Process death while RECORDING leaves an unplayable m4a marked retryable | sweep | Fixed — the sweep deletes interrupted RECORDING Messages and their files; TRANSCRIBING/TRANSLATING → FAILED | ✅ `FailInterruptedMessagesUseCaseTest`, SQL test |
| 6 | Low | Cancellation could split start/stop | | Fixed (C1 #2) | ✅ |
| 7 | Low | `stop()` not tied to a Recording (a late auto-stop could stop the next one) | controller | Fixed — the automatic callback stops only its own message id | ✅ `aLateAutomaticStopDoesNotStopTheNextRecording` |
| 8 | Low | `updatedAt` bumped by an accidental tap | | Accepted (C1 #7) | accepted |
| 9 | Low | Permission result after rotation lost; no Settings link | | Not fixed — the user taps again; a Settings deep link is a UX extra | open, Low |
| 10 | Low | Stale docs (ticket 04 TC-08; `inProgressId` name) | docs/seed | Fixed | ✅ |
| 11 | Low | Test gaps (separate schedulers, Koin graph) | tests | Partly — more controller/VM cases; Koin graph check still needs koin-test | open, Low |
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` → 244 host + 200 iOS-sim tests, 0 failures (builds re-run with cycle 3).

### Cycle 3 — Integration, security & quality · 2026-10-11 02:10
The first cycle-3 reviewer was stopped by the API usage limit, so cycle 3 was re-run after the reset (fresh read-only subagent).
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | `allowBackup="true"` with no rules, so Recordings and transcripts go to Google Drive backup | manifest | **Not fixed — your product decision** (open since ticket 02, planned for ticket 10) | open → user |
| 2 | Medium | The no-op app-scope handler only ever caught Errors (every Exception is handled), hiding crashes such as the Step 5 OOM | `SharedModule.kt` | Fixed — handler removed; Errors crash and get reported | ✅ |
| 3 | Medium | Lost Recordings from automatic stops (recorder error, background `stopAny`) vanished silently | controller, VM | Fixed — `lostRecordings: SharedFlow<conversationId>`; the Chat screen shows "The recording didn't work. Please try again." whoever stopped it; accidental taps aren't reported | ✅ `anAutomaticStopThatLosesTheRecordingIsReported`, `anAccidentalTapIsNotReportedAsLost`, VM `aRecordingLostByAnAutomaticStopIsExplained` |
| 4 | Medium | Docs contradicted the code (sweep, callback name, Starting/StopResult/stopAny) | docs | Fixed — plan banner (+plan.txt), grill D8/D12, domain model, TC-19, new TC-23–25 | ✅ |
| 5 | Low | Startup sweep unguarded on the main thread (a full disk would crash every launch) | `DoVashiApplication.kt` | Fixed — best effort (try/catch) | ✅ builds |
| 6 | Low | `recorder.start` throwing after the insert left a stuck "Recording…" row | controller | Fixed — a throwing start counts as a failed start and is discarded | ✅ `aThrowingRecorderLeavesNothingBehind` |
| 7 | Low | Check-then-set on the state | controller | Fixed — `compareAndSet(Idle, Starting)` | ✅ |
| 8 | Low | `controller.cancel()` dead (tests only) | controller | Fixed — removed, with `AudioRecorder.cancel` (Android keeps a private `discardCurrent`) | ✅ |
| 9 | Low | The first `onStop` builds the whole processing graph on main | `MainActivity.kt` | Not fixed — a one-time cost of a few objects; noted | accepted |
| 10 | Low | Platform-contract KDoc detached; `AudioRecorder` not listed | `SharedModule.kt` | Fixed | ✅ |
| 11 | Low | Test gaps (sequential "independent" test, `stopAny` during Starting, auto-stop failure) | tests | Fixed — `differentMessagesRunConcurrently`, `stopAnyDuringStartingStopsOnceStarted`, auto-stop tests | ✅ |
| 12 | Low | `discard` deleted the row before the file (orphan file on process death) | controller | Fixed — file first; the sweep removes a leftover RECORDING row | ✅ |
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 248 host + 204 iOS-sim tests, 0 failures.

**Stopping rule applied:** cycle 3 found no Critical/High issues → stopped after 3 cycles. (Cycle 1's one High, the double-tap race, was fixed and tested.)

## Final Report

**Outcome:** ✅ Done with open issues. The full voice flow is implemented and unit-tested:
- tap to record, then tap to stop
- each step persists before the next, and failures keep what was saved
- one processing job per Message
- startup recovery
- the microphone permission flow

**Not yet run on a device, and no live OpenAI call** (no API key here).

### Changes
- Interfaces: `audio/AudioRecorder.kt` (created), `audio/FileStorage.kt` (+`newRecordingReference`, `writablePath`, `delete`), `permissions/MicrophonePermission.kt` (created)
- Android: `audio/AndroidAudioRecorder.kt`, `permissions/AndroidMicrophonePermission.kt` (created); `storage/AndroidFileStorage.kt`, `di/AndroidPlatformModule.kt`; androidApp `AndroidManifest.xml` (RECORD_AUDIO), `MainActivity.kt` (provides permission; stops recording on `onStop`), `DoVashiApplication.kt` (startup sweep); `shared/build.gradle.kts` (activity-compose in androidMain)
- Data: `Message.sq` (+`deleteRecording`, interrupted-recording sweep, `failInterrupted`); `MessageRepository`/`SqlMessageRepository`; `ConversationRepository.getConversation`
- Domain: `ProcessRecordingUseCase`, `MessageProcessor`, `VoiceRecordingController`, `FailInterruptedMessagesUseCase` (created); `di/SharedModule.kt`
- UI: `presentation/conversation/{ChatUiState,ChatUiStateMapper,ChatViewModel,ChatScreen}.kt`, `navigation/AppNavHost.kt`, `ic_mic.xml`, `ic_stop.xml`; `data/debug/DebugSeeder.kt` (no mid-pipeline seed)
- Tests: `ProcessRecordingUseCaseTest`, `MessageProcessorTest`, `VoiceRecordingControllerTest`, `FailInterruptedMessagesUseCaseTest` (created); `ChatViewModelTest`, `TestSupport` (fakes + `PipelineFixture`), `SqlRepositoriesTest`, `AndroidFileStorageTest`, `OpenAiSpeechToTextServiceTest`
- Docs: ticket 04 `test-cases.md` (seed change)

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` | BUILD SUCCESSFUL — 248 tests, 0 failures (206 before) |
| `./gradlew :shared:iosSimulatorArm64Test` | BUILD SUCCESSFUL — 204 tests, 0 failures |
| `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| Device run (recording, permission dialog, MediaRecorder), live OpenAI calls | **Not run** — no device or API key in this environment |

### Deviations & Assumptions
- Implemented after 07–09, which it composes.
- `Starting` state, `StopResult` and `lostRecordings` were added in review.
- The startup sweep deletes cut-off Recordings instead of failing them.
- Recording stops when the app leaves the foreground.
- No microphone foreground service: background recording is out of scope.

### Open Issues & Risks
- **[Medium — your decision] Backup of voice data.** `allowBackup="true"` with no rules means Recordings (`files/audio/*.m4a`) and transcripts go to Google Drive Auto Backup. This has been open since ticket 02 and matters more now that real voice is stored.
- A permission result arriving after a rotation is lost (tap again). There is no Settings link when the user permanently denied access.
- An accidental tap still bumps the Conversation's `updatedAt` (it reorders Home).
- No Koin graph test. iOS has no recorder, player, HTTP engine or Koin start yet (ticket 14).
- A Message whose same Recording is re-processed can stay TRANSCRIBING until restart (only reachable via Retry — ticket 12 owns it).

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | End-to-end voice flow (TC-02–TC-05) | 1. Put `OPENAI_API_KEY=…` in `local.properties`, then install debug. 2. In an English ↔ Mandarin Chinese conversation, tap the mic and allow access. 3. Say "How are you?" and tap stop. 4. Do the same with "你好吗？". | Bubbles go Recording… → Transcribing… → Translating… → final. English gets a Chinese translation with pinyin on the left; Chinese gets English with no Reading on the right. Play plays your voice. | The core feature. Recorder, upload, OpenAI and playback have never run together on a device. |
| 2 | Permission flows (TC-01, TC-08, TC-25) | 1. Fresh install, tap the mic and deny. 2. Tap again and allow. 3. Fresh install again, double-tap the mic. | Denied: the explanation shows. Allowed: recording starts. Double tap: a single dialog with no false denial. | Permission handling is only testable on a device. |
| 3 | Background, rotation, short tap, cap (TC-13, TC-14, TC-18, TC-23) | 1. Record, then rotate. 2. Record, then press Home. 3. Tap twice quickly. 4. Record for 2 minutes. | 1: keeps recording, the timer continues. 2: stops and processes. 3: nothing left behind. 4: auto-stops at 2:00 and processes. | Lifecycle and MediaRecorder behaviour can't be unit-tested. |
| 4 | Failures keep the Recording (TC-09–TC-11) | 1. Airplane mode, record and stop. 2. Record 3 s of silence. | "Failed" bubble whose Play still works. Silence ends Failed with no invented text. | Proves nothing spoken is lost (doc 06 / doc 12). |
| 5 | Process death (TC-19) | Record, then `adb shell am kill com.example.dovashiapp`, then reopen. | No stuck "Recording…" or "Transcribing…" bubbles. | The startup sweep is only exercised in tests. |
| 6 | Release build (TC-26 of ticket 04) | Install release. | No seed data and no tone file; the mic works the same. | Debug-only code must never reach users. |

## Pull Request
`feature/06-voice-processing-pipeline` → `phase_1`, **stacked on `feature/09-pronunciation-romanization`** (merge order 03 → 04 → 05 → 07 → 08 → 09 → 06). `gh` isn't installed:
- While 09 is unmerged: https://github.com/nasimnu14/DoVashi/compare/feature/09-pronunciation-romanization...feature/06-voice-processing-pipeline?expand=1
- After the earlier tickets are merged: https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/06-voice-processing-pipeline?expand=1

**Title:** feat(voice): end-to-end voice pipeline — record, transcribe, detect, translate (ticket 06)

**Description:**
Adds the microphone to the Chat screen and the full pipeline behind it (doc 06):
- **Recording:** tap to record, tap to stop. Recordings are capped at 2 minutes, and accidental taps leave nothing behind. Recording stops when the app leaves the foreground.
- **Pipeline:** each step is persisted before the next (Recording → Transcribing → language detection → Translating → Completed), composing tickets 05, 07, 08 and 09. Any failure marks the Message Failed and keeps the Recording and any transcript.
- **Reliability:** an app-scoped processor runs one job per Message, and a startup sweep recovers Messages a killed process left behind.
- **Permission:** the Android microphone permission flow, with an explanation when access is denied.

Depends on ticket 09.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
