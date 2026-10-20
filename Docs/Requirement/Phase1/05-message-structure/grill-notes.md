# Grill Notes — 05-message-structure

Source: `Docs/Requirement/Phase1/05-message-structure.md` (with `06-voice-processing-pipeline.md`, `09-pronunciation-romanization.md`, `10-local-database.md`, `12-retry.md`, `16-architecture-and-extensibility.md`) · 2026-10-10.

**No interview was run** (user instruction 2026-10-10: skip grill, don't wait for permission). Every decision below is Claude's and is marked **(self)**.

Repo state on this branch's base (`feature/04-chat-screen`):
- The `Message` table and domain class exist.
- The Chat screen already renders every part of a Message: Recording, transcript, source Language, translation, Reading.
- The only write is `insertMessage`.
- No Language says whether it needs a Reading.

## Scope

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What does this ticket add, given ticket 04 already shows every part of a Message? | The rules that make a Message's structure hold, independent of the pipeline: (a) whether a Reading is needed is a property of the target Language; (b) a Message moves through its Message Status in a defined order; (c) each part is persisted the moment it is produced (doc 06 "every step persists its result as soon as it succeeds"), always bumping the Conversation's `updatedAt` (ticket 02 I2). (self) |
| D2 | What stays for later tickets? | Producing the parts: recording (06), transcription and detection (07), translation (08), the Reading prompt (09), and the Retry UI (12). This ticket provides the write API they call. (self) |

## Reading need

| # | Question | Resolved answer |
|---|----------|-----------------|
| D3 | Where does "needs a Reading" live? | `Language.requiresReading: Boolean` on the catalog metadata (doc 05 Phase 1 constraint, doc 09 AC2). Phase 1: `en` false, `zh` true. It defaults to false, so existing constructions stay valid. Phase 2 sets it per added Language (Bangla, Japanese, …). (self) |
| D4 | How is it applied? | When a translation is saved, the Reading is kept only if the Message's **target** Language requires one; otherwise it is stored as `null`. Blank counts as absent. An unknown target code doesn't require one. No `if (target == "zh")` anywhere. (self) |
| D5 | Target requires a Reading but none was produced? | The Message still completes, with `reading = null`. Doc 09's AC is "non-null only when", not "always when"; a missing optional Reading must not fail a Message. (self) |

## Message Status lifecycle

| # | Question | Resolved answer |
|---|----------|-----------------|
| D6 | Allowed transitions? | RECORDING → TRANSCRIBING or FAILED. TRANSCRIBING → TRANSLATING or FAILED. TRANSLATING → COMPLETED or FAILED. FAILED → TRANSCRIBING (retry from the Recording) or TRANSLATING (retry from the transcript), for ticket 12. COMPLETED is terminal. The domain owns this as `MessageStatus.canMoveTo(next)`. (self) |
| D7 | Where is it enforced? **(revised in review: step-specific guards via `MessageStep`; an explicit `retryTranslation` step is the only way from FAILED to TRANSLATING; stale-attempt protection is the callers' job, S8)** | In the repository's SQL: each update has `WHERE id = :id AND status IN (<allowed previous statuses>)`, and the call returns whether a row changed. Atomic, so a late pipeline step can't overwrite a newer state (e.g. a Message already FAILED or COMPLETED). The allowed sets come from the domain table, not duplicated literals. (self) |
| D8 | Write operations? **(revised: plus `retryTranslation` FAILED → TRANSLATING; `markTranscribing` needs a Recording)** | `markTranscribing(id)` (Recording saved); `saveTranscription(id, transcribedText, sourceLanguage, targetLanguage)` → TRANSLATING; `saveTranslation(id, translatedText, reading)` → COMPLETED; `markFailed(id)`. Each runs in one transaction with the Conversation `updatedAt` bump (I2), and each leaves already-saved parts untouched, so a failure never loses the Recording or a transcript (doc 06/12). (self) |
| D9 | Content validation? **(revised in review: text with nothing visible, including zero-width-only, returns false instead of throwing, since silence is an expected STT outcome; codes still `require`)** | `saveTranscription`: non-blank text, non-blank codes, source ≠ target. `saveTranslation`: non-blank translation. Violations are caller bugs → `require` in the use cases. The pipeline (06) must fail a Message rather than save blank STT output. (self) |
| D10 | One use case per action? **(revised: five, including `RetryTranslationUseCase`)** | Yes, matching ticket 02: `MarkMessageTranscribingUseCase`, `SaveTranscriptionUseCase`, `SaveTranslationUseCase` (applies D4, reading the stored target from the Message), `MarkMessageFailedUseCase`. `MessageRepository` gains `getMessage(id)` for the Reading lookup. (self) |
| D11 | Delete or cancel a Message? | Not added. No requirement asks for it yet; ticket 06 adds it if a cancelled recording needs it. (self) |

## Verification

| # | Question | Resolved answer |
|---|----------|-----------------|
| D12 | Tests? | Catalog flag values; transition table (every pair); use cases (Reading kept/dropped/blank/unknown target, validation); SQLite repository: each write persists its parts, leaves the other parts, bumps `updatedAt`, refuses invalid transitions without changes, and re-emits to observers. No UI changes, so no manual UI cases beyond a smoke check that the Chat bubbles still render. (self) |

Glossary: added **Message Status** (the five stages and their order). ADRs: none.
