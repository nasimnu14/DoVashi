# Plan — 05-message-structure

> **Revised during review (see progress.md):**
> - Writes are `MessageStep`s with per-step guards, plus `retryTranslation`; `canMoveTo` is derived from them, and there is no `previousStatusesOf` or `changes()`.
> - Text with nothing visible, or over 10,000 characters, returns false instead of throwing.
> - Transcription needs a Recording.
> - Tests live in `MessageStepUseCasesTest` / `MessageTextTest`.

Inputs: `grill-notes.md` (D1–D12), `domain-model.md` (S1–S7). No UI changes. Verify on Android plus the iOS compile.
Paths are under `shared/src/commonMain/kotlin/com/example/dovashiapp/` unless noted.

## Step 0 — Preconditions
- Base has ticket 04 (`Message`, `MessageRepository.observeMessages`, `insertMessage` + `touchConversation` in one transaction). Baseline: 96 tests green.

## Step 1 — Reading need on Language
- `domain/model/Language.kt`: add `val requiresReading: Boolean = false`.
- `domain/model/LanguageCatalog.kt`: `en` → `requiresReading = false` (explicit), `zh` → `true`.
- `domain/model/Reading.kt`: `fun readingFor(targetLanguageCode: String?, reading: String?, languageByCode: (String) -> Language?): String?` returns the trimmed reading only when the target exists in the catalog, `requiresReading` is true and the reading is non-blank; otherwise null.

## Step 2 — Message Status transitions
- `domain/model/MessageStatus.kt`: a private transition map per domain-model §1; `fun canMoveTo(next): Boolean`; `companion fun previousStatusesOf(next): Set<MessageStatus>`.

## Step 3 — Schema and repository
- `Message.sq`:
  - `selectById` and `changes: SELECT changes();`
  - `markTranscribing: UPDATE Message SET status = 'TRANSCRIBING' WHERE id = :id AND status IN :from;`
  - `saveTranscription` (sets `transcribedText`, `sourceLanguage`, `targetLanguage`, status TRANSLATING)
  - `saveTranslation` (sets `translatedText`, `reading`, status COMPLETED)
  - `markFailed` (status FAILED)
  - each with `WHERE id = :id AND status IN :from`. Status values are bound, not literal: use a `:status` parameter fed from `MessageStatus.X.name`.
- `MessageRepository`: add `getMessage(id): Message?` and the four writes returning `Boolean`.
- `SqlMessageRepository`: each write runs in `transactionWithResult`: read the Message's `conversationId`, run the UPDATE with `from = previousStatusesOf(next).map { it.name }`, and check `changes()`. Only if 1 row changed, `touchConversation(now, conversationId)`. Return whether it applied. Share the row → `Message` mapper between `observeMessages` and `getMessage`.

## Step 4 — Use cases (`domain/usecase`)
- `MarkMessageTranscribingUseCase(repo)(id)`, `MarkMessageFailedUseCase(repo)(id)`: delegate.
- `SaveTranscriptionUseCase(repo)(id, transcribedText, sourceLanguage, targetLanguage)`: `require` non-blank text and codes, and `source != target`; trims the text; delegates.
- `SaveTranslationUseCase(repo, languageByCode = LanguageCatalog::byCode)(id, translatedText, reading)`: `require` a non-blank translation; loads the Message (absent → false); stores `readingFor(message.targetLanguage, reading, languageByCode)`; delegates.
- Register all four in `di/SharedModule.kt` (factories).

## Step 5 — Tests
- commonTest `LanguageCatalogTest` (update): the `en`/`zh` equality cases include the flag; new: exactly the Languages with `requiresReading` are those whose flag is set (data-driven, no per-code branching in main code).
- commonTest `ReadingTest`: target requiring → trimmed reading kept; not requiring → null; unknown code → null; null target → null; blank reading → null.
- commonTest `MessageStatusTest`: the full 5×5 matrix of `canMoveTo` against the table; `previousStatusesOf` for each target.
- commonTest `MessageUseCasesTest` (fake repository): transcription validation (blank text, blank codes, same codes rejected; text trimmed); translation validation (blank rejected); Reading kept for a requiring target and dropped otherwise (using a test catalog, not `en`/`zh`); missing Message → false.
- androidHostTest `SqlRepositoriesTest`:
  - happy path RECORDING → TRANSCRIBING → TRANSLATING → COMPLETED stores each part, and each step bumps `updatedAt` (advance the fake clock between steps)
  - `markFailed` from each non-terminal status keeps the earlier parts
  - FAILED → TRANSCRIBING and FAILED → TRANSLATING (retry) are allowed
  - invalid transitions (COMPLETED → anything, RECORDING → TRANSLATING, TRANSCRIBING → COMPLETED) return false and change nothing, including `updatedAt`
  - an unknown id returns false
  - observers re-emit after a write
- Verify: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. Each Language in the catalog says whether it needs a Reading (`zh` yes, `en` no). The decision is a metadata lookup, with no code comparisons in logic.
2. A saved translation keeps its Reading only when the target Language needs one; otherwise `reading` is null. A missing Reading never fails the Message.
3. Message Status follows the transition table; invalid moves are refused atomically and change nothing.
4. Each step persists its parts immediately and keeps earlier parts, so a failure never loses the Recording or a saved transcript.
5. Every applied step bumps the Conversation's `updatedAt`, so Home re-sorts and Chat re-renders live.
6. All Step 5 tests pass; `assembleDebug` and the iOS compile succeed; existing Chat/Home behaviour is unchanged.
