# Feature: OpenAI API Security

## Requirements

- Do not hard-code the OpenAI API key in source code.
- Do not commit API keys to Git.
- Isolate the OpenAI integration behind:

  ```kotlin
  interface TranslationService
  ```

  and

  ```kotlin
  interface SpeechToTextService
  ```

  so a backend proxy can be introduced later without rewriting the app.
- For the MVP, the application may call OpenAI directly, but the
  architecture must be prepared for a future secure backend.
