# Domain Model — 05-message-structure

Sources: `05-message-structure.md`, `06-voice-processing-pipeline.md`, `09-pronunciation-romanization.md`, `10-local-database.md`, `12-retry.md`, `GLOSSARY.md`, `grill-notes.md`, code on `feature/04-chat-screen`.

## 1. Entities & relationships

### Language (modified, ticket 01)
`Language(code, name, nativeName, requiresReading: Boolean = false)`. Catalog: `en` → false, `zh` → true.

### MessageStatus (modified, ticket 02)
Gains `canMoveTo(next: MessageStatus): Boolean` (the transition table below). **Revised in review:** the writes are `MessageStep`s (`START_TRANSCRIPTION`, `SAVE_TRANSCRIPTION`, `RETRY_TRANSLATION`, `SAVE_TRANSLATION`, `FAIL`). Each has its own allowed previous statuses, a subset of the table, and output-carrying steps accept only the stage that produces them:

| From \ To | TRANSCRIBING | TRANSLATING | COMPLETED | FAILED |
|---|---|---|---|---|
| RECORDING | ✔ | | | ✔ |
| TRANSCRIBING | | ✔ | | ✔ |
| TRANSLATING | | | ✔ | ✔ |
| FAILED | ✔ (retry) | ✔ (retry) | | |
| COMPLETED | | | | |

RECORDING is only an initial status (set by `insertMessage`); nothing moves back into it.

### Message (consumed, ticket 04)
Parts and the step that writes them:
| Part | Written by | Status after |
|---|---|---|
| `audioPath` (Recording) | `insertMessage` (06) | RECORDING |
| — | `markTranscribing` | TRANSCRIBING |
| `transcribedText`, `sourceLanguage`, `targetLanguage` | `saveTranscription` | TRANSLATING |
| `translatedText`, `reading` | `saveTranslation` | COMPLETED |
| — | `markFailed` | FAILED (parts kept) |

### MessageRepository (extended)
`getMessage(id): Message?`, `markTranscribing(id)` (RECORDING/FAILED → TRANSCRIBING), `saveTranscription(id, text, source, target)` (TRANSCRIBING → TRANSLATING), `retryTranslation(id)` (FAILED → TRANSLATING, needs a saved transcript), `saveTranslation(id, translated, reading)` (TRANSLATING → COMPLETED), `markFailed(id)`, all `Boolean`. `true` = applied, `false` = no such Message or transition not allowed.

### Use cases (new)
`MarkMessageTranscribingUseCase`, `SaveTranscriptionUseCase` (codes `require`d; text with nothing visible → false), `SaveTranslationUseCase` (nothing visible → false; applies the Reading rule from the stored `targetLanguage`), `RetryTranslationUseCase`, `MarkMessageFailedUseCase`.

## 2. Bounded context / ownership

| Layer | Owns |
|---|---|
| `domain/model` | `Language.requiresReading`, catalog values, `MessageStatus` transition table, `MessageStep` guards, `readingFor(...)`, `visibleTextOrNull(...)` |
| `domain/repository` | The step-write contract above |
| `domain/usecase` | Validation and the Reading rule; one use case per step |
| `data/database` (`Message.sq`) | `selectById`, `selectConversationId`; guarded `UPDATE … WHERE id = ? AND status IN ?` statements (row count from the mutator's result) |
| `data/repository` | Each write plus the Conversation `touchConversation` bump in one transaction; the guard sets come from `MessageStep.from` |

## 3. Invariants

| ID | Invariant | Enforced by |
|---|---|---|
| S1 | A Reading is stored only when the target Language `requiresReading` | `SaveTranslationUseCase` + `readingFor` |
| S2 | Whether a Reading is needed is looked up from Language metadata, never by comparing codes | `Language.requiresReading`; no literal checks |
| S3 | A Message only moves along the transition table; COMPLETED is final; a late pipeline result can't move a Message out of FAILED (only retry steps can) | Guarded SQL + `MessageStep.from` |
| S4 | A rejected transition changes nothing (no fields, no `updatedAt`) | `WHERE status IN` + rows-affected checked before the bump, inside one transaction |
| S5 | Every applied write bumps the parent Conversation's `updatedAt` in the same transaction | Repository (ticket 02 I2) |
| S6 | Saving a later part never clears an earlier one (Recording, transcript survive failure) | Each UPDATE sets only its own columns |
| S7 | A COMPLETED Message has non-blank transcript and translation and distinct source/target codes | Use-case validation + the transition order |
| S9 | A Message interrupted mid-pipeline (process death in RECORDING/TRANSCRIBING/TRANSLATING) is marked FAILED on the next app start, so Retry applies | **Ticket 06** startup sweep, before any attempt runs |
| S8 | At most one pipeline attempt per Message runs at a time (guards stop out-of-order steps, not stale ones from a superseded attempt) | **Callers** — tickets 06/12 cancel and join the previous attempt before retrying (documented on `MessageRepository`) |
