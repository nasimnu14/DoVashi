# Grill Notes — 10-local-database

Source: `Docs/Requirement/Phase1/10-local-database.md` · 2026-10-11. **No interview was run** (user instruction); decisions are Claude's, marked **(self)**.

Repo state: SQLDelight schema (`Conversation.sq`, `Message.sq`) already has every column in doc 10; `MessageStatus` has the five values; Recordings live in `filesDir` and the DB stores a relative reference; Android driver exists; all reads are local. Open since ticket 02: no migration setup.

| # | Question | Resolved answer |
|---|----------|-----------------|
| D1 | Does the schema match doc 10? | Yes, column for column (Message columns other than id/conversationId/status/createdAt are nullable because a Message is built up step by step — ticket 05). No change. (self) |
| D2 | What is left? | (a) Migration safety (ticket 02 Medium): snapshot the schema and verify migrations in the build; (b) a test that pins the schema to doc 10 so drift is noticed. (self) |
| D3 | How are migrations set up? | `schemaOutputDirectory = src/commonMain/sqldelight/databases`, `verifyMigrations = true`, `1.db` generated from the current schema. No `.sqm` yet: version 1 is the first release. Procedure documented in README. (self) |
| D4 | Offline? | Viewing/opening/reading/playback touch only SQLite and `filesDir`; network is used only by the OpenAI adapters. Nothing to build; verified by reading and a manual airplane-mode check. (self) |
| D5 | Audio storage? | Already app-private `filesDir`, DB holds only the relative reference (ticket 04/06); excluded from backup (ticket 06). (self) |
| D6 | Extra indexes? | `message_conversation_created` exists; Conversation list sorts by `updatedAt` over a handful of rows. None added. (self) |
| D7 | iOS driver? | Ticket 14. (self) |
