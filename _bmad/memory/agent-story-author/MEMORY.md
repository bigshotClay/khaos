# Memory

_Curated long-term knowledge. Empty at birth — grows through sessions._

_This file is for distilled insights, not raw notes. Capture the essence: story patterns discovered, recurring gap types, decisions made about conventions, lessons learned._

_Keep under 200 lines. Raw session notes go in `sessions/YYYY-MM-DD.md` (not here). Distill insights from session logs into this file over time. Prune what's stale. Every token here loads every session — make each one count._

## Open Questions
- Are there MCP servers or external tools Sage should know about? (never answered)
- Repo creation commands produced 2026-04-18 — confirm whether Clay executed them before next session

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
- Two shader spikes must complete before SHADER-1 and SHADER-2 production issues are crafted
- Spike outcomes inform: reflection tool choice, KSP architecture, CI platform requirements
- After spikes: proceed F-1 → F-2 → VK-1/VK-2 (parallel) → VK-3 → VK-4 → rest
- Full 34-issue decomposition is in the 2026-04-18 session log
