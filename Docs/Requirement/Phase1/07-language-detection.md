# Feature: Language Detection

## Phase 1 scope

Each conversation knows its two expected languages, which in Phase 1 are
always English and Mandarin Chinese:

```text
Conversation languages:
English
Mandarin Chinese
```

## Behavior

If the user speaks:

```text
"Good morning"
```

the system identifies:

```text
sourceLanguage = English
targetLanguage = Mandarin Chinese
```

If the user speaks:

```text
"你好"
```

then:

```text
sourceLanguage = Mandarin Chinese
targetLanguage = English
```

## Requirements

- The user never manually selects the source language per message.
- Detection must use an approach compatible with the OpenAI-based
  architecture (e.g. transcription/detection result from the STT call).
- Detection logic must resolve `targetLanguage` generically:

  ```kotlin
  targetLanguage =
      if (sourceLanguage == conversation.language1)
          conversation.language2
      else
          conversation.language1
  ```

  **Not** `if (sourceLanguage == English) Chinese else English`. This is
  what lets Phase 2 reuse the exact same detection/target-resolution code
  for every additional language pair.
