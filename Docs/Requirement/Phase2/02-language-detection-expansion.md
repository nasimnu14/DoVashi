# Feature: Language Detection for Additional Pairs

## Description

A conversation still has exactly two expected languages (see
[Phase 1: Language Detection](../Phase1/07-language-detection.md)), but
those two can now be any pair drawn from the full catalog, e.g.
`Bangla ↔ Japanese`.

Example:

```text
Expected languages:
English ↔ Bangla

User speaks:
"How are you today?"

Detected:
English

Translation:
আপনি আজ কেমন আছেন?
```

```text
User speaks:
"আপনি কেমন আছেন?"

Detected:
Bangla

Translation:
How are you?
```

## Requirements

- Reuse the exact `targetLanguage = (sourceLanguage == language1) ?
  language2 : language1` resolution from Phase 1 — no new branch per
  language pair.
- Speech-to-text/detection must reliably distinguish between whichever two
  languages a given conversation specifies, including pairs with no English
  at all (e.g. `Bangla ↔ Japanese`).

## Acceptance criteria

- Detection correctness is verified for at least one non-English pair
  (e.g. `Bangla ↔ Mandarin Chinese` or `Bangla ↔ Japanese`), proving the
  pipeline does not implicitly depend on English being one of the two.
