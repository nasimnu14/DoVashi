# Feature: OpenAI Translation — Additional Language Combinations

## Description

The [TranslationService contract from Phase 1](../Phase1/08-openai-translation.md)
is reused unchanged; only the `sourceLanguage`/`targetLanguage` values sent
in requests differ.

Example (Bangla → Japanese):

```json
{
  "sourceLanguage": "bn",
  "targetLanguage": "ja",
  "transcribedText": "আপনি কেমন আছেন?",
  "translatedText": "お元気ですか？",
  "englishReading": "O-genki desu ka?"
}
```

## Requirements

- No new request/response shape — same JSON contract as Phase 1, just more
  language codes flowing through it.
- Validate translation quality for each newly supported language pair
  (at minimum: every new language paired with English, plus one
  non-English pair such as `Bangla ↔ Japanese`).

## Acceptance criteria

- All language pairs listed in [Phase 2 overview](./00-overview.md)
  translate successfully through the unmodified `TranslationService`
  interface from Phase 1.
