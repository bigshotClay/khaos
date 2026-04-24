# Thagomizer

This project uses [thagomizer](https://github.com/claybamer/thagomizer) for AI-native story-driven development.

## Workspace

All framework artifacts live in `.thagomizer/`:
- `stories/active/` — active story lock state
- `stories/<slug>/` — versioned story drafts
- `runs/` — execution run state and artifacts
- `case-law/` — verdict records and case index
- `personas/` — agent persona definitions

## CLI

```
npx thagomizer run      # start a new run
npx thagomizer resume   # resume an active run
npx thagomizer status   # show current run status
npx thagomizer history  # show run history
```

## Thagomizer Session Guard

At the start of any thagomizer skill, check for `session-token.yaml` in the active run directory. If it exists and you have no memory of writing it in this session, write `FAILURE.md` and stop. `/thagomizer-resume` is exempt from this check.
