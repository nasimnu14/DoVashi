# Test Cases — 15-openai-api-security

> Source: plan.md · Generated: 2026-10-11

| ID | Title | Steps | Expected | Priority |
|---|---|---|---|---|
| TC-01 | No hard-coded key [Auto] | Run `NoHardcodedSecretsTest` | Passes; adding `sk-` + 30 chars to a source file makes it fail | High |
| TC-02 | Key files ignored [Auto] | Same test | `.gitignore` lists `local.properties`, `*.key`, `Secrets.xcconfig` | High |
| TC-03 | Authorization masked in logs [Auto] | `HttpClientFactoryTest` | Bearer token absent from the log | High |
| TC-04 | Audio upload summarised [Auto] | `HttpClientFactoryTest` | "binary N-byte body omitted", no bytes dumped | Medium |
| TC-05 | No logger, no logs [Auto] | `HttpClientFactoryTest` | Nothing logged | High |
| TC-06 | Debug build logs to logcat (manual) | Run debug build, record a message, `adb logcat -s DoVashi-HTTP` | Requests/responses shown; `Authorization: ██` | Medium |
| TC-07 | Release build is silent (manual) | Install release build, repeat | No `DoVashi-HTTP` lines | High |
| TC-08 | Missing key fails fast (regression) | Build without a key, record | Message fails with MISSING_API_KEY, no network call | Medium |
