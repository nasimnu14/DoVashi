# Domain Model — 11-infinite-chat-scrolling

| Concept | Layer | Change |
|---|---|---|
| `MessageRepository.observeMessages(conversationId, limit)` | domain/repository | Now bounded: the newest `limit` Messages |
| `selectNewestByConversation` | data/database | `ORDER BY createdAt DESC, id DESC LIMIT :limit` |
| `ObserveMessagesUseCase(conversationId, limit)` | domain/usecase | Passes the limit through |
| `ChatViewModel.windowSize`, `onLoadOlder()`, `PAGE_SIZE` | presentation/conversation | Window state and growth rules |
| `ChatScreen` scroll trigger | presentation/conversation | Calls `onLoadOlder` near the oldest loaded item |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| P1 | At most `limit` Messages are read per observation | SQL `LIMIT` |
| P2 | The window grows only when full, by one page | `onLoadOlder` |
| P3 | A new Message never evicts an older loaded one | `track` widening |
| P4 | Opening a Conversation shows the newest page, positioned at the newest | Fresh view model + reversed list |
