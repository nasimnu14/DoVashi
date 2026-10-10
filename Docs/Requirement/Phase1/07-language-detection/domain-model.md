# Domain Model — 07-language-detection

Sources: `07-language-detection.md`, `13-technology-stack.md`, `15-openai-api-security.md`, `16-architecture-and-extensibility.md`, Phase 2 `02-language-detection-expansion.md`, `GLOSSARY.md`, `grill-notes.md`, the OpenAI API spec (transcriptions, `verbose_json`), code on `feature/05-message-structure`.

## 1. Entities & relationships

| Concept | Layer | Shape |
|---|---|---|
| `Script` | domain/model | enum LATIN, HAN, KANA, HANGUL, BENGALI, ARABIC, DEVANAGARI, CYRILLIC. `Script.of(char): Script?` by Unicode range. `scriptUnits(text): Map<Script, Int>`: word-like units (a run of an alphabetic script = 1, each Han/Kana character = 1). The resolver scores each Language on its own (non-shared) scripts |
| `Language.scripts` | domain/model | `Set<Script> = emptySet()`; catalog `en` {LATIN}, `zh` {HAN} |
| `MessageLanguages` | domain/model | `(sourceLanguage: String, targetLanguage: String)` Language Codes, both from the pair, distinct |
| `targetLanguageFor(source, conversation)` | domain/model | doc 07's formula |
| `Transcription` | domain/service | `(text: String, languageCode: String?)`; `text` is empty for silence |
| `SpeechToTextService` | domain/service | `suspend fun transcribe(recordingReference: String): Transcription`; throws `SpeechToTextException` |
| `SpeechToTextException` | domain/service | `(reason: Reason)`: MISSING_API_KEY, RECORDING_UNAVAILABLE, RECORDING_REJECTED (over 25 MB, 400/413/415, transcript over 10,000 chars — retry won't help), UNAUTHORIZED, RATE_LIMITED, SERVER, NETWORK, INVALID_RESPONSE |
| `ResolveMessageLanguagesUseCase` | domain/usecase | `(conversation, transcription, languageByCode = LanguageCatalog::byCode): MessageLanguages?` |
| `FileStorage.size(reference): Long?`, `read(reference): ByteArray?` | audio | Same reference rules as `resolve` |
| `OpenAiConfig` | data/network/openai | `(apiKey: String, baseUrl: String = "https://api.openai.com/v1")` |
| `OpenAiSpeechToTextService` | data/network/openai | `(httpClient, config, fileStorage, ioDispatcher)`: size check, multipart POST `{baseUrl}/audio/transcriptions`, Whisper's no-speech heuristic → empty transcript |
| `whisperLanguageCode(value): String?` | data/network/openai | Whisper name or code → Language Code |
| `createHttpClient(engine)` | data/network | Ktor client with JSON and timeouts |

```
Recording ref ──► SpeechToTextService ──► Transcription(text, code?)
Conversation(language1Code, language2Code) + Transcription ──► ResolveMessageLanguagesUseCase
      ├─ code ∈ pair ─────────────────────────┐
      ├─ higher score on own scripts ─────────┤──► source ──► targetLanguageFor ──► MessageLanguages
      └─ otherwise ──► null (pipeline fails the Message)
```

## 2. Bounded context / ownership
- **domain** owns the rule (resolution, target formula, scripts) and the STT port. It has no network types.
- **data/network** owns the HTTP client and the OpenAI adapter, including the provider-specific Whisper name table and error mapping.
- **androidMain/androidApp** provide the OkHttp engine, the API key (BuildConfig from `local.properties`/env) and `FileStorage.read`.
- **di** binds `SpeechToTextService` → `OpenAiSpeechToTextService`, the `HttpClient` (single) and `OpenAiConfig`.
- **Ticket 06** calls `transcribe` + `ResolveMessageLanguagesUseCase` + `SaveTranscriptionUseCase`.

## 3. Invariants

| ID | Invariant | Enforced by |
|---|---|---|
| L1 | Source and target are always the Conversation's two Languages, and distinct | Resolver only returns pair codes; `targetLanguageFor` |
| L2 | No branch on specific Language Codes or names anywhere in detection | Data-driven: pair, `scripts` metadata, Whisper table |
| L3 | The user never selects the source | No UI input; resolution only |
| L4 | Undetermined → null, never a guess | Resolver step 3 |
| L5 | The API key never appears in logs, exceptions or committed files | Adapter error messages; `local.properties` is gitignored |
| L6 | A missing key, a missing Recording or one over 25 MB fails fast without a network call | Adapter preconditions |
| L7 | Any STT failure surfaces as `SpeechToTextException` (never a raw Ktor/IO exception) | Adapter catches and maps, except `CancellationException` |
