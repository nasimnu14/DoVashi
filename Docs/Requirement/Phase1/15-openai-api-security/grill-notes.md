# Grill Notes — 15-openai-api-security

Source: `Docs/Requirement/Phase1/15-openai-api-security.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Repo state: `TranslationService` and `SpeechToTextService` are domain ports (`domain/service`); only `SharedModule` references the OpenAI adapters. The key comes from untracked `local.properties`/env into `BuildConfig` (ticket 07 D9). `OpenAiConfig.baseUrl` is configurable. `secrets.key` and `local.properties` are git-ignored. Uncommitted work in the tree adds debug-only HTTP traffic logging.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What is left? | (a) A regression guard that no key is hard-coded or tracked; (b) land the debug-only HTTP logging so it cannot leak the key or ship in release; (c) iOS secrets file ignored by Git (plumbing is ticket 14); (d) document key setup, the MVP trade-off and the proxy path. (self) |
| D2 | Ports already isolate OpenAI? | Yes. Callers (use cases, view models) depend on the two interfaces only; adapters are bound in `SharedModule`. No code change; a guard test in ticket 16 will lock the dependency direction. (self) |
| D3 | Backend proxy readiness? | Swap the two bindings (or `OpenAiConfig.baseUrl`) in DI; nothing else changes. Not building a proxy or extra config. (self) |
| D4 | Hard-coded-key guard? | A host test scans `shared/src`, `androidApp/src`, `iosApp` for `sk-…` tokens and checks `.gitignore` lists `local.properties` and `*.key`. Cheap and fails loudly. (self) |
| D5 | HTTP logging scope? | Keep the in-tree work: Ktor `Logging` is installed only when a `Logger` is bound; Android binds one only for debuggable builds; `Authorization` is sanitised; audio bodies are summarised. Release builds never log (bodies hold what users said). (self) |
| D6 | Key in the release APK? | Accepted MVP trade-off (doc 15 allows direct calls); documented in README with the proxy path as the fix. No obfuscation theatre. (self) |
| D7 | iOS secrets? | `iosApp/Configuration/Secrets.xcconfig` is git-ignored now; reading it into the app is ticket 14. (self) |
| D8 | Tests? | Existing `HttpClientFactoryTest` (masking, summarised audio, silent without logger) + the new guard test. (self) |
