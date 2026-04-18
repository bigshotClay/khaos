# Memory

_Curated long-term knowledge. Empty at birth — grows through sessions._

_This file is for distilled insights, not raw notes. Capture the essence: story patterns discovered, recurring gap types, decisions made about conventions, lessons learned._

_Keep under 200 lines. Raw session notes go in `sessions/YYYY-MM-DD.md` (not here). Distill insights from session logs into this file over time. Prune what's stale. Every token here loads every session — make each one count._

## Open Questions
- Are there MCP servers or external tools Sage should know about? (never answered)

## Project Context — khaos
- **PRD complete as of 2026-04-18** — 60 FRs, full NFR set, kernel gate conditions defined
- **FR numbering:** FR1–FR60, categorized into 8 groups
- **Development boundary 1 (Kernel/v0):** 6 gate conditions all must be green before v1
- **Development boundary 2 (v1/Growth):** PBR, full render graph, production resource lifetimes
- **Development boundary 3 (Vision):** Metal, Kotlin/Native, editor (separate product)
- **Solo developer, indefinite timeline** — no deadline pressure; ships when right
- **GitHub:** bigshotClay/khaos (repo may not exist yet as of 2026-04-18)

## Clay's Working Style
- Approves with "looks good" or "send it" — move immediately, don't re-confirm
- Doesn't tolerate unnecessary back-and-forth; ask one question at a time
- When he doesn't understand a term, he says so directly — explain with examples, not definitions

## Issue Sequencing (v0-kernel)
- All 34 issues created in `bigshotClay/khaos` as of 2026-04-18
- SHADER-1 (#16) and SHADER-2 (#17) are `blocked` + `needs-decision` until spike outcomes resolve
- Active path: SPIKE-SHADER-1 (#1) and SPIKE-SHADER-2 (#2) → then F-1 (#3) → F-2 (#4) → VK-1/VK-2 (#5/#6, parallel) → VK-3 (#7) → VK-4 (#8) → rest
- Kernel gate conditions tied to: GRAPH-5, TEST-3, TEST-4, DOCS-2, DIST-3, and the zero-VUIDs CI requirement
- v1-growth issues not yet crafted — deferred until kernel gate green
