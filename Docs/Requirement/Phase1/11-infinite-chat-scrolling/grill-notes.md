# Grill Notes — 11-infinite-chat-scrolling

Source: `Docs/Requirement/Phase1/11-infinite-chat-scrolling.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Repo state: `observeMessages(conversationId)` read every Message; the Chat screen is a reversed `LazyColumn` (newest at index 0), so it already opens at the newest Message; ticket 04 deferred bounding the list to this ticket.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Paging mechanism? | A growing window: `observeMessages(conversationId, limit)` reads the newest `limit` rows (`LIMIT`) as a live query. The view model starts at 30 and adds 30 each time the user nears the oldest loaded Message. Memory is bounded by how far the user has scrolled, not by the Conversation's size. Simple and stays live (status changes, new Messages). (self) |
| D2 | Why not keyset pages? | One-shot older pages plus a separate live window would need merging two sources for a benefit (re-reading ≤ a few hundred rows per change) the app does not need. (self) |
| D3 | New Message while reading older ones? | A new Message would slide the oldest loaded one out of a fixed-size window. The view model widens the window by the number of Messages that arrived since the last emission, so what the user sees stays loaded. (self) |
| D4 | When does it stop? | `onLoadOlder` only grows the window when the last result filled it (`size >= limit`); otherwise everything is loaded, so repeated calls are no-ops. (self) |
| D5 | Trigger? | `ChatScreen` watches the last visible item of the reversed list (`snapshotFlow`); within 8 items of the end it calls `onLoadOlder`. Re-evaluated when the item count changes. (self) |
| D6 | "Positioned near the latest message on return"? | Reversed layout opens at index 0 = newest, and a reopened Conversation gets a fresh view model (window back to 30). Rotation keeps the saved scroll state. No code needed beyond keeping this. (self) |
| D7 | Schema / index? | No change: `message_conversation_created(conversationId, createdAt)` serves the ordered `LIMIT` query. (self) |
| D8 | Page size 30? | Covers several screens of bubbles; a constant, not configuration. (self) |
