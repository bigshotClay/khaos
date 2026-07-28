# Test Plan — Issue #19: GRAPH-2: Render graph compiler — pure function

## Scope

`compile(spec: RenderGraphSpec): CompiledGraph` in `khaos-graph`. Pure function: no GPU, no Vulkan, no side effects. Determinism is a hard requirement — see also TEST-3 (#25). All tests run headlessly.

## Test Cases

### Acceptance

- **TC-1: compile is a top-level function** — Assert `compile` is a standalone top-level function — not a method on a class or a dependency-injected instance. Confirmed by source inspection.
- **TC-2: CompiledGraph structure** — Assert `CompiledGraph` contains: ordered pass list, `BarrierSpec` for each resource transition, and resource aliasing decisions. Confirmed by API inspection of `CompiledGraph` type.
- **TC-3: BarrierSpec typed fields** — `BarrierSpec` has typed fields: `srcStage: PipelineStage`, `dstStage: PipelineStage`, `srcAccess: AccessMask`, `dstAccess: AccessMask`, `oldLayout: ImageLayout`, `newLayout: ImageLayout`. No raw `Int` flags. Confirmed by API inspection.
- **TC-4: Triangle spec compilation** — Compile the standard triangle spec (clear pass → draw pass → present). Assert the resulting `CompiledGraph` contains exactly 2 passes. Assert a `BarrierSpec` for the color attachment transition from `Undefined` to `ColorAttachmentOptimal`. Assert specific `srcStage`, `dstStage` values match the expected pattern (see Barrier Cookbook).
- **TC-5: GPU-free execution** — Call `compile(triangleSpec)` with no `VkInstance` in scope. Assert no `UnsatisfiedLinkError` or native library loading. Assert return is non-null.
- **TC-6: KDoc Barrier Cookbook warning** — `BarrierSpec` KDoc includes the Barrier Cookbook warning about stage flag non-interchangeability. Confirmed by documentation check.

### Design Contract

- **TC-7: Determinism — 100 runs** — Call `compile(triangleSpec)` 100 times in the same JVM. Assert all 100 results are structurally equal (pass order, barrier sequence, aliasing decisions). Run with `-Djava.util.HashMap.randomSeed=true`. (This is also the TEST-3 gate condition — these are the same assertions.)
- **TC-8: No HashMap for pass ordering** — Inspect `compile()` implementation: internal pass ordering must use a `LinkedHashMap` or sorted structure — not `HashMap` or `HashSet`. Confirmed by code review. (If a `HashMap` is present for any ordering-sensitive data, this is a bug.)
- **TC-9: Aliasing decisions in output** — Assert `CompiledGraph.aliasingDecisions` (or equivalent) is present and non-null. For the triangle spec, assert transient resources are identified correctly.

### Failure Paths

- **TC-10: Cycle in spec** — Construct a `RenderGraphSpec` where pass A depends on pass B and pass B depends on pass A (a cycle). Assert `compile()` returns a typed `CyclicDependency` error or throws `IllegalArgumentException` with a message identifying the cycle — not an infinite loop.
- **TC-11: Unreachable pass** — Add a pass to the spec that no other pass depends on and that produces no output consumed by any other pass. Assert the compiler either includes it (valid) or returns a typed `UnreachablePass` warning — not silent omission.
- **TC-12: Empty spec compile** — Call `compile(RenderGraphSpec())` with no passes. Assert result is a valid empty `CompiledGraph` — not a crash.
