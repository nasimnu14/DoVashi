This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### OpenAI API key

The key is never committed. Transcription and translation call OpenAI directly (MVP).

- Android: put `OPENAI_API_KEY=sk-…` in the untracked `local.properties` (or export it as an environment variable) and rebuild; it is compiled into `BuildConfig`. Without it, recording fails fast with a missing-key error and no network call.
- iOS: put it in the git-ignored `iosApp/Configuration/Secrets.xcconfig` (wired up with the iOS platform components).
- Trade-off: the key is embedded in the built app (and in local Gradle caches), so treat any APK you share as holding it. The fix is a backend proxy: the app only knows the `TranslationService` and `SpeechToTextService` ports, so swap their two bindings in `SharedModule` (or point `OpenAiConfig.baseUrl` at the proxy).
- Debug builds log OpenAI traffic to logcat under `DoVashi-HTTP` (the `Authorization` header is masked); release builds never log it.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…