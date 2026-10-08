# Feature: Infinite Chat Scrolling

## Requirements

- The Chat screen must support large conversations without loading the
  entire conversation into memory.
- Messages load incrementally from the local database.
- Behavior:

  ```text
  Newest messages
        ↓
        ↓
        ↓
  Older messages loaded when scrolling upward
  ```

- When returning to a conversation, the user is positioned near the latest
  message.
