# Progress — 05 Message Structure

> /execute-plan · Plan: Docs/Requirement/Phase1/05-message-structure/plan.md · Started: 2026-10-10 16:55
> Status: ✅ Complete · Last updated: 2026-10-10 18:10
> Baseline: `feature/05-message-structure` at `feature/04-chat-screen` HEAD (`176f3cd`); 96 tests green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 0 | Preconditions | ✅ Done | — | `Message`, `observeMessages`, transactional `insertMessage` present; 96 tests green at ticket 04 end |
| 1 | Reading need on Language | ✅ Done | `domain/model/{Language,LanguageCatalog,Reading}.kt` | tests ✅ |
| 2 | Message Status transitions | ✅ Done | `domain/model/MessageStatus.kt` | tests ✅ |
| 3 | Schema + repository | ✅ Done | `Message.sq`, `domain/repository/MessageRepository.kt`, `data/repository/SqlMessageRepository.kt` | tests ✅ |
| 4 | Use cases | ✅ Done | `domain/usecase/{MarkMessageTranscribing,SaveTranscription,SaveTranslation,MarkMessageFailed}UseCase.kt`, `di/SharedModule.kt` | tests ✅ |
| 5 | Tests | ✅ Done | commonTest `LanguageCatalogTest` (updated +1), `ReadingTest` (5), `MessageStatusTest` (2), `MessageStepUseCasesTest` (8), `TestSupport.kt` (stateful fake); androidHostTest `SqlRepositoriesTest` (+6) | 118 at step end → 130 after reviews, 0 failures; assembleDebug + iOS compile ✅ |

Status legend: ⏳ Pending · 🔄 In progress · ✅ Done · ⚠️ Done with issues · ⛔ Blocked · ⏭️ Skipped (already exists)

## Assumptions
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.
- User instruction (2026-10-10): proceed without waiting for permission.

## Step Log
### Steps 1–4 — Implementation · ✅ Done
- `Language.requiresReading` (default false); catalog sets `en` false and `zh` true explicitly. `readingFor()` trims and keeps a Reading only for a requiring catalog target.
- `MessageStatus.canMoveTo` via an exhaustive `when`; `previousStatusesOf` is derived from it (no duplicated table).
- `Message.sq`: `selectById`, `selectConversationId`, `changes`, guarded `updateStatus` / `saveTranscription` / `saveTranslation` (`WHERE id = :id AND status IN :allowed`; status values bound as parameters).
- `SqlMessageRepository.applyStep`: inside one transaction it looks up `conversationId` (absent → false), runs the guarded UPDATE, checks `changes() == 1`, and only then `touchConversation`. A shared `toMessage` mapper serves `observeMessages` and `getMessage`.
- Four use cases; validation by `require` (caller bugs); `SaveTranslationUseCase` reads the stored target.
- `FakeMessageRepository` is now stateful and follows the transition table (reusable by tickets 06/12).
- Deviation: `markTranscribing`/`markFailed` share one `updateStatus` query (the plan listed them separately). Same guard, fewer statements.

### Step 5 — Tests · ✅ Done
- 22 new tests (catalog +1, Reading 5, status matrix 2, use cases 8, SQLite 6); 2 catalog equality tests updated for the flag.
- Verification: `./gradlew :shared:testAndroidHostTest` → 118 tests, 0 failures; `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL.

## Review Cycles
Cycles 1 and 2 ran in parallel (fresh read-only subagents, different lenses); fixes were applied together and cycle 3 reviews the result.

### Cycle 1 — Plan conformance & correctness · 2026-10-10 17:35
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | FAILED → TRANSLATING/TRANSCRIBING reachable through output-carrying writes, so a late STT result after `markFailed` revives the Message ("Translating…" forever) | `MessageStatus.kt`, `SqlMessageRepository.kt` | Fixed — `MessageStep` with per-step allowed statuses: `saveTranscription` only from TRANSCRIBING, `saveTranslation` only from TRANSLATING, new explicit `retryTranslation` (FAILED → TRANSLATING, needs transcript + codes) + `RetryTranslationUseCase` | ✅ tests `lateTranscriptCannotReviveAFailedMessage`, `failedMessagesCanBeRetriedOnlyThroughRetrySteps`, step-table tests |
| 2 | Medium | Tests couldn't catch bumping the wrong Conversation (ids coincided); TC-05 missing; `markFailed` bump untested | `SqlRepositoriesTest.kt` | Fixed — decoy conversation + Messages make the ids differ; asserts sort order, the decoy's `updatedAt` unchanged, status after each step, and the bump on every `markFailed` | ✅ |
| 3 | Low | `SELECT changes()` redundant (mutators return rows affected) and relies on connection pinning | `SqlMessageRepository.kt`, `Message.sq` | Fixed — uses `QueryResult.value == 1L`; `changes` query removed | ✅ |
| 4 | Low | Refused writes still notify, so observers re-emit identical lists | `SqlMessageRepository.kt` | Fixed — `distinctUntilChanged()` on `observeMessages` | ✅ |
| 5 | Low | Fake doesn't model the `updatedAt` bump / FKs / timestamps | `TestSupport.kt` | Documented in the fake's KDoc (the SQLite tests cover them) | noted |
| 6 | Low | `insertMessage` accepts any status (bypasses S3/S7) | `MessageRepository.kt` | Documented: the pipeline inserts RECORDING; other statuses are for seeding/tests | noted |
| 7 | Low | Reading decided from a target read outside the write transaction | `SaveTranslationUseCase.kt` | Not fixed — the target only changes on a new attempt, which S8 serialises | open, Low |

### Cycle 2 — Edge cases & regressions · 2026-10-10 17:35
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | High (latent until 06) | Same as Cycle 1 #1 | | Fixed (see above) | ✅ |
| 2 | High (latent until 12) | ABA: after a retry, a stale step from the superseded attempt is indistinguishable from the new one (could complete/fail the new attempt) | `SqlMessageRepository.kt` | Not fixed in schema — an `attempt` column would need a migration on existing installs (no migration setup yet; ticket 10). Recorded as invariant **S8** on `MessageRepository`: callers run at most one attempt per Message, cancelling and joining the previous one before retrying. Tickets 06/12 must implement it | open → requirement for 06/12 |
| 3 | Medium | `require` threw on blank STT output (an expected runtime outcome), so ticket 06 could crash | `SaveTranscriptionUseCase.kt` | Fixed — text with nothing visible returns false (no write); codes still `require` | ✅ `transcriptWithNothingVisibleIsNotAppliedAndDoesNotThrow`, `blankTranslationIsNotApplied` |
| 4 | Low | TOCTOU on the target (same as Cycle 1 #7) | | Not fixed (S8) | open |
| 5 | Low | Codes not normalised or checked against the catalog | `SaveTranscriptionUseCase.kt` | Not fixed — codes come from the Conversation (created from the catalog); requiring catalog membership would break legacy codes. Detection (07) produces them from the pair | accepted |
| 6 | Low | `trim()` keeps zero-width characters | use cases | Fixed — `visibleTextOrNull` ignores format (Cf) characters when deciding emptiness; also used for the Reading | ✅ `MessageTextTest` |
| 7 | Low | `changes()` redundant (same as Cycle 1 #3) | | Fixed | ✅ |
| 8 | Low | Retry from the Recording keeps the stale transcript/codes during "Transcribing…" | `SqlMessageRepository.kt` | Not fixed — ticket 12 decides whether retry-from-Recording clears them | open → ticket 12 |
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 126 tests, 0 failures. `domain-model.md` and `grill-notes.md` (D7, D9, S3, S4, new S8) updated to the revised design.

### Cycle 3 — Integration, security & quality · 2026-10-10 18:00
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | A Message interrupted mid-pipeline (process death) is stuck: RECORDING/TRANSCRIBING/TRANSLATING only leave through FAIL, and nothing calls it | `MessageStatus.kt`, `MessageRepository.kt` | Not fixed here — only ticket 06 creates such Messages. Recorded as invariant **S9**: ticket 06 runs a startup sweep marking them FAILED before any attempt | open → requirement for 06 |
| 2 | Medium | Codes not checked against the Conversation's pair; `require` would throw on network-shaped codes | `SaveTranscriptionUseCase.kt` | Not fixed — codes come from detection (07), which picks them from the pair, never from raw network text. KDoc now states this contract | accepted; enforce in 07 |
| 3 | Medium | No length limit: a runaway STT/LLM response could exceed Android's CursorWindow and crash Home on every launch | use cases | Fixed — `MAX_MESSAGE_TEXT_LENGTH = 10_000`; over-long transcript/translation → not applied; over-long Reading dropped | ✅ `overLongTextIsNotAppliedAndOverLongReadingIsDropped` |
| 4 | Low | START_TRANSCRIPTION didn't require a Recording | `Message.sq` | Fixed — `startTranscription` query adds `audioPath IS NOT NULL` (fake too) | ✅ `transcriptionCannotStartWithoutARecording`, SQL test |
| 5 | Low | Unknown stored status reads as FAILED but guards compare raw text | | Not fixed — only after a downgrade | open, Low |
| 6 | Low | Two transition tables (`canMoveTo` and `MessageStep`) | `MessageStatus.kt` | Fixed — `canMoveTo` is derived from `MessageStep`; the matrix test pins the table | ✅ |
| 7 | Low | `observeSummaries` re-emits identical lists after refused steps | `SqlConversationRepository.kt` | Fixed — `distinctUntilChanged()` | ✅ `refusedStepDoesNotReEmit` |
| 8 | Low | `visibleTextOrNull` kept edge format chars and treated control chars as visible | `MessageText.kt` | Fixed — trims whitespace, format and control characters from both ends | ✅ `MessageTextTest` |
| 9 | Low | Boolean result conflates silence / refused / missing | | Not fixed — every false leads ticket 06 to the same action (markFailed, itself refused if the Message moved on); documented in KDoc | accepted |
| 10 | Low | Stale docs | docs | Fixed — test-cases TC-06/12/13, plan revision note (+ plan.txt), grill D8/D10, S9, GLOSSARY wording | ✅ |
| 11 | Low | Test gaps: retry with a null code, refused step re-emission, commonTest never run on Native | tests | Fixed — two SQL tests added; `./gradlew :shared:iosSimulatorArm64Test` run: 93 commonTest tests pass on Kotlin/Native | ✅ |
- Two existing fixtures inserted RECORDING without a Recording and now (correctly) couldn't start transcription; they were given an `audioPath`.
- Verification: `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 :shared:iosSimulatorArm64Test` → BUILD SUCCESSFUL; 130 host tests and 93 iOS-simulator tests, 0 failures.

**Stopping rule applied:** cycle 3 found no Critical/High issues → stopped after 3 cycles. (Cycle 2's two High findings were latent until tickets 06/12; one fixed, one turned into invariant S8.)

## Final Report

**Outcome:** ✅ Done with open issues. The rules are in place; two invariants (S8 one attempt per Message, S9 startup sweep) are explicit requirements for ticket 06/12. No UI change. Not run on a device.

### Changes
- Step 1: `domain/model/Language.kt` (+`requiresReading`), `LanguageCatalog.kt` (flags), `domain/model/Reading.kt` (created), `domain/model/MessageText.kt` (created: `visibleTextOrNull`, `MAX_MESSAGE_TEXT_LENGTH`)
- Step 2: `domain/model/MessageStatus.kt` (`MessageStep` guards; `canMoveTo` derived)
- Step 3: `Message.sq` (+`selectById`, `selectConversationId`, guarded `updateStatus`/`startTranscription`/`saveTranscription`/`retryTranslation`/`saveTranslation`); `domain/repository/MessageRepository.kt`; `data/repository/SqlMessageRepository.kt` (rewritten around `applyStep`); `data/repository/SqlConversationRepository.kt` (`distinctUntilChanged`)
- Step 4: `domain/usecase/{MarkMessageTranscribing,SaveTranscription,RetryTranslation,SaveTranslation,MarkMessageFailed}UseCase.kt` (created); `di/SharedModule.kt`
- Step 5: commonTest `domain/model/{LanguageCatalogTest (modified),ReadingTest,MessageStatusTest,MessageTextTest}.kt`, `domain/usecase/MessageStepUseCasesTest.kt`, `testing/TestSupport.kt` (stateful `FakeMessageRepository`); androidHostTest `SqlRepositoriesTest.kt` (+9)
- Docs: `GLOSSARY.md` (+Message Status); ticket docs revised to match.

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` | BUILD SUCCESSFUL — 130 tests, 0 failures (96 before) |
| `./gradlew :shared:iosSimulatorArm64Test` | BUILD SUCCESSFUL — 93 commonTest tests on Kotlin/Native, 0 failures |
| `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| Device run | Not run (no UI change; smoke check below) |

### Deviations & Assumptions
- Per-step guards (`MessageStep`) instead of guards derived per target status; explicit `retryTranslation` step; transcription needs a Recording.
- Blank/zero-width/over-long text returns false instead of throwing; codes still `require` (they come from the Conversation's pair via detection).
- No schema change, so no migration risk.

### Open Issues & Risks
- **S8 (for 06/12):** at most one attempt per Message; cancel and join before retry. The guards can't tell a stale attempt's step from the current one.
- **S9 (for 06):** startup sweep marking interrupted Messages FAILED, otherwise they show a status forever with no Retry.
- Retry-from-Recording keeps the old transcript visible until replaced (ticket 12 decides).
- Unknown stored status reads as Failed but can't be retried (downgrade only).
- The Reading decision reads the target outside the write transaction (safe under S8).

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Chat and Home unchanged (TC-07, TC-08) | Clear app data, launch debug, open the top conversation, then Home. | Same bubbles and list as ticket 04: italic Reading only under the Mandarin Chinese translation; four conversations newest first. | The repository was rewritten and both screens read through it; a regression would break the app's two main screens. |
| 2 | Existing debug install still opens | Install over a ticket-04 debug build without clearing data. | App opens; data intact (no schema change). | Confirms no accidental schema or migration change reached installed databases. |

## Pull Request
`feature/05-message-structure` → `phase_1`, **stacked on `feature/04-chat-screen`** (merge 03, then 04, then this). `gh` isn't installed:
- While 04 is unmerged (diff shows only ticket 05): https://github.com/nasimnu14/DoVashi/compare/feature/04-chat-screen...feature/05-message-structure?expand=1
- After 03 and 04 are merged: https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/05-message-structure?expand=1

**Title:** feat(message): Message structure rules — Reading per target Language, guarded Message Status steps (ticket 05)

**Description:**
Adds the rules that keep a Message's structure valid:
- **Reading per target Language:** `Language.requiresReading` (Mandarin Chinese yes, English no). A Reading is stored only when the target Language needs one.
- **Guarded Message Status steps:** start transcription, save transcription, retry translation, save translation, fail. Each persists its parts immediately, never clears earlier ones, refuses out-of-order or late writes atomically, and bumps the Conversation's `updatedAt`.
- **Pipeline-ready use cases** for tickets 06 and 12.

No schema or UI change. Depends on ticket 04.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
