# Grill Notes — 09-pronunciation-romanization

Source: `Docs/Requirement/Phase1/09-pronunciation-romanization.md` (with `05-message-structure.md`, `08-openai-translation.md`, Phase 2 `03-pronunciation-expansion.md`) · 2026-10-10.

**No interview was run** (user instruction 2026-10-10). Decisions are Claude's, marked **(self)**.

Repo state:
- Ticket 05 added `Language.requiresReading` and stores a Reading only for a requiring target.
- Ticket 08 sends `readingRequired` to OpenAI and makes `englishReading` a required string when needed.
- Ticket 04 shows the Reading under the translation.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What is left for this ticket? | Two gaps against doc 09 and Phase 2 doc 03: (a) the Reading's **style** per target Language (pinyin with tone marks for Mandarin Chinese, romaji for Japanese, …) so OpenAI produces the right romanization; (b) making sure a stored Reading is actually "English-readable" (Latin script), not, say, Han characters. (self) |
| D2 | Where does the style live? | On the Language metadata: `Language.readingSystem: String?`, e.g. "Hanyu Pinyin with tone marks" for `zh`; null for `en`. **Its presence is the "needs a Reading" flag:** `requiresReading` becomes `readingSystem != null`. One source of truth, and Phase 2 just adds the entry (doc 09 AC2, Phase 2 doc 03). (self) |
| D3 | How does OpenAI learn it? | `TranslationRequest` carries `readingSystem: String?` (`readingRequired` is derived from it). The prompt fields include `pronunciationSystem`, and the instruction asks for the Reading "using <system>". Still no per-language code in the adapter. (self) |
| D4 | What counts as a valid Reading? **(review: implemented as an allow-list — Latin incl. diacritics, IPA/phonetic letters, later Latin extensions, full-width Latin; at least one letter; emoji are not letters)** | Every letter is Latin script (tone marks and diacritics count as Latin, e.g. ǐ ā ü); spaces, digits and punctuation are fine. A Reading containing letters of any other script is dropped (null), not saved. The Message still completes (ticket 05 D5). Enforced in the domain `readingFor` rule, so it applies whatever the source. (self) |
| D5 | Display? | Unchanged: ticket 04 already shows the Reading in italics under the translation, only when present. (self) |
| D6 | Acceptance per doc 09? | "Reading present and non-null only when the target is Mandarin Chinese in Phase 1": `zh` is the only catalog entry with a `readingSystem`; `readingFor` drops it for any other target. (self) |
| D7 | Tests? | Catalog metadata; `readingFor` (Latin with tone marks kept; Han, Kana or mixed dropped; non-requiring target dropped); request/prompt carries the system; the use case passes the target's system; end-to-end through the fake repository. No UI change. The live pinyin check rides on ticket 06. (self) |

Glossary: **Reading** entry gains "in Latin script, in the target Language's Reading system". ADRs: none.
