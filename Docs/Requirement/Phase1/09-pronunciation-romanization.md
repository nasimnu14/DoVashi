# Feature: Pronunciation / Romanization

## Description

An optional `reading` field, useful when the other speaker may not be able
to read the target script.

## Phase 1 example

Chinese:

```text
你好

Nǐ hǎo
```

## Requirements

- The OpenAI response determines whether a useful romanized/English-
  readable pronunciation is appropriate for the target language — this must
  be a property looked up per target language, not an `if target == "zh"`
  special case, since Phase 2 adds more scripts (Bangla, Japanese, etc.)
  that need the same field.
- When the target language is English, `reading` is `null`/not required.

## Acceptance criteria

- `reading` is present and non-null only when the target language is
  Mandarin Chinese in Phase 1.
- The field/flag that decides "does this language need a reading" lives on
  the language's metadata or a lookup table, so Phase 2 only adds entries.
