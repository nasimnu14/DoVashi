# Feature: Pronunciation / Romanization for Additional Scripts

## Description

Extend the optional `reading` field (introduced in Phase 1 for Chinese
pinyin) to the other non-Latin / less-familiar scripts added in Phase 2.

Examples:

### Bangla

```text
আপনি কেমন আছেন?

Apni kemon achhen?
```

### Japanese

```text
元気ですか？

Genki desu ka?
```

## Requirements

- The "does this target language need a reading" decision continues to be
  looked up per target language (per Phase 1's design), not special-cased
  per language name — Phase 2 should only need to mark each new language as
  needing/not needing a reading.
- Latin-script targets (Spanish, French, German) are expected to *not*
  require a `reading` — same as English in Phase 1.

## Acceptance criteria

- `reading` is populated for Bangla and Japanese targets, and `null` for
  Spanish/French/German targets, without any per-language conditional in
  the pipeline code — only metadata/config changes.
