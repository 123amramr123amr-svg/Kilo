---
name: systematic-debugging
description: Find the root cause of a bug, crash, wrong output or failing behaviour before changing code. Use when something is broken and the cause is not obvious.
---
# Systematic debugging

A fix without a cause is a coin flip. Never change code before you can say *why* it failed.

## How to work
1. **Reproduce the exact symptom.** Copy the real message verbatim. If it is not reproducible,
   say that instead of guessing.
2. **Narrow the search** with evidence, not intuition:
   - `grep` the message text and the identifier through the workspace.
   - `read` the code around each match with 20 lines of context.
   - `diagnostics` on the file to rule out a syntax/JSON problem.
3. **List hypotheses** (usually 2–4) and for each write the observation that would *disprove* it.
   Cheapest and most likely first.
4. **Test one hypothesis at a time.** One change, one observation, recorded.
5. **Fix the cause, not the symptom.** Then say what would have caught it earlier (a test,
   a guard, a message that names the real cause).

## Rules
- Never say "fixed" if you could not observe the new behaviour.
- Prefer a change that makes the failure *loud* over one that hides it.
- If the bug is in text the user reads, check the string resources in both languages.

بالعربية: السبب الأول ثم الإصلاح — مفيش تخمين، ومفيش «جرّب وشوف».
