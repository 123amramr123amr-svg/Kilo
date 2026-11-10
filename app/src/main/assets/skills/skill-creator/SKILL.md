---
name: skill-creator
description: Create, repair or review agent skills (SKILL.md files) for this project, with a good trigger description and lean instructions. Use when the user wants a new skill, or an existing skill improved.
---
# Skill creator

A skill is a short instruction file the agent loads only when it is relevant. It is not
documentation: it is a nudge that changes behaviour.

## Anatomy
```
---
name: short-kebab-case
description: One sentence — WHAT it does and WHEN to use it. The "when" is the trigger.
---
# Title
Goal in one line.
## How to work
1. …
## Rules
- …
```

## Rules for a good skill
- **Description is the trigger.** Write the situations in the user's words ("when the user
  asks to review, audit or double-check code"). Without a trigger the skill never loads.
- **Under 60 lines.** Every line must change an action. Delete anything that is merely true.
- **Concrete verbs.** "Use `grep` before `read`" beats "be thorough".
- **No duplication** of the system prompt, the tools list or another skill.
- **One language for the steps** (English here) plus a one-line native-language reminder.

## Workflow
1. Ask what the skill should make the agent do differently (or infer it from the request).
2. Draft the front matter first, read it back as a trigger sentence.
3. Write the body as numbered steps and a short rules list.
4. Save with `write_file` at `skills/<name>/SKILL.md`, then `read` it back.
5. Test the trigger: read only the description — would the agent load it at the right moment?

بالعربية: الوصف هو الزناد؛ من غيره المهارة مش هتتحمّل أصلاً.
