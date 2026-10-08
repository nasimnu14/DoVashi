# Feature: OpenAI Translation

## Requirements

OpenAI receives structured instructions containing:

- Source language
- Target language
- Original transcription
- Whether pronunciation/romanization is required

Example conceptual request (English → Chinese):

```text
Source language: English
Target language: Mandarin Chinese

Text:
How are you today?

Translate this into Mandarin Chinese.

Also provide an English-readable pronunciation of the Chinese translation.
```

The response is structured JSON:

```json
{
  "sourceLanguage": "en",
  "targetLanguage": "zh",
  "transcribedText": "How are you?",
  "translatedText": "你好吗？",
  "englishReading": "Nǐ hǎo ma?"
}
```

Chinese → English:

```json
{
  "sourceLanguage": "zh",
  "targetLanguage": "en",
  "transcribedText": "你好吗？",
  "translatedText": "How are you?",
  "englishReading": null
}
```

## Phase 1 constraint

- The request/response contract must carry `sourceLanguage` /
  `targetLanguage` as data fields (language codes), not be hard-coded to
  "English" and "Chinese" in the calling code — Phase 2 sends the exact same
  shape with different codes.
- Access OpenAI only through a `TranslationService` interface (see
  [OpenAI API Security](./15-openai-api-security.md)).
