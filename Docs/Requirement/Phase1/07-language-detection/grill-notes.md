# Grill Notes — 07-language-detection

Source: `Docs/Requirement/Phase1/07-language-detection.md` (with `06-voice-processing-pipeline.md`, `13-technology-stack.md`, `15-openai-api-security.md`, `16-architecture-and-extensibility.md`, Phase 2 `02-language-detection-expansion.md`) · 2026-10-10.

**No interview was run** (user instruction 2026-10-10). Every decision is Claude's, marked **(self)**.

Order note: ticket 07 is implemented before 06. The voice pipeline (06) composes detection (07), translation (08) and Reading (09); building those first lets 06 ship end to end. 06 wires 07 into the recording flow.

## Scope

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What does detection deliver? | (a) A `SpeechToTextService` port (named in doc 15) that turns a Recording into a transcript **plus the language the STT call detected**; (b) its OpenAI implementation; (c) generic resolution of the Message's source and target Language from the Conversation's Language Pair. Calling it from the recording flow is ticket 06. (self) |
| D2 | Where do the parts live? | Port and result types in a new `domain/service` package (doc 16's tree doesn't list one; ports for outside services fit domain better than data). OpenAI adapter in `data/network/openai`. HTTP client factory in `data/network`. (self) |

## Speech-to-text

| # | Question | Resolved answer |
|---|----------|-----------------|
| D3 | Which OpenAI model and format? | `whisper-1` with `response_format=verbose_json`. Per the OpenAI API spec it is the only transcription option that returns the detected `language` (`gpt-4o-transcribe` / `-mini` support `json` only). This satisfies doc 07's "transcription/detection result from the STT call". (self) |
| D4 | How is Whisper's language mapped? | Whisper returns an English name ("english", "chinese"). The adapter maps names to Language Codes with Whisper's own language table (~100 entries, data in the adapter), so Phase 2 languages need no code change. A value that already looks like a code (`zh`, `zh-CN`) is normalised to its lowercase base subtag. Unknown → null. (self) |
| D5 | Result type? | `Transcription(text: String, languageCode: String?)`. The code is a normalised Language Code or null. (self) |
| D6 | Audio input? **(review: `FileStorage` also gains `size`, used for the 25 MB pre-check; reads run on an IO dispatcher)** | The Recording reference. The adapter reads its bytes through `FileStorage`, which gains `read(reference): ByteArray?`. The upload filename and content type come from the extension (m4a → `audio/mp4`, wav → `audio/wav`, else `application/octet-stream`). (self) |
| D7 | Errors? **(review: + RECORDING_REJECTED for 400/413/415 and Recordings over 25 MB, checked before upload; Whisper's no-speech heuristic turns silence into an empty transcript)** | `SpeechToTextException(reason)` with reasons MISSING_API_KEY, RECORDING_UNAVAILABLE, UNAUTHORIZED (401/403), RATE_LIMITED (429), SERVER (5xx/other), NETWORK (I/O, timeout) and INVALID_RESPONSE (unparseable or missing text). Messages never include the API key or the response body. The pipeline (06) turns any of them into a FAILED Message. (self) |
| D8 | HTTP stack? | Ktor client (doc 13) with kotlinx-serialization JSON (ignore unknown keys) and `HttpTimeout` (connect 15 s, request 120 s for uploads). The engine is provided by the platform module (OkHttp on Android; iOS Darwin engine comes with ticket 14). One shared `HttpClient`. (self) |
| D9 | API key and base URL? **(review: release APKs embed the key in plain text, and Gradle's configuration/build caches hold it locally — acceptable only for the MVP, tracked for ticket 15; surrounding quotes and non-printable characters are stripped)** | `OpenAiConfig(apiKey, baseUrl = "https://api.openai.com/v1")`. On Android the key is read at build time from the untracked `local.properties` (`OPENAI_API_KEY`) or the env var of the same name into `BuildConfig`; never committed (doc 15). A missing key fails fast with MISSING_API_KEY and no network call. `baseUrl` is configurable so a backend proxy can replace OpenAI later. Hardening is ticket 15. (self) |

## Resolution (the generic rule)

| # | Question | Resolved answer |
|---|----------|-----------------|
| D10 | How is the source chosen? | Only from the Conversation's two Languages: (1) if the STT-detected code is one of them (compared by base subtag on both sides), that one; (2) else by script: each Language scores the transcript units written in its *own* scripts (those the other doesn't share), and the higher score wins; (3) else undetermined (null), and the pipeline fails the Message. Never a hard-coded language. (self; revised in review) |
| D11 | Target? | Exactly doc 07's formula, in one place: `if (source == conversation.language1Code) language2Code else language1Code`. (self) |
| D12 | Script metadata? | `Language.scripts: Set<Script>` (empty = unknown, no script fallback). `Script` is an enum with Unicode-range classification: LATIN, HAN, KANA, HANGUL, BENGALI, ARABIC, DEVANAGARI, CYRILLIC, which covers the Phase 2 list. Catalog: `en` {LATIN}, `zh` {HAN}. Non-letters (digits, punctuation, spaces) are ignored. (self) |
| D13 | Mixed text ("我用iPhone")? | Units approximate words: a run of a space-separated script counts once, and each Han/Kana character counts once. So "我用iPhone" is Han 2 : Latin 1 and resolves to Mandarin Chinese, while "I really love the city of 北京" stays English. Equal scores → undetermined. (self; revised twice: plain letter counts let "iPhone" outvote Han, and a ×3 weight made the example a tie) |
| D14 | Same-script pairs (Phase 2, e.g. English ↔ Spanish)? | Step 1 (Whisper) decides. If Whisper names a third language, the script step can't separate them and the Message fails with Retry available. Documented limitation. (self) |

## Verification

| # | Question | Resolved answer |
|---|----------|-----------------|
| D15 | Tests? | Pure commonTest for resolution, target rule, script classification and Whisper name mapping. The adapter is tested against Ktor `MockEngine`: request shape (URL, bearer header, multipart fields, filename), parsing, and every error reason including no request when the key is missing. `AndroidFileStorage.read` on a temp dir. No UI and no real network in tests; a live call is a manual check in ticket 06. (self) |

Glossary: added **Source Language / Target Language**. ADRs: none.
