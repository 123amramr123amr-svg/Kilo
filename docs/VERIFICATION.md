# Verification report (phase 1)

## What could and could not be executed here

This sandbox has **no JDK, no Gradle and no Android SDK**, and outbound network access
is limited to GitHub / npm / PyPI. The Android build needs Maven Central, Google Maven
and a Gradle distribution, which are not reachable:

```
$ java -version            → java: command not found
$ which gradle             → gradle: MISSING
$ which sdkmanager adb     → android-sdk: MISSING
$ curl https://repo.maven.apache.org        → BLOCKED (HTTP 000, TLS refused)
$ curl https://maven.google.com             → BLOCKED
$ curl https://services.gradle.org          → BLOCKED
$ curl https://plugins.gradle.org           → BLOCKED
```

Therefore `gradle :app:assembleDebug` / `:app:testDebugUnitTest` **could not be run
here**. Nothing in this report claims a successful compilation. The project is wired
so that CI (`.github/workflows/android.yml`, JDK 17 + Gradle 8.9 + Android SDK) runs
those exact commands on push.

## Static verification that *was* run

| Check | Method | Result |
|---|---|---|
| Kotlin brace/paren/bracket balance, all 97 `.kt` files | script (strings/comments stripped) | **0 imbalances** |
| `package` ↔ directory agreement (main + test) | script | **0 mismatches** |
| Duplicate top-level type declarations per package | script | **none** |
| All `com.personalaiagent.*` imports resolve to a declared package/type | script | **0 unresolved** |
| All `R.string.*` references exist | script vs `values/strings.xml` | **0 missing** |
| AR/EN string parity + format-specifier parity (`%1$s`, `%1$d`) | script | **321/321 keys, 0 mismatches** |
| Every repository interface is fully implemented | script (interface member diff) | **0 missing members** (Provider, Model, Chat, Task, Settings, CredentialStore, ProviderClient ×2) |
| Hilt graph: every `@Inject` constructor dependency is provided/bound/injectable | script | **0 unresolved** |
| DAO method calls exist in the DAO interfaces | script | **0 unknown methods** |
| All XML resources + manifest parse | `xml.etree` | **14/14 valid** |
| Version catalog parses; no dangling `libs.*` references | `tomllib` | **OK** |
| Screen ↔ navigation callback signatures match | manual review | **OK** |
| Composable-only calls (`stringResource`, `remember`, …) kept out of `onClick`/`LaunchedEffect`/`collect` bodies | script + manual review | **OK** (localised strings are pre-computed before coroutine blocks) |

## Defects found during review and fixed

1. **Duplicate Dagger binding** — `provideOpenAiClient` and `provideAnthropicClient` both
   returned `ProviderClient` unqualified, which is an `@Provides` duplicate. They now
   return the concrete `OpenAiCompatibleClient` / `AnthropicClient` types.
2. **Duplicate binding for `BackupManager`** — `@Inject constructor` *and* an `@Provides`
   method. The constructor annotation was removed.
3. **Dependency-injection safety** — credential storage was refactored behind a
   `KeyValueStore` interface so it can be unit-tested on the JVM; the Keystore-backed
   cipher is the only Android-specific part.
4. **Language-change loop risk** — `MainActivity` used to recreate itself whenever the
   settings language differed from the value read in `attachBaseContext`, without first
   writing that value; a stale mirror would have caused an infinite recreate loop. The
   mirror is now written *before* `recreate()`.
5. **RTL polish** — back arrows now use the auto-mirrored icon; `send` already did.
6. **Optional navigation argument** — `provider_edit` was navigated with an empty
   `?providerId=`; it now omits the query parameter for the "add" case.
7. **Double navigation-bar inset** — removed `navigationBarsPadding()` from the
   `NavigationBar` (Material 3 already applies its own window insets).
8. **SQLite ordering** — DAO ordering no longer relies on the implicit `rowid`.
9. **Double back-stack clear** — sign-out now pops to `navController.graph.id`.

## Tests written (56 test methods, run on CI / locally with an SDK)

`app/src/test/java/com/personalaiagent/`

- `SseStreamParserTest` (7) — SSE framing: multi-line data, named events, comments,
  flush at EOF, state reset.
- `StreamDecoderTest` (10) — OpenAI and Anthropic chunk decoding, usage, `[DONE]`,
  malformed JSON → `Parse` error, Anthropic error events.
- `ModelCatalogTest` (8) — non-chat model filtering, unknown ids kept, dedupe,
  capability inference, context windows, bundled fallback.
- `ProviderValidatorTest` (8) — https accepted, http rejected for public hosts,
  private/loopback http allowed, normalisation, malformed URLs.
- `EncryptedCredentialStoreTest` (4) — ciphertext-only persistence, key lifecycle,
  corrupt payload → `null`, full clear.
- `BackupSerializationTest` (4) — round-trip, Arabic content, **no key material in the
  file**, future-version detection.
- `HttpEngineStreamingTest` (5, MockWebServer) — real SSE streaming end-to-end,
  `Authorization` header, 401 → `Auth`, 429 → `RateLimit`, catalog filtering,
  non-streaming single delta.
- `SendMessageUseCaseTest` (10) — streaming + persistence + task success, all five
  execution steps recorded, request payload (system prompt, model, key), blank input,
  missing key, no model, no provider, provider error → failed message + failed task,
  empty completion → parse error, conversation reuse.

Run them with:

```bash
gradle :app:testDebugUnitTest
```

## Honest limitations of this verification

- Static analysis cannot prove that Compose Material 3 icon names, experimental
  annotations or exact parameter names match the resolved library version. Those are
  the first things CI will confirm.
- No instrumented tests (Room, Keystore, `SpeechRecognizer`, SAF flows) exist in
  phase 1; they require a device/emulator.
- No APK was produced, so runtime behaviour is unverified.
