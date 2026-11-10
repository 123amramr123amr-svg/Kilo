---
name: code-review
description: Review code changes or a file for bugs, regressions, security and maintainability. Use when the user asks to review, audit or double-check code or a diff.
---
# Code review

Goal: find what will actually break, in the order a user would feel it.

## How to work
1. **Read before judging.** Use `glob` to find the file, `read` to see all of it (page with
   `offset` when it is long). Never review a file you have only guessed at.
2. **Look for the four killers first**, in this order:
   - *Crash*: unchecked null, index out of bounds, parse of untrusted input, empty list `first()`.
   - *Data loss*: write without reading, overwrite of a user file, delete without a guard.
   - *Wrong behaviour*: off-by-one, inverted condition, missing `else`, forgotten `await`.
   - *Security*: secrets in code, path traversal (`..`), `http://` keys, logging of tokens.
3. **Then** style and naming — but never pad the review with them.

## What to report
For each finding: `path:line`, what happens, why it happens, and the smallest fix.
Order findings by severity. Say explicitly when something is fine.

## Rules
- Quote the line you are talking about so the user can find it.
- Do not invent problems to look thorough. "No blocking issues" is a valid review.
- Never rewrite the whole file for a one-line fix; use `edit`.
- If the code is in Arabic UI text, check RTL and plural forms too.

بالعربية: راجع من الأعلى خطورة للأقل، واذكر رقم السطر والسبب والإصلاح الأصغر.
