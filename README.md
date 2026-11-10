# AURA — أورا

**AURA** is a native Android app (Kotlin + Jetpack Compose + Material 3) with the calm of an
old museum: sand, parchment, ink and one gold. It talks to the AI
providers **you** configure. Conversations, providers and logs stay in a local
database; API keys are encrypted with the Android Keystore and never leave the
device. There is no first-party server, no account, no analytics.

<p align="center">
  <em>Nine screens, a pyramid mark, a live “what is the agent doing” panel, and a real test suite.</em><br/>
  <a href="https://github.com/123amramr123amr-svg/Kilo/actions/workflows/android.yml"><img alt="Android CI" src="https://github.com/123amramr123amr-svg/Kilo/actions/workflows/android.yml/badge.svg?branch=arena%2F89eb7ff1-kilo"/></a>
</p>

## Try it on your phone (no build needed)

1. Open this link **on the Android device**:
   <https://github.com/123amramr123amr-svg/Kilo/releases/download/phase1-debug/personal-ai-agent-debug.apk>
2. Allow your browser to install unknown apps when prompted
   (Settings → Apps → Special access → Install unknown apps).
3. Install and open **AURA** (debug build, `com.personalaiagent.debug`,
   Android 8.0 / API 26 or newer).
4. Pick العربية or English → **المحادثة → تهيئة المزوّد** → choose a preset (OpenAI,
   Anthropic, Gemini, Groq, DeepSeek) → paste your own API key → **اختبار الاتصال** → save.
5. The model list is fetched from your provider; choose a model in the composer and chat.

The APK is rebuilt and re-published by CI on every push to this branch. It is a
**debug-signed** build and it contains **no API keys** — you add your own provider key
inside the app, where it is encrypted with the Android Keystore.

Verified in CI: build **success**, unit tests **147/147 passing** (see
[`docs/VERIFICATION.md`](docs/VERIFICATION.md)).

## Feature highlights

- **The AURA look** — sand page, parchment cards, ink text and old gold accents, with a serif
  display face for headings; the pyramid mark (Canvas) is the launcher icon, the splash and the
  in-app emblem. Dark (burnt umber) and AMOLED themes remain one tap away in Settings.
  Responsive for phones and tablets.
- **Every conversation is saved with a name** — a history list with the last message, when
  it happened, how many messages it holds, search across names *and* messages, pin, rename
  and delete. Opening one resumes exactly where it stopped.
- **Fewer false errors** — the provider's own sentence, its HTTP status and its `Retry-After`
  header are kept and shown; 429/5xx/timeouts/dropped streams retry up to 4 times with
  backoff (and the provider's own wait); an empty quota or a bad key is never retried; a
  conversation that is too long is shortened once and sent again.
- **Show me what the model is doing** — the panel above the composer streams the model's own
  reasoning (`reasoning_content`), the plan, and every tool step with what went in (`→`) and
  what came back (`←`). Hide it from the panel or from Settings; the choice is remembered.
- **Fewer wasted round trips** — the “review before answering” step is now opt-in, so replies
  start immediately; a stream cut off without its terminator is retried instead of saved, and a
  truncated answer can be continued with «أكمل الإجابة».
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
- **Reads documents, not just pictures**: a typed PDF's text layer is extracted on the phone
  and sent as *text* — any page count, no image tokens, and it works even with a model that
  has no vision; a scanned PDF falls back to a rasteriser (10 pages, and the label says so);
  plain text/code/CSV/JSON files are read whole. Nothing is cut silently: the model is told
  exactly how much was omitted.
- **Ten shell-style tools**: `read` (line numbers), `edit` (refuses an ambiguous match),
  `patch` (all-or-nothing), `grep`, `glob` (`**` crosses folders), `shell` (the real Android
  shell in a real working directory, deny-list, timeouts, no root/PTY/python — said plainly),
  `diagnostics` (JSON/XML/brackets/quotes — shape, not a compiler), `question` (blocks the run
  until you answer), `skill` (loads instructions on demand) and the file tools below.
- **Skills** — bundled markdown instructions (`code-review`, `port-from-project`,
  `skill-creator`, `spike`, `systematic-debugging`, `pdf-and-docs`) that the agent loads only
  when a task needs them. Settings lists them with their triggers, deletes them, and
  «إضافة مهارة +» writes a new one into the app.
- **Token usage you can see**: a count under every answer, a running total for the
  conversation in the composer, and a total on each card in the history list.
- **Creates images**: `generate_image` calls `/images/generations` and saves the picture
  into your workspace.
- **Model switcher above the input**: every connected model, one tap, across providers.
- **«جلب النماذج» / Fetch models**: paste a base URL and API key, press one button and the
  provider's model names appear as a checklist. Tick the ones you want; they are saved with
  the provider and show up in the app immediately.
- **The phone is the agent's workspace**: pick a folder with the system folder picker, then
  the agent can *create, read, edit, patch, search and delete* real files in it through the
  file tools (`list_files`, `read_file`, `write_file`, `append_file`, `create_folder`,
  `delete_path`, plus `read`, `edit`, `patch`, `grep`, `glob`, `diagnostics`).
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
network call), and [`docs/PHASE5.md`](docs/PHASE5.md) for the model picker before a
new chat, conversation memory, run recovery and the agent-activity panel, and
[`docs/PHASE6.md`](docs/PHASE6.md) for the AURA identity (sand/ink/gold, serif type, pyramid
icon), the rewritten activity panel with the model's reasoning, the request to hide it, and the
speed/connection fixes, and [`docs/PHASE7.md`](docs/PHASE7.md) for the real error causes
(backoff, `Retry-After`, context trimming), the conversation history with names and search,
the rebuilt composer, and the comparison with claude-code / OpenMinis / ai-manus.
[`docs/PHASE8.md`](docs/PHASE8.md) covers the ten tools, the skills (and why custom commands
are *not* built), the token counters and the document pipeline — with the limits stated.
The design page from phase 6 lives at [`design/aura-preview.html`](design/aura-preview.html).
