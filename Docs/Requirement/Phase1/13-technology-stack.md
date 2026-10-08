# Feature: Technology Stack

Because the application ships on both Android and iOS:

## Kotlin Multiplatform (KMP)

Shared code:

- Data models
- Business logic
- Database
- Repository
- Use cases
- OpenAI networking
- Translation logic
- Conversation management
- Message processing
- State management
- Most UI

## Compose Multiplatform

Use for shared UI where practical.

## SQLDelight

Use for the shared local database. Do not use Android Room in shared code.

## Ktor Client

Use for OpenAI API communication.

## Kotlin Coroutines + Flow

Use for asynchronous processing and reactive state.

## Kotlin Serialization

Use for JSON request/response models.

## Dependency Injection

Use a lightweight KMP-compatible DI approach such as Koin, if DI is
required.

This stack is set once in Phase 1 and is not revisited in Phase 2 — adding
languages is a data/content change, not a stack change.
