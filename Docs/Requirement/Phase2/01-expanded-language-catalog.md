# Feature: Expanded Language Catalog

## Description

Add the remaining languages to the catalog introduced in Phase 1's
[Language Model](../Phase1/01-language-model.md), using the same
`Language(code, name, nativeName)` model — no schema or model change.

Target catalog:

```text
English          (already in Phase 1)
Mandarin Chinese  (already in Phase 1)
Bangla
Japanese
Korean
Arabic
Spanish
French
German
Hindi
...
```

## Acceptance criteria

- Each new language is a new row/entry in the existing catalog — zero
  changes to detection, translation, pronunciation, or UI *code* to add a
  row.
- Catalog remains open-ended: adding a future language again requires only
  a new entry.
