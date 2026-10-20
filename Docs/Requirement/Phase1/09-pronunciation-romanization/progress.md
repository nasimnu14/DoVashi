# Progress — 09 Pronunciation / Romanization

> /execute-plan · Plan: Docs/Requirement/Phase1/09-pronunciation-romanization/plan.md · Started: 2026-10-10 22:41
> Status: ✅ Complete · Last updated: 2026-10-10 23:20
> Baseline: `80e6896` (ticket 08); 196 host + 158 iOS-sim tests green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Metadata | ✅ Done | `domain/model/{Language,LanguageCatalog}.kt`, `GLOSSARY.md` | compile ✅ |
| 2 | Reading rule | ✅ Done | `domain/model/Reading.kt` | tests ✅ |
| 3 | Request + prompt | ✅ Done | `domain/service/TranslationService.kt`, `domain/usecase/TranslateMessageUseCase.kt`, `data/network/openai/OpenAiTranslationService.kt` | tests ✅ |
| 4 | Tests | ✅ Done | 5 test files updated to the new constructor/request shape; +3 new tests (catalog system, Latin kept, non-Latin dropped) and prompt assertions | 199 host + 161 iOS-sim, 0 failures |

## Step Log
### Steps 1–4 · ✅ Done
- `Language.readingSystem: String?` replaces the `requiresReading` constructor parameter; `requiresReading` is computed from it. Catalog: `zh` = "Hanyu Pinyin with tone marks".
- `readingFor` also requires every letter of the Reading to be Latin script (or a non-letter).
- `TranslationRequest.readingSystem` (`readingRequired` derived). The use case passes the target's system; the prompt adds `pronunciationSystem` and names it in the instruction.
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 199 host + 161 iOS-sim tests, 0 failures.

## Review Cycles
Small ticket: one fresh read-only reviewer covered the Cycle 1 (conformance) and Cycle 2 (edge cases) lenses in one pass, labelling each finding. Cycle 3 is a separate reviewer.

### Cycles 1–2 — Conformance & edge cases · 2026-10-10 23:00
| # | Lens | Severity | Finding | File:line | Action | Result |
|---|------|----------|---------|-----------|--------|--------|
| 1 | C2 | Medium | The Latin rule was a deny-list: letters of unlisted scripts (Zhuyin, half-width kana, Greek, Han outside the BMP via surrogates) passed, breaking R2 | `Reading.kt` | Fixed — allow-list: Latin (incl. diacritics), IPA/modifier letters, Latin Ext-C/D/E, full-width Latin; surrogates count as non-Latin | ✅ new ReadingTest cases |
| 2 | C2 | Low | A Reading with no letters ("—", "?") was stored | `Reading.kt` | Fixed — at least one letter required | ✅ |
| 3 | C1 | Low | The null-field assertion couldn't tell missing from null | test | Fixed — `assertEquals(JsonNull, …)` | ✅ |
| 4 | C1 | Low | TC-06 "Message still completes" not tested end to end | test | Fixed — `nonLatinReadingIsDroppedButTheMessageStillCompletes` | ✅ |
- Checked correct: 'ɑ', ü/ǖ–ǜ, Vietnamese letters, apostrophes and combining marks pass; `requiresReading` as a body property is fine for equality; no stale callers; R1/R3/R4 and AC1–AC3 hold; the seed's pinyin still shows.
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 202 host + 164 iOS-sim tests, 0 failures.

### Cycle 3 — Integration, security & quality · 2026-10-10 23:15
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | Every surrogate counted as non-Latin, so emoji ("Xièxie 👍") dropped the Reading | `Reading.kt` | Fixed — U+1F000–1FBFF (emoji/symbols) skipped as non-letters; other supplementary characters stay non-Latin | ✅ `emojiAreNotLetters` |
| 2 | Low | Docs described the old deny-list | docs | Fixed — plan banner (+plan.txt), domain model, grill D4, `SaveTranslationUseCase` KDoc, TC-10–12 | ✅ |
| 3 | Low | Progress log outside the ticket folder | | Moved into the ticket folder at the end of Step 8, as for every ticket | ✅ |
| 4 | Low | A blank `readingSystem` would count as needing a Reading | `Language.kt` | Fixed — `init { require(...) }` | ✅ test |
| 5 | Low | Second "Latin" definition in `Reading.kt`; Phonetic Extensions missing | `Script.kt` | Fixed — shared `Script.isLatinLetter` (adds U+1D00–1DBF); `Script.LATIN` (detection) unchanged | ✅ |
| 6 | Low | The real catalog value never went through the full path | test | Fixed — `realCatalogAsksForPinyinForMandarinChinese` | ✅ |
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 206 host + 168 iOS-sim tests, 0 failures.

**Stopping rule applied:** no Critical/High findings → stopped after 3 cycles.

## Final Report

**Outcome:** ✅ Done. Readings are driven by per-Language metadata (`readingSystem`), requested from OpenAI in that system, and stored only if English-readable. Live pinyin quality is unverified (no API key).

### Changes
- `domain/model/Language.kt` (`readingSystem`; `requiresReading` computed; blank rejected), `LanguageCatalog.kt` (`zh` pinyin), `Reading.kt` (Latin allow-list), `Script.kt` (`isLatinLetter`)
- `domain/service/TranslationService.kt` (`readingSystem` in the request), `domain/usecase/TranslateMessageUseCase.kt`, `domain/usecase/SaveTranslationUseCase.kt` (KDoc), `data/network/openai/OpenAiTranslationService.kt` (prompt names the system)
- Tests: `LanguageCatalogTest`, `ReadingTest`, `MessageStepUseCasesTest`, `TranslateMessageUseCaseTest`, `OpenAiTranslationServiceTest`
- `GLOSSARY.md` (Reading refined)

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` | BUILD SUCCESSFUL — 206 tests, 0 failures (196 before) |
| `./gradlew :shared:iosSimulatorArm64Test` | BUILD SUCCESSFUL — 168 tests, 0 failures |
| `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |

### Deviations & Assumptions
- `requiresReading` became a computed property (the constructor takes `readingSystem`).
- The request carries the system name instead of a boolean.

### Open Issues & Risks
- If a model returns a Reading in another script, it is silently dropped (the Message still completes). No logger exists to record it.

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Live pinyin (TC-05) | With a key, after ticket 06, speak English in an English ↔ Mandarin Chinese Conversation. | Pinyin with tone marks under the Chinese translation; no Reading under English translations. | Romanization quality depends on the model following the named system. |
| 2 | Chat regression (TC-04) | Open the seeded top conversation. | Italic "Nǐ jīntiān hǎo ma?" under "你今天好吗？". | Confirms nothing in the display path changed. |

## Pull Request
`feature/09-pronunciation-romanization` → `phase_1`, **stacked on `feature/08-openai-translation`** (merge order 03 → 04 → 05 → 07 → 08 → 09). `gh` isn't installed:
- While 08 is unmerged: https://github.com/nasimnu14/DoVashi/compare/feature/08-openai-translation...feature/09-pronunciation-romanization?expand=1
- After the earlier tickets are merged: https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/09-pronunciation-romanization?expand=1

**Title:** feat(reading): per-Language Reading system and Latin-script Reading rule (ticket 09)

**Description:**
- `Language.readingSystem` (Mandarin Chinese: Hanyu Pinyin with tone marks) is now the single metadata flag for whether a target needs a Reading, and names its style.
- OpenAI is asked for that system.
- A Reading is stored only if it is English-readable Latin script; otherwise it is dropped and the Message still completes.

Phase 2 only adds catalog entries. Depends on ticket 08.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
