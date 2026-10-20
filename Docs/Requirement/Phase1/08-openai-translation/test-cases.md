# Test Cases — 08-openai-translation

> Source: Docs/Requirement/Phase1/08-openai-translation/plan.md (grill-notes D1–D11, domain-model T1–T6)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

## Scope
`TranslationService` and its OpenAI Chat Completions implementation (strict structured output in doc 08's JSON contract), plus `TranslateMessageUseCase`. No UI. The recording flow is ticket 06, so cases are **[Auto]** (MockEngine) except the live calls.

## Test Cases

### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | English → Mandarin Chinese request (AC1) [Auto] | Request en → zh, "How are you?", Reading required | 1. Translate | Request body has the model, the strict schema with `sourceLanguage` enum ["en"] and `targetLanguage` enum ["zh"], and a user message whose fields carry both codes and names, the text and `pronunciationRequired: true`; `englishReading` typed `string` | High |
| TC-02 | Parse the contract (AC2) [Auto] | Response content `{"sourceLanguage":"en","targetLanguage":"zh","translatedText":"你好吗？","englishReading":"Nǐ hǎo ma?"}` | 1. Translate | Result carries all five contract fields (transcript filled from the request) | High |
| TC-03 | Chinese → English, no Reading (AC2) [Auto] | zh → en, Reading not required; `englishReading` null | 1. Translate | Result Reading null | High |
| TC-04 | Use case completes a Translating Message (AC5) [Auto] | Message TRANSLATING with transcript, en → zh | 1. Translate the Message | Status COMPLETED with translation and Reading | High |
| TC-05 | Reading flag from target metadata (AC3) [Auto] | Test catalog: target requires a Reading / doesn't | 1. Translate each | Request flag true / false respectively | High |
| TC-06 | Live translation (manual) | `OPENAI_API_KEY` configured; ticket 06 build | 1. Speak "How are you?" in English ↔ Mandarin Chinese<br>2. Speak "你好吗？" | Chinese translation with pinyin; English translation without a Reading | High |

### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-07 | Missing key [Auto] | Blank key | 1. Translate | MISSING_API_KEY; no request | High |
| TC-08 | HTTP failures [Auto] | 401, 429, 400, 500 | 1. Translate each | UNAUTHORIZED, RATE_LIMITED, REJECTED, SERVER | Medium |
| TC-09 | Transport failure / cancellation [Auto] | I/O error; caller cancelled | 1. Translate | NETWORK; cancellation propagates | Medium |
| TC-10 | Refusal [Auto] | `message.refusal` set | 1. Translate | REFUSED | Medium |
| TC-11 | Truncated or empty output [Auto] | `finish_reason` "length"; empty choices; non-JSON content; an `error` object | 1. Translate | REJECTED for "length"; INVALID_RESPONSE otherwise | Medium |
| TC-12 | Echo mismatch [Auto] | Response `sourceLanguage` "zh" for an en request | 1. Translate | INVALID_RESPONSE | Medium |
| TC-13 | Use case preconditions [Auto] | Message not TRANSLATING / no transcript / unknown id | 1. Translate | false, no service call | Medium |

### Edge Cases
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-14 | Unneeded Reading is dropped [Auto] | Target doesn't require a Reading but the model returns one | 1. Translate the Message | Stored Reading null | Medium |
| TC-15 | Service failure leaves the Message for the caller [Auto] | Service throws | 1. Translate the Message | Exception propagates; Message still TRANSLATING (06 marks it failed) | Medium |
| TC-16 | Oversized response [Auto] | Content-Length over 1 MB | 1. Translate | INVALID_RESPONSE | Low |
| TC-17 | STT behaviour unchanged after refactor [Auto] | Ticket 07 tests | 1. Run them | All pass | High |
| TC-18 | Over-long translation [Auto] | Translation of 10,001 characters | 1. Translate | REJECTED (retry won't help) | Medium |
| TC-19 | Inputs changed during the call [Auto] | The Message is failed, retried and re-transcribed while the request is in flight | 1. Translate the Message | Result discarded; returns false; the new attempt's Message is untouched | High |
| TC-20 | Content filter [Auto] | `finish_reason` "content_filter" | 1. Translate | REFUSED | Low |
