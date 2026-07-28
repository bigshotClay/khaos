# Test Plan — Issue #25: TEST-3: Render graph determinism test

## Scope

Prove `compile(RenderGraphSpec)` is truly deterministic across 100 JVM runs with hash randomization enabled. GPU-free. This is kernel gate condition #2.

## Preconditions

- GRAPH-1 (`RenderGraphSpec`) and GRAPH-2 (`compile()`) complete.
- No GPU required.

## Test Cases

### Acceptance

- **TC-1: Multi-pass spec construction** — Test constructs a `RenderGraphSpec` with at least 2 render pass nodes and 3 resource transitions. Assert construction succeeds with no Vulkan instance.
- **TC-2: 100-run equality check** — Call `compile(spec)` 100 times in a single JVM run. Collect all results. Assert all 100 `CompiledGraph` instances are structurally equal: same pass order, same barrier sequence, same resource aliasing decisions.
- **TC-3: Hash randomization stress** — Test JVM args include `-Djava.util.HashMap.randomSeed=true` (or equivalent for the JVM version in use). Confirmed by Gradle test task `jvmArgs` configuration.
- **TC-4: No Vulkan in scope** — Assert `compile()` runs successfully with no `VkInstance` in the JVM — no `UnsatisfiedLinkError`, no Vulkan driver load. Confirmed by test running with `VULKAN_SDK` unset.
- **TC-5: Part of ./gradlew test** — This test is in the standard Gradle test task, not a separate custom task. Assert it runs on every `./gradlew test` invocation.

### Design Contract

- **TC-6: Equality is structural** — `CompiledGraph` equality checks pass order, barrier sequence, AND aliasing decisions — not object identity. Assert `compile(spec) == compile(spec)` via structural equality (`equals()`/`data class`).
- **TC-7: JVM args present in test task** — Inspect `build.gradle.kts` test task configuration: `-Djava.util.HashMap.randomSeed=true` (or equivalent) is present. Confirmed by grep on build file.
- **TC-8: Pass order is deterministic** — Assert that the compiled pass order is alphabetical (or another declared stable ordering) — not dependent on hash-map iteration. Confirmed by asserting a specific pass ordering in the 100-run test.

### Failure Paths

- **TC-9: Non-determinism detection** — If any of the 100 compiled graphs differs from the first (reference), the test fails with a message identifying which run differed and what was different (pass order? barrier? aliasing?). Assert the assertion message is informative — not just `expected true but was false`.
- **TC-10: Large spec (10 passes, 15 transitions)** — Run the 100-iteration test with a larger spec (10 passes, 15 resource transitions) to stress hash-map ordering. Assert same determinism guarantee holds.
