# Phase 6 — AURA identity, a live “thinking” panel, faster and sturdier replies

## 1. The name

The app is now **AURA** (Arabic: **أورا**) — the name used in the design you sent. It replaces
“Personal AI Agent / المساعد الذكي الشخصي” everywhere: the launcher label, the onboarding headline,
the chat wordmark and the foreground-service notification.

Changing it again is a single line per language in `res/values/strings.xml` (`app_name`,
`onboarding_headline`, `agent_run_notification_title`, `app_tagline`) — nothing is hard-coded in Kotlin.

## 2. The look (from the design you sent)

| Token | Value | Where |
|---|---|---|
| Sand page | `#E8E0D5` | light background (`LightBackground`) |
| Parchment card | `#F5F0EB` | surfaces, chat bubbles of the agent, input field |
| Ink | `#1C1814` | text, user bubbles, send button |
| Old gold | `#B08A5D` / `#8A6A3F` | accent, status dot ring, hairlines, agent avatar ring |
| Nile teal | `#4C7A6B` | tertiary accents, the horizon under the mark |
| Serif display | `FontFamily.Serif` | headline / title styles (Noto Serif + Noto Naskh Arabic on device) |

- The four blue/purple tokens that every screen already used (`BrandBlue`, `BrandPurple`, …) were
  re-pointed to the sand palette, so **all nine screens** adopt the new identity without touching them.
- Dark and AMOLED themes were rebuilt on burnt umber instead of blue-black.
- Chat screen: wordmark header (pyramid mark, serif `A U R A` with wide letter spacing, gold hairline,
  live status dot), ink user bubbles with an 18/4 dp corner, agent answers as plain text with a
  gold-ringed serif “A”, parchment input and an ink send disc.
- `AuraMark` draws the pyramid, its sun and the horizon on a Compose canvas — no bitmap, any size,
  gently pulsing while a provider is connected. It replaces the robot orb on onboarding and the empty chat.

## 3. The icon: a pyramid

`ic_launcher_foreground` is now the pyramid (lit face, shadow face, gold rim light, sun disc, two sand
horizons) on a sand-gradient background, with a `<monochrome>` layer so Android 13+ themed icons work.
The same mark is used for the splash screen (`ic_splash_logo`) and inside the app (`ic_app_logo`).

## 4. “Show me what the AI is doing”

The activity panel above the composer — the arrow you asked for — now shows **three** things:

1. **العقل / Thinking** — the model's own reasoning, captured from `reasoning_content` (DeepSeek, Qwen,
   vLLM and other OpenAI-compatible “thinking” providers). It is streamed live, throttled to 60 ms, kept
   out of the saved answer, and capped at 8 000 characters.
2. **الخطة / Plan** — the steps the agent committed to before starting.
3. **كل خطوة / Every step** — each tool with what went in (`→ notes/plan.md`) and what came back
   (`← 240 lines`), a ✓/✗ mark and a connecting line. Tool arguments are mapped per tool
   (`query`, `url`, `prompt`, `app`, `path`), not just `path`.

It opens by itself when the first tool runs, the arrow rotates, and it can be **hidden for good** from
the ✕ in its header or from **Settings → خطوات الوكيل** (`AppSettings.showAgentActivity`, persisted).

If a provider does not expose reasoning (Anthropic, some relays), the panel simply shows the plan and the
tool steps — no invented “thoughts”.

## 5. Speed

- **The review step is off by default.** It used to add a whole extra model round trip *before* every
  message; now the toggle in the composer turns it on when you want questions and a plan
  (it is still capped at 6 s when enabled and skipped for messages under 12 characters).
- Streamed text and reasoning are pushed to the UI at most every 60 ms (accumulated in a `StringBuilder`),
  so the list no longer re-composes per token.
- Scrolling jumps instead of restarting an animation while text streams.

## 6. Connection problems during / after a conversation

- A stream that **closes without its terminator** (`[DONE]`) is now treated as a broken connection: it is
  retried once when nothing was shown yet, instead of silently saving a truncated answer.
- Transient failures (network, timeout, 429, 5xx) keep their single silent retry.
- A run with no event for 120 s is aborted with a timeout instead of hanging.
- If an answer is cut off anyway, the failed bubble offers **أكمل الإجابة** next to «إعادة المحاولة»:
  the instruction is bilingual, the partial text is already in the history, so the model continues
  without repeating itself.
- `start()` reports whether it really started, so the composer can never stay in a “generating” state.

## 7. The design page

`design/aura-preview.html` is the updated version of the file you sent: the same layout, but rebuilt to
match the app exactly — the real palette, the real copy (model picker, memory, activity panel with
`<details>` so it opens and closes), dunes and pyramid drawn in CSS instead of relying on a remote photo,
and a small design-system strip (colours, icon, type).

## Verification

| Item | Result |
|---|---|
| `:app:assembleDebug` | **SUCCESS** |
| `:app:testDebugUnitTest` | **SUCCESS — 122 passed, 0 failed** |
| Workflow run | `42645214520` (success) |
| APK | 19,053,170 B, re-published on `phase1-debug` |

New tests (2): the OpenAI decoder separates `reasoning_content` from the answer text, and also reads the
shorter `reasoning` field.

## Known limits (honest)

- Reasoning is only shown when the provider streams it; it is never synthesised.
- The audio/TTS screen still uses the old orb (voice is a different metaphor); everything else uses the mark.
- The icon is flat vector art — no gradients beyond the two sand bands, by design (crisp at 48 px).
