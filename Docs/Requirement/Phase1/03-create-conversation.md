# Feature: Create Conversation

## Phase 1 scope

The language pickers are driven by the Phase 1 language catalog, which
contains only English and Mandarin Chinese. In practice this means the
dropdowns offer exactly those two entries, and the only valid conversation
created in Phase 1 is `English ↔ Mandarin Chinese`.

## Requirements

Tapping **+** on the Home screen shows:

```text
Create Conversation

Language 1
[ English ▼ ]

Language 2
[ Mandarin Chinese ▼ ]

[ Start Conversation ]
```

- The language pickers must read their options from the language catalog
  (see [Language Model](./01-language-model.md)) — not a hard-coded pair —
  so Phase 2 can add options without changing this screen's logic.
- The same language cannot be selected for both Language 1 and Language 2.
- On confirming, create a new conversation record and open the Chat screen
  for it.

## Acceptance criteria

- Selecting English for both fields is rejected/disabled.
- New conversation is persisted with `language1Code=en`, `language2Code=zh`
  (or the reverse, depending on selection order).
