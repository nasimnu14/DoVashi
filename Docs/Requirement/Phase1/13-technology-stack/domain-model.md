# Domain Model — 13-technology-stack

No domain concepts. Stack → where it lives:

| Doc 13 item | Realised as |
|---|---|
| Kotlin Multiplatform | `shared` (commonMain/androidMain/iosMain) |
| Compose Multiplatform | `presentation/*`, `navigation`, `App.kt` |
| SQLDelight | `sqldelight/*.sq`, `data/repository/Sql*`, platform drivers |
| Ktor Client | `data/network/*` (OkHttp / Darwin engines) |
| Coroutines + Flow | repositories, view models, processor |
| Kotlin Serialization | `data/network/openai/*` request/response models |
| Koin | `di/SharedModule`, `androidPlatformModule`, `iosPlatformModule` |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| T1 | The shared module uses the mandated stack | `TechStackTest` |
| T2 | No Room in shared code | `TechStackTest` |
| T3 | Guard files are declared task inputs | `shared/build.gradle.kts` |
