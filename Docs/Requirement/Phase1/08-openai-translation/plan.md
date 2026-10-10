# Plan — 08-openai-translation

> **Revised during review (see progress.md):**
> - Schema without the transcript echo; `englishReading` is `string` or `null` per request; `max_completion_tokens` set.
> - Mappings: 404, `length` and over-long output → REJECTED; `content_filter` → REFUSED.
> - The use case re-reads the Message before saving.
> - 422 → REJECTED now also applies to STT.

Inputs: `grill-notes.md` (D1–D11), `domain-model.md` (T1–T6). Paths under `shared/src/commonMain/kotlin/com/example/dovashiapp/`.

## Step 1 — Port and use case
- `domain/service/TranslationService.kt`: request, result, port, exception + reasons.
- `domain/usecase/TranslateMessageUseCase.kt` per D10. The reading flag is `languageByCode(target)?.requiresReading == true`.

## Step 2 — Shared OpenAI plumbing (`data/network/openai/OpenAiHttp.kt`, internal)
- `enum OpenAiStatus { OK, UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER }`; `classifyStatus(code)` (2xx OK; 401/403; 429; 400/404/413/415/422 rejected; else server).
- `suspend inline fun <T> openAiCall(onFailure: (Exception) -> T, block: () -> T): T` catches `Exception`, calls `ensureActive()`, then `onFailure`.
- `MAX_RESPONSE_BYTES` (1 MB, a Content-Length sanity check only).
- Refactor `OpenAiSpeechToTextService` onto these (same reasons, same tests).

## Step 3 — OpenAI translation adapter
- `OpenAiConfig`: `translationModel: String = "gpt-4.1-mini"` (`toString` still redacts the key).
- `OpenAiTranslationService(httpClient, config, languageByCode = LanguageCatalog::byCode)`:
  - Blank key → MISSING_API_KEY.
  - POST `{baseUrl}/chat/completions` (per-request timeout 180 s), bearer, JSON body (`@Serializable` DTOs): `model`, `temperature` 0.2, `max_completion_tokens = min(8192, 1024 + 4 × chars)`, `messages` [system, user], `response_format` = json_schema `translation` strict.
  - Schema properties: `sourceLanguage` enum [source], `targetLanguage` enum [target], `translatedText` string, `englishReading` (`string` when required, else `["string","null"]`); all required; `additionalProperties` false.
  - The transcript isn't echoed by the model: the result takes it from the request.
  - Statuses map to reasons. Decode `choices[0]`:
    - `refusal` or `content_filter` → REFUSED
    - `length` → REJECTED
    - other non-"stop" or blank `content` → INVALID_RESPONSE
  - Decode the content as the contract. A code mismatch → INVALID_RESPONSE; a translation over 10,000 chars → REJECTED; the Reading is null when not required.
  - Returns `TranslationResult`.
- DI: `single<TranslationService> { OpenAiTranslationService(get(), get()) }`, `factory { TranslateMessageUseCase(get(), get(), get()) }`.

## Step 4 — Tests
- `OpenAiTranslationServiceTest` (MockEngine):
  - The request has the model, temperature, strict schema with one-value enums and nullable `englishReading`, a user message carrying codes, names, text and the flag, and the bearer header.
  - Parses the contract both directions; `englishReading` null.
  - Reasons: blank key (no request), 401, 429, 400 → REJECTED, 500, transport → NETWORK, cancellation passes through, refusal → REFUSED, finish_reason "length" → INVALID_RESPONSE, empty choices, bad JSON content, echo mismatch, a response over 1 MB.
- `TranslateMessageUseCaseTest` (fake repository + fake service):
  - The reading flag follows the target's metadata (test catalog).
  - Saves the translation → COMPLETED and drops an unneeded Reading.
  - Not TRANSLATING / missing transcript / missing codes / unknown id → false with no call.
  - A service exception propagates, leaving the Message TRANSLATING.
- STT tests still pass after the refactor.
- Verify: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. OpenAI receives source code and name, target code and name, the transcript, and whether a Reading is required, as structured data.
2. The response is doc 08's JSON; codes echo the request; `englishReading` is null when not required.
3. Calling code has no English/Chinese literals; the same contract works for any pair.
4. OpenAI is reached only through `TranslationService`; failures are typed; no key or text leaks into errors.
5. Translating a Translating Message stores the translation (and Reading only if needed) and completes it.
6. Tests pass on host and iOS simulator; builds succeed.
