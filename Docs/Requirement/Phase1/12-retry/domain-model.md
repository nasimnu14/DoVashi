# Domain Model — 12-retry

| Concept | Layer | Change |
|---|---|---|
| `Message.hasTranscript`, `Message.isRetryable` | domain/model | New derived properties |
| `RetryMessageUseCase` | domain/usecase | New: translation-only or from-Recording, by what is saved |
| `failUnlessCompleted` | domain/usecase | Extracted from `ProcessRecordingUseCase`; shared failure handling |
| `MessageProcessor.retry(messageId)` | domain/usecase | New; ignores a Message already being processed |
| `ChatViewModel.onRetryClick`, `MessageBubbleUi.canRetry`, Retry button | presentation/conversation | New |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| R1 | Retry never discards the Recording or saved transcript | Step guards; only `saveTranscription`/`saveTranslation` write |
| R2 | Retry never requires a new Recording | Uses saved audioPath / transcript |
| R3 | One live attempt per Message; a double tap changes nothing | `MessageProcessor.retry` |
| R4 | A retry that fails again leaves a retryable Failed Message | `failUnlessCompleted` |
