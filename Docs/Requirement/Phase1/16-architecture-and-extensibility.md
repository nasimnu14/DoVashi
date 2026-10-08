# Feature: Architecture & Extensibility

This is the feature that makes Phase 2 possible without a rewrite — treat it
as a hard requirement of Phase 1, not a nice-to-have.

## Module structure

```text
commonMain
│
├── data
│   ├── database
│   ├── network
│   ├── repository
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── presentation
│   ├── home
│   ├── conversation
│   └── createconversation
│
├── audio
│
├── navigation
│
└── di
```

Platform implementations:

```text
androidMain
├── audio
├── permissions
└── storage

iosMain
├── audio
├── permissions
└── storage
```

## Design principle (mandatory)

Do not hard-code logic such as:

```text
if English then Chinese
else Chinese then English
```

Instead use:

```kotlin
targetLanguage =
    if (sourceLanguage == language1)
        language2
    else
        language1
```

This is the single rule that allows the same system, unchanged, to support
`English ↔ Bangla`, `English ↔ Japanese`, `Bangla ↔ Arabic`,
`Japanese ↔ Korean`, etc. in Phase 2 — by adding language metadata only.

## Definition of done for Phase 1

Phase 2 ([see feature list](../Phase2/00-overview.md)) must be achievable by:

1. Adding `Language` rows to the catalog.
2. Ensuring speech-to-text/translation/pronunciation support each new
   language/script.

No changes to the conversation system, detection logic, database schema, or
OpenAI request/response contract should be required.
