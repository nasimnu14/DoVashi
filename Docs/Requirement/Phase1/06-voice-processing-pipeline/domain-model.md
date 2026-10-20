# Domain Model — 06-voice-processing-pipeline

| Concept | Layer | Shape |
|---|---|---|
| `AudioRecorder` | audio (commonMain) | `start(absolutePath, maxDurationMillis, onStoppedAutomatically): Boolean` (max duration reached or recorder error), `stop(): Long?` (duration ms; null if nothing usable) |
| `FileStorage` (+) | audio | `newRecordingReference(): String` (`audio/<uuid>.m4a`), `writablePath(reference): String?` (creates parent dirs; same validation), `delete(reference)` |
| `MicrophonePermission`, `LocalMicrophonePermission` | permissions (commonMain) | `isGranted()`, `request(onResult)`; default unavailable |
| `MessageRepository` (+) | domain/repository | `deleteRecording(id): Boolean` (only while RECORDING), `failInterruptedMessages(): Int` |
| `ConversationRepository` (+) | domain/repository | `getConversation(id): Conversation?` |
| `ProcessRecordingUseCase` | domain/usecase | `(messageId, conversationId, recordingReference)`; steps per D9, failure per D10 |
| `MessageProcessor` | domain/usecase | App-scoped; `processRecording(...)`: one job per Message, cancel and join any previous |
| `VoiceRecordingController` | domain/usecase | App-scoped; `state: StateFlow<RecordingState>` (Idle / Starting(conversationId, startedAt) / Recording(conversationId, messageId, startedAt)); `start(conversationId): StartResult` (atomic Idle → Starting); `stop(): StopResult` (PROCESSING / TOO_SHORT / FAILED / NOT_RECORDING); `stopIfRecording(conversationId)`, `stopAny()`; `lostRecordings: SharedFlow<conversationId>` |
| `FailInterruptedMessagesUseCase` | domain/usecase | Startup sweep: deletes RECORDING Messages and their files, marks TRANSCRIBING/TRANSLATING Failed |
| Android | androidMain | `AndroidAudioRecorder` (MediaRecorder), `rememberMicrophonePermission()`, file-storage additions |
| Chat | presentation/conversation | `ChatUiState.Content.mic: MicUi` (IDLE / RECORDING(startedAt) / UNAVAILABLE), `micMessage: String?`; `ChatViewModel.onMicClick()`, `onMicrophoneDenied()` |

```
tap mic ─► permission? ─► VoiceRecordingController.start ─► insert RECORDING(audioPath) ─► AudioRecorder.start
tap again / 2 min ─► stop ─► <0.5 s: delete row + file │ else MessageProcessor.processRecording
   └─► ProcessRecordingUseCase: TRANSCRIBING ─► STT ─► text? ─► resolve pair ─► TRANSLATING ─► translate ─► COMPLETED
                                  any failure ─► FAILED (parts kept)
app start ─► FailInterruptedMessagesUseCase (RECORDING ─► deleted with file; TRANSCRIBING/TRANSLATING ─► FAILED)
```

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| P1 | Every step persists before the next runs; failure never loses the Recording or a saved transcript | Ticket 05 step writes; pipeline order |
| P2 | Source/target come only from detection and the pair; no literals | Ticket 07 resolver |
| P3 | At most one processing job per Message | `MessageProcessor` cancel-and-join (S8) |
| P4 | Nothing stays mid-pipeline across a restart; cut-off Recordings are removed | Startup sweep (S9) |
| P5 | At most one Recording at a time (atomic Idle → Starting); ≤ 2 min; stopped when the app leaves the foreground; accidental taps leave nothing behind; a lost Recording is explained | Controller + Activity `onStop` |
| P6 | Recording requires microphone permission; denial is explained, never silent | Permission gate in the Chat route |
