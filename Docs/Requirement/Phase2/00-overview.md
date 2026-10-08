# Phase 2 — Remaining Languages

## Scope

Phase 2 extends the Phase 1 app (built as English ↔ Mandarin Chinese only)
to support the full language set described in the original requirement:

- Bangla
- Japanese
- Korean
- Arabic
- Spanish
- French
- German
- Hindi
- ...and any further language added later

Any two of these (plus English/Chinese) can now be paired in a conversation,
e.g.:

```text
English ↔ Bangla
Bangla ↔ Mandarin Chinese
English ↔ Japanese
Bangla ↔ Japanese
English ↔ Arabic
Japanese ↔ Korean
```

## Precondition

Phase 2 assumes [Phase 1's architecture constraint](../Phase1/16-architecture-and-extensibility.md)
was honored: no logic anywhere hard-codes `English`/`Chinese`. If any such
hard-coding exists, it must be fixed as part of Phase 2 before new languages
are added, not worked around.

## What Phase 2 does *not* add

The "Future Expansion" ideas from the original requirement — text-only
messages, voice-to-voice translation, automatic TTS, speaker ID,
conversation export, search, rename, delete, favorites, cloud sync, user
accounts, backend API, translation history, AI summaries — are **not**
part of Phase 2. They remain future backlog beyond this two-phase plan.

## Feature list

1. [Expanded Language Catalog](./01-expanded-language-catalog.md)
2. [Language Detection for Additional Pairs](./02-language-detection-expansion.md)
3. [Pronunciation / Romanization for Additional Scripts](./03-pronunciation-expansion.md)
4. [Create Conversation — Any Pair](./04-create-conversation-any-pair.md)
5. [OpenAI Translation — Additional Language Combinations](./05-openai-translation-expansion.md)
