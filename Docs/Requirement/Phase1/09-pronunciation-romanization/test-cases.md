# Test Cases — 09-pronunciation-romanization

> Source: Docs/Requirement/Phase1/09-pronunciation-romanization/plan.md (grill-notes D1–D7, domain-model R1–R4)
> Type: Feature · Generated: 2026-10-10 (create-manual-test-cases format)

## Scope
Reading system as Language metadata, the Latin-script rule for stored Readings, and the Reading system passed to OpenAI. No UI change.

## Test Cases

### Positive / Happy Path
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-01 | Mandarin Chinese has a Reading system (AC1, AC2) [Auto] | Catalog | 1. Read `zh` and `en` | `zh`: "Hanyu Pinyin with tone marks", needs a Reading; `en`: none | High |
| TC-02 | Pinyin with tone marks is stored (AC1) [Auto] | Target `zh`; Reading "Nǐ hǎo ma?" | 1. Save the translation | Reading stored as "Nǐ hǎo ma?" | High |
| TC-03 | OpenAI is asked for the system (AC3) [Auto] | Request to `zh` | 1. Translate | User fields include `"pronunciationSystem":"Hanyu Pinyin with tone marks"`; the instruction names it | High |
| TC-04 | Chat shows the Reading (regression) | Fresh debug install | 1. Open the top conversation | "Nǐ jīntiān hǎo ma?" in italics under "你今天好吗？"; none under the English translation | Medium |
| TC-05 | Live pinyin (manual) | Key configured; ticket 06 build | 1. Speak English in an English ↔ Mandarin Chinese Conversation | Translation with pinyin tone marks underneath | High |

### Negative
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-06 | Non-Latin Reading dropped (R2) [Auto] | Target `zh`; Reading "你好" or "Ni hao 你" | 1. Save | Reading null; Message still completes | High |
| TC-07 | No Reading for English (AC1) [Auto] | Target `en`; model sends "x" | 1. Save | Reading null | High |

### Edge Cases
| ID | Title | Preconditions | Steps | Expected Result | Priority |
|---|---|---|---|---|---|
| TC-08 | Phase 2 shape needs only metadata (AC2) [Auto] | Test Language with `readingSystem = "Hepburn romaji"` | 1. Save Reading "Genki desu ka?" | Stored; the request carries "Hepburn romaji" | Medium |
| TC-09 | Digits and punctuation in a Reading [Auto] | Reading "Nǐ 3 suì le!" | 1. Save | Stored | Low |
| TC-10 | Reading without letters [Auto] | Reading "—" | 1. Save | Reading null | Low |
| TC-11 | Other phonetic scripts dropped [Auto] | Reading in Zhuyin, half-width kana, Greek or supplementary Han | 1. Save | Reading null | Medium |
| TC-12 | IPA, full-width Latin and emoji kept [Auto] | Reading "ɑ", "Ｎｉ", "Xièxie 👍" | 1. Save | Stored as given | Low |
