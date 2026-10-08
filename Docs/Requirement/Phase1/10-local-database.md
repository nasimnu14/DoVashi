# Feature: Local Database

## Requirements

All conversations and messages are stored locally. The app must work
offline for:

- Viewing conversations
- Opening previous conversations
- Reading previous messages
- Playing previously recorded audio

Translation and transcription require network access to OpenAI. No backend
server is required for the MVP.

## Conversation table

```text
Conversation
------------
id
title
language1Code
language2Code
createdAt
updatedAt
```

Example (Phase 1):

```text
id: 123
title: English ↔ Mandarin Chinese
language1Code: en
language2Code: zh
createdAt: ...
updatedAt: ...
```

## Message table

```text
Message
-------
id
conversationId
sourceLanguage
targetLanguage
audioPath
transcribedText
translatedText
reading
status
createdAt
```

Status values:

```text
RECORDING
TRANSCRIBING
TRANSLATING
COMPLETED
FAILED
```

- Audio files are stored in application-private storage; the database only
  stores the file path/reference.
- `language1Code`/`language2Code`/`sourceLanguage`/`targetLanguage` are
  plain language codes (`en`, `zh`) — the schema itself is already
  language-agnostic and needs no changes for Phase 2.
