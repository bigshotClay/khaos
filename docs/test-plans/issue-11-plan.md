# Test Plan — Issue #11: VK-7: Command pool and RecordingScope context type

## Scope

Command recording safety enforced by Kotlin context parameters in `khaos-cmd`. `draw()` outside a `RecordingScope` is a compile error. Requires Lavapipe for runtime tests; compile tests are GPU-free.

## Preconditions

- `KhaosInstance`, `KhaosDevice` available (VK-3, VK-4 complete).
- Kotlin 2.2+ with `-Xcontext-parameters` enabled (set in F-1/#4 build).
- VUID gate active for runtime tests.

## Test Cases

### Acceptance

- **TC-1: KhaosCommandPool typed allocation** — Create a `KhaosCommandPool` with pool-level reset flag. Allocate a command buffer via `pool.allocate(level = CommandLevel.Primary)`. Assert result is `KhaosCommandBuffer`. [VUID gate]
- **TC-2: Recording functions require RecordingScope** — Negative compile test: call `commandBuffer.draw(...)` without a `RecordingScope` in scope. Assert the code does not compile (expected compiler error). This test lives in a separate module that is expected to fail compilation.
- **TC-3: RecordingScope is a context parameter** — Inspect `KhaosCommandBuffer.draw()` signature: it is declared `context(RecordingScope) fun draw(...)`, not `fun draw(scope: RecordingScope, ...)` and not using an implicit receiver. Confirmed by source inspection.
- **TC-4: beginRecording / endRecording pair** — `beginRecording()` returns `RecordingScope`; `endRecording()` consumes it and closes the recording. Assert a full begin→draw→end cycle executes without VUID. [VUID gate]
- **TC-5: Recording functions compile with RecordingScope** — Positive compile test: call `draw()`, `bindPipeline()`, `setViewport()`, `setScissor()` inside a valid `RecordingScope` context. All compile successfully.
- **TC-6: KDoc on RecordingScope** — `RecordingScope` has KDoc explaining it is a context type and why it is not a regular parameter. Confirmed by documentation check.

### Design Contract

- **TC-7: CommandLevel typed enum** — `CommandLevel.Primary` and `CommandLevel.Secondary` are typed values — not raw `Int` VK constants. No raw `VK_COMMAND_BUFFER_LEVEL_PRIMARY` literal in call sites.
- **TC-8: KhaosCommandBuffer wraps VkCommandBuffer** — Raw `VkCommandBuffer` handle is not exposed in public API. `KhaosCommandBuffer` holds a `CommandBufferHandle` (from VK-2). Confirmed by API inspection.
- **TC-9: Teardown order** — Command buffers freed, pool destroyed, device destroyed, instance destroyed in order. Assert zero VUIDs. [VUID gate]

### Failure Paths

- **TC-10: Double-begin recording** — Call `beginRecording()` twice on the same command buffer without `endRecording()`. Assert VUID fires or typed error returned — not silent corruption. [VUID gate — intentional test]
- **TC-11: Command buffer use after pool reset** — Reset the pool via `vkResetCommandPool`; then attempt to use a previously-allocated command buffer. Assert typed error or VUID. [VUID gate]
