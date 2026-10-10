# Grill Notes — 03-create-conversation

Source: `Docs/Requirement/Phase1/03-create-conversation.md` · 2026-10-10.

**No interview was run.** On 2026-10-10 the user said to skip the grill skill and not to wait for
permission. Claude made every decision below from the requirement docs, `GLOSSARY.md`, ticket
01/02 decisions and the existing code. Each one is marked **(self)**. The user can override any of
them in review.

Repo state: tickets 01 (Language Catalog) and 02 (Home, schema, `CreateConversationUseCase`,
`CreateConversationRoute` stub) are merged into `phase_1`.

## Scope

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | What replaces the "Hello World" Create stub? | A real Create Conversation screen in `presentation/createconversation` (package named in doc 16). (self) |
| D2 | Does this ticket build the Chat screen? | No. Start Conversation navigates to the existing Chat stub with the new id; ticket 04 builds Chat. (self) |
| D3 | New persistence code? | None. Reuse `CreateConversationUseCase` (title from `conversationTitle`, I7 `require`) and `ConversationRepository.createConversation` from ticket 02. (self) |

## Pickers

| # | Question | Resolved answer |
|---|----------|-----------------|
| D4 | Where do the options come from? | The Language Catalog (`LanguageCatalog.all`), injected into the ViewModel as a list. No `en`/`zh` literals. (self) |
| D5 | Picker widget? | Material 3 `ExposedDropdownMenuBox`, one per field, labelled "Language 1" and "Language 2". Each option shows the Language `name`. (self) |
| D6 | Defaults? | Language 1 = the first catalog entry. Language 2 = the first entry whose code differs from Language 1. With a catalog of fewer than two Languages, the missing field stays empty and Start is disabled. In Phase 1 this gives English / Mandarin Chinese, as in the mockup. (self) |
| D7 | How is "same language for both" rejected? | Both dropdowns list every Language, so the user can reorder the pair in two steps. If both fields hold the same Language, an inline error "Choose two different languages" appears and **Start Conversation is disabled**. `CreateConversationUseCase` still `require`s it (defence in depth). This matches the AC wording "rejected/disabled". (self) |
| D8 | Swap button or auto-swap? | No. It isn't in the requirement, and auto-swapping would silently change the other field. (self) |

## Start Conversation

| # | Question | Resolved answer |
|---|----------|-----------------|
| D9 | Language order? | Stored as selected: Language 1 → `language1Code`, Language 2 → `language2Code`. Title = `"<L1 name> ↔ <L2 name>"`. (self) |
| D10 | Double tap? | The button is disabled while creating, and the ViewModel ignores a second start while one is running, so exactly one row is inserted. (self) |
| D11 | Navigation after create? | Navigate to `ChatRoute(newId)` and pop `CreateConversationRoute` off the back stack, so Back from Chat returns to Home, not to the form. (self) |
| D12 | One-shot event mechanism? | A `createdConversationId` field in UI state. The screen navigates in a `LaunchedEffect` and then calls `onNavigationHandled()`. This survives configuration changes, unlike a callback captured by the old NavController. (self) |
| D13 | Creation failure (DB exception)? | Show "Couldn't create the conversation. Please try again." and re-enable the button. Nothing is persisted (insert runs in one transaction). (self) |

## Screen

| # | Question | Resolved answer |
|---|----------|-----------------|
| D14 | Layout? | Material 3 `Scaffold`, `TopAppBar("Create Conversation")` with a Back navigation icon (iOS has no system back), then the two labelled dropdowns and a full-width "Start Conversation" button, as in the mockup. (self) |
| D15 | Back icon source? | A vector drawable in `composeResources/drawable` via the already-present `components-resources`. No new icon dependency. (self) |
| D16 | Rotation / process death? | The ViewModel keeps selections across configuration changes. Process death resets the form to the defaults (accepted for Phase 1). (self) |
| D17 | Accessibility? | Each dropdown field carries its visible label. The Back icon has content description "Back". The error text is plain visible text. (self) |

## Verification

| # | Question | Resolved answer |
|---|----------|-----------------|
| D18 | Automated tests? | `commonTest` ViewModel tests: defaults, the duplicate rule (error + disabled), start persists the selected order (both directions), double start inserts once, failure surfaces the error and allows retry, a catalog of size 1 leaves Start disabled. Plus a repository test that the persisted row has the selected codes. No Compose UI tests (same as ticket 02 Q15). (self) |

Glossary: no new terms. "Language 1"/"Language 2" are already defined under **Language Pair**. ADRs: none (no decision meets all three ADR criteria).
