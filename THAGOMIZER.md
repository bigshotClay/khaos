# Thagomizer

This project uses [thagomizer](https://github.com/bigshotClay/thagomizer) **v2.0.0** for the
plan → develop → review loop.

v2 is a Claude Code plugin suite and a thin orchestration layer over
[superpowers](https://github.com/obra/superpowers). One rule sits above the rest: **planning is
the only place decisions are made.** Review routes design gaps back to planning, never to dev.

## Plugins

| Plugin | Role |
|---|---|
| `thag-planning` | Wraps superpowers brainstorming + writing-plans. Turns a GitHub issue into a structured plan. Sole decision authority. |
| `thag-tdd` | Wraps superpowers TDD with a mechanical red-green evidence gate — outcome proven from test counts, not narrated. Refuses to start without a valid plan. |
| `thag-github` | Hosts the `thag` CLI, the suite's single deterministic core: GitHub I/O plus all bookkeeping. |
| `thag-review` | One review pass, writes a findings file, routes design gaps back to planning. |

## The `thag` CLI

One Python CLI hosted in `thag-github`, invoked once per stage with a JSON payload:

```
echo '{...}' | python3 -m thag <subcommand>
```

Subcommands: `resolve`, `gh-fetch`, `gh-publish`, `ste-check`, `plan-validate`, `runlog`
(the red-green gate), `route` (the route-back ledger), `report`, `orchestrate`.

`thag orchestrate` runs **one stage per invocation**. An autonomy tier — scored from planning's
complexity/risk, human-overridable — sets the pace: `auto` runs unattended, `checkpoint` pauses
for plan approval, `manual` approves every stage boundary.

## Dependencies

- **superpowers** — hard dependency, no fallback
- **`gh` CLI**, authenticated
- **Python 3 with PyYAML** — every `thag` subcommand fails at import without it
- **writing-ste** — soft dependency; gates plans in ASD-STE100 when present

## Migration note — v1 removed 2026-07-28

v1 was a different system: a persona pool, a 69-method elicitation library, an adversarial
court, and slot-based model routing. All of it is gone. The `/thagomizer-plan`, `-story`,
`-run`, `-resume`, `-elicit` and `-party` skills, the `.thagomizer/` workspace (`case-law/`,
`personas/`, `corpus/`, `runs/`, `stories/`), and the `npx thagomizer` CLI no longer apply.

v1 was never committed to the thagomizer repository — its only copy lived in this project's
gitignored `.claude/skills/`. It is archived at `~/thagomizer-v1-archive-20260728.tar.gz`
(296 files) should any of it need to be read again.

The VK-1 and VK-2 stories were authored and executed under v1; their run artifacts are in that
archive, not in this tree.
