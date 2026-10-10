# Domain Model — 08-openai-translation

| Concept | Layer | Shape |
|---|---|---|
| `TranslationRequest` | domain/service | `(sourceLanguage, targetLanguage, transcribedText, readingRequired: Boolean)` |
| `TranslationResult` | domain/service | `(sourceLanguage, targetLanguage, transcribedText, translatedText, reading: String?)` |
| `TranslationService` | domain/service | `suspend fun translate(request): TranslationResult`, throws `TranslationException` |
| `TranslationException` | domain/service | `(reason)`: MISSING_API_KEY, UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER, NETWORK, INVALID_RESPONSE, REFUSED |
| `TranslateMessageUseCase` | domain/usecase | `(messageRepository, translationService, saveTranslation, languageByCode)`, `invoke(messageId): Boolean` |
| `OpenAiConfig.translationModel` | data/network/openai | default `gpt-4.1-mini` |
| `OpenAiTranslationService` | data/network/openai | `(httpClient, config, languageByCode)`; Chat Completions + strict json_schema |
| `openAiCall` / `classifyStatus` | data/network/openai (internal) | Shared by STT and translation |

```
Message(TRANSLATING, transcript, source, target)
  └─ TranslateMessageUseCase ─► TranslationRequest(codes, text, readingRequired = target.requiresReading)
        └─ TranslationService (OpenAI chat, strict schema) ─► TranslationResult
              └─ SaveTranslationUseCase (blank/long refused, Reading only if needed) ─► COMPLETED
```

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| T1 | Request and response carry Language Codes as data; same shape for every pair | Port types; per-request schema |
| T2 | The response's codes echo the request's | Schema enums + adapter check |
| T3 | `readingRequired` comes from target metadata, never from a code comparison | Use case via `requiresReading` |
| T4 | OpenAI is reached only through `TranslationService` | DI; no other callers |
| T5 | Failures surface as `TranslationException`; own cancellation propagates | Shared `openAiCall` |
| T6 | Only a TRANSLATING Message with transcript and codes is sent | Use case preconditions |
