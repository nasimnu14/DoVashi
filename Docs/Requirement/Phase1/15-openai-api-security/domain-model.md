# Domain Model — 15-openai-api-security

| Concept | Layer | Role |
|---|---|---|
| `TranslationService`, `SpeechToTextService` | domain/service | Ports; the only OpenAI surface the app depends on |
| `OpenAiTranslationService`, `OpenAiSpeechToTextService` | data/network/openai | Adapters; bound only in `SharedModule` |
| `OpenAiConfig(apiKey, baseUrl)` | data/network/openai | Key from untracked build config; `toString` redacts the key |
| `createHttpClient(engine, trafficLogger?)` | data/network | Installs Ktor `Logging` only when a logger is given; masks `Authorization` |
| `LogcatHttpLogger` | androidMain/data/network | Logcat sink, bound only for debuggable builds |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| S1 | No API key in source or Git | `NoHardcodedSecretsTest` + `.gitignore` |
| S2 | Callers depend on the ports, not OpenAI classes | DI binding (ticket 16 guard) |
| S3 | Release builds never log HTTP traffic | Logger bound only when `FLAG_DEBUGGABLE` |
| S4 | The key never appears in logs | `sanitizeHeader` + `OpenAiConfig.toString` |
| S5 | A proxy replaces OpenAI by DI change only | Ports + `baseUrl` |
