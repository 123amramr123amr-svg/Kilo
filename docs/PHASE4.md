# Phase 4 — why the provider screen said «حدث خطأ ما.» and why the models were not saved

Reported symptom (screenshots of **تعديل المزود**): base URL `https://inference.dahl.global/v1`,
name `deepseek-ai/DeepSeek-V4-Flash-0731`, OpenAI-compatible, key typed. Fetching models worked
(3 models listed, «تم الاتصال · ٣ نموذجًا متاحًا»), but a red banner above the form read only
«حدث خطأ ما.» and pressing apply/save left the provider without models, so chat had nothing to send to.

## Root causes (three, all real)

1. **Every unmapped HTTP status became `AppError.Unknown("HTTP n")`.**
   `httpStatusToAppError` only knew 401/403, 429, ≥500 and timeouts. A 404 from
   `/models` (wrong path or `/v1` missing) fell through to `Unknown`, whose UI text was the
   generic `error_unknown` sentence — the status code and the reason were thrown away.
2. **Applying the model ticks ran a second network request.**
   `applyModelSelection()` called `refreshCatalog()`, which re-reads the **stored** base URL and the
   **stored** key. Right after editing the URL on screen, that request hit the old/incorrect endpoint,
   failed, and its failure was rendered as `fetchError`. The ticked models were never persisted, so the
   provider stayed empty and chat failed. Nothing on screen was saved either.
3. **A silent bundled-catalog fallback masked the failure.**
   `refreshCatalog` used to store hard-coded model names when the provider could not be reached.
   Those names do not exist on the user's gateway, which produced later 404s in chat and made the
   bug look intermittent.

## Fixes

| Area | Change |
|---|---|
| `core/AppError.kt` | new `HttpNotFound` and `NotFound` cases |
| `data/remote/SseParser.kt` | 404 → `HttpNotFound`; 408/504 → `Timeout` (504 was previously unreachable, it was swallowed by `>= 500`); every other 4xx → `Server(code)` with the real status |
| `ui/util/UiText.kt` | both new cases have their own Arabic/English text, and `Unknown` now shows its underlying reason instead of a bare generic sentence |
| `domain/usecase/SendMessageUseCase.kt` | chat failures use the same wording, so a 404 in a conversation says "endpoint/route not found" |
| `domain/repository/Repositories.kt` + `ModelRepositoryImpl` | new `storeCatalog(providerId, models, enabledIds)` — persists a catalog **without any network call**; the bundled fallback is gone, a failed sync now surfaces as an error |
| `ProviderEditViewModel.save()` / `applyModelSelection()` | form (name, URL, kind, key) is persisted **first**, then the list that is already on screen is stored. Applying a selection never re-fetches. Ticks default to "all enabled" when nothing is ticked |
| `ProviderEditViewModel.save()` (existing provider, empty screen catalog) | still refreshes — because a changed base URL invalidates the cached models — but if that refresh fails the user **stays on the screen** with the reason instead of navigating away from a provider with zero models |
| `AddProviderUseCase` | accepts `knownModels`; creating a provider stores the fetched list instead of triggering another request |
| `ProviderEditScreen` | both banners are dismissible, "fetching…"/"saving…" labels, enabled-model counter, green confirmation after saving |

## Verification

| Item | Result |
|---|---|
| `:app:assembleDebug` | **SUCCESS** |
| `:app:testDebugUnitTest` | **SUCCESS — 109 passed, 0 failed** |
| Workflow run | `42569939319` (success) |
| APK | 18,999,678 B, re-published on `phase1-debug` |

New regression tests (11 added in this phase, 98 → 109):

- `data/remote/ErrorMappingTest` (5): 404 → `HttpNotFound`, 400/402/405/415/422 → `Server(code)`,
  408/504 → `Timeout`, ≥500 → `Server(code)`, and **no** status in 400..599 may map to `Unknown`.
- `ui/screens/providers/ProviderEditViewModelTest` (6): applying ticks saves the endpoint and stores
  the already-fetched catalog with **zero** refresh calls; a rejected update surfaces the real error
  and stores nothing; a failed fetch keeps the typed key and shows the reason; a new provider keeps
  its ticks until saved and stores the catalog once; saving after editing only the URL re-reads the
  catalog; a failed re-read keeps the user on the screen with the reason.

## What to do on the device

1. Providers → edit the provider → keep `https://inference.dahl.global/v1` (the `/v1` suffix is required).
2. «جلب النماذج» → tick the models you want → «حفظ وتطبيق».
3. You should now see a green confirmation with the enabled count, and models appear in the model
   switcher in chat. If the gateway answers with an error, the banner names it: 404 means the base URL
   path is wrong, 401/403 means the key is rejected, 429 means a rate limit.

Note on `inference.dahl.global`: it is a third-party relay. If it returns 404 for `/v1/models` while
its own dashboard works, try `https://inference.dahl.global/v1` exactly, or ask that service for the
OpenAI-compatible base path. The app now reports the status code instead of hiding it.

## Phase 4b — «provider not found»: the provider screen could not create a provider

Second report, same screen: «تعديل المزود» opens with a red banner `حدث خطأ: provider not found`,
so nothing can be saved.

**Root cause (navigation, not the network).** The chat setup card («أضفه الآن») navigated with
`navController.navigate(Routes.PROVIDER_EDIT)` — the *route pattern itself*,
`provider_edit?providerId={providerId}`. Navigation matched it and filled the argument with the
literal text `{providerId}`. The ViewModel therefore believed it was editing, opened the edit form
for a row that does not exist, and `ProviderRepositoryImpl.updateProvider` answered
`AppError.Unknown("provider not found")` on every save. The create path (`addProvider`) was
unreachable from that button, so the provider was never created.

A second, misleading detail: the blue «مفتاح مشفّر محفوظ على هذا الجهاز» banner was rendered
unconditionally, so a brand new provider looked like a stored one.

### Fixes

| File | Change |
|---|---|
| `ui/navigation/AppNavHost.kt` | the chat card navigates to `Routes.providerEdit(null)` |
| `ui/navigation/Routes.kt` | `providerEdit(null)`/blank → `provider_edit`; ids are percent-encoded (also `modelSettings`, `taskDetail`) with a dependency-free encoder, so an id can never corrupt a route |
| `ui/screens/providers/ProviderEditViewModel.kt` | an id that no longer resolves (deleted row, stale argument) degrades to the **add** form with an explanation instead of a dead edit screen; and if a **new** provider is created but its catalog cannot be fetched, the screen switches to editing that provider and shows the reason — no silent provider without models, no duplicate row on retry |
| `ui/screens/providers/ProviderEditScreen.kt` | the "key stored" banner only appears when a key really is stored |
| `res/values{,-ar}/strings.xml` | `error_not_found` now explains that the add form was opened |

### Verification

| Item | Result |
|---|---|
| `:app:assembleDebug` | **SUCCESS** |
| `:app:testDebugUnitTest` | **SUCCESS — 116 passed, 0 failed** |
| Workflow run | `42579957713` (success) |
| APK | 18,999,730 B, re-published on `phase1-debug` |

New tests (7): `RoutesTest` — no id for a new provider, ids are encoded, plain UUIDs stay readable,
non-ASCII stays escaped; `ProviderEditViewModelTest` — an unknown id degrades to adding a provider
(the save calls `addProvider`, never `updateProvider`), a deleted row falls back the same way, and a
created provider whose catalog failed stays on screen in edit mode with the error.
