# Feature: Voice Processing Pipeline

## Pipeline

```text
Tap microphone
      ↓
Start recording
      ↓
User speaks (English or Mandarin Chinese)
      ↓
Stop recording
      ↓
Save audio locally
      ↓
Speech-to-text
      ↓
Determine source language        (en or zh — see Language Detection)
      ↓
Determine target language        (the conversation's other language)
      ↓
Send text to OpenAI
      ↓
Translate
      ↓
Generate pronunciation if required (pinyin, when target = zh)
      ↓
Save message locally
      ↓
Display message
```

## Requirements

- Every step persists its result as soon as it succeeds, so a later-stage
  failure (see [Retry](./12-retry.md)) never loses the recording or an
  already-completed transcription.
- "Determine source/target language" must call the language-detection and
  conversation logic, never branch directly on `en`/`zh` string literals in
  the pipeline code.
