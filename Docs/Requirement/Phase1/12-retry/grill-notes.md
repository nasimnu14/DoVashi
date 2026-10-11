# Grill Notes — 12-retry

Source: `Docs/Requirement/Phase1/12-retry.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Repo state: failures keep the Recording and everything saved (tickets 05/06). `RetryTranslationUseCase` (FAILED → TRANSLATING, needs transcript + both codes) and `START_TRANSCRIPTION` from FAILED exist but nothing calls them. `MessageProcessor` runs at most one job per Message and cancels the previous one. Open: S8 (one attempt per Message), ticket 06 #6 (replaced attempt can strand TRANSCRIBING), ticket 05 #8 (stale transcript on retry from Recording).

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What does Retry do? | Failed with a saved transcript + codes → retry translation only (doc 12: "reuses the stored audio / already-transcribed text"). Failed without a transcript → restart the pipeline from the saved Recording. Anything else (not Failed, nothing to resume from) → no-op. (self) |
| D2 | Stale transcript on retry-from-Recording (05 #8)? | Cannot happen: a transcript always comes with both codes (one write), and then the translation path is taken. Retry-from-Recording only runs when there is no transcript. (self) |
| D3 | Double tap / S8? | `MessageProcessor.retry` returns the running job instead of replacing it. Replacing would cancel the attempt, leave TRANSLATING/TRANSCRIBING behind and make the second run no-op. Recording processing keeps newest-wins. (self) |
| D4 | Where does it live? | `RetryMessageUseCase` (domain) composes the existing step use cases; `MessageProcessor.retry` runs it in the app scope so it survives leaving the screen. The failure handling shared with the original pipeline is one helper, `failUnlessCompleted`. (self) |
| D5 | Who can retry? | `Message.isRetryable` (Failed + transcript or Recording) drives `MessageBubbleUi.canRetry`; the mapper and the use case agree by construction. (self) |
| D6 | UI? | A "Retry" `TextButton` under the red "Failed" label. While retrying the status label shows Transcribing…/Translating… and the button disappears. (self) |
| D7 | Retry limit / backoff? | None; manual and user-driven. (self) |
| D8 | Schema change (attempt counter for ticket 05 S8)? | No: the processor serialises attempts, so no migration is needed. (self) |

Glossary: **Retry** added. ADRs: none.
