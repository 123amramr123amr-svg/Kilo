# Phase 7 — real error causes, a conversation history, and a calmer composer

This phase started from three complaints:

1. «بيعمل أخطاء كتير» — the app keeps failing with “the provider failed”, “rate limited”,
   “the provider took too long”, and a generic “an error happened”.
2. «عايز أقدر أرجع للمحادثات القديمة… كل محادثة ليها اسم».
3. «الحتة اللي ببعت فيها الرسائل شكلها وحش» — the composer was too tall and its controls
   were three different sizes.

---

## 1. Why the errors were real (and what changed)

### What was actually happening

| Symptom | Cause in the code | Fix |
|---|---|---|
| «حصل خطأ» with no detail | every non-2xx response became a bare code: `Server(429)` was rendered as “the provider returned an error (429)” and the provider’s own sentence was thrown away | the error body is read (max 8 KB), the message inside `error.message` is kept, and the UI shows it |
| «عدد ضربات كثير» (429) that never recovered | one retry only, after a **flat 1.2 s**, then a permanent failure banner | up to **4 attempts**, exponential backoff (2 s → 4 s → 8 s, ±20 % jitter), and the provider’s `Retry-After` / `x-ratelimit-reset` header wins over the backoff (capped at 60 s) |
| «المزوّد تأخر في الرد» on a model that was simply thinking | the shared OkHttp client had a **90 s read timeout**, and a stream is silent until the first token | streams now run on a clone of the client **without a read timeout**; the run watchdog (120 s with *no event at all*) is the only thing that declares a stream dead |
| «فشل» right after a dropped connection | a stream that closed without `[DONE]` was retried **only once** and then saved as a truncated answer | it is treated as a network failure and retried with the same policy; a genuinely cut answer still offers «أكمل الإجابة» |
| Errors when fetching the model list | `fetchModels` had no retry at all | it retries twice with backoff (capped at 6 s / 20 s for `Retry-After`) |
| A long conversation suddenly failing | no reaction to “context length exceeded” | the error is recognised, the prompt is **halved once** (the system prompt always survives, the newest turns stay) and the request is sent again; only if that fails does the user see «المحادثة أطول من قدرة النموذج» |
| An exhausted account retrying forever | 429 was always treated as transient | `insufficient_quota` / `402` become `QuotaExceeded`, which is **never** retried and says «اشحن الحساب أو بدّل المزوّد» |

While a retry is waiting, the chat shows a calm line instead of an error:

> المزوّد مشغول — إعادة المحاولة بعد ٧ ثانية (٢/٤) · تجاوز حدود الطلبات

The execution log records each attempt (`retry 2/4 after rate_limit`), so the task screen
tells the same story.

**New files:** `data/remote/ProviderErrors.kt` (pure classification + `Retry-After` parsing),
`domain/usecase/RetryPolicy.kt` (pure decision + backoff).
**Changed:** `HttpEngine`, `ChatModels` (`ProviderException` now carries the status, the
`Retry-After` and the detail), `AppError` (`RateLimit`, `Server`, `QuotaExceeded`,
`ContextTooLong`), `SendMessageUseCase`, `AgentRunController` (`RetryNotice`),
`ChatViewModel`, `ChatScreen` (retry line), `ModelRepositoryImpl`, `UiText`, both `strings.xml`.

## 2. Conversation history — «كل محادثة ليها اسمها»

New screen `ui/screens/history/ConversationHistoryScreen.kt` + `HistoryViewModel`:

- one card per conversation: **name**, last message as a preview, “٣ ساعة” / “أمس” / “12 Mar”,
  and how many messages it holds;
- **search** across names *and* message bodies (one SQL query, no message bodies loaded);
- **pin to top**, **rename**, **delete** (with confirmation);
- tapping a card opens that conversation exactly where it stopped.

The names are real names: `ConversationTitles.from()` takes the first line with words in it,
strips markdown (`##`, `-`, fences, backticks), collapses spaces and caps at 48 chars — no
extra model call, no extra tokens. Older chats that still carry “New conversation” are named
on their next message.

Implementation notes:

- `ConversationDao.observeSummaries()` / `searchSummaries()` compute the preview and the
  message count in SQLite (correlated sub-queries), so the list opens instantly.
- Pins are stored in DataStore (`pinnedConversations`), so **no Room migration** was needed
  and existing data is untouched.
- The chat screen’s header gained a history button; the route stays `chat`, the selected
  conversation is handed over through the entry’s `SavedStateHandle` (the same mechanism the
  voice screen already used).

## 3. The composer — «متناسقه مش بالشكل الوحش»

Before: a row of model chips, then a row of attach/review/model-dropdown pills, then a row
with mic + a 56 dp field + a 46 dp button. Three bands, four different heights.

After: **one** rounded card (22 dp) that grows with the text (54 → 140 dp, 6 lines max),
with its own toolbar inside it — attach, mic on one side, the ink send disc on the other.
The active model is a single quiet line above the card next to the review switch. The
streamed answer now uses the full width next to the avatar instead of a narrow bubble, and
message text is 16 sp / 26 sp line height (was 14 sp in answers, 14 sp in bubbles).

## 4. What was taken from the three projects

| Project | What it really is | What we took | What we did **not** take (and why) |
|---|---|---|---|
| **anthropics/claude-code** | the product is closed source; the public repo is documentation, plugins, hooks and changelogs | session **resume with a name per session**, a compact **summary line per session**, retry on overload, and **compaction when the context is too long** — which here becomes “halve the prompt once, keep the system prompt” | terminal UI, sub-agents, MCP servers, git worktrees: they need a desktop shell and a repository, not a phone agent |
| **OpenMinis/OpenMinis** (Swift, 1000+ files) | an open-source iOS agent with a genuinely deep provider layer | the **error taxonomy** (`network / transient+status / rateLimited / invalidKey / cancelled`), **`Retry-After` driving the wait**, a **request budget** idea (we use a context budget), and the **session list** model: search, pin, rename, preview, relative time, title generation | iCloud sync, MCP, bash/ISH, iSH shell, image-budget re-encoding, Live Activities: iOS-only or far outside this app’s scope |
| **quyetcao/ai-manus** | **not a working project** — the repository contains a marketing README, three empty `test_file*.py` stubs and a download link to a `.rar` | nothing: there is no code to compare or reuse. Recorded here so the comparison is honest | everything |

The concrete debt this phase paid: our old provider layer had exactly the two weaknesses
OpenMinis spends hundreds of lines avoiding — a single flat retry and no distinction between
“this model has no capacity” and “your key/quota is wrong”. Both are now explicit.

## 5. Verification

| Item | Result |
|---|---|
| `:app:assembleDebug` | **SUCCESS** |
| `:app:testDebugUnitTest` | **SUCCESS — 147 passed, 0 failed** (was 122) |
| Workflow run | `42767407934` |
| APK | 19,140,678 B, re-published on `phase1-debug` |

New tests (25): `RetryPolicyTest` (11) — backoff growth, `Retry-After` wins, absurd hints are
capped, give-up after the last attempt, no retry for a bad key / empty quota / 4xx, no retry
once text is on screen, context trimming happens once; `ProviderErrorsTest` (10) — detail
extraction, quota vs rate limit, context-length detection, `Retry-After` as seconds **and**
as an HTTP date, status-only mapping unchanged; `ConversationTitlesTest` (5) — first line,
markdown stripping, Arabic punctuation, truncation, placeholders.

Two of these tests failed on their first CI run and caught a real bug: the policy ignored
the `Retry-After` carried by the error object. Fixed in the very next commit (`fix(retry): honour the Retry-After hint carried by the error itself`)
and green afterwards — the schedule is now tested, not just written.

## 6. Still honest about

- **No device screenshots**: the build and the JVM tests run in CI, but nothing here renders
  the UI. If a card overlaps on a small screen, it will be visible on your phone first.
- **Search is `LIKE`-based**: it matches substrings in titles and message bodies; it is not
  a tokenised full-text index yet.
- **Pins live in DataStore**, not in a database column: they survive restarts and backups of
  preferences, but a JSON backup/restore does not carry them.
- **Folders** (OpenMinis has them) were not added: pinning covers the “keep this on top” need
  without a second navigation level.
- **Compaction is a halving, not a summary**: we drop the oldest turns and keep the system
  prompt; a model-written summary of the dropped part is a possible later improvement (it
  costs one extra request, which is exactly what this phase tried to avoid).
