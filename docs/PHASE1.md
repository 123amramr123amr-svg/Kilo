# Phase 1 — Implementation report

Native Android app **Personal AI Agent** (Compose + MVVM + Hilt + Room).
Everything below describes what is actually implemented in `app/src/main`.

> **Build status:** `:app:assembleDebug` and `:app:testDebugUnitTest` run green on
> GitHub Actions (56/56 tests). An installable debug APK is published on the
> `phase1-debug` release. Details and the defects caught by the real compiler:
> [`VERIFICATION.md`](VERIFICATION.md).

## Screens (all interactive, all localised AR/EN)

| # | Screen | Route | Notes |
|---|--------|-------|-------|
| 1 | Onboarding + language | `onboarding` | Language cards write the preference and recreate the activity so resources switch AR/EN; Start/Skip persist `onboardingCompleted`. |
| 2 | Main chat | `chat` | Message list, streaming bubble, model picker, mic button, send/stop, retry, error banner, provider CTA, conversation delete/new. |
| 3 | Account | `account` | Local badge, profile entry, privacy, API keys, backup, version, "sign out" wipes local data. |
| 4 | Settings | `settings` | Language + theme dialogs, streaming/TTS/haptics switches, provider/model/task-log/permission/account entries. |
| 5 | Models | `models` | Search, filter chips, enable switch, default star, capability tags, context size. |
| 6 | Model settings | `model_settings/{modelId}` | Temperature, max tokens, top-P sliders, system prompt, streaming, enable, set default, reset, save. |
| 7 | AI providers | `providers` + `provider_edit` | List with status/default/key presence, add/edit/delete, presets, test connection, refresh catalog. |
| 8 | Permissions | `permissions` | Runtime status, per-permission request, system-settings shortcut, grant-missing. |
| 9 | Execution log | `tasks` + `task_detail/{taskId}` | Search, status pills, duration, tokens, per-step timeline. |
| 10 | Voice assistant | `voice` | System `SpeechRecognizer`, partial transcript, waveform, confirm → returns text to the composer. |
| 11 | Privacy | `privacy` | Data-flow explanations, delete conversations/logs. |
| 12 | API keys | `api_keys` | Add/update/delete encrypted keys per provider; value never displayed. |
| 13 | Backup | `backup` | SAF export/import of JSON without keys. |
| 14 | Profile | `profile` | Local display name. |

## Architecture

```
ui/ (Compose screens, one ViewModel per screen, StateFlow + sealed events)
 └─ domain/ (models, repository interfaces, use cases)
     └─ data/ (Room, DataStore, Keystore credential store, OkHttp provider clients, backup)
```

* **MVVM + unidirectional data flow**: immutable `UiState` data classes, `SharedFlow`/`Channel` for one-off events (snackbars, speak, navigation).
* **Use cases**: `SendMessageUseCase` (agent task orchestration), `AddProviderUseCase`, `ClearLocalDataUseCase`, `ProviderValidator`.
* **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`), qualifiers for `IoDispatcher`, interfaces bound to implementations in `RepositoryModule`.
* **Coroutines/Flow**: Room `Flow` → `StateFlow` in ViewModels; streaming completions are `Flow<ChatChunk>` collected inside the send use case.
* **Navigation**: Navigation-Compose with nested routes; bottom bar only on top-level destinations; voice → composer hand-off through `SavedStateHandle`.

### Execution log
Every send creates a `task_runs` row plus five `task_steps` (`RESOLVE_PROVIDER → BUILD_PROMPT → SEND_REQUEST → RECEIVE_RESPONSE → PERSIST_MESSAGE`) with statuses and timings. Cancelled/ failed generations keep the partial text.

## Security decisions

| Concern | Implementation |
|---|---|
| No keys in source/VCS | No key literals anywhere; `.gitignore` blocks `secrets.properties`, `local_api_keys.json`, keystores. Keys are only entered by the user at runtime. |
| Secure storage | AES-256-GCM via a non-exportable Android Keystore key (`AndroidKeystoreCipher`), ciphertext in private prefs under obfuscated keys (`EncryptedCredentialStore`). |
| No key leakage in logs | No `Log` calls involving credentials; OkHttp logging is `BASIC` (no bodies) only in debug, with `Authorization`/`x-api-key` redacted. |
| No plaintext transport | `ProviderValidator` rejects `http://` except loopback/private-network hosts (local model servers). |
| Backups | Cloud backup + device transfer exclude prefs/DB; manual backup DTO has no key field and sets `keysIncluded = false`. |
| No fake cloud account | Account is explicitly local; no login/sync UI, "sign out" deletes local data only. |
| Permissions | Only `INTERNET`, `RECORD_AUDIO`, `POST_NOTIFICATIONS`; nothing else in the manifest. |

## Compatibility notes

* `minSdk 26` — required for the adaptive-icon/`java.time` baseline, reliable `Locale`/RTL support and the Keystore GCM guarantees used here. Devices below Android 8.0 are not supported.
* `targetSdk 35`, `compileSdk 35`, AGP 8.7.3, Kotlin 2.0.21 (Compose compiler plugin), JDK 17.
* `POST_NOTIFICATIONS` is requested only on Android 13+; on older versions it is reported as *not applicable*.
* Plain-HTTP provider URLs are limited to local/private hosts (Android cleartext policy is not globally relaxed anywhere).
* Text-to-speech and speech recognition depend on engines installed on the device; both degrade gracefully with an explicit error state.
* Backup/restore requires a system document provider (SAF); no storage permission is requested.

## Known limitations (phase 1)

1. Streaming is token-based text only — no tool/function calling execution yet (the `TOOLS` capability is displayed, not executed).
2. Anthropic model discovery uses `GET /v1/models`; if a gateway does not implement it the app falls back to a small bundled catalog (marked as such in code) and lets the user retry.
3. Only one conversation is resumed at a time (latest); a conversation-list screen is intentionally deferred.
4. Notification permission is surfaced in the Permissions screen but no foreground notification is emitted yet (task execution happens while the app is visible).
5. Instrumented UI tests and screenshot tests are not part of phase 1; JVM unit tests cover parsers, catalog logic, validation, credential storage, backup format and the send use case.
