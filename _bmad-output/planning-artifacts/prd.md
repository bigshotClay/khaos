---
stepsCompleted: [step-01-init, step-02-discovery, step-02b-vision, step-02c-executive-summary, step-03-success, step-04-journeys, step-05-domain, step-06-innovation, step-07-project-type, step-08-scoping, step-09-functional, step-10-nonfunctional, step-11-polish]
inputDocuments:
  - docs/engine-design-brief.md
  - _bmad-output/planning-artifacts/research/technical-kotlin-vulkan-bindings-multiplatform-rendering-research-2026-04-18.md
workflowType: 'prd'
briefCount: 1
researchCount: 1
brainstormingCount: 0
projectDocsCount: 0
classification:
  projectType: Developer Tool — Runtime Library with compiled API contract
  domain: Real-Time Graphics Runtime (Systems-Level Graphics / Vulkan)
  complexity: high
  complexityDimensions:
    - Vulkan technical (synchronization, memory, explicit state machine)
    - JVM/Native boundary (command recording is native-side invariant; two ABIs in parallel)
    - Agentic/epistemological (structurally inspectable by AI is novel territory)
  projectContext: greenfield
  primaryUsers:
    - Kotlin developers learning Vulkan
    - General game developers wanting a Kotlin-native 3D rendering library
  secondaryStakeholders:
    - AI Development Agent (requires structurally predictable module boundaries, self-describing APIs, refactoring-safe abstraction layers)
---

# Product Requirements Document - khaos

**Author:** Clay
**Date:** 2026-04-18

## Executive Summary

Khaos is a Vulkan-first 3D rendering engine written in Kotlin Multiplatform — a runtime library that gives Kotlin developers a first-class path to building 3D games without abandoning the language, toolchain, or engineering discipline they've built their identity around. It targets two overlapping audiences: **Kotlin enthusiasts** who want to use the language they love for game development, and **FOTM-driven learners** who are drawn to modern, well-designed technology and will grow into the ecosystem as it matures.

The 3D game development ecosystem has four structural failures that Khaos directly addresses:

1. **Type Safety Gap** — Existing engines expose stringly-typed APIs and magic constants; errors surface at draw time, not compile time.
2. **Testability Gap** — No established pattern exists for unit-testing a render pipeline; the dominant practice is testing by running the game, making CI/CD and shift-left design effectively impossible.
3. **Ecosystem Fragmentation** — JVM developers must abandon Gradle, Kotlin, IntelliJ, and their existing library ecosystem entirely to enter 3D development.
4. **Agent-Hostile Architecture** — Current engine APIs are implicit, stateful, and undocumented in machine-readable ways; an AI agent cannot reason about a 200-flag OpenGL state machine.

The underlying user experience is an identity mismatch: a disciplined, type-safe, test-driven Kotlin engineer is asked to become a beginner again — not just technically, but philosophically — the moment they attempt 3D game work. Khaos eliminates that mismatch.

### What Makes This Special

The intersection of (Kotlin Multiplatform + Vulkan-first + full rendering framework) is genuinely unoccupied. kool Engine's temporary Vulkan suspension signals the difficulty of this space; that difficulty is the moat. Khaos's differentiation is not incremental — it is the only option for a Kotlin developer who wants Vulkan without C++.

The core architectural thesis — verbosity as invariant surface, functional core / imperative shell, types over comments / tests over types — brings business-software engineering standards to a domain that has never had them. The render graph is pure data, testable without a GPU. Shader bindings are generated Kotlin types, not strings. Invalid states are unrepresentable at compile time.

**Why now:** The convergence of capable AI agents, modern context management, and shift-left discipline makes this achievable at a scope and velocity that would have required a dedicated C++ engine team previously. Agentic authorship is a first-class design constraint: the codebase is structured to be legible without full context, safe to refactor by agents, and fast to iterate headlessly. Agents serve as the senior graphics programmer that Kotlin game developers have never had access to.

## Project Classification

- **Project Type:** Developer Tool — Runtime Library with compiled API contract
- **Domain:** Real-Time Graphics Runtime (Systems-Level Graphics / Vulkan)
- **Complexity:** High — three distinct dimensions: (1) Vulkan technical complexity (synchronization, memory, explicit state machine); (2) JVM/Native boundary (command recording is a native-side architectural invariant; two ABIs in parallel); (3) Agentic/epistemological (no established pattern for "structurally inspectable by AI" in a rendering engine)
- **Context:** Greenfield — constrained by Vulkan's external contract; design decisions are primarily mapping and ergonomics over an existing mental model, not blank-slate invention
- **Primary Human Users:** Kotlin enthusiasts; FOTM-driven developers learning new technology
- **Secondary Stakeholder:** AI Development Agent — requires structurally predictable module boundaries, self-describing APIs, and refactoring-safe abstraction layers (product requirement, not code quality preference)

## Success Criteria

### User Success

User success is documentation-driven. A graphics engine is only as good as developers can understand how to use it. The primary user success metric is the existence and quality of a GitHub Wiki guide that takes a developer from a completely blank Kotlin project to a presentation window displaying a spinning, bouncing 3D ball on a 3D plane — the modern Kotlin equivalent of the classic Commodore demo reel.

**Assumed baseline for the guide reader:** Comfortable with Kotlin, Gradle, and IntelliJ. No prior Vulkan or graphics programming experience required. The guide must work for the FOTM learner encountering 3D rendering for the first time, not just the developer who already knows what a render pass is.

This guide is the acceptance test for user success at each scope tier: if a developer matching this baseline can follow it without hitting an undocumented wall, the tier ships.

### Business Success

Personal utility is the measure: Clay reaches for Khaos by default when starting a new Kotlin graphics project. This is a behavior, not a feeling — it is falsifiable at project-start decision time.

### Technical Success

- Validation layer and synchronization validation run clean on every commit in CI, against Lavapipe (Mesa CPU Vulkan) — zero VUIDs fire, zero suppressed warnings
- SPIR-V validation (`spirv-val`) and shader reflection correctness checks run as a fast CI gate — layout compatibility assertions catch shader/pipeline mismatches before GPU dispatch
- All test tiers pass: property tests (math primitives), record-phase unit tests (command generation without GPU), render graph compilation tests (pure function with determinism assertion), resource lifetime tests (with explicit pass/fail semantics), golden image tests (SSIM with defined thresholds and governed update process)
- Render graph compilation is **deterministic**: identical graph spec produces identical barrier sequence, resource aliasing decisions, and pass ordering across N runs regardless of hash map iteration order or other non-determinism sources — tested explicitly
- Resource lifetime tests define explicit pass/fail semantics: destruction order assertions, handle validity checks (no use-after-free on typed handles), deferred deletion queue drains at correct frame boundaries
- Agents are productive in the kernel: an agent reading one file knows what to do next without holding the whole engine in context
- Refactors are caught by the compiler and tests — a rename or signature change produces a compile error or test failure, not a silent regression
- Headless iteration is fast: render graph changes are verifiable in seconds, not by running a demo
- **Golden image governance:** a golden update requires an explicit human decision with a diff artifact attached — uncontrolled re-runs that auto-update goldens are not permitted; the threshold value is a first-class artifact that requires review to change

### Measurable Outcomes

- **Kernel complete:** Triangle renders, golden image test passes, zero VUIDs, typed wrapper layer proven, SPIR-V validation gate green, render graph determinism confirmed, CI green without GPU hardware — plus all six kernel gate conditions met
- **v1 complete:** PBR scene with shadows and post-processing renders correctly; render graph compiles as a pure function with exhaustive tests; all shader bindings are generated Kotlin types; performance regression gate defined and green; "Commodore demo" documentation guide complete and walkable by the defined baseline user
- **Vision complete:** Metal backend functional; editor exists as a separate product; scene graph, material system, and asset pipeline available as consumers of the engine

## Product Scope

### MVP — Kernel (v0) — Development Boundary 1

The kernel is the proving ground for every architectural commitment. Nothing expands outward until all six kernel gate conditions are green.

- Typed Vulkan wrapper layer: all handles as `@JvmInline value class`, all enums as sealed hierarchies, all results as `VulkanOutcome`, all lifetimes bound to scope types
- VMA memory allocator integration with typed allocations and scope-tracked lifetimes
- **Minimal resource lifetime contracts (type-level, not full production):** `FrameIndex` type, deferred deletion queue interface, scope type encoding "this resource lives for N frames" — trivially-correct single-frame implementation; v1 replaces the implementation without changing the API
- Shader pipeline: GLSL → SPIR-V via shaderc, Gradle reflection task emitting Kotlin types for descriptor bindings, push constants, and vertex inputs; reflection schema designed for multi-pass extensibility
- Minimal render graph: clear, draw triangle, present — graph as data, compiler as pure function, output deterministic
- CI harness: GitHub Actions + Lavapipe, validation layer + synchronization validation enabled, SPIR-V validation gate, golden image test (triangle), record-phase unit test (command generation), render graph determinism test, property tests for math — zero VUIDs, all green

**Documentation milestone:** blank project → window opens, triangle on screen (baseline user: Kotlin developer, no prior graphics experience)

**Kernel gate — all six conditions must be green before v1 begins:**
1. CI green on Lavapipe with sync validation, zero VUIDs, zero suppressed warnings
2. Triangle renders correctly under golden image test, reproducible in CI and locally
3. Public API surface documented and frozen — KDoc on every public type, semver tag cut
4. "Blank project → triangle" doc milestone walkable by a developer who has never seen the codebase (not a contributor — an outsider matching the baseline)
5. No known correctness issues deferred; API held frozen for at least one full development cycle without pressure to change it
6. Render graph compile function callable without a Vulkan instance in scope — the pure-function claim must hold at the CI boundary

### Growth — v1 — Development Boundary 2

Expand outward from the proven kernel. No v1 feature begins until the kernel gate is declared green.

- Vulkan 1.3+ renderer: physically-based shading, shadow maps, post-processing
- Full render graph: shadow pass + forward pass, automatic barrier insertion, layout transitions, resource aliasing
- Complete shader reflection pipeline for multi-pass scenes (extends Kernel schema additively)
- Resource lifetime management — production implementation: deferred deletion queue with frame-in-flight tracking, sealed-class resource state machine, replacing the Kernel's trivially-correct single-frame implementation
- Frame abstraction: double/triple buffering, per-frame resource scoping
- **Performance regression gate:** frame time regression threshold defined and enforced in CI; method (headless GPU runner or synthetic proxy) determined at v1 kickoff

**Documentation milestone:** "Commodore demo" guide — blank project → spinning, bouncing 3D ball on a 3D plane (baseline user: Kotlin developer, no prior graphics experience)

### Vision — Future — Development Boundary 3

Evaluated only after v1 ships and feels correct.

- Metal backend: MoltenVK first; native Metal only if App Store constraints or specific features justify it
- Kotlin/Native target: port after JVM interfaces are stable
- Editor: separate product, separate repository
- Scene graph, material system, asset pipeline: consumers of the engine, not part of it
- Deferred features: bindless, mesh shaders, hardware ray tracing, clustered lighting, virtual shadow maps, Android/iOS

## User Journeys

### Journey 1: The Indie Dev — "Finally, a Kotlin Game"

**Persona:** Jordan, 28. Backend Kotlin developer at a fintech company. Has been designing an indie puzzle-platformer in notebooks for two years. Knows Kotlin well. Has never touched a GPU API. Has tried Unity twice, quit both times — the C# ecosystem felt foreign, the editor felt like a black box, and the testing story was nonexistent.

**Opening Scene:** Jordan finds Khaos via a Reddit thread. Reads the README. Finds this in the first screen:

```kotlin
// This is a compile error, not a runtime crash:
commandBuffer.draw(mesh)  // ERROR: draw() requires RecordingScope context

context(RecordingScope)
fun recordFrame(mesh: Mesh) {
    commandBuffer.draw(mesh)  // OK
}
```

Not a marketing claim. Code. Jordan decides to invest time.

**Rising Action:** Jordan opens the wiki. Step 0 is *Platform Setup* — one page, single `brew install` command for macOS, a Gradle property to set, a verification test to confirm MoltenVK is working before touching rendering code. The starter template is embarrassingly minimal: three Gradle modules, a `main.kt` under 50 lines, every non-obvious line commented with why it exists. The Gradle config is pinned and CI-tested across Linux, Windows, and macOS. The dependency is on Maven Central.

The first wiki section carries a time estimate: *"This section takes about 45 minutes."* Jordan checks the time — 9:15pm. Saves it for Saturday morning.

Every first use of a Vulkan term has a plain-English callout box before pointing at the spec. Jordan hits `VulkanOutcome.SwapchainOutOfDate` — the wiki sidebar explains it in one sentence. At each checkpoint — window open, triangle drawn — a resume landmark states: *"Stopped here? Your project should have these three files, and `./gradlew test` should show 2 tests passing."*

**Climax:** Six weeks in. Jordan has a 3D plane, a bouncing ball, a directional light. `./gradlew test` — every render graph assertion green, no GPU required. *This is just software engineering.*

**Resolution:** Jordan's puzzle-platformer prototype exists. Shadow pass is next. Jordan opens the render graph documentation, reads one module, and knows where to add it. The "next steps" section at the end of the triangle chapter already pointed here.

**Requirements revealed:** Embarrassingly minimal starter template, explicit platform setup guide (MoltenVK for macOS as Step 0), starter template pinned and CI-tested across Linux/Windows/macOS, Maven Central publishing, concrete type-safety code examples in README, typed loud errors on silent failure, Vulkan term definitions on first use, time estimates on wiki sections, resume landmarks at each checkpoint, "next steps" section at end of each wiki chapter.

---

### Journey 2: Jordan Hits a Wall — "Validation Layer Says What?"

**Opening Scene:** Two weeks into the guide. Jordan adds a second draw call and gets `VUID-vkCmdDrawIndexed-None-02686`. UUID and Vulkan spec language. No plain-English translation.

**Rising Action:** Jordan searches GitHub. Finds a closed issue with a maintainer link to a wiki page that doesn't exist yet. Opens the Vulkan spec — impenetrable without GPU programming background. What saves Jordan: the Khaos type is literally called `VertexInputBinding`, same as the spec term, and its KDoc says: *"Describes how vertex data is laid out in a buffer — stride between elements and whether data advances per-vertex or per-instance."* That sentence bridges the gap the spec left. Fix found in thirty minutes.

Jordan opens a GitHub issue. CONTRIBUTING.md: *"I will respond to issues and PRs within two weeks. If you haven't heard from me, ping the thread."* Three days later Jordan opens a PR. Wiki is in the same repo. No CLA. Merged within a week. Jordan marks the VUID coverage tracker — a pinned issue listing common validation errors with status ✅ documented / 🔄 in progress / ❌ missing — from ❌ to ✅.

**Resolution:** Jordan's fix ships. The next developer finds it in under five minutes. Jordan finds two more ❌ entries to close from experience already gained.

**Requirements revealed:** KDoc bridging spec language to developer understanding, VUID-to-documentation cross-reference, contributor-friendly wiki in same repo, no CLA, contribution response SLA in CONTRIBUTING.md, community VUID coverage tracker as a pinned issue.

---

### Journey 3: The Kotlin Enthusiast — "I Know What I Want"

**Persona:** Priya, 34. Senior engineer with prior OpenGL and Metal experience. Knows what a deferred rendering pass is. Doesn't want to relearn C++.

**Opening Scene:** Priya scans `RenderGraph.kt` in twenty minutes — vocabulary learned. Before writing code she reads the architecture doc's *Graph Lifecycle* section: *"Compiled graphs are immutable. Resources owned by a graph cannot be declared in another graph — register them as external resources before compilation. See: Cross-Graph Resource Sharing."* The invariant that would have cost her a week is documented before she needs it.

**Rising Action:** Priya adds `SsaoPassNode`. First attempt: validation layer fires — GBuffer depth attachment needs `VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL` but she declared a depth attachment read, not a sampled texture read. The error message names both layouts and links to the resource access types documentation. Priya fixes the declaration. Second attempt: correct. Unit test asserts the barrier transition without a GPU. Passes.

She tries a conditional reflection pass next. The advanced usage guide has a *Conditional Rendering Patterns* section. She implements it correctly on the first attempt.

**Climax:** SSAO and reflection passes render correctly. Priya publishes a gist. The answer to "how did you do it" is: read the graph lifecycle section, then the module. That's all.

**Requirements revealed:** Architecture doc covering graph lifecycle and cross-graph resource ownership, advanced usage guide with conditional rendering patterns, pass inputs/outputs statically typed, validation error messages naming layouts with doc links, architecture doc as a living document.

---

### Journey 4: The AI Development Agent — "One File Is Enough (Mostly)"

**Opening Scene:** Developer asks an agent to add secondary camera render target support. Agent reads `RenderGraph.kt` (200 lines, sealed hierarchies, self-describing names) and the *Barrier Cookbook* in the architecture doc — a table of correct `(srcStage, dstStage, layout, accessMask)` combinations for canonical access patterns.

**Rising Action:** Before writing, the agent reads the KDoc on `BarrierSpec`:

```kotlin
/**
 * WHY: Stage flags are not interchangeable across access patterns.
 * For correct combinations per use case, see the Barrier Cookbook.
 * Copying stage flags from an existing BarrierSpec without checking
 * the access pattern is the primary cause of vendor-specific rendering
 * bugs that pass Lavapipe CI but fail on real hardware.
 */
```

The secondary render target is sampled in the fragment stage — `srcStage = FRAGMENT_SHADER_BIT`, not `COLOR_ATTACHMENT_OUTPUT_BIT` as used by the primary target. The agent sets it correctly from the cookbook. Uses Kotlin named parameters throughout. Exhaustive `when` in `GraphCompiler` flags the new variant unhandled — agent adds the case. Unit test asserts barrier presence and stage flags specifically.

**Climax:** PR opens. Reviewer cites the KDoc. Merges same day. Three weeks later, user tests on AMD — no regression. The cookbook made the first implementation semantically correct, not just structurally correct.

**Requirements revealed:** Barrier cookbook in architecture doc, KDoc "why" warning on `BarrierSpec` pointing to cookbook, component-level unit tests with stage flag assertions, CI against real GPU for release candidates, Kotlin named parameters throughout public API, "why" KDoc on fields with non-obvious invariants, `FrameResourceModel` / lifetime contract documentation.

---

### Journey 5: The Consumer Developer — "Khaos as a Foundation"

**Persona:** Sam, building a game framework on top of Khaos — scene graph, ECS, asset loading. Making a deliberate bet on a v0 library.

**Opening Scene:** Before writing integration code, Sam runs one test: two `KhaosContext` instances in the same process. It works. The no-ambient-state promise is real. Sam proceeds.

**Rising Action:** Sam integrates the render graph. When Khaos can't expose something needed, `KhaosContext.vulkanDevice()` provides the raw `VkDevice` — documented as a last resort, not an anti-pattern. Sam uses it once, for a platform-specific surface extension.

Sam's integration tests catch structural contract breaks in seconds. Sam documents a known gap: *"These tests verify structural contracts. Behavioral contract changes — barrier timing, transition ordering — require manual verification."*

Sam subscribed to the Khaos pre-release channel. Khaos v0.3.0-alpha.1 landed two weeks before stable. Sam migrated then, at Sam's own pace. The old `RenderGraph.build` signature carried `@Deprecated("Use build(frameIndex, ...). Removal in 0.4.0.")` since v0.2.0. The v0.3.0 migration removes code Sam already stopped using. The migration guide covers Sam's usage pattern explicitly — not just the tutorial case.

**Climax:** Sam's framework ships. Breaking changes are batched (no two to the same API surface per minor cycle), telegraphed in advance, and migration paths documented. Sam files three issues and two PRs over six months — all responded to within two weeks.

**Requirements revealed:** Two `KhaosContext` instances in one process (no hidden globals), raw Vulkan escape hatches documented as last resort, pre-release alpha/beta channel, `@Deprecated` with removal-version notice, semver with explicit breaking-changes changelog, migration guides covering advanced patterns, no two breaking changes to same API surface per minor cycle.

---

### Journey 6: The Dropout — "The Docs Weren't Good Enough"

**Persona:** Alex, 31. Full-stack developer. Web apps, mobile apps. Heard about Khaos from Kotlin Discord. Wants to build a small 3D exploration game. Has never written GPU code. Saturday-project time budget only.

**Opening Scene:** Alex gets the window open in forty minutes. Encouraged. Gets the triangle. Follows the wiki to the next section: adding a second mesh.

**Rising Action:** Alex creates a second `DrawCommand` and adds it to the render graph. The triangle still renders. The second mesh is invisible. No validation error. No error output. Just: the second mesh does not appear.

Three hours of debugging. The types are clean. Nothing is obviously wrong. Wiki search for "multiple meshes," "second draw call," "draw multiple objects" — no results. The wiki covers the triangle. It does not cover the step after the triangle.

What is actually happening: two `DrawCommand` objects sharing a `PipelineHandle` not declared as reusable. The type system allows it — no compile error. The synchronization validation layer doesn't catch it — no VUID violation. Runtime produces a silent omission. The fix is two lines, but the wiki doesn't cover this pattern, the error surface gives no signal, and no KDoc explains that a `PipelineHandle` must be declared reusable for multi-draw use.

Alex posts in Discord. No response for two days.

**Climax:** Alex closes the laptop. Opens Godot documentation. Has a scene with multiple objects in thirty minutes. Closes the Khaos repo.

**Resolution — what would have retained Alex:** A two-sentence callout in the wiki: *"To draw multiple objects with the same pipeline, declare the PipelineHandle as reusable: `PipelineHandle(reusable = true)`. Without this, the pipeline state resets between draws."* Alex would have stayed.

**Abandonment triggers (specific):**
- Silent failure with no diagnostic signal and no type-system protection
- Wiki coverage that ends at the tutorial with no "what's next" path
- No community response within 48 hours on a blocking question
- A working alternative that solves the immediate need in thirty minutes

**Requirements revealed:** Wiki must cover at least one step past each checkpoint, silent failures must be documented as footguns in KDoc, "next steps" at end of each chapter, community response target for blocking questions, `PipelineHandle` reusability (and any handle with non-obvious multi-use semantics) flagged in KDoc with silent failure mode described.

---

### Type-System Limits: The Sequencing-Error Class

> **What Khaos's type system does and does not catch:**
> The type system prevents wrong-enum errors, mismatched handle types, and operations outside their declared scope. It does not prevent *sequencing errors* — submitting a command buffer before its dependency fence has signaled, using a resource before its layout transition completes, or recording commands that are structurally valid but ordered incorrectly for the GPU's execution model.
>
> Khaos's answers to the sequencing-error class: (1) synchronization validation layer in CI via Lavapipe, catching sequencing violations as VUIDs in test runs; (2) the Barrier Cookbook, documenting correct `(srcStage, dstStage, accessMask)` combinations for canonical patterns; (3) record-phase unit tests asserting command stream ordering as data without a GPU. These three mechanisms address what compile-time types cannot. They do not catch every sequencing error — vendor-specific timing behavior on real GPUs is the remaining gap, addressed by the GPU CI gate at v1.
>
> This limit must be stated honestly in the documentation's introduction.

---

### Platform Support Matrix

| Platform | Runtime | Status | Notes |
|---|---|---|---|
| Linux (x64, ARM64) | JVM + LWJGL | **v0 in scope** | Vulkan native; primary CI platform |
| Windows (x64) | JVM + LWJGL | **v0 in scope** | Vulkan native |
| macOS (x64, Apple Silicon) | JVM + LWJGL + MoltenVK | **v0 in scope** | Explicit setup guide required |
| Android | JVM or Native | **Deferred — Vision** | Mobile optimization cost deferred |
| iOS | Kotlin/Native | **Deferred — Vision** | Native port required first |
| Web / WebGPU | — | **Explicitly out of scope** | No roadmap entry |
| Consoles | — | **Explicitly out of scope** | No roadmap entry |
| VR / XR | — | **Explicitly out of scope** | No roadmap entry |

macOS is the hardest onboarding case. Linux is the easiest. The wiki's documented experience baseline is macOS — if it works there, it works everywhere in scope.

---

### Journey Requirements Summary

| Capability | Revealed By |
|---|---|
| Embarrassingly minimal starter template (readable in 20 min) | Journey 1 |
| Explicit platform setup guide (MoltenVK for macOS as Step 0) | Journey 1 |
| Starter template pinned and CI-tested across Linux/Windows/macOS | Journey 1 |
| Maven Central publishing | Journey 1 |
| Concrete type-safety code examples in README | Journey 1 |
| Typed loud errors on silent failure | Journey 1 |
| Vulkan term definitions on first use (plain English callout boxes) | Journeys 1, 2 |
| Time estimates on wiki sections | Journey 1 |
| Resume landmarks at each wiki checkpoint | Journey 1 |
| Wiki coverage one step past each checkpoint | Journey 6 |
| "Next steps" section at end of each wiki chapter | Journeys 1, 6 |
| Silent failures documented as footguns in KDoc | Journey 6 |
| Community response target for blocking questions | Journey 6 |
| KDoc bridging spec language to developer understanding | Journey 2 |
| VUID-to-documentation cross-reference | Journey 2 |
| Contributor-friendly wiki in same repo, no CLA | Journey 2 |
| Contribution response SLA in CONTRIBUTING.md | Journeys 2, 5 |
| Community VUID coverage tracker (pinned issue) | Journey 2 |
| Architecture doc: graph lifecycle and cross-graph resource ownership | Journeys 3, 4 |
| Advanced usage guide with conditional rendering patterns | Journey 3 |
| Pass inputs/outputs statically typed (not string-keyed) | Journey 3 |
| Validation error messages naming layouts with doc links | Journey 3 |
| Barrier cookbook with correct stage flag combinations per access pattern | Journey 4 |
| KDoc warning on BarrierSpec pointing to cookbook | Journey 4 |
| Component-level unit tests with stage flag assertions | Journey 4 |
| CI against real GPU for release candidates | Journey 4 |
| Kotlin named parameters throughout public API | Journey 4 |
| "Why" KDoc on fields carrying non-obvious invariants | Journeys 3, 4 |
| FrameResourceModel / lifetime contract documentation | Journey 4 |
| Two KhaosContext instances in one process (no hidden globals) | Journey 5 |
| Raw Vulkan escape hatches documented as last resort | Journey 5 |
| Pre-release alpha/beta channel for breaking changes | Journey 5 |
| @Deprecated with removal-version notice before breaking changes | Journey 5 |
| Semver with explicit breaking-changes changelog | Journey 5 |
| Migration guides covering documented advanced patterns | Journey 5 |
| Policy: no two breaking changes to same API surface in one minor cycle | Journey 5 |
| Honest documentation of type-system limits (sequencing-error class) | Structural |
| Barrier cookbook + sync validation as explicit answer to sequencing errors | Structural |
| Platform support matrix with explicit in-scope / out-of-scope callouts | Structural |

## Domain-Specific Requirements

### Technical Constraints

- **GPU vendor divergence** — Vulkan behavior on AMD, NVIDIA, and Intel diverges on underspecified corners of the spec. Lavapipe catches VUID violations but not vendor-specific behavior. CI against real hardware (minimum one AMD and one NVIDIA runner) is required for release candidates. Known gap: driver bugs in production that Lavapipe CI never surfaces.
- **JNI boundary constraint** — command recording must stay native-side; per-command JNI crossings at Vulkan's call volumes (~10k–100k per frame) produce non-viable overhead (~200–250 cycles per call). This is an architectural invariant: Kotlin operates at scene/frame orchestration level; the Vulkan command stream is sealed below the JNI boundary at submit time only.
- **GC pause risk on the render loop** — Kotlin/JVM garbage collection can produce frame drops if per-frame allocations are not controlled. Mitigation: object pooling at startup, zero-allocation render loop design, concurrent GC marking enabled. This must be called out in the architecture doc as a known production risk.
- **Shader compilation toolchain fragility** — the GLSL → SPIR-V → Kotlin bindings pipeline has external tool dependencies (shaderc, spirv-val, spirv-cross for Metal). Silent failures in this pipeline produce bindings that compile but bind incorrect data at runtime. Mitigation: spirv-val runs as a CI gate; Gradle task failures must surface as build errors, never warnings.
- **Kotlin/Native compilation speed** — Kotlin/Native compile times are the primary build pain point for the Native target. Mitigation: debug-only builds during development, `kotlin.incremental.native=true`, `.konan` cache in CI, modular cinterop `.def` files.

### Domain Patterns

- **Render graph as pure data** — the canonical pattern for modern Vulkan engines (Granite, Frostbite FrameGraph). The graph compiler is a pure function: graph declaration → barrier-annotated execution plan. This pattern is load-bearing for Khaos's testability thesis.
- **Functional core / imperative shell** — scene traversal, render graph declaration, and resource management are the pure functional core, testable without GPU. Command buffer recording and queue submission are the imperative shell. This boundary must be architecturally enforced, not just documented.
- **Deferred deletion queue** — standard Vulkan resource lifetime pattern. Resources tagged with `currentFrame + N` (frames in flight) are enqueued for deletion; the queue drains each frame. Prevents `vkDeviceWaitIdle` stalls.
- **Bindless descriptor indexing** — Vulkan 1.2+ core (`VK_EXT_descriptor_indexing`). One large descriptor array allocated once; resources assigned stable indices at load time. Retrofitting from traditional per-draw binding is expensive — Khaos designs bindless from the start.

### Risk Mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| GPU vendor divergence in production | High | GPU CI gate for release candidates; document known vendor gaps |
| Kotlin/JVM GC pauses on render loop | High | Zero-allocation render loop; concurrent GC; pool objects at startup |
| JNI overhead on Vulkan hot paths | High | Architectural invariant: command recording stays native-side |
| SPIR-V reflection generating incorrect bindings | Medium | spirv-val CI gate; reflection output tested against expected Kotlin types |
| Kotlin/Native cinterop compile times | Medium | Debug-only builds; incremental native; .konan CI cache |
| External tool dependency failures (shaderc, spirv-cross) | Medium | Gradle task failures as build errors; pin tool versions |
| Vulkan spec evolution invalidating abstractions | Low | Monitor Khronos spec changelog; semver abstractions that may need versioning |

## Innovation & Novel Patterns

### Detected Innovation Areas

**1. Agentic Authorship as First-Class Design Constraint**

Khaos is designed so that agent-generated contributions are semantically correct, not just structurally correct — a distinction that drives specific decisions about module size, KDoc content, sealed hierarchy design, and test granularity. No existing GPU framework has made agent-navigability a first-order architectural concern: module boundaries are sized for agent context windows, every public API surface carries KDoc that encodes invariants as machine-readable contracts, and sealed hierarchies are designed to make the space of illegal states unrepresentable so that an agent exploring a type cannot accidentally construct an invalid program.

**2. Type-Driven Vulkan — Verbosity as Invariant Surface**

Where other Vulkan abstractions use types to achieve ergonomics, Khaos uses types to achieve provability. Every explicit field in a Vulkan struct is a point where a type, a test, or an invariant can be attached. Existing wrappers — LWJGL raw bindings, Vulkano (Rust), bgfx — universally treat Vulkan's verbosity as friction and abstract it away. Khaos treats it as signal: a statement of what the GPU will do, and therefore a surface for correctness verification. This is a philosophical reversal of the entire Vulkan abstraction literature.

**3. Test-Driven Rendering — Render Graph as Testable Pure Data**

By making the render graph a pure data structure and its compiler a pure function, Khaos makes render pass correctness unit-testable without a GPU, without a window, at millisecond speed. Correctness claims — barrier placement, dependency ordering, load/store op assignment — become falsifiable assertions rather than manual inspection targets. TDD for rendering has never been systematically attempted; the field has relied on visual inspection, GPU debugging tools, and integration tests that require hardware. The render graph architecture makes this possible by enforcing a strict functional core / imperative shell: all graph construction logic is pure, all GPU submission is isolated to a thin imperative boundary.

**4. Shift-Left Thesis Applied to GPU Programming**

The hypothesis: a type-driven, deeply-tested rendering codebase will accumulate bugs at a fundamentally lower rate than conventionally-developed engines, making long-term velocity higher despite the upfront cost of type design and test authorship. The shift-left thesis succeeds if the types-and-tests-preventable class of bugs (lifetime errors, synchronization races, invalid descriptor states, barrier misordering) dominates the maintenance burden in conventional engines — which the literature and post-mortems suggest it does. The Khaos kernel is the empirical test of this claim. The engine is its own validation instrument.

**5. Confluence Timing — Three Enabling Technologies Converging in 2025–2026**

This approach is achievable now because three specific things arrived at the same time: (1) Kotlin 2.2+ context parameters, which enable scope enforcement — a CommandBuffer scope that makes it a compile error to call a command-recording function outside a recording context — without the boilerplate of manual capability passing; (2) AI agents capable of navigating sealed type hierarchies and generating code that respects invariant contracts, turning the type-richness from a maintenance burden into a force multiplier; (3) Lavapipe reaching production-grade Vulkan 1.3 compliance, making CI-resident GPU testing possible without hardware — the key enabler for the TDD thesis at scale. Each of these was absent or immature 18 months ago. Their convergence defines the window.

---

### Market Context & Competitive Landscape

The Vulkan abstraction space is dominated by three strategies: raw bindings (LWJGL, ash), thin safety wrappers (Vulkano), and opinionated high-level renderers (wgpu, bgfx, Godot). None of these treat verbosity as correctness signal, none are designed for agent authorship, and none have attempted GPU-headless unit testing as a first-class workflow. The Kotlin game/rendering ecosystem is sparse — no production-grade Vulkan engine exists for KMP. The combination of platform (KMP + JVM-first), methodology (type-driven + TDD), and target developer profile (Kotlin-native, agent-assisted) has no direct competitors in any language.

### Validation Approach

| Innovation Claim | Falsifiable Test | Timeline |
|---|---|---|
| Agentic authorship | Agent completion rate on Khaos tasks vs. raw LWJGL baseline | Alpha milestone |
| Type-driven provability | Ratio of type-caught vs. runtime-caught Vulkan errors in kernel dev log | Ongoing from day 1 |
| TDD for rendering | Percentage of render graph correctness bugs caught by unit tests vs. integration tests | Beta milestone |
| Shift-left velocity | Bug arrival rate per KLOC vs. comparable engine projects | Post-1.0 retrospective |
| Confluence timing | Kotlin 2.2 context parameter adoption rate in Khaos codebase | Tracked at each release |

### Risk Mitigation

**Risk 1 — Type overhead kills agent productivity.** If sealed hierarchies become too deep, agents lose navigation context. Mitigation: module size caps, KDoc invariant encoding, and regular agent-usability audits during development.

**Risk 2 — Render graph purity breaks under Vulkan's imperative demands.** Some Vulkan operations may resist pure-functional modeling. Mitigation: the functional core / imperative shell architecture intentionally isolates these; if a pattern cannot be made pure, it is pushed to the shell and documented as an exception.

**Risk 3 — Lavapipe diverges from hardware behavior.** CPU Vulkan validation may pass tests that hardware fails. Mitigation: periodic hardware validation gates, SPIR-V validation as a separate CI stage, and golden image tests on real GPU as release criteria.

**Risk 4 — Shift-left thesis is wrong for GPU code.** If the dominant bug class turns out to be algorithmic (incorrect rendering math, wrong shader logic) rather than structural, the type-and-test investment is partially wasted. Mitigation: this is acceptable — the engine remains well-tested and type-safe regardless. The thesis failure only means the velocity claim is weaker, not that the approach is wrong.

## Developer Tool Specific Requirements

### Project-Type Overview

Khaos is a runtime library distributed as a Kotlin Multiplatform artifact on Maven Central with a JVM-first release posture. The distribution contract is Gradle + Maven Central — no JitPack fallback, no snapshot repositories. The API surface is plain data classes and sealed hierarchies; no DSL builder layer is planned. Shader binding generation is handled by a combination of a Gradle task (build-time, headless) and a KSP processor (compile-time, IDE-integrated).

### Language Matrix

| Target | Version | Status | Notes |
|---|---|---|---|
| Kotlin/JVM | 2.2+ | **v0 / v1 in scope** | Primary target; LWJGL + JNI; context parameters available |
| Kotlin/Native | TBD | **Vision** | Port begins only after JVM API is stable and frozen |
| Java interop | N/A | Not a design goal | Kotlin-first API; Java callers not supported or tested |

Kotlin 2.2+ is the minimum for context parameters, which are load-bearing for the `CommandBuffer` scope enforcement invariant. This is a hard floor — the language version cannot be relaxed without removing a core type-safety mechanism.

### Installation Methods

**Gradle (Kotlin DSL):**

```kotlin
dependencies {
    implementation("dev.khaos:khaos-core:<version>")
    ksp("dev.khaos:khaos-ksp:<version>")  // shader binding generation
}
```

**Requirements:**
- Published to Maven Central under a stable `dev.khaos` group ID
- No snapshot/SNAPSHOT publishing; pre-release versions use Maven Central staging with `-alpha.N` / `-beta.N` suffixes per semver pre-release convention
- Gradle plugin or Gradle task wrapping shaderc + spirv-val included in the `khaos-gradle` artifact; not bundled into `khaos-core`
- Minimum Gradle version pinned and documented; tested in CI
- No JitPack fallback; Maven Central is the only distribution channel

### API Surface

The Khaos public API surface is governed by three structural rules:

1. **Plain data classes and sealed hierarchies** — no DSL builders, no fluent chaining, no magic. Every API entry point is a constructor, a function, or a `when` branch. The goal is that an agent reading one file understands the full contract without needing to trace method chains.

2. **`@JvmInline value class` for all handles** — `ImageHandle`, `BufferHandle`, `PipelineHandle`, `RenderPassHandle`. Zero runtime overhead; distinct types prevent handle misuse at compile time.

3. **`VulkanOutcome` sealed hierarchy for all fallible operations** — no thrown exceptions on the GPU path. `VulkanOutcome.Success`, `VulkanOutcome.Error`, and domain-specific subtypes. Callers use exhaustive `when`.

**KSP processor — `khaos-ksp`:**

The KSP processor runs at Kotlin compile time and generates typed Kotlin binding classes from annotated shader entry points. Key requirements:

- Annotate a shader entry point with `@KhaosShader("path/to/shader.glsl")` — KSP generates a typed `ShaderBindings` class with properties for each descriptor set binding, push constant block, and vertex input
- Generated types are checked for layout compatibility against the SPIR-V reflection output at compile time — layout mismatch is a compile error, not a runtime failure
- KSP output is deterministic (same annotation → same generated file, regardless of declaration order) — required for incremental compilation correctness
- KSP processor is a separate artifact (`khaos-ksp`) — not bundled into the runtime

**Gradle task — `khaos-gradle`:**

- `./gradlew compileShaders` — GLSL → SPIR-V via shaderc, spirv-val gate, reflection schema emission
- Gradle task failure propagates as a build error (not a warning)
- Shader compilation is incremental: only recompile changed GLSL files
- Reflection schema output is a checked-in artifact (not generated on-the-fly) — schema changes require explicit review

### Code Examples

**Starter template** (v0 milestone requirement):
- Published as a GitHub template repository
- Three Gradle modules: `app`, `khaos-demo`, `khaos-shaders`
- `main.kt` under 50 lines, all non-obvious lines carry KDoc `why` comments
- Gradle config pinned to tested versions; CI-verified across Linux, Windows, macOS
- Represents the "blank project → triangle" documentation milestone

**Companion demo repository** (stretch goal / first project built on Khaos):
- A full PBR scene with shadows and post-processing implemented using Khaos v1
- Serves as the reference implementation for the "Commodore demo" documentation milestone
- Scope: first real project built on top of Khaos, not a bundled sample — separate repo, separate schedule, evaluated after v1 ships

### Migration Guide

Governed by the versioning commitments already established in User Journeys:

- Semver with explicit `CHANGELOG.md` breaking-changes section
- `@Deprecated(message = "Use X. Removal in v0.N.0.")` on every deprecated API, minimum one full minor cycle before removal
- No two breaking changes to the same API surface in a single minor release cycle
- Migration guides cover the advanced usage patterns (framework consumers, not just tutorial users)
- KSP-generated binding classes are versioned alongside the API — KSP output format changes are treated as breaking changes

## Project Scoping & Phased Development

### MVP Strategy & Philosophy

**MVP Approach:** Foundation MVP — the Kernel is the smallest shippable unit of a confident architectural thesis. The thesis (type-driven Vulkan + TDD for rendering + agentic authorship) is treated as proven by design; the Kernel's purpose is to instantiate it correctly, not to test whether it should exist.

**Resource model:** Solo developer, indefinite timeline. Scope does not shrink under time pressure — the project ships when it's right, not on a deadline.

### MVP Feature Set — Kernel (v0)

**Core journeys supported at Kernel completion:**
- Journey 1 (Indie Dev) through the triangle milestone — window opens, triangle renders, CI green
- Journey 4 (AI Agent) — agent reads one module and knows what to do next
- Journey 2 (Debugging) — VUID fires, KDoc bridges the gap, first contribution path open

**KSP processor is a v0 requirement**, not a v1 enhancement — the typed shader binding guarantee is load-bearing for the type-driven thesis, not a DRY convenience added after.

### Kernel Gate Conditions

All six conditions must be green before v1 begins. No partial credit.

| # | Gate | Type | Notes |
|---|---|---|---|
| 1 | CI green on Lavapipe with sync validation, zero VUIDs, zero suppressed warnings | Pass/fail | Primary integration gate |
| 2 | Triangle renders under golden image test, reproducible in CI and locally | Pass/fail | SSIM threshold calibrated across ≥2 test scenarios before declared; threshold value is a reviewed artifact |
| 3 | API *shape* frozen — semver tag cut, no public API changes without a deprecation cycle | Pass/fail | API shape is the gate; KDoc completeness is a quality bar tracked separately |
| 4 | "Blank project → triangle" doc walkable by an outsider matching the baseline | Pass/fail | This is the integration test for the mental model — if the walkthrough can't be written cleanly, the API is not actually frozen |
| 5 | Deferred issues list exists and is written — every open item classified as: deferred-to-v1 / out-of-scope-forever / known-bug-tolerated | Pass/fail | "No deferred correctness issues" has no pass/fail condition without this list |
| 6 | Render graph compile function callable without a Vulkan instance in scope | Pass/fail | If this isn't true, the pure-function claim doesn't hold at the CI boundary |

### Kernel Scope Boundaries

**In scope for v0:**
- Typed Vulkan wrapper layer: `@JvmInline value class` handles, sealed hierarchies, `VulkanOutcome`
- VMA: one allocator instance, default VMA behavior, no custom pools, no defragmentation — explicitly bounded
- Resource lifetime: deferred deletion queue interface with `frameN` contract (interface expresses "resource done after frame N" even if Kernel impl is trivially single-frame — the interface cannot be simplified later without an API break)
- Shader pipeline: GLSL → SPIR-V via shaderc, SPIR-V validation gate, KSP processor generating Kotlin types for descriptor bindings, push constants, vertex inputs
- KSP determinism: output stable across incremental processing invocations (change one shader → only affected outputs regenerate) — not just same-run repeatability
- Minimal render graph: clear, draw triangle, present — compiler as pure function, output deterministic
- CI: GitHub Actions + Lavapipe, validation layer, golden image (SSIM, calibrated threshold), record-phase unit tests, render graph determinism test, barrier semantics tests (correct stage/access flags for canonical patterns — not just stability), property tests for math (spot-checked against GLM/nalgebra reference values)

**Explicit non-goals for v0:**
- Multiple render passes
- Windowing abstraction (surface creation only)
- Custom VMA memory pools or defragmentation
- KDoc completeness (tracked separately from API shape)
- Java interop
- Kotlin/Native target
- Performance optimization (correctness first)
- Android, iOS, Web, consoles, VR/XR

### Post-Kernel Phases

**Phase 2 — v1 (Growth):** Full PBR renderer, complete render graph, production resource lifetime (N-frame-in-flight replacing single-frame Kernel impl — API unchanged), performance regression gate, "Commodore demo" documentation milestone.

**Phase 3 — Vision:** Metal backend, Kotlin/Native port, editor (separate product), scene graph / material system / asset pipeline as downstream consumers.

### Risk Mitigation

| Risk | Severity | Mitigation |
|---|---|---|
| Vulkan complexity causes kernel stall | Medium | Gate structure prevents expansion pressure; no v1 work until all 6 gates green |
| shaderc native dependency breaks CI matrix | Medium | Verify CI image has shaderc before building pipeline around it; offline SPIR-V fallback option if needed |
| KSP incremental correctness | Medium | Explicit incremental-case test (change one shader, verify only affected outputs regenerate) required before gate 6 |
| SSIM threshold miscalibrated | Medium | Threshold set after ≥2 test scenarios, not just triangle; threshold is a reviewed artifact |
| Deferred deletion queue interface retrofit | Low | Interface expresses `frameN` contract at v0; impl is trivially single-frame; upgrade path is implementation-only |
| Solo bandwidth limits community response | Low | Two-week SLA achievable solo; VUID tracker and wiki are async |

## Functional Requirements

This is the capability contract for all downstream work. Every epic, story, and design decision must trace to an FR listed here. Capabilities not listed here do not exist in the product.

---

### Engine Initialization & Device Management

- **FR47:** A developer can initialize a Vulkan instance, select a physical device, and create a logical device with configured queue families.
- **FR48:** A developer can create and manage GPU synchronization primitives to coordinate CPU-GPU and GPU-GPU execution ordering.

---

### Vulkan Wrapper Layer

- **FR1:** A developer can represent Vulkan handles as distinct opaque types that prevent cross-handle misuse at compile time.
- **FR2:** A developer can express all Vulkan operation outcomes as a typed result hierarchy with exhaustive handling enforced by the compiler.
- **FR3:** A developer can be prevented at compile time from invoking GPU command-recording operations outside an explicitly declared recording scope.
- **FR4:** A developer can call all public Vulkan-facing API methods using named parameters.
- **FR5:** A developer can access underlying raw Vulkan handles through a documented escape hatch explicitly marked as a last resort.
- **FR54:** A developer can attach human-readable debug labels to GPU resources and command recording regions for use with external GPU debugging tools.

---

### Render Graph & Pipeline

- **FR6:** A developer can declare a render graph as a pure data structure specifying passes, attachments, and resource dependencies.
- **FR7:** The render graph compiler can produce a barrier-annotated execution plan as a pure function of its declared input.
- **FR8:** The render graph compiler can be invoked in a test environment without a live Vulkan instance or GPU device.
- **FR9:** Identical render graph declarations produce byte-identical barrier sequences and resource ordering across repeated compilations.
- **FR10:** A developer can declare render pass inputs and outputs as statically typed resource references rather than string-keyed names.
- **FR11:** A developer can declare external resources for sharing across graph boundaries with explicitly defined ownership semantics.
- **FR12:** A developer can create a platform-specific Vulkan surface and swap chain to display rendered frames on a platform window.
- **FR50:** A developer can create and manage GPU pipeline state objects as named, reusable rendering configurations.
- **FR51:** A developer can handle recoverable Vulkan conditions (swapchain invalidation, surface loss) without application restart.

---

### Shader System

- **FR13:** A developer can compile GLSL shader sources to SPIR-V as a Gradle build task with failure surfaced as a build error.
- **FR14:** The build system can validate SPIR-V output for specification compliance before any GPU dispatch.
- **FR15:** A developer can generate typed Kotlin binding classes from annotated shader entry points at Kotlin compile time.
- **FR16:** KSP-generated binding classes express descriptor set bindings, push constant blocks, and vertex inputs as typed properties.
- **FR17:** A layout mismatch between a shader and its generated Kotlin binding class produces a compile error, not a runtime failure.
- **FR18:** KSP-generated output is stable across incremental compilation — changing one shader regenerates only affected binding classes.

---

### Resource & Memory Management

- **FR19:** A developer can express resource lifetimes in terms of typed frame indices rather than raw integer values.
- **FR20:** The engine can enqueue resources for destruction at a specified future frame boundary, deferring deallocation without blocking the render loop.
- **FR21:** A developer can allocate GPU memory through an engine-managed allocator without configuring custom pools or defragmentation strategies.
- **FR22:** Multiple engine context instances can coexist in the same process without shared global state.
- **FR49:** A developer can create, populate, and bind vertex and index buffers for geometry submission.
- **FR55:** A developer can create, upload data to, and configure sampling parameters for GPU image resources.
- **FR56:** A developer can transfer data from CPU-accessible staging memory to GPU-optimal memory as a managed engine operation.
- **FR59:** A developer can signal frame completion to the engine to trigger deferred resource cleanup for that frame's queued deletions.

---

### Testing & Verification

- **FR23:** A developer can run Vulkan integration tests against a CPU-resident Vulkan implementation without physical GPU hardware.
- **FR24:** The CI system can enforce that zero validation layer VUIDs fire on any commit, with zero suppressed warnings.
- **FR25:** A developer can assert the pixel-accuracy of rendered output against governed reference images using a calibrated similarity threshold.
- **FR26:** A developer can write unit tests that assert command buffer recording behavior without GPU dispatch.
- **FR27:** A developer can write tests that assert the barrier stage and access flags produced for a given GPU access pattern are semantically correct.
- **FR28:** A developer can write property-based tests for math primitives with assertions verified against reference implementation values.
- **FR29:** A developer can assert that generated shader binding types are layout-compatible with their SPIR-V reflection output without runtime execution.

---

### Distribution, Versioning & Build

- **FR30:** A developer can add Khaos to a Gradle project via a Maven Central dependency declaration without additional repository configuration.
- **FR31:** A developer can receive pre-release builds via versioned alpha/beta artifacts on Maven Central before stable release.
- **FR32:** Deprecated APIs carry machine-readable removal-version notices surfaced in IDE tooling.
- **FR33:** Breaking API changes are preceded by at minimum one full minor release cycle of deprecation notice on the affected surface.
- **FR34:** A developer can bootstrap a new Khaos project from a published starter template with a verified, CI-tested Gradle configuration.
- **FR57:** A developer can configure or redirect engine diagnostic and validation output to a provided handler rather than a fixed output stream.
- **FR58:** The Khaos Gradle plugin declares its minimum compatible Gradle version and produces an actionable error when an incompatible version is detected.

---

### Documentation, Discoverability & Community

- **FR35:** A developer can follow a documented path from a blank Kotlin project to a rendered triangle without prior graphics programming experience.
- **FR36:** A developer can find a plain-English explanation of any Vulkan term on its first occurrence in the documentation.
- **FR37:** A developer can find the correct barrier stage and access flag combination for any canonical GPU access pattern in the architecture documentation.
- **FR38:** A developer can cross-reference a Vulkan validation error (VUID) to a plain-English explanation and recommended fix.
- **FR39:** A developer can find documented silent failure modes for any public API type with non-obvious multi-use semantics.
- **FR40:** A developer can find render graph lifecycle rules and cross-graph resource ownership contracts in the architecture documentation before writing render graph code.
- **FR41:** A developer can find time estimates and resume landmarks at each checkpoint in the getting started guide.
- **FR42:** Every documentation chapter ends with a "next steps" section pointing toward the following capability.
- **FR43:** A developer can find explicit platform setup instructions for macOS (MoltenVK) as the first step of the getting started guide.
- **FR44:** A developer can contribute documentation and code changes via pull request without signing a contributor license agreement.
- **FR45:** A contributor can expect a response to issues and pull requests within a published, written SLA.
- **FR46:** A developer can find the community-maintained VUID coverage status (documented / in-progress / missing) in a single tracked location.
- **FR52:** A developer can find advanced usage patterns (multi-pass, conditional rendering, secondary command buffers) in the documentation beyond the tutorial path.
- **FR53:** A developer can find a migration guide for advanced usage patterns when upgrading across breaking changes.
- **FR60:** A developer can access a maintained architecture document that describes engine design decisions, invariants, and known constraints as a distinct artifact from the tutorial wiki.

## Non-Functional Requirements

### Performance

- **NFR-PERF-1:** The reference v1 PBR scene establishes a frame time baseline at v1 kickoff; no release introduces a frame time regression exceeding 10% of that baseline without an explicit architectural justification and documented trade-off.
- **NFR-PERF-2:** The render loop allocates zero JVM heap objects on the per-frame hot path — all per-frame allocations are pooled at startup.
- **NFR-PERF-3:** The full headless test suite (record-phase unit tests, render graph determinism, property tests) completes in under 60 seconds on CI hardware as a steering goal; additions that push beyond this target require justification.
- **NFR-PERF-4:** Incremental shader recompilation for one changed GLSL source completes in under 10 seconds on a development machine.
- **NFR-PERF-5:** KSP incremental regeneration for one changed shader annotation completes in under 15 seconds on a development machine.

### Correctness

- **NFR-CORR-1:** Zero Vulkan validation layer VUIDs fire on the Lavapipe CI target at any commit on main — this is a hard correctness floor, not a soft quality bar.
- **NFR-CORR-2:** The render graph compiler produces byte-identical output across 1,000 repeated invocations with identical input, regardless of JVM run state or hash map iteration order.
- **NFR-CORR-3:** All math primitive operations produce results within double-precision floating point tolerance of reference implementation values (GLM or nalgebra) for the tested input domain.
- **NFR-CORR-4:** SPIR-V validation (`spirv-val`) passes on every compiled shader artifact before CI is declared green.

### Reliability

- **NFR-REL-1:** Flaky tests — any test that produces non-deterministic pass/fail results — are treated as P0 correctness bugs and block merges to main.
- **NFR-REL-2:** Golden image SSIM thresholds are stable across releases; a threshold change requires a diff artifact and written justification before merge.
- **NFR-REL-3:** Identical source inputs to the Gradle build produce byte-identical JAR artifacts, enabling effective remote caching.
- **NFR-REL-4:** No unannounced breaking changes ship on the stable artifact stream — every public API break is preceded by `@Deprecated` with a removal-version notice for at minimum one full minor release cycle.

### Maintainability

- **NFR-MAINT-1:** No single Kotlin source file in the public API surface exceeds 500 lines. Modules that are inherently large (render graph compiler, device initialization) may extend to 800 lines, with a KDoc section index at the file header.
- **NFR-MAINT-2:** 100% of public types and functions carry at minimum a single-sentence KDoc — the quality bar for Kernel gate 3.
- **NFR-MAINT-3:** Every public API type or function with a non-obvious invariant carries a `WHY:` KDoc comment explaining the constraint, not just describing the behavior.
- **NFR-MAINT-4:** An agent with access to a single source file and its KDoc can determine the correct usage of that module's public API without loading additional context files.

### Toolchain & Integration

- **NFR-TOOL-1:** All external tool versions (shaderc, spirv-val, spirv-cross, Lavapipe) are pinned in CI configuration and checked into source control — no tool is resolved from `latest` at CI time.
- **NFR-TOOL-2:** Any toolchain failure (shader compile error, SPIR-V validation violation, KSP processing error) produces a Gradle build failure with a non-zero exit code — never a warning or a log message.
- **NFR-TOOL-3:** Vulkan 1.3 is the minimum API version; no 1.0/1.1-only code paths exist in the public API surface.
- **NFR-TOOL-4:** The engine operates correctly on all three in-scope platforms (Linux x64/ARM64, Windows x64, macOS x64/Apple Silicon) without platform-specific divergence in the Kotlin-side API.
