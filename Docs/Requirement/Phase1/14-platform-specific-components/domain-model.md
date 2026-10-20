# Domain Model — 14-platform-specific-components

| Interface (commonMain) | Android | iOS (new) |
|---|---|---|
| `AudioRecorder` | `AndroidAudioRecorder` (MediaRecorder) | `IosAudioRecorder` (AVAudioRecorder) |
| `AudioPlayer` | `AndroidAudioPlayer` (MediaPlayer, + audio focus) | `IosAudioPlayer` (AVAudioPlayer) |
| `FileStorage` | `AndroidFileStorage` (`filesDir`) | `IosFileStorage` (Application Support, excluded from backup) |
| `MicrophonePermission` | `rememberMicrophonePermission` | `IosMicrophonePermission` |
| `SqlDriver` | `createAndroidSqlDriver` | `createIosSqlDriver` |
| `HttpClientEngine` | OkHttp | Darwin |
| Koin platform module | `androidPlatformModule` | `iosPlatformModule` |

## Invariants
| ID | Invariant | Enforced by |
|---|---|---|
| A1 | Common code never touches a platform API; it sees only the interfaces | Module layout |
| A2 | Both platforms resolve references under the storage root only | `AndroidFileStorageTest`, `IosFileStorageTest` |
| A3 | A recorder's own stop never reports "stopped automatically" | Delegate detached before `stop()` |
| A4 | Playing audio claims audio focus/session and releases it when it ends | Player implementations |
| A5 | The iOS app starts Koin and sweeps interrupted Messages before the first screen | `MainViewController` |
