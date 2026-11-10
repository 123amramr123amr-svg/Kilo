---
name: port-from-project
description: Copy features, UI screens or functions from another project folder into the current project, following the current project's structure. Use when the user wants to copy, port, migrate or reuse something from project A in project B.
---
# Port from another project

Goal: bring the *idea* over, not the file.

## How to work
1. **Map both sides first.**
   - `glob` in the source folder to find the feature (screen, use case, model).
   - `glob` in the target folder for the equivalent layer (same package/role).
   - Read the target's conventions: naming, dependency injection, error type, test style.
2. **Decide the smallest seam.** A port is usually: one model, one interface method, one
   implementation, one screen, one string block. Anything more is a rewrite and needs a plan.
3. **Translate, do not paste.** Replace the source's framework with the target's:
   - other state holder → the target's ViewModel/StateFlow
   - other storage → the target's repository interface
   - other strings → both language files of the target
4. **Port the tests too**, in the target's test style. A feature without its test is a guess.
5. **Write only through the file tools**, then `read` back what you wrote to confirm.

## Report
- What came from where: `source/path → target/path`.
- What was intentionally left behind (native APIs, platform-only code).
- Anything that must be wired by hand (navigation entry, DI binding, manifest).

## Rules
- Never copy a secret, a token, an endpoint or a licence header you did not check.
- Keep the target project's language: do not introduce a second HTTP client, a second DI
  framework or a second design system to host one feature.

بالعربية: انقل الفكرة مش الملف، وحافظ على أسلوب المشروع الحالي واختباراته.
