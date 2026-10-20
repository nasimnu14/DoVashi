# Plan — 15-openai-api-security

Inputs: `grill-notes.md` (D1–D8), `domain-model.md` (S1–S5).

## Step 1 — Land the debug HTTP logging
- Keep the in-tree changes (`ktor-client-logging`, `createHttpClient(engine, trafficLogger)`, `LogcatHttpLogger`, `androidPlatformModule(logHttpTraffic)`, `DoVashiApplication`, `SharedModule` `getOrNull<Logger>()`) and `HttpClientFactoryTest`.
- Verify they compile and pass.

## Step 2 — Hard-coded-key guard
- `shared/src/androidHostTest/.../security/NoHardcodedSecretsTest.kt`: scan source dirs for `sk-[A-Za-z0-9_-]{20,}`; assert `.gitignore` lists `local.properties` and `*.key`.

## Step 3 — iOS secrets ignore + docs
- `.gitignore`: add `Secrets.xcconfig`.
- `README.md`: "OpenAI API key" section: `local.properties`/env on Android, `Secrets.xcconfig` on iOS (ticket 14), the release-APK trade-off, and the proxy path (swap the two DI bindings or `baseUrl`).

## Verify
`./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:assembleRelease`

## Acceptance criteria
1. No key in source or Git; a test fails if one appears.
2. OpenAI is reachable only through the two ports; swapping the bindings is the only change a proxy needs.
3. Release builds log no HTTP traffic; debug logs mask the key.
4. Setup and trade-offs are documented.
