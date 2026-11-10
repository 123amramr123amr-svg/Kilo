# Phase 5 — model picker, memory, sturdier runs, agent activity and smoother UI

Reported in one go (chat screen):

1. «قبل ما أفتح محادثة جديدة يقول لي أنهي نموذج أختار ويكمل معايا باقي المحادثة» — ask which model to use
   before a new conversation starts, then keep working with it.
2. «يبقى عنده ذاكرة وعارف المحادثة اللي أنا كلمته فيها» — the agent should remember the conversation and
   the earlier ones.
3. «ساعات بيقول لي لقد فشل وبيعلق جامد… عايز أسرع سرعه الرد» — failures that hang the screen, slow answers.
4. «الحركة بتاعتي وحشه» — navigation and animations feel rough.
5. «اعرض لي الوكيل بيعمل ايه… يظهر لي سهم لو دوست عليه» — show what the agent is doing, behind a tap.

## 1. Choosing the model before a new chat

- `ChatViewModel.requestNewChat()` opens the model sheet instead of silently starting an empty chat;
  `chooseModel()` stores the pick as the default model (`modelRepository.setDefault`) and only then
  starts the conversation, so the next launch keeps the same model.
- The empty chat has a primary «اختر نموذجًا لهذه المحادثة» action plus a secondary link to the full
  models screen; the same sheet switches models mid-conversation (`openModelPicker()`).

## 2. Memory

| Layer | Behaviour |
|---|---|
| Current conversation | The whole thread is sent (capped at 30 turns **and** 24 000 characters, so a long chat is trimmed by size, not silently by count). |
| Earlier conversations | Up to 3 previous conversations contribute their last two messages (180 chars each, max 900) as a labelled background block in the system prompt. |
| Source | Built locally from Room (`conversationDao.getRecent`), no extra request, no cost, nothing leaves the phone except into the user's own provider. |
| After a restart | The chat re-attaches to the latest conversation (`observeLatestConversation`), so the thread continues where it stopped. |

## 3. Failing runs, hangs and speed

- **One silent retry** for a transient failure (network, timeout, 429, 5xx) when nothing of that round was
  shown yet — the user never sees a duplicated answer.
- **`start()` now returns `Boolean`**: when a run is already active the composer no longer flips into a
  "generating" state that nothing will ever finish (this was the endless spinner).
- **Stall watchdog**: no event for 120 s → the run is cancelled and reported as a timeout with a retry
  button instead of hanging forever.
- **Review step**: skipped for messages shorter than 12 characters and hard-capped at 6 s; if the provider
  is slow the message is sent exactly as written instead of waiting.
- **Streaming throttle**: text is pushed to the UI at most every 60 ms (and accumulated in a
  `StringBuilder`) instead of rebuilding the string and re-composing the list per token.

## 4. Smoother UI

- Explicit 160–260 ms navigation transitions (fade + short horizontal slide) instead of hard cuts.
- `Modifier.animateItem()` on chat bubbles, `animateContentSize()` on the agent panel.
- Scrolling: a new message animates, streamed text only jumps when the user is already at the bottom —
  no animation restart on every token.
- The agent panel rotates its arrow with an animated value instead of swapping icons.

## 5. “What is the agent doing?” panel

A single compact row above the composer with an arrow (`ExpandMore`, rotating 180°):

- collapsed: a spinner (or a success tick) and the current step in plain language —
  «يقرأ ملفًا · notes/todo.md», «يبحث في الويب · أفضل هواتف 2026»;
- expanded: the plan the agent committed to, then every tool call with a ✓/✗ and the file, query, link,
  app or prompt it touched;
- it opens itself when the first tool runs, and stays openable afterwards for the last conversation step.

Tool names are mapped to human labels for all 11 tools (`list_files`, `read_file`, `write_file`,
`append_file`, `create_folder`, `delete_path`, `list_apps`, `open_app`, `web_search`, `open_url`,
`generate_image`). The detail passed to the UI now uses the *useful* argument of each tool
(`query` for search, `url` for links, `prompt` for images, `app` for apps, `path` for files).

## Verification

| Item | Result |
|---|---|
| `:app:assembleDebug` | **SUCCESS** |
| `:app:testDebugUnitTest` | **SUCCESS — 120 passed, 0 failed** |
| Workflow run | `42637757363` (success) |
| APK | 19,035,710 B, re-published on `phase1-debug` |

New tests (4): a new chat carries context from earlier conversations (and keeps it out of the turn list);
a transient failure is retried once and then succeeds; a rejected key is not retried; only momentary errors
are considered retryable.

## Known limits (honest)

- Memory is **text only**. Model ids, provider keys and settings are not part of it.
- There is still no conversation-list screen, so earlier chats are used as context but not browsable;
  they can only be reached through a full backup export. That is the next feature worth building.
- The stall watchdog aborts a run after two silent minutes; a model that legitimately needs longer to
  think without emitting anything (very rare with streaming) would be cut off — the retry button covers it.
