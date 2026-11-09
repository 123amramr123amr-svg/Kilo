# Personal AI Agent — المساعد الذكي الشخصي

A native Android app (Kotlin + Jetpack Compose + Material 3) that talks to the AI
providers **you** configure. Conversations, providers and logs stay in a local
database; API keys are encrypted with the Android Keystore and never leave the
device. There is no first-party server, no account, no analytics.

<p align="center">
  <em>Phase 1: full UI, local data layer, provider clients, execution log, tests.</em><br/>
  <a href="https://github.com/123amramr123amr-svg/Kilo/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/123amramr123amr-svg/Kilo/actions/workflows/android.yml/badge.svg?branch=arena%2F93372d4c-kilo"/></a>
</p>

## Try it on your phone (no build needed)

1. Open this link **on the Android device**:
   <https://github.com/123amramr123amr-svg/Kilo/releases/download/phase1-debug/personal-ai-agent-debug.apk>
2. Allow your browser to install unknown apps when prompted
   (Settings → Apps → Special access → Install unknown apps).
3. Install and open **Personal AI Agent** (debug build, `com.personalaiagent.debug`,
   Android 8.0 / API 26 or newer).
4. Pick العربية or English → **المحادثة → تهيئة المزوّد** → choose a preset (OpenAI,
   Anthropic, Gemini, Groq, DeepSeek) → paste your own API key → **اختبار الاتصال** → save.
5. The model list is fetched from your provider; choose a model in the composer and chat.

The APK is rebuilt and re-published by CI on every push to this branch. It is a
**debug-signed** build and it contains **no API keys** — you add your own provider key
inside the app, where it is encrypted with the Android Keystore.

Verified in CI: build **success**, unit tests **56/56 passing** (see
[`docs/VERIFICATION.md`](docs/VERIFICATION.md)).

## Feature highlights

- **Modern light UI by default** — white cards, soft lavender page, blue/purple accents,
  rounded surfaces and rich typography (headings, lists, code) in answers. Dark (midnight)
  and AMOLED themes remain one tap away in Settings. Responsive for phones and tablets.
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
- **Plan before work**: the agent shows a task list and asks up to three clarifying
  questions (with tap-to-answer chips) before starting, then keeps the checklist visible.
- **Background execution**: tasks continue when you leave the app (foreground service +
  app-scoped run controller) and a stop button is always on screen.
- **Opens apps & searches**: `list_apps`, `open_app`, `web_search`, `open_url`.
- **Reviews images and PDFs**: attach a photo or a PDF (pages are rasterised on device) and
  a vision model reads it.
- **Creates images**: `generate_image` calls `/images/generations` and saves the picture
  into your workspace.
- **Model switcher above the input**: every connected model, one tap, across providers.
- **«جلب النماذج» / Fetch models**: paste a base URL and API key, press one button and the
  provider's model names appear as a checklist. Tick the ones you want; they are saved with
  the provider and show up in the app immediately.
- **The phone is the agent's workspace**: pick a folder with the system folder picker, then
  the agent can *create, read, append to and delete* real files in it through six tools
  (`list_files`, `read_file`, `write_file`, `append_file`, `create_folder`, `delete_path`).
  Every tool call is shown in the chat and recorded as an `EXECUTE_TOOL` step in the
  execution log.

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

## Workspace & file permissions

The agent never asks for broad storage access. You choose a folder with the system folder
picker (`OpenDocumentTree`); the app keeps a *persistable* read/write grant for exactly that
tree, and every path is normalised and checked so `..` or absolute paths cannot escape it.

| Android version | What is requested |
|---|---|
| 13+ (API 33+) | Nothing for files — the Storage Access Framework grant is enough |
| 8.0 – 12L (API 26–32) | `READ_EXTERNAL_STORAGE` is declared (maxSdkVersion 32) so the folder picker can hand out the tree; it is also listed on the Permissions screen |
| Any | Microphone (voice), notifications (task results) — requested when you use them |

Background runs use a `dataSync` foreground service with a visible notification; the agent
never reads other apps' screens (no accessibility service).

`MANAGE_EXTERNAL_STORAGE` is deliberately **not** used: the app stays installable from Play
and can only touch the folder you picked. Tools are on by default once a folder is
connected; a switch in Settings → Workspace turns them off, and no tools are sent to the
model at all while no folder is connected.

## Phase 1 scope

See [`docs/PHASE1.md`](docs/PHASE1.md) for the screen map, architecture notes,
compatibility constraints and the list of known limitations,
[`docs/PHASE2.md`](docs/PHASE2.md) for the fetch-models flow and the workspace agent
(tool schemas, agent loop, safety rules), and
[`docs/PHASE3.md`](docs/PHASE3.md) for the light theme, planning/questions flow,
background runs, app/URL tools, image & PDF support and image generation, and
[`docs/PHASE4.md`](docs/PHASE4.md) for the provider/model-saving fixes (real HTTP
status codes instead of a generic error, applying model ticks without a second
network call).
