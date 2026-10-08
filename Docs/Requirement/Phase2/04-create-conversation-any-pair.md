# Feature: Create Conversation — Any Pair

## Description

Extend [Phase 1: Create Conversation](../Phase1/03-create-conversation.md)
so the Language 1 / Language 2 dropdowns now list the full catalog from
[Expanded Language Catalog](./01-expanded-language-catalog.md), not just
English/Chinese.

```text
Create Conversation

Language 1
[ Bangla ▼ ]

Language 2
[ Japanese ▼ ]

[ Start Conversation ]
```

## Requirements

- No UI logic change expected beyond the dropdown now being backed by a
  longer catalog — this validates that Phase 1 built the picker
  data-driven rather than hard-coded to two options.
- The same-language-twice restriction still applies.

## Acceptance criteria

- A user can create `Bangla ↔ Japanese`, `English ↔ Arabic`,
  `Hindi ↔ Spanish`, etc., with no code changes beyond the catalog update.
