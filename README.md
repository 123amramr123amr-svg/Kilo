# Personal AI Agent — المساعد الذكي الشخصي

A native Android app (Kotlin + Jetpack Compose + Material 3) that talks to the AI
providers **you** configure. Conversations, providers and logs stay in a local
database; API keys are encrypted with the Android Keystore and never leave the
device. There is no first-party server, no account, no analytics.

<p align="center">
  <em>Phase 1: full UI, local data layer, provider clients, execution log, tests.</em>
</p>

## Feature highlights

- **Dark branded UI** (black/navy + blue/purple), responsive for phones and tablets.
- **Arabic (RTL) and English (LTR)** — full string parity, mirrored layouts/icons,
  per-app language switching without restarting into a wrong direction.
- **Streaming chat** with a composer, model picker, microphone and stop button.
- **Providers**: OpenAI-compatible (OpenAI, Groq, DeepSeek, Gemini compat, local
  gateways) and Anthropic Messages API; model catalog fetched from the provider.
- **Per-model settings**: temperature, max tokens, top-P, system prompt, streaming.
- **Execution log**: every send becomes a task with five timed steps, tokens and
  the partial answer when generation is cancelled or fails.
- **Voice assistant** screen using the system speech recogniser; optional spoken
  replies through the system TTS engine.
- **Backup/restore** to a JSON file via the Storage Access Framework — keys excluded.

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
| Gradle | 8.9+ (AGP 8.7.3) |
| Android SDK | compileSdk 35, minSdk 26, targetSdk 35 |
| Kotlin | 2.0.21 |

## Build & test

```bash
# from the repository root, with an Android SDK available
gradle :app:testDebugUnitTest      # JVM unit tests
gradle :app:assembleDebug          # debug APK -> app/build/outputs/apk/debug/
```

No Gradle wrapper JAR is committed (binaries are not added to this repo). Use your
installed `gradle` 8.9+, or generate a wrapper once with `gradle wrapper --gradle-version 8.9`.
Android Studio can also import the project directly.

CI (`.github/workflows/android.yml`) installs Gradle 8.9, runs the unit tests and
builds the debug APK.

## First run

1. Pick Arabic or English.
2. Open **المحادثة → تهيئة المزوّد** (or Settings → AI providers → Add).
3. Choose a preset (e.g. OpenAI), paste your API key, press **اختبار الاتصال**, save.
4. The model catalog is downloaded; pick a model in the composer and start chatting.

## Project layout

```
app/src/main/java/com/personalaiagent/
├── core/                  AppError, Outcome, constants, injectable clock/ids
├── domain/
│   ├── model/             Immutable models (Provider, AiModel, ChatMessage, TaskRun…)
│   ├── repository/        Repository + CredentialStore interfaces
│   └── usecase/           SendMessageUseCase, AddProviderUseCase, ClearLocalData,
│                          ProviderValidator
├── data/
│   ├── local/             Room entities, DAOs, database, mappers
│   ├── secure/            Keystore AES-GCM cipher, encrypted credential store
│   ├── prefs/             DataStore settings repository
│   ├── remote/            OkHttp engine, SSE parser, OpenAI/Anthropic clients
│   ├── repository/        Repository implementations
│   └── backup/            Backup DTOs + export/import manager
├── di/                    Hilt modules
├── platform/              Locale wrapper, TTS speaker
└── ui/                    theme, components, navigation, screens/, util
app/src/test/java/com/personalaiagent/   unit tests
docs/PHASE1.md                           implementation report & limitations
```

## Security model (summary)

- No API keys in source, Gradle files, resources or Git (`.gitignore` also blocks
  `secrets.properties`, `local_api_keys.json` and keystores).
- Keys are encrypted with AES-256-GCM using a **non-exportable** Android Keystore key;
  only ciphertext is written to private storage under obfuscated keys.
- Keys and message bodies are never logged. HTTP logging is `BASIC` and debug-only,
  with `Authorization` / `x-api-key` redacted.
- Cloud backup and device transfer exclude the credential file, database and preferences.
- Manual backups contain no key material (`keysIncluded: false`).
- Plain `http://` provider URLs are rejected except for loopback/private-network
  hosts (local model servers), so secrets are not sent unencrypted over the internet.
- The account is explicitly **local**; "sign out" deletes local data. No cloud
  login or sync is implied anywhere in the UI.

## Phase 1 scope

See [`docs/PHASE1.md`](docs/PHASE1.md) for the screen map, architecture notes,
compatibility constraints and the list of known limitations. Phase 2 work is not
included per the current brief.
