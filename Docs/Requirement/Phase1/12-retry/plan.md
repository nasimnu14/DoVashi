# Plan — 12-retry

Inputs: `grill-notes.md` (D1–D8), `domain-model.md` (R1–R4).

## Step 1 — Domain
- `Message.hasTranscript` / `isRetryable`.
- Extract `failUnlessCompleted` from `ProcessRecordingUseCase`; add `RetryMessageUseCase`.
- `MessageProcessor.retry` (no replacement of a running job); DI.

## Step 2 — Presentation
- `MessageBubbleUi.canRetry`; mapper; `ChatViewModel.onRetryClick(messageId)` (takes `MessageProcessor`); Retry button in `ChatScreen`; nav wiring; `GLOSSARY.md` Retry.

## Step 3 — Tests
- `RetryMessageUseCaseTest` (translation-only, from Recording, fails again, silence, not-Failed no-op, nothing to resume).
- `MessageProcessorTest` double tap; mapper `canRetry`; VM retry end to end.

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug`; mutation: `retry` replacing a running job fails the double-tap test.

## Acceptance criteria
1. A Failed Message shows Retry; tapping it completes it without recording again.
2. Translation failures do not call speech-to-text again.
3. Audio and saved data survive a failed retry.
4. Double tap does not cancel the attempt.
