# Phase 2 — fetch models & phone-workspace agent

Two additions on top of phase 1. Both are wired end to end (UI → ViewModel → use case →
repository → data) and covered by JVM tests; the debug APK is rebuilt on every push.

## 1. «جلب النماذج» — fetch models with only URL + key

Screen: **الإعدادات → المزوّدون → إضافة/تعديل مزوّد**.

1. Type the base URL and the API key (nothing needs to be saved first).
2. Press **جلب النماذج**. The app calls `GET {baseUrl}/models`
   (`Authorization: Bearer …` for OpenAI-compatible, `x-api-key` + `anthropic-version`
   for Anthropic) using the values currently on screen — a draft `Provider` object, so no
   row is written to the database and no key is persisted.
3. The returned model names are listed in a dialog as a checklist (select all / clear /
   count). Everything is ticked by default.
4. Applying a selection:
   - **Existing provider** → the catalog is synced and `applySelection()` enables exactly
     the ticked models, disables the rest, and guarantees a usable default model.
   - **New provider** → the ticks are kept in memory and applied right after the provider
     is created.
5. Errors are specific: no key → `MissingKey`, empty catalog → `NoModel`, network/HTTP →
   `Network`/`Auth`/`RateLimit`/`Server`, all localised in AR and EN.

`ModelCatalog.filter` keeps chat models when the provider returns a mixed list and falls
back to the full list when nothing matches, so the checklist is never mysteriously empty.

## 2. The phone as the agent's workspace

### Granting access

- `ActivityResultContracts.OpenDocumentTree` → the user picks one folder.
- `SafWorkspaceRepository.connect()` takes a **persistable** read/write URI grant and
  stores the tree URI + display name in settings.
- No `MANAGE_EXTERNAL_STORAGE`. `READ_EXTERNAL_STORAGE` is declared with
  `maxSdkVersion="32"` (Android 13+ needs nothing) and appears on the Permissions screen.

### Tools

| Tool | Purpose |
|---|---|
| `list_files` | list a folder (`""` = root) with sizes |
| `read_file` | read a text file (capped at 60 KB, marked truncated) |
| `write_file` | create or overwrite a file, creating parent folders |
| `append_file` | append text, creating the file if missing |
| `create_folder` | create a folder (and missing parents) |
| `delete_path` | delete a file/folder the user asked to remove |

Schemas live in `AgentToolCatalog` and are converted to the provider's wire format
(OpenAI `tools[].function`, Anthropic `tools[]` + `tool_use`/`tool_result` blocks).

### Agent loop

`SendMessageUseCase` streams a completion; if the model asks for tools it:

1. appends an assistant turn carrying `toolCalls`,
2. runs each tool through `AgentToolExecutor` (path guard first),
3. emits `ToolActivity` so the chat shows *which* file was touched and whether it worked,
4. appends one `TOOL` turn per result with its `toolCallId`,
5. requests the next completion — up to `MAX_AGENT_ROUNDS = 6`, then a localised
   "tool limit" error.

Every tool call also becomes an `EXECUTE_TOOL` step in the task log with the path as detail.
If the model returns no prose after finishing the file work, the chat shows the list of
paths it touched instead of an empty bubble.

### Safety rules

- Paths are normalised (`WorkspacePaths`): backslashes → `/`, `.` segments dropped, and
  `..`, control characters, names > 128 chars and paths > 512 chars are rejected **before**
  any content-resolver call. The guard is applied in the executor *and* the repository.
- File names must be a single safe segment; MIME types are inferred from the extension.
- The model only sees the workspace when a folder is connected and the switch is on;
  otherwise `tools` is an empty list and the system prompt is not injected.
- The agent cannot read anything outside the granted tree — there is no shell, no intent
  launching, no network tool.

## Screens added or changed

| Screen | Change |
|---|---|
| `ProviderEditScreen` | fetch-models banner, button, result summary and selection dialog |
| `WorkspaceScreen` (new) | connect/change/disconnect folder, breadcrumb + up navigation, file list, create file/folder, text editor dialog, delete confirmation, tools switch |
| `ChatScreen` | "give the agent a folder" prompt when none is connected, live tool-activity strip, workspace shortcut in the overflow menu |
| `SettingsScreen` | Workspace row (opens the screen, tools switch shows the connected folder) |
| `PermissionsScreen` | storage row (Android ≤ 12) + workspace summary line |

## Known limitations

- Text files only (no binary documents, images or archives).
- The editor is a plain text field capped at `MAX_EDITOR_BYTES` (400 KB).
- Choosing a folder inside Google Drive or another cloud provider that does not expose
  `COLUMN_SIZE` will show `0 B` in the list; content reading still works.
- No instrumented tests for SAF yet — see `docs/VERIFICATION.md`.
