# Progress — 12 Retry

> /execute-plan · Plan: Docs/Requirement/Phase1/12-retry/plan.md · Started: 2026-10-11 04:00
> Status: ✅ Complete · Baseline: `ca2eed0` (ticket 10), green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Domain | ✅ Done | `Message.kt`, `ProcessRecordingUseCase.kt`, `RetryMessageUseCase.kt`, `MessageProcessor.kt`, `SharedModule.kt` | compile + tests ✅ |
| 2 | Presentation | ✅ Done | `ChatUiState.kt`, `ChatUiStateMapper.kt`, `ChatViewModel.kt`, `ChatScreen.kt`, `AppNavHost.kt`, `GLOSSARY.md` | tests ✅, `assembleDebug` ✅ |
| 3 | Tests | ✅ Done | `RetryMessageUseCaseTest`, `MessageProcessorTest`, `ChatUiStateMapperTest`, `ChatViewModelTest`, `TestSupport` | 266 host tests ✅ |

## Step Log
- `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug` → BUILD SUCCESSFUL (266 host tests).
- Mutation: `retry(...)` with `replaceRunning = true` → `retryingAMessageThatIsAlreadyBeingProcessed…` FAILED; restored from a backup of that one file.
- Deviation: `ProcessRecordingUseCase.invoke` was refactored to share `failUnlessCompleted` (identical behaviour; existing tests pass unchanged).

## Review Cycles (self-review; full diff re-read each cycle)
### Cycle 1 — Conformance
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | Medium | First draft of the double-tap test gated the wrong service (retry-with-transcript never calls STT) so it proved nothing | Fixed — test uses a Failed Message without transcript so the first retry holds at STT; mutation now fails it |
| 2 | Low | `isRetryable` default test helper has a transcript by default, so a "nothing to resume" case needs `transcribedText = null` | Fixed in the mapper test |
### Cycle 2 — Edge cases
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | Low | A retry for a Message whose Recording file was deleted fails in STT and ends Failed again (retryable, no crash) | Accepted |
| 2 | Low | Retry bumps the Conversation's `updatedAt` so it moves to the top of Home | Accepted — activity |
### Cycle 3 — Integration / quality
| # | Severity | Finding | Action |
|---|---|---|---|
| 1 | Low | `ChatViewModel` now depends on `MessageProcessor` (domain class) rather than a use case | Accepted — same layer the recording controller already uses; one class owns attempt serialisation |
| 2 | — | No schema change, no new permissions, no network calls beyond the existing adapters | ok |

## Final Report
**Outcome:** ✅ Done. Closes ticket 05 #8/S8 and 06 #6 (attempts are serialised; retry-from-Recording only runs without a transcript). Not run on a device.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected | Why critical |
|---|---|---|---|---|
| 1 | Real retry | Airplane mode, record, wait for Failed; network on, tap Retry | Completes without recording again | The core promise of doc 12 |
| 2 | Leave during retry | Tap Retry, go Home | Message completes in the background | App-scope job survives the screen |
| 3 | Kill during retry | Tap Retry, kill app, reopen | Failed with Retry (startup sweep) | No stuck "Translating…" |
