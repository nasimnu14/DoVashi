# Plan — 07-language-detection

> **Revised during review (see progress.md):**
> - `dominantScript` became `scriptUnits` (word-like units), and the resolver scores each Language on its non-shared scripts.
> - Codes are compared by base subtag on both sides (exactly one match), and identical pair codes → null.
> - The adapter now:
>   - takes an IO dispatcher and checks `FileStorage.size` (over 25 MB → RECORDING_REJECTED)
>   - maps 400/413/415 and transcripts over 10,000 characters to RECORDING_REJECTED
>   - turns Whisper-detected silence into an empty transcript
>   - caps the response at 1 MB
> - Our own cancellation propagates.
> - The 4-argument DI binding and the explicit INTERNET permission are added.

Inputs: `grill-notes.md` (D1–D15), `domain-model.md` (L1–L7). No UI. Verify on Android, the iOS compile and iOS-simulator commonTest.
Paths are under `shared/src/commonMain/kotlin/com/example/dovashiapp/` unless noted.

## Step 0 — Preconditions
- Base has ticket 05 (`Language.requiresReading`, step use cases, `FileStorage.resolve`). Baseline: 130 host tests green.

## Step 1 — Dependencies
- Version catalog: Ktor 3.6.0 (`ktor-client-core`, `-content-negotiation`, `-okhttp`, `-mock`), `ktor-serialization-kotlinx-json`, `kotlinx-serialization-json` 1.11.0.
- `:shared`: core, content-negotiation and serialization-json in commonMain; okhttp in androidMain; mock and serialization-json in commonTest.
- `:androidApp`: `buildFeatures.buildConfig = true`; `buildConfigField("String", "OPENAI_API_KEY", …)` from `local.properties` `OPENAI_API_KEY`, else env var `OPENAI_API_KEY`, else `""`, escaped as a Java string literal.
- Verify: `./gradlew :androidApp:assembleDebug`.

## Step 2 — Domain: scripts, pair resolution
- `domain/model/Script.kt`: enum with Unicode ranges (LATIN: Basic Latin/Latin-1/Extended letters; HAN: CJK Unified + Ext A + Compatibility; KANA: Hiragana, Katakana; HANGUL: syllables + jamo; BENGALI; ARABIC; DEVANAGARI; CYRILLIC). `Script.of(Char): Script?` (non-letters → null). `dominantScript(text): Script?` (tie or no letters → null).
- `domain/model/Language.kt`: `scripts: Set<Script> = emptySet()`; catalog `en` {LATIN}, `zh` {HAN}.
- `domain/model/MessageLanguages.kt`: data class plus `targetLanguageFor(source: String, conversation: Conversation): String`.
- `domain/service/SpeechToTextService.kt`: `Transcription`, `SpeechToTextService`, `SpeechToTextException(reason)` + `Reason` enum.
- `domain/usecase/ResolveMessageLanguagesUseCase.kt` per D10: normalise the STT code (lowercase, base subtag); pair membership; else the dominant script matched against each pair Language's `scripts` from `languageByCode`; exactly one match → source; build `MessageLanguages(source, targetLanguageFor(...))`.

## Step 3 — Network and OpenAI adapter
- `audio/FileStorage.kt`: add `read(reference): ByteArray?`; `AndroidFileStorage.read` = `resolve(reference)?.let { File(it).readBytes() }`, catching IOException → null.
- `data/network/HttpClientFactory.kt`: `createHttpClient(engine: HttpClientEngine): HttpClient` with `ContentNegotiation { json(Json { ignoreUnknownKeys = true }) }`, `HttpTimeout(connect 15 s, request 120 s, socket 120 s)`, `expectSuccess = false`.
- `data/network/openai/OpenAiConfig.kt`.
- `data/network/openai/WhisperLanguages.kt`: name → code table (Whisper's list) + `whisperLanguageCode(value)`.
- `data/network/openai/OpenAiSpeechToTextService.kt`:
  - Blank key → MISSING_API_KEY.
  - `fileStorage.read` null → RECORDING_UNAVAILABLE.
  - `submitFormWithBinaryData("$baseUrl/audio/transcriptions")` with `model=whisper-1`, `response_format=verbose_json`, and `file` (filename = last path segment; `Content-Type` by extension); bearer auth header.
  - Status: 401/403 → UNAUTHORIZED, 429 → RATE_LIMITED, other non-2xx → SERVER.
  - Body `{text, language}` via `@Serializable` DTO; missing or unparseable → INVALID_RESPONSE.
  - `IOException`/timeout → NETWORK. `CancellationException` rethrown.
  - Returns `Transcription(text.trim(), whisperLanguageCode(language))`.
- DI: `sharedModule` binds `single { createHttpClient(get()) }`, `single<SpeechToTextService> { OpenAiSpeechToTextService(get(), get(), get()) }`, `factory { ResolveMessageLanguagesUseCase() }`. `androidPlatformModule` provides `HttpClientEngine` (`OkHttp.create()`) and `OpenAiConfig(apiKey)`; `androidPlatformModule(context, openAiApiKey)` receives the key from `DoVashiApplication` (`BuildConfig.OPENAI_API_KEY`).

## Step 4 — Tests
- commonTest `ScriptTest`: classification of sample chars per script; non-letters ignored; dominant script for pure, mixed ("我用iPhone" → HAN), tie → null, empty → null.
- commonTest `ResolveMessageLanguagesUseCaseTest` (test catalog A {LATIN}, B {HAN}, C {LATIN}): STT code = Language 1 → (1, 2); = Language 2 → (2, 1); region/uppercase subtags normalised; third-language code → script fallback; null code → script; same-script pair with an unknown code → null; no letters → null; Language without scripts → null; target formula both ways.
- commonTest `WhisperLanguagesTest`: english → en, chinese → zh, "Japanese" → ja, "zh-CN" → zh, unknown → null, blank → null.
- commonTest `OpenAiSpeechToTextServiceTest` (`MockEngine`, in-memory `FileStorage` fake): URL, `Authorization: Bearer k`, multipart contains model/response_format/file with filename; parses text and language; 401 → UNAUTHORIZED; 429; 500 → SERVER; malformed JSON → INVALID_RESPONSE; engine throws IOException → NETWORK; blank key → MISSING_API_KEY with zero requests; missing Recording → RECORDING_UNAVAILABLE with zero requests.
- androidHostTest `AndroidFileStorageTest`: `read` returns bytes, null for missing/escaping.
- Verify: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`.

## Acceptance criteria
1. Given a Conversation and a Recording, the system yields a transcript and resolves source/target Language without user input (STT detection first, script fallback second).
2. "Good morning" in an English ↔ Mandarin Chinese Conversation → source English, target Mandarin Chinese; "你好" → source Mandarin Chinese, target English (either Language order).
3. The target is computed only as "the other Language of the pair"; detection code contains no Language literals.
4. Undetermined input yields no guess (null).
5. OpenAI is reached only through `SpeechToTextService`; the key comes from untracked config, never from source; a missing key fails without a network call; failures surface as `SpeechToTextException`.
6. All Step 4 tests pass on host and iOS simulator; `assembleDebug` and the iOS compile succeed.
