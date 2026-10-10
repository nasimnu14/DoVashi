# Test Cases — 07-language-detection

> Source: Docs/Requirement/Phase1/07-language-detection/plan.md (with grill-notes.md D1–D15 and domain-model.md L1–L7)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

## Scope
- Speech-to-text through `SpeechToTextService` (OpenAI `whisper-1`), returning the transcript and detected language.
- Generic resolution of a Message's Source and Target Language from the Conversation's Language Pair: STT detection first, script fallback second, "the other Language" for the target.
- API key handling.

This ticket has no UI and the recording flow is ticket 06, so nearly every case is **[Auto]** (MockEngine, no real network). The live-call cases are manual and need a real key; they become fully exercisable in ticket 06.

## Open Questions
- Whisper sometimes reports Mandarin speech as another language (e.g. "cantonese"/"yue", "japanese"). The script fallback resolves Han text to the Han-script Language of the pair. In a Phase 2 Mandarin Chinese ↔ Japanese pair, kanji-only Japanese text stays undetermined (D14). Accepted.

## Test Cases

### Positive / Happy Path

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | English speech in an English ↔ Mandarin Chinese Conversation (AC2) [Auto] | Conversation (en, zh); STT returns "Good morning", "english" | 1. Resolve | Source en, target zh | High |
| TC-02 | Chinese speech (AC2) [Auto] | Same Conversation; STT returns "你好", "chinese" | 1. Resolve | Source zh, target en | High |
| TC-03 | Language order doesn't matter (AC2) [Auto] | Conversation (zh, en) | 1. Resolve "Good morning"/english and "你好"/chinese | (en → zh) and (zh → en) respectively | High |
| TC-04 | Target is always "the other Language" (AC3) [Auto] | Any test pair | 1. Compute the target for Language 1 and for Language 2 | Language 2 and Language 1 respectively | High |
| TC-05 | Region and case variants of the detected code [Auto] | STT code "ZH-cn" | 1. Resolve | Source zh | Medium |
| TC-06 | Script fallback when STT names a third language (D10) [Auto] | STT returns "你好" with "yue" | 1. Resolve | Source zh (Han text, only zh is Han in the pair) | High |
| TC-07 | Mixed text resolves by dominant script (D13) [Auto] | STT returns "我用iPhone", null code | 1. Resolve | Source zh | Medium |
| TC-08 | Transcription request shape (AC5) [Auto] | MockEngine; key "k"; Recording "audio/a.m4a" | 1. Transcribe | One POST to `…/v1/audio/transcriptions` with bearer "k", multipart `model=whisper-1`, `response_format=verbose_json`, file "a.m4a" (audio/mp4); returns the text and code | High |
| TC-09 | Whisper language names map to codes [Auto] | — | 1. Map english, chinese, Japanese, zh-CN | en, zh, ja, zh | Medium |
| TC-10 | Live transcription with a real key (manual) | `OPENAI_API_KEY` set in `local.properties`; a debug build from ticket 06 or a scratch harness | 1. Record "Good morning" in an English ↔ Mandarin Chinese Conversation<br>2. Record "你好" | Transcripts match the speech; detected sources English and Mandarin Chinese | High |

### Negative

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-11 | Missing API key fails fast (AC5, L6) [Auto] | Blank key | 1. Transcribe | MISSING_API_KEY; zero HTTP requests | High |
| TC-12 | Missing Recording (L6) [Auto] | Reference doesn't resolve | 1. Transcribe | RECORDING_UNAVAILABLE; zero requests | Medium |
| TC-13 | Rejected key (L7) [Auto] | Server returns 401 | 1. Transcribe | UNAUTHORIZED; message doesn't contain the key | High |
| TC-14 | Rate limit and server errors [Auto] | Server returns 429, then 500 | 1. Transcribe twice | RATE_LIMITED, then SERVER | Medium |
| TC-15 | Network failure [Auto] | Engine throws an I/O error | 1. Transcribe | NETWORK | Medium |
| TC-16 | Malformed response [Auto] | 200 with invalid JSON or without `text` | 1. Transcribe | INVALID_RESPONSE | Medium |
| TC-17 | Same-script pair with a third-language code is undetermined (D14, L4) [Auto] | Pair A {LATIN} ↔ C {LATIN}; STT code unknown | 1. Resolve "hola" | null (no guess) | Medium |
| TC-18 | Text without letters is undetermined (L4) [Auto] | STT "123 !!!", null code | 1. Resolve | null | Low |

### Edge Cases

| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-19 | Language without script metadata [Auto] | Pair where one Language has no scripts; unknown code | 1. Resolve | Only the Language with matching scripts can be chosen; none → null | Low |
| TC-20 | Script tie is undetermined (D13) [Auto] | Text "hello 你" (1 Latin word, 1 Han character), no detected code | 1. Resolve | null | Low |
| TC-21 | Key never committed (L5, manual) | Repository checkout | 1. `git grep -n "sk-"` and inspect `local.properties` tracking | No key in tracked files; `local.properties` is ignored | High |
| TC-22 | File storage read (FileStorage.read) [Auto] | Temp dir | 1. Read an existing, a missing and an escaping reference | Bytes, null, null | Medium |
| TC-23 | Oversized or rejected Recording [Auto] | Recording reported as 26 MB; or server 400/413/415 | 1. Transcribe | RECORDING_REJECTED; no request for the oversized file | Medium |
| TC-24 | Silence becomes an empty transcript [Auto] | Response with every segment `no_speech_prob > 0.6`, `avg_logprob < -1` | 1. Transcribe | Transcript is empty (ticket 06 then doesn't save it) | High |
| TC-25 | Leaving the screen mid-request cancels cleanly [Auto] | Request in flight | 1. Cancel the caller | Cancellation propagates; no NETWORK failure is reported | Medium |
| TC-26 | Over-long transcript [Auto] | Response text of 10,001 characters | 1. Transcribe | RECORDING_REJECTED | Low |
