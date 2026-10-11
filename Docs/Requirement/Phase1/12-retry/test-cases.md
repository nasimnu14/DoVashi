# Test Cases — 12-retry

| ID | Title | Expected | Priority |
|---|---|---|---|
| TC-01 | Translation failure retried from transcript [Auto] | COMPLETED; STT called once in total | High |
| TC-02 | Transcription failure retried from Recording [Auto] | COMPLETED; STT called twice | High |
| TC-03 | Retry fails again [Auto] | FAILED, transcript + audio kept, retryable again | High |
| TC-04 | Silence retry [Auto] | FAILED again | Low |
| TC-05 | Non-Failed / unknown / nothing-to-resume [Auto] | No-op | Medium |
| TC-06 | Double tap [Auto] | Same job; attempt not cancelled; COMPLETED | High |
| TC-07 | Mapper `canRetry` [Auto] | Failed + (transcript or audio) only | High |
| TC-08 | VM retry click [Auto] | Bubble completes, button gone | High |
| TC-09 | Live: airplane mode, record, see Failed + Retry; network on, tap Retry (manual) | Translation appears; no new recording | High |
| TC-10 | Retry then leave screen (manual) | Message completes in the background | Medium |
| TC-11 | Kill app mid-retry (manual) | Reopens as Failed with Retry | Medium |
