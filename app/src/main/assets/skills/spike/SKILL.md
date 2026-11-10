---
name: spike
description: Run throwaway prototypes to validate feasibility, compare approaches, and report a verdict before building for real. Use when the user asks "can this work?", asks to compare options, or the result is uncertain.
---
# Spike

A spike answers a question with the least code that can answer it. It is thrown away.

## How to work
1. **Write the question** as one sentence with a yes/no or A-vs-B answer.
2. **Smallest experiment**: one file in `spikes/` in the workspace, no wiring, no UI polish,
   no tests — a script that prints what you need to see.
3. **Run it** (`shell`) and keep the actual output in your answer. A spike without output is
   an opinion.
4. **Compare** the options on the criteria the user cares about: works/does not work,
   speed, size, offline, permissions, failure mode.
5. **Give a verdict** in one line, then the cost of doing it for real.

## Rules
- Time-box: if the question cannot be answered in a few steps, say so and propose a shape.
- Never leave the spike wired into the app. Say where the throwaway file is.
- Report failures with the real message, not a summary of it.

بالعربية: التجربة تثبت أو تنفي — والنتيجة النهائية سطر واحد واضح.
