# Domain Model — 03-create-conversation

Sources: `03-create-conversation.md`, `01-language-model.md`, `16-architecture-and-extensibility.md`, `GLOSSARY.md`, `grill-notes.md`, existing code on `phase_1`.

## 1. Entities & relationships

### Language / Language Catalog (consumed, owned by ticket 01)
`Language(code, name, nativeName)`. `LanguageCatalog.all` gives the picker options in catalog order. This screen never assumes the catalog has exactly two entries.

### Language Pair (the screen's output)
Language 1 and Language 2 (two different Languages) as selected by the user. The pair is not stored as its own type; it maps onto `Conversation.language1Code` / `language2Code`.

### Conversation (persisted, owned by ticket 02)
Created through `CreateConversationUseCase(language1: Language, language2: Language): Long`, which:
- rejects `language1.code == language2.code` (`require`, I7)
- sets `title = conversationTitle(l1, l2)` = `"<l1.name> ↔ <l2.name>"`
- calls `ConversationRepository.createConversation`, which sets `createdAt = updatedAt = now` (I1).

### CreateConversationUiState (presentation state, new)
| Field | Type | Meaning |
|---|---|---|
| `languages` | `List<Language>` | Picker options (catalog order) |
| `language1` | `Language?` | Selected Language 1 (null only if the catalog is empty) |
| `language2` | `Language?` | Selected Language 2 (null if no second distinct Language exists) |
| `isCreating` | `Boolean` | A create call is in flight |
| `errorMessage` | `String?` | Creation failure text, cleared on the next start or selection |
| `createdConversationId` | `Long?` | One-shot navigation target; cleared by `onNavigationHandled()` |

Derived: `isSameLanguage = language1 != null && language1.code == language2?.code`. `canStart = language1 != null && language2 != null && !isSameLanguage && !isCreating`.

### Routes (navigation, owned by ticket 02)
`CreateConversationRoute` → now hosts the real screen. `ChatRoute(conversationId)` is still the stub.

```
LanguageCatalog.all ──► CreateConversationUiState.languages
selected (language1, language2) ──► CreateConversationUseCase ──► Conversation row ──► ChatRoute(id)
```

## 2. Bounded context / ownership

| Layer | Owns |
|---|---|
| `domain/usecase` | `CreateConversationUseCase` (unchanged): I7 and title |
| `presentation/createconversation` (new) | `CreateConversationUiState`, `CreateConversationViewModel` (selection, duplicate rule, single-flight create, error), `CreateConversationScreen` (stateless UI) |
| `navigation` | `AppNavHost` wires the screen. On success it navigates to `ChatRoute(id)` with `popUpTo<CreateConversationRoute> { inclusive = true }`. The Create stub is removed |
| `di` | `viewModel { CreateConversationViewModel(get()) }` (catalog defaults to `LanguageCatalog.all`, like `HomeViewModel`'s lookup) |
| `composeResources` | Back-arrow vector drawable |

## 3. Invariants

| ID | Invariant | Enforced by |
|---|---|---|
| C1 | Picker options = the Language Catalog in catalog order; no Language literals in screen logic | ViewModel takes `languages: List<Language>` (default `LanguageCatalog.all`) |
| C2 | Start is enabled only when both Languages are chosen, differ, and no create is in flight | `canStart` |
| C3 | A start inserts exactly one Conversation, with codes in selected order | ViewModel single-flight guard + use case |
| C4 | Identical codes never reach the database | `canStart` (UI) + `require` in the use case (domain); I7 from ticket 02 |
| C5 | After a successful create, Back from Chat returns to Home | `popUpTo<CreateConversationRoute>(inclusive = true)` |
| C6 | A failed create persists nothing and leaves the form usable | Repository transaction + ViewModel catch → `errorMessage`, `isCreating = false` |
