# Bond — Clay and Sentinel

## Who Clay Is
- Kotlin developer building Khaos: a Vulkan-first 3D rendering engine (Kotlin Multiplatform)
- Experienced software engineer with business-software discipline — type safety, testability, shift-left are first-class values
- Solo developer, indefinite timeline — ships when right, no deadline pressure
- AI agents are the primary authoring model; agentic legibility is a hard constraint
- BMad workflow: Story Author → Architect → Sentinel → Dev → Gauntlet

## Testing Stack

### Kotlin / Khaos
- **Unit/property tests:** Kotest (kotest-runner-junit5, kotest-property)
- **Test target:** JVM first (Kotlin Multiplatform, JVM target primary)
- **CI GPU:** Lavapipe (CPU Vulkan) via `jakoch/install-vulkan-sdk-action`
- **Shader toolchain:** glslc + spirv-val + spirv-cross (all CLI subprocesses via Gradle tasks)
- **Build system:** Gradle + Kotlin DSL; custom `@CacheableTask` for shader pipeline

## Coverage Philosophy
- Shift-left and thorough: happy path + failure paths + VUID gate
- Kernel gate conditions are non-negotiable: zero VUIDs = build failure
- Testable without GPU preferred where possible (render graph as pure data, command recording as pure function)
- Golden image tests via Lavapipe when GPU behavior must be verified
- Property-based tests for math primitives and deterministic functions

## AC Format
- Checkboxes. Each item is a discrete, testable condition.
- No Given/When/Then. No "should" statements.

## Clay's Working Style
- Approves with "looks good" or "send it" — move immediately, no re-confirmation
- Doesn't tolerate unnecessary back-and-forth; one question at a time
- When he doesn't understand a term, he says so directly

## Settled Architecture (Do Not Re-Argue)
- Handles: `@JvmInline value class` — mismatched handles are compile errors
- Results: `VulkanOutcome` sealed hierarchy — no unchecked exceptions for Vulkan errors
- Enums: sealed class hierarchies, not int flags
- Render graph: pure data structure; compiler is a pure function → testable without GPU
- Command recording: `RecordingScope` context type — `draw()` outside scope is a compile error
- Shader pipeline: glslc → spirv-val → spirv-cross → `ShaderBindingGenTask` (Gradle, NOT KSP)
- Memory: VMA, typed allocations, scope-tracked lifetimes, `FrameIndex` type

## Active Project
- Khaos, Vulkan-first 3D rendering engine. GitHub: bigshotClay/khaos
- Status (2026-04-18): Spikes done (SPIKE-SHADER-1 + SPIKE-SHADER-2 design artifacts complete)
- Next in queue: F-1 (#3) → F-2 (#4) → VK-1/VK-2 (#5/#6, parallel) → VK-3 (#7) → ...
- SHADER-1 (#16) and SHADER-2 (#17): still labeled `blocked`/`needs-decision` — spike decisions exist but issues not updated yet
