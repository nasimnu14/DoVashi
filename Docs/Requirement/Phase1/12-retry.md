# Feature: Retry

## Requirements

If transcription or translation fails, the recorded audio and any
successfully generated data must remain stored locally.

```text
Voice recorded
      ↓
Transcription successful
      ↓
Translation failed
      ↓
Message status = FAILED
```

- The user can tap **Retry**.
- The app retries translation without requiring the user to record the
  voice again (reuses the stored audio / already-transcribed text).
