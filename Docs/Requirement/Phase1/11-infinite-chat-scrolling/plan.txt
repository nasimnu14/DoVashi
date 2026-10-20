# Plan — 11-infinite-chat-scrolling

Inputs: `grill-notes.md` (D1–D8), `domain-model.md` (P1–P4).

## Step 1 — Data
- `Message.sq`: `selectNewestByConversation` with `LIMIT`; `SqlMessageRepository`, `MessageRepository`, `ObserveMessagesUseCase` take `limit`.

## Step 2 — View model
- `windowSize` (30), `flatMapLatest` over `observeMessages`, `onLoadOlder()`, widening on arrivals; `PAGE_SIZE` internal.

## Step 3 — UI
- `ChatScreen`/`AppNavHost`: `onLoadOlder`, scroll trigger with a prefetch of 8.

## Step 4 — Tests
- SQL: limit and live sliding window. VM: first page only, page-by-page growth until all loaded, small conversation, arrivals keep the window, reopen resets.

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug`; mutation: removing the arrivals widening fails its test.

## Acceptance criteria
1. A Conversation of any size loads only the newest page on open.
2. Scrolling up loads older pages incrementally; stops when all are loaded.
3. Returning to a Conversation shows the newest Message.
4. Live updates (status, new Messages) still work.
