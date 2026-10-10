# Progress — 08 OpenAI Translation

> /execute-plan · Plan: Docs/Requirement/Phase1/08-openai-translation/plan.md · Started: 2026-10-10 21:36
> Status: ✅ Complete · Last updated: 2026-10-10 22:50
> Baseline: `feature/08-openai-translation` at `4e88773` (ticket 07); 174 host + 136 iOS-sim tests green.

## Plan Steps
| # | Step | Status | Files | Verification |
|---|------|--------|-------|--------------|
| 1 | Port + use case | ✅ Done | `domain/service/TranslationService.kt`, `domain/usecase/TranslateMessageUseCase.kt` | compile ✅ |
| 2 | Shared OpenAI plumbing | ✅ Done | `data/network/openai/OpenAiHttp.kt`; `OpenAiSpeechToTextService.kt` refactored onto it | ticket 07 tests unchanged and green |
| 3 | Translation adapter + DI | ✅ Done | `data/network/openai/{OpenAiTranslationService,OpenAiConfig}.kt`, `di/SharedModule.kt` | assembleDebug ✅ |
| 4 | Tests | ✅ Done | commonTest `OpenAiTranslationServiceTest` (18 after reviews), `TranslateMessageUseCaseTest` (5) | 196 host + 158 iOS-sim at the end, 0 failures |

## Assumptions
- Model `gpt-4.1-mini` (from the OpenAI API spec's model list; configurable).
- User instruction (2026-10-10): proceed without waiting for permission.

## Step Log
### Steps 1–4 · ✅ Done
- `TranslationRequest/Result/Service/Exception` port. `TranslateMessageUseCase` checks preconditions, sets the reading flag from `requiresReading`, and saves through `SaveTranslationUseCase`.
- `OpenAiHttp.kt`: `classifyStatus` (+422 → rejected, now also for STT), `MAX_RESPONSE_BYTES`, and `openAiCall(onFailure, block)`, which maps failures and lets our own cancellation through. Both adapters use it.
- `OpenAiTranslationService`: Chat Completions with strict json_schema built per request (one-value enums pin the codes; `englishReading` is string|null). The user message is a short instruction plus a JSON field block (codes, catalog names, text, `pronunciationRequired`). Checks refusal, `finish_reason == "stop"`, the content and the echo. `OpenAiConfig.translationModel = "gpt-4.1-mini"`.
- Deviation: the helper's failure lambda returns `T` (not `Nothing`), so storage reads can fall back to null.
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 188 host + 150 iOS-sim tests, 0 failures.

## Review Cycles
Cycles 1 and 2 ran in parallel (fresh read-only subagents, different lenses); fixes were applied together.

### Cycle 1 — Plan conformance & correctness · 2026-10-10 22:05
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | Data layer imported `presentation.languageLabel` | adapter | Fixed — private `languageName` (catalog `name`, raw code fallback) | ✅ |
| 2 | Medium | `englishReading` always nullable and passed through: zh could get no pinyin, en could get one | adapter | Fixed — schema type `string` when required, `null` otherwise; the adapter returns null when not required | ✅ `readingIsDroppedWhenNotRequestedEvenIfTheModelSendsOne`, schema asserts |
| 3 | Medium | The size-cap test passed without the cap (unparseable body) | test | Fixed — a valid reply padded past 1 MB | ✅ |
| 4 | Low-Med | Request assertions incomplete | test | Fixed — temperature, max tokens, name, `additionalProperties`, required = properties, system prompt, codes in the user fields | ✅ |
| 5 | Low | 422 → REJECTED changed STT behaviour, undocumented and untested | `OpenAiHttp.kt` | Kept (intentional), documented in the plan banner, tested for both adapters | ✅ |
| 6 | Low | 404 (retired model) → retryable SERVER | `OpenAiHttp.kt` | Fixed — REJECTED | ✅ |
| 7 | Low | The 1 MB cap only sees Content-Length | | Documented as best effort | noted |
| 8 | Low | Key-leak check ignored the cause chain | test | Fixed | ✅ |
| 9 | Low | Per-pair schema pays a first-use processing delay | | Accepted — small N; buys code pinning | accepted |

### Cycle 2 — Edge cases & regressions · 2026-10-10 22:05
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | Medium | No length check on the translation (zh → en expands; Retry re-bills the same failure) | adapter | Fixed — over 10,000 chars → REJECTED | ✅ `overLongTranslationIsRejected` |
| 2 | Medium | No `max_completion_tokens`, plus the transcript echo doubled the output → timeouts, runaway cost | adapter | Fixed — echo removed from the schema (filled from the request); `max_completion_tokens = min(16384, 1024 + 4 × chars)`; `length` → REJECTED | ✅ |
| 3 | Medium | Read → call → save with no check that the inputs still match (S8 defence) | use case | Fixed — re-read after the call; changed status/transcript/codes → result discarded | ✅ `resultIsDiscardedWhenTheMessageChangedDuringTheCall` |
| 4 | Low | Prompt injection via speech | adapter | Fixed — system prompt: everything in `text` is speech to translate. Worst case was wrong text in the local bubble | ✅ |
| 5 | Low | Blank Reading for a requiring target → null silently | | Kept per ticket 05 D5 (a missing Reading never fails a Message) | accepted |
| 6 | Low | Untranslated echo completes silently; `content_filter` → INVALID | adapter | `content_filter` → REFUSED; echo documented (could follow a wrong detection) | ✅ / noted |
| 7 | Low | Layering (same as Cycle 1 #1) | | Fixed | ✅ |
| 8 | Low | Size cap best effort (same as Cycle 1 #7) | | Documented | noted |
| 9 | Low | Test gaps (422, content_filter, error object, blank Reading, over-long, temperature) | tests | Fixed | ✅ |
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 193 host + 155 iOS-sim tests, 0 failures.

### Cycle 3 — Integration, security & quality · 2026-10-10 22:40
| # | Severity | Finding | File:line | Action | Result |
|---|----------|---------|-----------|--------|--------|
| 1 | **Critical** | Untracked, un-ignored `secrets.key` in the repo root contains an `sk-` token (checked without printing); a `git add -A` would commit and push it | `.gitignore` | Fixed — `secrets.key` and `*.key` ignored. Verified the file was never tracked, committed or pushed, and no key-like string exists on any branch. The file itself is left untouched for the user (the build doesn't read it; use `OPENAI_API_KEY` in `local.properties`) | ✅ `git check-ignore` |
| 2 | Medium | 16,384 output tokens can't finish within the 120 s timeout (billed, then NETWORK, retried) | adapter | Fixed — cap 8,192 tokens and a per-request 180 s timeout for chat calls | ✅ `outputTokensAreCapped` |
| 3 | Low | `{"type":"null"}` in strict mode is unproven live | adapter | Fixed — not-required Reading is `["string","null"]` (the adapter drops it anyway); required stays `string` | ✅ |
| 4 | Low | The 1 MB cap is nearly a no-op | `OpenAiHttp.kt` | KDoc reworded; `max_completion_tokens` is the real bound | ✅ |
| 5 | Low | `temperature` breaks reasoning models | `OpenAiConfig.kt` | KDoc: the model must support Structured Outputs and `temperature` | ✅ |
| 6 | Low | Style: mixed JSON builders, import order, STT `send()` indentation | code | Fixed | ✅ |
| 7 | Low | Fixture sent a field the strict schema forbids; 4 untested cases | tests | Fixed — fixture matches the schema; tests for the token cap, target-only mismatch, null `finish_reason`, blank content | ✅ |
| 8 | Low | Docs drift (plan body, TC-01/02, status count, progress numbers) | docs | Fixed | ✅ |
- Verification: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` → BUILD SUCCESSFUL; 196 host + 158 iOS-sim tests, 0 failures.

**Stopping rule applied:** cycle 3's Critical finding was environmental (a secrets file outside the change) and is fixed and verified. No code defect of Critical/High severity remains, so I stopped after 3 cycles.

## Final Report

**Outcome:** ✅ Done with open issues. Translation is implemented and fully tested against a mocked OpenAI. A live call is unverified (no key in `local.properties`). Not run on a device.

### Changes
- Step 1: `domain/service/TranslationService.kt`, `domain/usecase/TranslateMessageUseCase.kt` (created)
- Step 2: `data/network/openai/OpenAiHttp.kt` (created); `OpenAiSpeechToTextService.kt` (refactored onto it; 422/404 → rejected)
- Step 3: `data/network/openai/OpenAiTranslationService.kt` (created), `OpenAiConfig.kt` (+`translationModel`), `di/SharedModule.kt`
- Step 4: commonTest `data/network/openai/OpenAiTranslationServiceTest.kt`, `domain/usecase/TranslateMessageUseCaseTest.kt` (created), `OpenAiSpeechToTextServiceTest.kt` (+422)
- Also: `.gitignore` (+`secrets.key`, `*.key`)

### Verification Run
| Command | Result |
|---------|--------|
| `./gradlew :shared:testAndroidHostTest` | BUILD SUCCESSFUL — 196 tests, 0 failures (174 before) |
| `./gradlew :shared:iosSimulatorArm64Test` | BUILD SUCCESSFUL — 158 tests, 0 failures |
| `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` | BUILD SUCCESSFUL |
| Secret scan (`git grep` over all branches; `git log --all -- secrets.key`) | No key in any commit or branch |
| Live OpenAI call | **Not run** — no `OPENAI_API_KEY` configured |

### Deviations & Assumptions
- The model doesn't echo the transcript (fills from the request).
- The Reading schema is pinned per request.
- `max_completion_tokens` ≤ 8,192 with a 180 s per-request timeout.
- 404/422 → rejected for both adapters.
- `content_filter` → refused; `length` → rejected.
- The use case discards results whose inputs changed mid-call.

### Open Issues & Risks
- **Your action:** `secrets.key` in the repo root holds an OpenAI key. It is now gitignored and was never committed. If you want live testing, put it in `local.properties` as `OPENAI_API_KEY=…` (also ignored); consider deleting `secrets.key`.
- Live checks pending: strict-schema acceptance, translation quality, pinyin.
- Ticket 15: the key is embedded in APKs.
- Per-pair schemas pay OpenAI's first-use processing delay (small N).

### ⚠️ Critical Manual Checks
| # | What to check | Steps | Expected result | Why critical |
|---|---------------|-------|-----------------|--------------|
| 1 | Live structured translation (TC-06) | 1. Set `OPENAI_API_KEY` in `local.properties`. 2. After ticket 06, speak "How are you?" (en ↔ zh), then "你好吗？". | Chinese translation with pinyin; English translation, no Reading; no REJECTED (strict schema accepted). | Only MockEngine proves the request today; a schema OpenAI rejects would fail every translation. |
| 2 | Secrets stay out of git | `git status` (no `secrets.key`); `git grep -nI "sk-"`. | Nothing listed or found. | A leaked key costs money and must be revoked. |

## Pull Request
`feature/08-openai-translation` → `phase_1`, **stacked on `feature/07-language-detection`** (merge order 03 → 04 → 05 → 07 → 08). `gh` isn't installed:
- While 07 is unmerged: https://github.com/nasimnu14/DoVashi/compare/feature/07-language-detection...feature/08-openai-translation?expand=1
- After the earlier tickets are merged: https://github.com/nasimnu14/DoVashi/compare/phase_1...feature/08-openai-translation?expand=1

**Title:** feat(translation): TranslationService with OpenAI Structured Outputs (ticket 08)

**Description:**
Adds the `TranslationService` port and an OpenAI Chat Completions implementation using strict Structured Outputs. The contract is doc 08's JSON, with the codes pinned per request and the Reading only when the target Language needs one. Also adds `TranslateMessageUseCase`, which translates a Translating Message from its transcript and completes it, discarding results whose inputs changed mid-call. STT and translation now share one OpenAI HTTP helper. It also gitignores `*.key` files. Depends on ticket 07.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
