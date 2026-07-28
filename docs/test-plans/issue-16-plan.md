# Test Plan — Issue #16: SHADER-1: Gradle GLSL → SPIR-V compilation task

## Scope

`ShaderCompileTask` and `SpirvValidateTask` Gradle tasks in `khaos-gradle`. CI gate: any `spirv-val` failure fails the build. Tests are Gradle task integration tests; platform matrix covers Linux/macOS/Windows.

## Preconditions

- Vulkan SDK installed; `VULKAN_SDK` env var set.
- `glslc` and `spirv-val` on PATH via `$VULKAN_SDK/bin/`.
- `jakoch/install-vulkan-sdk-action` configured on CI.

## Test Cases

### Acceptance

- **TC-1: ShaderCompileTask compiles glsl → spv** — Place a valid `.vert` and `.frag` shader in `src/main/glsl/`. Run `./gradlew compileShaders`. Assert `.spv` files appear in `build/generated/shaders/`. Assert exit code 0.
- **TC-2: SpirvValidateTask runs spirv-val on output** — After `compileShaders`, run `./gradlew validateShaders`. Assert `spirv-val` runs on all `.spv` files. Assert exit code 0 for valid SPIR-V.
- **TC-3: @CacheableTask with declared inputs/outputs** — Verify `ShaderCompileTask` has `@CacheableTask` annotation and declares `@InputFiles` on glsl sources and `@OutputDirectory` on spv output. Run task twice; assert the second run is UP-TO-DATE (Gradle cache hit).
- **TC-4: Incremental compilation** — Modify one of two shaders. Run `compileShaders`. Assert only the modified shader's `.spv` is regenerated (via `InputChanges` — task output timestamp or Gradle task log shows 1 file processed, not 2).
- **TC-5: validateShaders before processResources** — Verify task graph: `processResources` depends on `validateShaders`. Run `./gradlew processResources`; assert `validateShaders` ran first (Gradle task execution order log).
- **TC-6: VULKAN_SDK unset fails with clear message** — Unset `VULKAN_SDK`. Run `./gradlew compileShaders`. Assert build fails immediately with an error message containing `"VULKAN_SDK"`. Assert no partial compilation output.
- **TC-7: Compilation error includes file + line** — Introduce a syntax error in a `.glsl` file. Run `compileShaders`. Assert error message contains the source file name and line number. Assert exit code != 0.
- **TC-8: CI platform matrix** — CI build passes on Linux (Lavapipe), macOS, and Windows runners. Confirmed by CI job matrix success.
- **TC-9: spirv-val version logged** — In CI output, `spirv-val --version` (or equivalent) is logged before validation runs. Assert log line is present in CI artifact.

### Design Contract

- **TC-10: isIgnoreExitValue = false** — Inspect `SpirvValidateTask` implementation: `isIgnoreExitValue` is not set to `true`. A non-zero `spirv-val` exit code propagates as a build failure. Confirmed by code review.
- **TC-11: Input/output directory paths documented** — Source: `src/main/glsl/`; output: `build/generated/shaders/`. These paths are declared as constants or properties — not hardcoded strings scattered across task code.

### Failure Paths

- **TC-12: Invalid SPIR-V fails build** — Produce an invalid `.spv` file (e.g., by truncating a valid one). Run `validateShaders`. Assert build fails with a non-zero exit code and a message identifying the failing file.
- **TC-13: glslc not on PATH** — Set `VULKAN_SDK` to a directory where `glslc` does not exist. Run `compileShaders`. Assert build fails with a message naming `glslc` as missing — not a cryptic JVM exception.
