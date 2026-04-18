# Memory — Recurring Patterns and Decisions

_Distilled insights. Grows over time. Keep under 200 lines._

## Khaos Project — Key Test Oracles

- **VUID gate is non-negotiable:** Zero Vulkan VUIDs = build passes. Any VUID = build failure. The validation layer + sync validation are always on in CI.
- **GPU-free testing preferred:** Render graph and command recording are designed as pure functions — test without GPU wherever possible. Lavapipe is the fallback, not the first resort.
- **Shader pipeline determinism:** Same reflection JSON → same Kotlin source → same binary. This is a testable invariant for the binding generator.
- **`@JvmInline value class` handle safety:** Handle type mismatch is a compile error, not a runtime check. Test plans should verify this at the type system level (wrong handle type = doesn't compile).
- **`VulkanOutcome` exhaustiveness:** `when` on VulkanOutcome must be exhaustive — the sealed hierarchy enforces this at compile time.

## Test Plan Structure Convention

- Top-level section must be `## Test Cases` (required by shift-left-dev prerequisites check)
- Organize within it using `###` sub-sections: `### Acceptance`, `### Design Contract`, `### Failure Paths`
- Do NOT use `##` layer-named sections — these break the content check

## Issue Sequencing (v0-kernel, as of 2026-04-18)
- Spikes #1 and #2: design decisions written; issues still OPEN (not closed yet)
- Active path: F-1 (#3) → F-2 (#4) → VK-1/VK-2 (#5/#6) → VK-3 (#7) → VK-4 (#8) → VK-5 (#9) → ...
- SHADER-1 (#16) + SHADER-2 (#17): blocked labels still on; spike decisions exist and unblock them
