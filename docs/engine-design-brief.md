# Vulkan-First 3D Engine for Kotlin Multiplatform
## Architectural Design Brief

---

## Mandate

You are the architect for a new 3D rendering engine written in Kotlin, targeting Vulkan as the primary and only first-class rendering backend. Metal support is an explicit stretch goal, deferred from v1. This document defines what we are building, why we are building it, and the philosophical commitments that must guide every structural decision you make.

Read this brief completely before proposing architecture. The philosophy sections are not preamble — they are constraints. When a decision is ambiguous, resolve it by returning to the principles stated here. If a principle appears to conflict with a pragmatic shortcut, the principle wins unless you can make a reasoned case otherwise and surface it explicitly.

---

## Vision

We are building a rendering engine that treats Vulkan's explicitness as a feature, Kotlin's expressiveness as a force multiplier, and AI-agent-assisted development as the primary authoring model.

The thesis is this: a domain as structured as GPU rendering — a deterministic state machine, explicit resource lifetimes, a machine-readable specification, and a robust validation layer — is precisely where a shift-left, deeply-tested, type-driven toolkit produces rock-solid code *faster* than conventional engine development produces fragile code. Most of game and engine work in practice is bug-fixing and refactoring. We intend to close that loop at design time, not discover it in production.

Every line of explicit Vulkan code is not a burden; it is a named surface on which we can attach a type, a test, or an invariant. We are not hiding Vulkan. We are armoring it.

---

## Philosophy

### Verbosity as invariant surface

Where a C++ engine treats Vulkan's verbosity as friction to be abstracted away, we treat it as a sequence of places to hang proof. A `VkImageMemoryBarrier2` is not twelve fields of boilerplate; it is twelve fields we can type-check, test, and assert against. The fields are the specification's handles; we do not throw them away, we grip them.

### Functional core, imperative shell

Command recording is a pure function from scene state to command stream. Resource creation, queue submission, and presentation are the only truly imperative boundaries. The *core* of the renderer — the logic that decides what to draw and how — must be testable without a GPU, without a window, without a driver. Drivers are for integration tests.

### Types over comments, tests over types

If a constraint can be expressed in the type system, it must be. If it cannot be expressed in the type system, it must have a test. Documentation is the fallback when neither is possible, not the primary mechanism. The goal is an architecture where invalid states are unrepresentable; where that fails, invalid states must be unreachable at runtime via asserted invariants with tests behind them.

### Agentic authorship is a first-class design constraint

The primary developer experience is a human directing AI agents against this codebase. The codebase must therefore be:

- **Legible without full context.** Small, composable, locally-named surfaces. An agent reading one file should not need to hold the whole engine in its head to contribute correctly.
- **Safe to refactor.** Sweeping changes must be caught by the compiler or by tests, not discovered in production. If a rename or signature change can silently break a caller, the architecture has failed.
- **Fast to iterate.** Headless tests, pure functions, deterministic outputs. A change to the render graph compiler must be verifiable in seconds, not by running a demo.
- **Structurally inspectable.** The render graph, pipeline state, and resource lifetimes must be *data* that tools (including agents) can reason about, not imperative sequences buried in methods.

Agents are weak at tracking global mutable state and strong at pattern-matching on strongly-typed, locally-explicit code. Design for that asymmetry.

### Close the spec loop

Vulkan has a machine-readable specification and a validation layer enforcing it. Treat Khronos's Valid Usage IDs (VUIDs) as first-class test oracles. A VUID firing in CI is a build failure, not a warning. The synchronization validation layer is our race detector. We run both against a CPU Vulkan implementation in CI so every commit is validated by a real Vulkan driver.

### The kernel is the product until it isn't

Before anything resembling a scene graph, material system, asset pipeline, or feature surface exists, a small kernel must demonstrate every philosophical commitment above. We do not build outward from the kernel until the kernel feels rock-solid. We would rather ship a perfect triangle than a buggy forest.

---

## Goals (v1)

- A Vulkan 1.3+ renderer with a small, composable core capable of drawing physically-based scenes with shadows and post-processing.
- Validation layer and synchronization validation clean in CI, on every commit, without a GPU.
- A typed Kotlin API where every Vulkan handle is a distinct type and every piece of pipeline state is exhaustively modeled as a sealed hierarchy.
- A render graph expressed as pure data, with compilation to barriers, layout transitions, and submission order implemented as a pure function with exhaustive unit tests.
- A shader pipeline that takes GLSL (and optionally HLSL) through SPIR-V to *Kotlin-typed* bindings derived by reflection. Descriptor sets, push constants, and vertex inputs become Kotlin types, not strings.
- Fast iteration loops suitable for human-plus-agent authorship.
- JVM-first, LWJGL-based, with an architecture that does not preclude Kotlin/Native targets later but does not pay their cost now.

## Non-Goals (v1)

- Editor or tooling UI — the engine is a library. An editor is a later, separate product.
- Physics, audio, animation, gameplay scripting — these are consumers of the engine, not part of it.
- AAA feature parity. Bindless, mesh shaders, hardware ray tracing, clustered lighting, virtual shadow maps are deferred until the kernel is proven.
- Non-Vulkan backends (D3D12, WebGPU). These are explicitly excluded.
- Mobile optimization. We will not pay a structural cost in v1 to support phones.
- OpenGL mental models. We are not shipping a VAO/VBO-shaped API under a modern skin.

## Stretch Goal: Metal

Metal support is planned but deferred. The architecture should make it *possible* to introduce a second backend later without re-plumbing the engine, but it should not be designed around that possibility. The most likely path is MoltenVK first, with native Metal evaluated only if specific needs (App Store constraints, Metal-only features) justify the additional backend. The architect should note, for each major abstraction, whether that abstraction would survive a second backend or would need to be generalized. That is a note, not a blocker.

---

## Target Platforms

**Primary (v1):** Linux (Vulkan), Windows (Vulkan), macOS (Vulkan via MoltenVK)
**Deferred:** iOS (Metal or MoltenVK), Android (Vulkan)
**Excluded from roadmap:** Web/WebGPU, consoles, VR/XR

The runtime is the JVM. LWJGL 3 provides the Vulkan, GLFW, shaderc, and VMA bindings. Kotlin 2.2+ is required (for context parameters).

---

## Architectural Principles

### 1. Every handle is a distinct type

Every Vulkan handle is an inline value class wrapping the underlying `Long`. Image handles, buffer handles, device memory handles, command buffer handles, descriptor set handles, pipeline handles — each is a distinct compile-time type with zero runtime cost. It must be impossible to pass a buffer handle where an image is expected. Raw longs are prohibited in public APIs.

### 2. Sealed hierarchies for state

Pipeline state, render pass configuration, barrier kinds, shader stages, blend modes, descriptor types, image layouts, access masks — anything Vulkan expresses as an enum, flag bitmask, or struct-of-options — is modeled as a sealed Kotlin hierarchy. Exhaustive `when` is the enforcement mechanism for "did you handle the new variant?" When a sealed hierarchy grows, the compiler identifies every site that must adapt.

### 3. Explicit scope, no ambient state

No singletons. No thread-locals. No implicit global device. Every API that requires a device, a frame, an allocator, or a command scope receives it via Kotlin context parameters. Recording commands outside a frame is a compile error, not a runtime error. The code reads clean because the scopes are declared once at the top of a function, not threaded as arguments everywhere.

### 4. Lifetimes are asserted, not hoped

Every resource has a declared scope — frame, pass, device, explicit. A scope abstraction tracks create/destroy pairs. Leaks are test failures. Double-frees are compile errors where possible and test failures elsewhere. Kotlin's `AutoCloseable` and structured concurrency are the substrate; we build scope types that make the intended lifetime explicit in the API surface.

### 5. Results, not exceptions, for expected failure

`VkResult` is wrapped in a typed sealed `VulkanOutcome` hierarchy. Out-of-memory, device-lost, swapchain-out-of-date, surface-lost — each is a named case callers must handle. Exceptions are reserved for programmer errors (null handles where non-null was promised, etc.), which should be rare because types catch most of them. `when` over outcomes is how errors propagate.

### 6. The render graph is data

A frame is described declaratively: passes, their reads, their writes, their dependencies, their attachments. The graph compiler is a pure function that produces a command stream — barriers, layout transitions, submission order. Tests assert graph properties: no read-after-write without a barrier, every resource has a producer, no cycles, every transition is valid for the image's declared usage. The graph is inspectable data both at runtime (for debugging) and at test time (for assertion).

### 7. Shader types are generated, not strung

SPIR-V reflection is part of the build. For each shader, Kotlin types are generated for its push constants, its uniform buffer layouts, its vertex inputs, and its descriptor bindings. A uniform buffer is a Kotlin data class. Binding a descriptor set is a type-checked operation. String-based binding lookups are prohibited in engine code and in consumer code.

---

## The Kernel

Before anything resembling a scene graph, material system, or asset pipeline exists, the architect must design and the team must build a *kernel* that demonstrates every philosophical commitment above in miniature. The kernel is the proving ground.

**Kernel components:**

1. **Typed Vulkan wrapper layer.** All handles as distinct types. All enums as sealed hierarchies. All results as a `VulkanOutcome`. All lifetime-critical objects bound to scope types. LWJGL is the substrate; this is the idiomatic Kotlin surface over it.
2. **Memory allocator.** VMA integration via LWJGL, wrapped in a Kotlin allocator abstraction with typed allocations, usage modes modeled as sealed types, and scope-tracked lifetimes.
3. **Shader pipeline.** GLSL to SPIR-V via shaderc. A Gradle task performs reflection on compiled SPIR-V and emits Kotlin source for descriptor bindings, push constants, and vertex inputs. The output is checked in or cached deterministically.
4. **Minimal render graph.** Clear, draw one triangle, present. The graph is data. The compiler is pure. The command stream is deterministic and testable.
5. **CI harness.** GitHub Actions (or equivalent) running Lavapipe (Mesa's CPU Vulkan implementation) with validation layer and synchronization validation enabled. A golden-image test for the triangle. A record-phase unit test for command generation. A property test for the math primitives in use. No VUIDs fire. All tests green.

**The test:** if the kernel feels rock-solid — agents productive in it, refactors safe, tests fast, validation clean — the rest of the engine is grinding through primitives that structurally resemble primitives already proven. If it does not feel that way, stop expanding scope and fix the kernel.

The kernel is not a throwaway prototype. It is v0 of the engine and its bones persist.

---

## Testing Strategy

The testing pyramid is the engine's load-bearing structure. It is not a nice-to-have. It is the mechanism by which the shift-left thesis succeeds or fails.

- **Property tests** for math primitives — vectors, matrices, quaternions, transforms, packing/unpacking routines.
- **Record-phase unit tests** for command generation. Record to a mock or captured command buffer; assert the emitted command stream as data. No GPU required. Fast. Deterministic. This is where most renderer logic is verified.
- **Render graph compilation tests.** Input graph as data, asserted output as data — barriers, transitions, submission order. Pure function tests.
- **Validation integration tests.** Headless Vulkan under Lavapipe in CI, with validation and synchronization validation enabled. Any VUID fires the build. This is the hidden superpower of a Vulkan engine designed this way: we get a real driver in CI, for free, reproducibly, without hardware.
- **Golden image tests.** Curated, small, perceptual-diff-based (SSIM or similar). Used sparingly — they are expensive to maintain — but essential for catching visual regressions at the presentation layer.
- **Resource lifetime tests.** Scope-based assertions that every create has a destroy, no leaks across frames, no use-after-free.

**What the pyramid cannot catch:** driver-specific behavior (different GPU vendors diverge on underspecified corners) and performance regressions. Plan for a hardware matrix in CI later and frame-time assertions as first-class tests as the engine matures. Surface these as known gaps; do not pretend they are covered.

---

## Key Libraries and Dependencies

- **LWJGL 3** — Vulkan, GLFW, shaderc, VMA bindings
- **kotest** (or equivalent) — property and unit testing
- **Lavapipe (Mesa)** — CPU Vulkan implementation for CI; no GPU dependency
- **Khronos validation layers** — including synchronization2 and sync validation
- **Kotlin 2.2+** — required for context parameters
- **Gradle with Kotlin DSL** — build system; shader reflection runs as a Gradle task

No dependency on existing Kotlin engines (kool, KorGE, Materia, thelema). This is a greenfield build. No dependency on any game-engine-shaped library. The engine is not a fork.

---

## Open Questions for the Architect

These are unresolved. Propose answers as part of the architectural design. Each answer should be reasoned from the philosophy, not chosen by taste.

1. **Module structure.** How do we split the kernel into modules? A starting proposal: `core` (handles, results, scopes), `memory` (VMA wrapper), `shader` (SPIR-V pipeline and reflection), `graph` (render graph and compiler), `cmd` (command recording), `test-harness` (mocks, golden-image harness, Lavapipe runners). Refine, justify, or replace.

2. **Context parameter shape.** What are the canonical context receivers and at what granularity? `Device`, `FrameContext`, `Allocator` is a starting proposal. Are there finer scopes worth reifying (e.g., `RenderPassScope`, `ComputeScope`)? Where do they compose?

3. **Render graph API shape.** DSL-style authoring (`graph { pass("gbuffer") { reads(...); writes(...) } }`) or data-class-driven builder, or both? The DSL is agent-friendlier and reads well. The data-class form is more inspectable and more directly testable. Propose a reconciliation — likely a DSL that produces data — and show what the boundary looks like.

4. **Shader reflection code generation.** At build time (Gradle task producing Kotlin sources, checked in or cached) or at runtime (reflection into generic typed wrappers)? Build-time is safer, faster at runtime, and more agent-legible; runtime is simpler to set up. The recommendation is build-time, but make the tradeoff explicit.

5. **Frame abstraction.** Double buffering, triple buffering, frames-in-flight — what is the canonical `FrameContext`, how are per-frame resources scoped, and how does the scope model prevent a resource from one frame being used in another?

6. **Error recovery policy.** What does the engine do on `ERROR_DEVICE_LOST`? On swapchain out-of-date? On allocation failure? Propose a recovery policy that fits the `VulkanOutcome` model and makes failure modes loud, not silent.

7. **Threading model.** Single-threaded command recording for v1 with a path to parallel, or parallel recording from the kernel onward? Be explicit about what the kernel commits to and how the scope model forbids illegal cross-thread usage at compile time.

8. **What the Metal stretch goal costs us today.** For each major abstraction, note whether it would survive a second backend unchanged, need to be generalized, or need to be replaced. This is a scouting report, not a design task. We want to know the shape of the debt we are taking on by being Vulkan-first.

---

## Deliverables Expected from the Architect

1. A module map with responsibilities, public API surfaces, and dependency direction.
2. A proposed set of canonical types: handles, results, scopes, and the top-level sealed hierarchies for pipeline state.
3. A render graph API proposal with a concrete example of defining and compiling a two-pass frame (shadow map + forward).
4. A sketch of the command recording model, including how recording is tested without a GPU.
5. A shader reflection pipeline proposal: Gradle task, generated code shape, example consumer code.
6. Answers — or explicit, reasoned deferrals — for every open question above.
7. A staging plan: v0 (compiles, opens a window, clears to a color), v1 (triangle end-to-end with reflected shader), v2 (textured mesh with PBR shader and a real render graph with depth pre-pass), and beyond.

---

## Final Note

The bar is higher than "it works." The bar is:

- The code is structurally clean enough that when an agent reads one file cold, it knows what to do next.
- When a refactor is needed, the compiler and tests catch every site that must change.
- When a bug appears, the test that would have caught it is obvious in retrospect — and then written.
- When a new feature is proposed, the existing philosophy gives a clear answer about where it belongs and how it is tested.

If an architectural decision makes the code shorter but makes that bar harder to meet, make the code longer. Verbosity is not a cost here. It is the material we build with.
