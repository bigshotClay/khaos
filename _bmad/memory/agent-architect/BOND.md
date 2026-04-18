# Bond — Clay and Atlas

## Who Clay Is

- Kotlin developer building a Vulkan-first 3D rendering engine (Khaos) as a personal project
- Experienced software engineer who brings business-software discipline (type safety, testability, shift-left) to a domain that has historically lacked it
- Treating AI agents as the primary authoring model — agentic development is a first-class constraint, not an afterthought
- BMad workflow user: Story Author → Atlas → Shift-Left Reviewer pipeline

## Settled Patterns (Do Not Re-Argue)

*Established from PRD and design brief — validate with Clay if any seem wrong.*

- **Language:** Kotlin Multiplatform (JVM target first, K/N later)
- **Graphics API:** Vulkan first-class; Metal deferred to Vision tier
- **Handles:** All Vulkan handles as `@JvmInline value class` — no raw pointer leakage
- **Error types:** All results as `VulkanOutcome` sealed hierarchy — no exceptions for Vulkan errors
- **Enums:** All Vulkan enums as sealed class hierarchies — not int flags
- **Architecture:** Functional core / imperative shell — command recording is a pure function
- **Render graph:** Graph as data structure, compiler as pure function, output deterministic
- **CI:** GitHub Actions + Lavapipe (CPU Vulkan), validation layer + sync validation — zero VUIDs = build failure
- **Shader reflection:** Gradle task emitting Kotlin types for descriptor bindings, push constants, vertex inputs
- **Memory:** VMA integration with typed allocations and scope-tracked lifetimes
- **Scope encoding:** `FrameIndex` type, deferred deletion queue, resource lifetime as type

## Design Philosophy

- **Verbosity as invariant surface** — Vulkan's explicitness is where you attach types, tests, and assertions
- **Types over comments, tests over types** — invalid states unrepresentable > unreachable > documented
- **Kernel first** — prove every commitment in the kernel before expanding outward; perfect triangle > buggy forest
- **Agentic legibility** — one-file comprehension, compiler-caught renames, headless test speed
- **VUID as oracle** — Khronos VUIDs are first-class test oracles, not warnings

## Active Projects

- **Khaos** — Vulkan-first 3D rendering engine, Kotlin Multiplatform. Status: PRD complete, greenfield, no code yet. Scope: Kernel (v0) → v1 → Vision. (2026-04-18)

## Patterns to Avoid

*None established yet — build this from Clay's feedback over time.*

## Complexity Tolerance

*Not yet explicitly stated — infer: thorough-upfront, given the kernel-gate discipline and shift-left commitment. Confirm with Clay.*
