# Feature: Home Screen

## Description

Displays all saved conversations. In Phase 1 every conversation's language
pair is `English ↔ Mandarin Chinese`, but the screen must render whatever
language pair the conversation record holds — it must not label or format
the row based on a hard-coded pair.

## Requirements

Each conversation row shows:

- Conversation title
- Language pair
- Last message
- Last updated time
- Number of messages (if useful)

Example:

```text
English ↔ Mandarin
你好吗？
Yesterday
```

- Conversations sorted by `updatedAt DESC` — most recently active first.
- A **+ button** to create a new conversation (see
  [Create Conversation](./03-create-conversation.md)).

## Acceptance criteria

- List updates when a message is added to any conversation (re-sorts to
  put that conversation first).
- Works fully offline (reads from local DB only).
