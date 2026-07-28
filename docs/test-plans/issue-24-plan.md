# Test Plan — Issue #24: TEST-2: Record-phase unit test framework

## Scope

`TestCommandBuffer` test harness in `khaos-test-harness`. Implements the `context(RecordingScope)` interface; records commands to an in-memory list. GPU-free. All tests run in `./gradlew test`.

## Test Cases

### Acceptance

- **TC-1: TestCommandBuffer satisfies RecordingScope contract** — `TestCommandBuffer` can be used anywhere a real `KhaosCommandBuffer` is accepted in a `context(RecordingScope)` lambda. Confirmed by compilation: existing code paths that take `context(RecordingScope)` lambdas accept `TestCommandBuffer`.
- **TC-2: In-memory command recording** — After recording `bindPipeline` + `draw`, call `testBuffer.recordedCommands`. Assert a list with two entries, in order.
- **TC-3: assertCommandAt** — `assertCommandAt(0, CommandType.BindPipeline)` passes when `bindPipeline` was the first command. `assertCommandAt(0, CommandType.Draw)` fails for the same buffer.
- **TC-4: assertBarrier** — Record a pipeline barrier (via GRAPH-4 executor or a direct call). Call `assertBarrier(srcStage = PipelineStage.ColorAttachmentOutput, dstStage = PipelineStage.FragmentShader)`. Assert passes.
- **TC-5: assertDrawCall** — Record `draw(vertexCount = 3)`. Call `assertDrawCall(vertexCount = 3)`. Assert passes. Call `assertDrawCall(vertexCount = 6)`. Assert fails.
- **TC-6: GPU-free construction** — Construct `TestCommandBuffer` with no arguments (no device, no instance, no native library). Assert no `UnsatisfiedLinkError`.
- **TC-7: Zero native library loading** — Run a test using `TestCommandBuffer` with `VULKAN_SDK` unset and no Vulkan driver present. Assert test passes — no native loading at all.
- **TC-8: Three reference tests implemented** — Assert the following three tests exist and pass: (a) bind pipeline + draw call; (b) barrier insertion before draw; (c) empty command buffer (zero commands recorded).

### Design Contract

- **TC-9: Drop-in for real buffer** — The same `context(RecordingScope)` lambda that works with a `KhaosCommandBuffer` in production works with `TestCommandBuffer` in tests, with no code changes to the lambda. Confirmed by a shared test fixture.
- **TC-10: Module placement** — `TestCommandBuffer` lives in `khaos-test-harness` module — not in production `khaos-cmd`. Confirmed by module structure inspection.

### Failure Paths

- **TC-11: assertCommandAt out of range** — Call `assertCommandAt(99, CommandType.Draw)` on a buffer with 2 commands. Assert an informative assertion failure message including the actual command count — not an `IndexOutOfBoundsException`.
- **TC-12: Record after end** — Attempt to record a draw call after `endRecording()` on the `TestCommandBuffer`. Assert a typed error or assertion failure — not a silent no-op that corrupts the recorded list.
