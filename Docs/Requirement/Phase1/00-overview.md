# Phase 1 — MVP (English ↔ Mandarin Chinese)

## Scope

Phase 1 delivers the full app experience (recording, transcription, language
detection, translation, pronunciation, storage, retry) but **restricts the
supported languages to exactly two**:

- English (`en`)
- Mandarin Chinese (`zh`)

A conversation in Phase 1 is always `English ↔ Mandarin Chinese`.

## Why these two first

English and Chinese together exercise every hard part of the pipeline once:

- A language with no special reading (`en`) and a language that requires
  romanization (`zh`, pinyin) — proves the optional/language-dependent
  `reading` field end-to-end in both directions.
- Validates speech-to-text, detection, and OpenAI translation against a
  non-Latin script early, rather than deferring that risk to Phase 2.

## Non-negotiable constraint

Even though only two languages ship in Phase 1, **the implementation must not
hard-code `English`/`Chinese` anywhere in the pipeline, UI logic, or data
model** (see [16-architecture-and-extensibility](./16-architecture-and-extensibility.md)).
All logic must be expressed as `sourceLanguage` / `targetLanguage` /
`language1` / `language2`, driven by data, not conditionals. Phase 2 must be
addable by **adding language metadata only** — no pipeline rewrite.

## Out of scope for Phase 1

Deferred to [Phase 2](../Phase2/00-overview.md):

- Bangla, Japanese, Korean, Arabic, Spanish, French, German, Hindi, and any
  other language pair.
- Everything listed in the source requirement's "Future Expansion" section
  (text-only messages, TTS, speaker ID, export, search, rename, delete,
  favorites, cloud sync, accounts, backend API, history, AI summaries) —
  these are beyond both Phase 1 and Phase 2 and are not planned here.

## Feature list

1. [Language Model](./01-language-model.md)
2. [Home Screen](./02-home-screen.md)
3. [Create Conversation](./03-create-conversation.md)
4. [Chat Screen](./04-chat-screen.md)
5. [Message Structure](./05-message-structure.md)
6. [Voice Processing Pipeline](./06-voice-processing-pipeline.md)
7. [Language Detection](./07-language-detection.md)
8. [OpenAI Translation](./08-openai-translation.md)
9. [Pronunciation / Romanization](./09-pronunciation-romanization.md)
10. [Local Database](./10-local-database.md)
11. [Infinite Chat Scrolling](./11-infinite-chat-scrolling.md)
12. [Retry](./12-retry.md)
13. [Technology Stack](./13-technology-stack.md)
14. [Platform-Specific Components](./14-platform-specific-components.md)
15. [OpenAI API Security](./15-openai-api-security.md)
16. [Architecture & Extensibility](./16-architecture-and-extensibility.md)
