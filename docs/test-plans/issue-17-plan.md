# Test Plan — Issue #17: SHADER-2: Gradle codegen task — SPIR-V reflection → typed Kotlin bindings

## Scope

`ShaderBindingGenTask` and `reflectShaders` Gradle tasks in `khaos-gradle`. Deterministic codegen: same reflection JSON → byte-identical Kotlin output. Generated bindings visible to all KMP targets.

## Preconditions

- SHADER-1 complete: `compileShaders` produces `.spv` files.
- `spirv-cross` on PATH via `$VULKAN_SDK/bin/`.
- `VULKAN_SDK` env var set.

## Test Cases

### Acceptance

- **TC-1: reflectShaders produces .reflection.json** — After `compileShaders`, run `./gradlew reflectShaders`. Assert `*.reflection.json` files appear in `build/generated/shaders/reflection/`. Assert exit code 0.
- **TC-2: ShaderBindingGenTask reads JSON, emits Kotlin** — Run `./gradlew generateShaderBindings`. Assert one `*Shader.kt` file per shader appears in `build/generated/shader-bindings/kotlin/`. Assert exit code 0.
- **TC-3: @CacheableTask with declared inputs/outputs** — `reflectShaders` and `generateShaderBindings` both carry `@CacheableTask` with `@InputFiles` / `@OutputDirectory`. Run each twice; assert UP-TO-DATE on second run.
- **TC-4: Emitted types are correct Kotlin** — For a UBO with a single binding, assert the generated file contains a `@JvmInline value class`. For a push constant block, assert a `data class`. For vertex input attributes, assert a `sealed interface`.
- **TC-5: Generated sources in commonMain** — Assert `generateShaderBindings.outputDir` is wired into `kotlin.sourceSets.named("commonMain")`. Running `./gradlew compileKotlin` after codegen succeeds without "unresolved reference" errors on generated types.
- **TC-6: Incremental — only changed shaders regenerate** — Modify one of two shaders. Run `generateShaderBindings`. Assert only the modified shader's `.kt` file is regenerated (one file processed in `InputChanges`).
- **TC-7: Task graph wiring** — Assert `generateShaderBindings` depends on `reflectShaders` and `reflectShaders` depends on `compileShaders`. Verified by `./gradlew generateShaderBindings --dry-run` output.
- **TC-8: VULKAN_SDK unset fails with clear message** — Unset `VULKAN_SDK`. Run `./gradlew reflectShaders`. Assert build fails with a message containing `"VULKAN_SDK"`.

### Design Contract

- **TC-9: Determinism** — Run `generateShaderBindings` 5 times with the same inputs, each in a fresh Gradle daemon. Assert all 5 output files are byte-identical. (If Gradle cache reuses the UP-TO-DATE output, force a clean between runs: `./gradlew clean generateShaderBindings`.)
- **TC-10: Binding type naming convention** — Generated file is named `{ShaderName}Shader.kt`. Confirmed by inspection of a generated file.

### Failure Paths

- **TC-11: Malformed reflection JSON fails with named field** — Pass a reflection JSON with a missing required field (e.g., `"type"` absent). Assert `ShaderBindingGenTask` throws with the shader filename and missing field name in the error message — not a NullPointerException with no context.
- **TC-12: spirv-cross not on PATH** — Run `reflectShaders` with `spirv-cross` missing. Assert build fails with a message naming `spirv-cross` — not a cryptic process error.
- **TC-13: Empty shader (no bindings)** — Process a shader with no UBOs, no push constants, no vertex inputs. Assert the codegen task runs without error and produces a minimal (possibly empty or comment-only) `.kt` file — not a crash.
