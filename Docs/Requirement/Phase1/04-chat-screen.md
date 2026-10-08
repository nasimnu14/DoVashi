# Feature: Chat Screen

## Description

Chat UI similar to WhatsApp / Messenger / iMessage. Messages render
chronologically; each message is one spoken interaction, shown as original
+ translation (+ optional reading).

## Examples (Phase 1 pair)

English → Chinese:

```text
┌─────────────────────────────┐
│ 🎵 Voice                    │
│ "How are you?"              │
│                             │
│ Mandarin Chinese            │
│ "你好吗？"                   │
│                             │
│ ▶ Play                      │
└─────────────────────────────┘
```

Chinese → English:

```text
┌─────────────────────────────┐
│ 🎵 Voice                    │
│ "你好吗？"                   │
│                             │
│ English                     │
│ "How are you?"              │
│                             │
│ ▶ Play                      │
└─────────────────────────────┘
```

## Requirements

- Each bubble labels the *detected/target* language by name, read from the
  message record — never assume which side is English vs. Chinese.
- Tapping Play/Pause plays the original recorded audio for that message.
- See [Infinite Chat Scrolling](./11-infinite-chat-scrolling.md) for
  loading behavior and [Message Structure](./05-message-structure.md) for
  what each bubble is built from.
