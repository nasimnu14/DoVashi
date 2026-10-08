# Feature: Platform-Specific Components

Some functionality remains native.

## Android

Native APIs for:

- Audio recording
- Audio playback
- Microphone permissions
- Application file storage

## iOS

Native APIs for:

- Audio recording
- Audio playback
- Microphone permissions
- Application file storage

## Shared interfaces

```kotlin
interface AudioRecorder

interface AudioPlayer

interface FileStorage
```

Provide Android and iOS implementations behind these interfaces.
