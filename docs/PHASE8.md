# Phase 8 — the ten tools, real skills, document reading and token counts

This phase started from one message:

> «ضيف له shell Tools (Read/Edit/Patch/Grep/Glob/Shell/Code/Diagnostics/Question/Skill»
> «وكمان المهارات ديت ديفالو» · «عايز يظهر كده حد استخدام التكن وبيظهر كل محادثة اخدت كم توكن»
> «خليه يقدر يقرا الملفات البي دي اف اللي معموله من الصور والملفات العاديه ومهما كان حجمها كبير»

The rule for the whole phase: **no tool is a label**. Every one of the ten has an
implementation behind it, a test that calls it, and an honest sentence about what it cannot do.

---

## 1. The ten tools

| Tool | What it really does | Where the code is |
|---|---|---|
| `read` | reads a workspace file and returns it **with line numbers**, so the model can quote exactly what it will edit; `offset`/`limit` in lines | `AgentTools.readLines` |
| `edit` | replaces one exact `old_string` with `new_string`; **refuses** when the match is not unique, and asks for more context or `replace_all` instead of guessing | `AgentTools.editFile` |
| `patch` | applies a list of edits **all or nothing** — the first failure leaves the file untouched | `AgentTools.patchFile` |
| `grep` | case-insensitive regex search, one hit per line, `path:line: text`; caps at 120 hits / 1500 files / 512 KB per file and says when it stopped | `WorkspaceSearch.grep` |
| `glob` | shell-style patterns over the workspace: `**` crosses folders, `*` stays inside one, `?` is one character; a pattern without `/` (`*.kt`) matches anywhere | `WorkspaceSearch.glob` |
| `shell` | runs `/system/bin/sh -c` through `ProcessBuilder` in a **real** working directory | `platform/Shell.kt` |
| `diagnostics` | structural check: JSON validity, XML tag stack + duplicate `<string name>`, bracket/quote balance, NUL bytes, missing trailing newline | `Diagnostics.check` |
| `question` | suspends the run until the user answers (180 s), then either continues with the answer or tells the model «the user did not answer» | `QuestionBroker.ask` |
| `skill` | lists the installed skills, or loads one by name into the conversation | `AgentTools.loadSkill` |
| `code` | *not a separate tool*: the code tools are `read`/`edit`/`patch`/`grep`/`glob`/`diagnostics` above. There is no seventh "Code" tool pretending to compile — the diagnostics tool says plainly «this is not a compiler» | — |

The catalog the model sees is now 20 entries: the eleven older file/app/media tools remain
(so old conversations keep working) plus the nine above.

**Shell, honestly:** it is the Android system shell (`/system/bin/sh` with toybox), not a
Linux distribution. No root, no PTY, no `python`, no compiler, no `apt`. What it is good for:
`ls`, `find`, `wc`, `du`, `sed`, `grep`, `head`, `tail`, `sort`, `date`, `df`, small
`sh` loops. The tool description, the system prompt and the Settings copy all say this —
it is not presented as a full terminal.

- **Deny-list, checked twice** (`ShellPolicy`, in the tool layer *and* in the runner):
  `rm -rf /`, `mkfs`, `dd if=`, `reboot`, `shutdown`, `su -c`, `chmod -R 777 /`. The check is
  regex-based, so `rm -rf /storage/emulated/0/Download/x` still works while `rm -rf /` does not.
- **Timeout**: 1–60 s per call (default 20). A command still running is killed
  (`destroyForcibly`) and reported as exit 124, never left in the background.
- **Working directory**: the workspace folder when it has a real path
  (`/storage/emulated/0/Documents/…` via `SafPaths`), otherwise the app's private
  `filesDir/agent-home`. In the second case the tool output starts with a note saying so,
  because a shell that silently runs somewhere else is worse than one that admits it.
- **Output** is capped at 20 000 characters (the oldest part is kept, and the cap is visible).

## 2. Skills — the five from the screenshot, plus one

`app/src/main/assets/skills/<name>/SKILL.md`, front matter `name` + `description`, where the
**description is the trigger** ("use this when…"). The catalog (`- name: description`) is put
in the system prompt, so the model knows what exists without loading anything; the body is
loaded only when the `skill` tool is called.

| Skill | Trigger |
|---|---|
| `code-review` | reviewing a change before it ships |
| `port-from-project` | bringing a feature over from another project |
| `skill-creator` | writing a new skill |
| `spike` | a time-boxed experiment to answer one question |
| `systematic-debugging` | a bug that does not reproduce on the first try |
| `pdf-and-docs` | reading a PDF/long document (added for this phase) |

**They are editable, not decoration.** Settings → «مهارات الوكيل» lists every skill with its
description, a delete icon on each row, and «إضافة مهارة +» which writes a real
`filesDir/skills/<name>/SKILL.md` in the same format. Deleting a bundled skill writes a
tombstone (`filesDir/skills/.deleted`) instead of touching the APK — so «إزالة» works, and a
reinstall brings the bundled ones back.

Not implemented, and said out loud: the screenshot also shows **custom commands**
(`.clozed/commands/*.md`, triggered by `/name`). Skills are the part that was asked for and
the part that is built; commands are not.

## 3. Token usage — per answer and per conversation

- The provider's `usage` chunk was already stored on the message; now it is **shown**:
  a small line under each answer («١٢.٤k توكن») and a chip in the composer with the running
  total for the conversation.
- History cards show the same total per conversation (`SUM(tokensIn)`/`SUM(tokensOut)` over
  `task_runs`, computed in SQLite, so the list stays instant).
- Zero is rendered as **nothing** rather than «0 توكن» — a chip that always says zero is noise.
- Runs that failed or were cancelled still count: the tokens were spent either way.

## 4. PDFs and large files

Before: every PDF was rasterised, **first 5 pages only**, and any other file type was refused.

Now there are two paths, chosen by what the file *is*:

**a. Text layer (typed PDFs, and every text/code/CSV/JSON file).**
`platform/PdfTextExtractor.kt` reads the PDF itself: it walks the file for `stream … endstream`
blocks, inflates them with the JDK's zlib, decodes the text operators (`Tj`, `TJ`, `'`, `"`),
and reports a quality score. If the file has real text, it is sent **as text** — a 200-page
typed PDF becomes ~90 000 characters instead of 200 images (≈300 000 tokens) — and it works
even with a model that cannot see images at all.
Plain text, markdown, code, CSV, JSON, XML, HTML, logs, YAML read directly, up to 2 M characters.

**b. Pixels (scanned PDFs).** When there is no usable text layer, the file is rasterised with
the platform `PdfRenderer`: up to **10 pages** at 1700 px, with the label saying
«صورة صفحة 3 من 80 (أول 10)». Images are downscaled to 1400 px and re-encoded as JPEG.

**The honest limit.** No model can take a 600-page book in one message. What changed is that
the limit is (1) much bigger, (2) stated in the text the model receives, and (3) stated to the
model in a way it can repeat to the user:

> [... تم اختصار 41 000 حرف من أصل 89 000 — الملف أطول من المساحة المتاحة في هذه الرسالة ...]

The head and the tail of the document are both sent, so the question «what does this contract
say about cancellation?» can still be answered when the section is near the end.

Everything runs on the device: no upload to a parser, no extra permission, and the extracted
text is never written to disk outside the app's cache.

## 5. Tests

`190 passed, 0 failed` (CI run 42803886907), up from 147. New this phase:

- `AgentToolExecutorTest` — edit (unique / ambiguous / replace_all), patch all-or-nothing,
  grep hits and path:line format, glob at the root, shell timeout + deny-list + workdir,
  skill list/load/unknown, question answering (a real suspend/resume), diagnostics.
- `PdfTextExtractorTest` — plain and Flate streams, a scan that must **not** be reported as
  text, page order, the character budget, a missing file.
- `WorkspaceSearchTest` — glob to regex (`*`, `**/`, `?`), grep case-insensitivity, binary skip.
- `DiagnosticsTest` — invalid JSON, duplicate string resources, unclosed brace with its line,
  braces inside strings/comments ignored, empty file.
- `SafPathsTest` — `primary:`, `home:`, a card UUID, downloads `raw:`, and cloud providers
  (which have **no** path, so `shell` falls back and says so).
- `TokenUsageTest` — «8.5k» / «21k» / «1.2M» and the zero case.
- `SendMessageUseCaseTest` — a document is sent as text (not as an image part), and a
  follow-up question still sees the document (the in-memory vault).

## 6. Files

**New:** `domain/usecase/agent/{Skills,QuestionBroker,WorkspaceSearch,Diagnostics,DocumentVault}.kt`,
`core/SafPaths.kt`, `platform/{Shell,PdfTextExtractor}.kt`, six `assets/skills/*/SKILL.md`,
five test files.
**Changed:** `AgentTools.kt` (catalog + executor), `SendMessageUseCase` (document block, skill
catalog in the prompt, attachments as text), `AttachmentLoader` (two paths, no 5-page cap),
`ChatViewModel`/`ChatScreen` (question dialog, per-answer tokens, document picker),
`SettingsViewModel`/`SettingsScreen` (skills section + add dialog),
`ConversationHistoryScreen` (per-conversation tokens), `Daos`/`ChatRepositoryImpl`
(token sums), `Attachment`, `Chat.kt` (`TokenUsage`), both `strings.xml` (+11 keys each).

## 7. One more thing: the follow-up question

Attachments belong to one message, so «لخّص الـ PDF» worked but «وإيه اللي في آخر صفحة؟»
did not — the text was gone from the next request. `DocumentVault` keeps the extracted text
of the last **two** documents per conversation, in memory only (text, never images, never
written to disk), and re-sends it while the user keeps asking about it.

## 8. Known limits

- **Custom commands** (the `/name` list in the screenshot) are not built — see §2.
- **Scanned PDFs**: 10 pages per file, because each page is real money in tokens.
- **Very long documents**: 48 000 characters per message, stated in the text.
- **`shell`** is the Android shell, not Linux: no python, no compiler, no root.
- **Cloud workspaces** (Drive/OneDrive): file tools work through SAF, `shell` cannot see them.
- **`diagnostics`** checks shape, not meaning. It cannot prove a file compiles.
