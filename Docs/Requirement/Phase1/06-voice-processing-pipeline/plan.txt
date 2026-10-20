# Plan — 06-voice-processing-pipeline

> **Revised during review (see progress.md):**
> - The controller has a `Starting` state (atomic Idle → Starting) and `stop()` returns `StopResult`. Start and stop run `NonCancellable`; `cancel()` was dropped (unused).
> - An automatic stop (max duration, recorder error) only stops its own Recording; `stopAny()` runs from `MainActivity.onStop`.
> - `lostRecordings` reports unusable long Recordings to the Chat screen.
> - `AudioRecorder` has no `cancel`, and its callback is `onStoppedAutomatically`.
> - The startup sweep deletes RECORDING rows with their files, and is best effort.
> - The permission request ignores a double tap.
> - The app scope has no exception handler, so Errors still crash.

Inputs: `grill-notes.md` (D1–D16), `domain-model.md` (P1–P6). Paths under `shared/src/commonMain/kotlin/com/example/dovashiapp/` unless noted.

## Step 1 — Storage, recorder and permission interfaces
- `audio/AudioRecorder.kt`; `audio/FileStorage.kt` (+`newRecordingReference`, `writablePath`, `delete`); `permissions/MicrophonePermission.kt` (interface, `MicrophonePermissionUnavailable`, `LocalMicrophonePermission`).
- Android: `AndroidFileStorage` additions; `androidMain/audio/AndroidAudioRecorder.kt` (MediaRecorder per D3/D4; a max-duration flag keeps an auto-stopped file); `androidMain/permissions/AndroidMicrophonePermission.kt` (`rememberMicrophonePermission()`); shared androidMain depends on activity-compose. The manifest declares `RECORD_AUDIO`; `MainActivity` provides the permission.

## Step 2 — Repository additions
- `Message.sq`: `deleteRecording` (`DELETE … WHERE id = ? AND status = 'RECORDING'`), `failInterrupted` (`UPDATE … SET status = 'FAILED' WHERE status IN (…)`).
- `MessageRepository.deleteRecording(id): Boolean`, `failInterruptedMessages(): Int`; `ConversationRepository.getConversation(id)` (via `observeConversation(id).first()`).

## Step 3 — Pipeline
- `ProcessRecordingUseCase` per D9/D10: catches `Exception` (with `ensureActive`), marks FAILED; a refused first step → return.
- `MessageProcessor(scope, processRecording)`: a map of Jobs per message id. `processRecording` cancels and joins any existing job, then launches, removing itself when done.
- `VoiceRecordingController(recorder, fileStorage, insertMessage, messageRepository, processor, clock, scope, mainDispatcher)`: start/stop/cancel per D1–D3 and D8. Recorder calls run on the main dispatcher. The max-duration callback launches `stop()`.
- `FailInterruptedMessagesUseCase`.
- DI: app `CoroutineScope(SupervisorJob() + Dispatchers.Default)`; singles for processor and controller. Android binds `AudioRecorder`. `DoVashiApplication` runs the sweep (`runBlocking`) before seeding.

## Step 4 — Chat UI
- `ChatUiState.Content` + `mic: MicUi`, `micMessage: String?`. `ChatViewModel` takes the controller and maps `controller.state` (recording this Conversation → RECORDING(startedAt); another → UNAVAILABLE; else IDLE). `onMicClick()` stops if recording here, else stops playback and starts. `onMicrophoneDenied()` sets the message; `onCleared` stops a Recording of this Conversation.
- `ChatScreen`: bottom mic bar per D14 (`ic_mic.xml`, `ic_stop.xml`; elapsed timer ticking each second from `startedAt`); the message above it.
- `AppNavHost`: the permission gate (granted → `onMicClick`; else request → granted → `onMicClick`, denied → `onMicrophoneDenied`).
- Debug seed per D13.

## Step 5 — Tests
- `ProcessRecordingUseCaseTest`: COMPLETED both directions; silence, undetermined language, STT error, translation error, missing Conversation → FAILED keeping the saved parts; refused first step → no calls.
- `MessageProcessorTest`: a second call for the same Message cancels the first before starting; different Messages run in parallel.
- `VoiceRecordingControllerTest` (fake recorder/storage):
  - start inserts RECORDING with the reference and starts on the writable path
  - a second start → AlreadyRecording
  - recorder failure → row and file removed, Failed
  - stop ≥ 0.5 s → processing launched
  - stop < 0.5 s or null → row and file removed
  - the max-duration callback stops and processes
  - cancel removes everything
- `SqlRepositoriesTest`: `deleteRecording` only while RECORDING; `failInterruptedMessages` touches exactly the 3 statuses; `getConversation`.
- `AndroidFileStorageTest`: reference format, writable path (dirs created, escape rejected), delete.
- `ChatViewModelTest`: mic states; toggling starts and stops; playback stopped on start; denied message; clearing stops this Conversation's Recording.
- Seeder test update.
- Verify: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. Tap mic → record → tap again → the Message appears and moves through Recording…, Transcribing…, Translating… to the finished bubble (original, detected language, translation, Reading if needed, Play).
2. Each step persists immediately; a failure at any step leaves the Recording (and a saved transcript) in a FAILED Message.
3. Source/target are resolved generically from the Conversation's pair.
4. Microphone permission is requested on first use; denial is explained.
5. Recordings auto-stop at 2 minutes; taps under 0.5 s leave nothing behind; only one Recording at a time.
6. Interrupted Messages become FAILED on the next start; one processing job per Message.
7. All tests pass; debug/release builds and the iOS compile succeed.
