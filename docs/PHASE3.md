# Phase 3 — modern look, planning, background work, device & media tools

Everything below is wired end to end (UI → ViewModel → use case → repository →
platform) and covered where possible by JVM tests. Build and test results are in
[`docs/VERIFICATION.md`](docs/VERIFICATION.md).

## 1. New default look: light, airy, modern

| Before | Now |
|---|---|
| Dark navy only | **Light is the default**: white cards on a soft lavender page, ink text, blue→purple accents |
| Plain text answers | **Rich rendering** of headings, bullet lists, quotes, code blocks and `inline code`, parsed by a pure-Kotlin Markdown subset (`MarkdownLite`) |
| Small crowded text | Larger line heights for Arabic and Latin, tighter headings, rounded 18–24 dp corners |

`ThemeMode` now has three values — `LIGHT` (default), `MIDNIGHT`, `AMOLED` — and the
choice is persisted and restorable from a backup. Existing installs keep working: any
stored theme is honoured, and the default only applies to fresh installs.

## 2. The agent asks before it works (task list + questions)

Pressing send no longer fires the request blindly. With **«راجع طلبي» / Review my
request** on (the default), the app first asks the model to read the request and reply
with JSON containing:

- `questions` — up to 3 tap-to-answer clarifying questions (language, length, target file…),
- `plan` — a 3–6 step checklist,
- `improved_prompt` — a sharper rewording of what you asked,
- `clarity` — an optional 0–100 clarity score.

The sheet shows the checklist and the questions as chips; your answers are appended to
the prompt, so the agent does not have to guess. Buttons: **ابدأ التنفيذ** (run the
improved prompt), **أرسل كما هو** (send exactly what you typed), dismiss to keep editing.
If the provider is unreachable the app builds a local review instead (language / detail /
where to save), so the flow never blocks. The toggle is persisted in settings.

While the run is going, the accepted checklist stays visible as **قائمة المهام** with a
running/done badge, and every tool call appears underneath it.

## 3. Runs survive leaving the app

- The run is owned by `AgentRunController` in an **application-scoped coroutine**, not by
  the chat ViewModel. Leaving the screen, opening another app or locking the phone no
  longer cancels it; reopening the chat re-attaches to the live stream.
- A **foreground service** (`AgentRunService`, type `dataSync`) shows a low-priority
  notification («المساعد الذكي يعمل») while a task is active, which is also what keeps the
  process alive on modern Android.
- **Stop is always reachable**: a persistent bar with the stop button sits above the
  messages while a run is active, and the composer button turns into stop as well.

## 4. The agent can open apps and search on your behalf

New tools:

| Tool | What it does |
|---|---|
| `list_apps` | lists launchable apps (label + package) visible to the app |
| `open_app` | opens the best match by name or package (`افتح واتساب`) |
| `web_search` | starts a search with `ACTION_WEB_SEARCH`, falls back to a browser URL |
| `open_url` | opens a link in the browser |
| `share_text` | hands text to the system share sheet (used by the UI, exposed internally) |

Package visibility is declared in the manifest (`<queries>`) so Android 11+ lets the app
see launcher apps, browsers, the search intent and share targets.

**Honest limitation:** the agent can *launch* an app and start a search, but it does not
tap inside another app's screens. Driving a third-party UI needs an `AccessibilityService`,
which this app deliberately does not ship (it would mean reading everything on screen).
So «افتح كذا وابحث عن كذا» works as: open the app / open the browser with the query.

## 5. Reviewing images and PDFs

- Attach a picture (**📎**) or a PDF from the chat composer.
- Images are downscaled to ≤1400 px and JPEG-encoded on device.
- PDFs are rasterised with the platform `PdfRenderer` — up to 5 pages per pick — and sent
  as images, so a vision model can review the pages without any PDF library or server
  parsing.
- Attachments are sent as `image_url` data URLs (OpenAI-compatible) or base64 `image`
  blocks (Anthropic). The message bubble keeps a 📎 list of what was attached.
- Models without vision will answer normally; the request only contains images when you
  attach something.

## 6. Creating images

`generate_image` calls the provider's `POST /images/generations` (DALL·E 3, `gpt-image-1`,
compatible gateways), accepts both `b64_json` and `url` responses, and **saves the result
into the workspace** (default `generated/image-<timestamp>.png`) so it is a real file you
own and can reopen, share or edit later.

Steps: configure an OpenAI-compatible provider whose key can generate images, then ask
«ارسم/أنشئ صورة …». The agent calls the tool, the file appears in the Workspace screen and
the path is reported in the answer.

**Not implemented:** video generation. No provider in the supported set exposes a stable
video API through these endpoints, so rather than a fake button the app says so and stops.
If you want a specific service (Runway, Veo, …) wired in, that is a small addition because
the image tool already defines the pattern (call endpoint → bytes → workspace file).

## 7. Switch models right where you type

Above the input there is now a horizontally scrollable row of **every connected model**
across all providers, with the active one highlighted, plus the compact picker menu that
also links to the Models screen. The choice applies to the next message immediately.

## 8. Bigger files

- Agent reads: `MAX_TOOL_READ_BYTES` 60 KB → **240 KB**, with `offset`/`limit` arguments so
  a large file can be read in chunks (`read_file {"path":"x","offset":24000,"limit":24000}`).
- Tool output forwarded to the model: 8 K → **24 K characters**.
- Workspace editor: 400 KB → **1.2 MB**.
- Unknown-size documents (some cloud providers) are read fully instead of showing `0 B`.

## Files added in this phase

```
domain/model/Attachment.kt          images/PDF attachments, generated images
domain/model/DraftReview.kt         questions + plan + improved prompt
domain/repository/DeviceControl.kt  app launching / search contract
domain/repository/ImageGeneration.kt image generation contract
domain/usecase/ReviewDraftUseCase.kt plan & questions (model JSON + local fallback)
domain/usecase/agent/AgentRunController.kt  app-scoped run + foreground service
platform/AndroidDeviceControl.kt    intents for apps, links, search, share
platform/AttachmentLoader.kt        image downscale + PdfRenderer → base64
platform/AgentRunService.kt         foreground service + notification
data/remote/RemoteImageGeneration.kt /images/generations client
ui/components/RichText.kt           Markdown → Compose typography
ui/util/MarkdownLite.kt             pure parser (unit tested)
ui/theme/*                          Light theme, palette, gradients
```
