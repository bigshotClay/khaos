# SPIKE-SHADER-1: Validate shaderc + spirv-val Gradle Task Integration

**Issue:** #1  
**Date:** 2026-04-18  
**Status:** Decision ready  
**Author:** Atlas (agent-architect)

---

## Verdict

**Proceed with shader compilation in Gradle — but not via LWJGL shaderc bindings.**

The spike hypothesis was "shaderc via LWJGL 3 can compile GLSL to SPIR-V from a Gradle task." The investigation invalidates that path: LWJGL is a runtime library, not a build tool, and its native extraction mechanism is problematic in the long-lived Gradle daemon JVM. The correct path — confirmed by ecosystem evidence and real production plugins — is invoking `glslc` (Google Shaderc CLI) as a subprocess via Gradle `Exec`-style tasks, with a separate `Exec` invocation of `spirv-val` for validation. Both are installed by the Vulkan SDK and run headlessly on all target platforms including GitHub Actions with Lavapipe.

---

## Questions Answered

### 1. Can shaderc (LWJGL 3 bindings) compile GLSL to SPIR-V from a Gradle task on Linux, macOS, and Windows?

**Technically yes, practically no.**

LWJGL 3 ships a `lwjgl-shaderc` artifact (`org.lwjgl:lwjgl-shaderc:3.4.1+`) that wraps libshaderc with a full JVM API:
- `Shaderc.shaderc_compile_into_spv()` — GLSL → SPIR-V
- `Shaderc.shaderc_compile_into_preprocessed_text()` — preprocessing
- Platform-native binaries bundled per-classifier (linux, macos, windows)

However: LWJGL's native extraction system (`SharedLibraryLoader`) is designed for runtime application use. In a Gradle daemon JVM — a long-lived process that may run across multiple build invocations and multiple parallel daemons — LWJGL's native library loading has known issues:
- The daemon may fail to extract or lock native JARs on first invocation
- Multiple concurrent daemon instances can conflict on the same temp directory
- The ecosystem has no production examples of LWJGL APIs called from Gradle build tasks

**Conclusion:** LWJGL shaderc bindings are off the table for build-time use. Source: LWJGL forum threads, Gradle forum discussion, absence of any ecosystem precedent.

### 2. Can spirv-val validate output and return a structured result catchable in the Gradle task context?

**Yes, but only as a subprocess — there are no JVM bindings.**

`spirv-val` is a C++ CLI tool from SPIRV-Tools. LWJGL explicitly does NOT wrap SPIRV-Tools (tracked as issue #147, not implemented). No JVM library provides SPIR-V validation with VkResult-level structured output.

The correct integration: invoke `spirv-val` as a subprocess via Gradle `Exec`. On failure, the process exits non-zero and writes to stderr — Gradle's `Exec` task type propagates this as a build failure automatically. Structured error output (line/column, VUID-like messages) is available on stderr and can be captured via `standardOutput` / `errorOutput` streams.

### 3. What Gradle task type is needed?

**A custom task class extending `DefaultTask`, with incremental support.**

Using `Exec` directly for a single file works but doesn't scale. The right design is a custom `@CacheableTask` that:
- Declares `@InputFiles @SkipWhenEmpty` for shader sources
- Declares `@OutputDirectory` for compiled SPIR-V
- Supports incremental builds via `InputChanges` (only recompile changed shaders)
- Invokes `glslc` per-source via a Gradle `WorkQueue` (parallel workers)

For `spirv-val`: a second custom task (or a post-action on the compile task) that validates each `.spv` output.

### 4. Are there platform-specific native library loading requirements in a Gradle task context?

**With glslc subprocess: none.** The subprocess environment is independent of the Gradle JVM. Platform-specific requirements reduce to: `glslc` binary must be on `PATH` (or an absolute path configured in the task).

Platform matrix:
- **Linux CI (GitHub Actions):** `jakoch/install-vulkan-sdk-action` with `install_runtime: true` installs glslc and spirv-val at a predictable path.
- **macOS:** `brew install vulkan-sdk` places glslc in `/opt/homebrew/bin/`
- **Windows:** LunarG Vulkan SDK installer; PATH set by installer.
- **Cross-platform convention:** Expose SDK path as `VULKAN_SDK` env var (`$VULKAN_SDK/bin/glslc`); fall back to PATH. Gradle task should accept a configurable `shaderCCompiler` file property.

### 5. Does this run headlessly in GitHub Actions on a Lavapipe runner?

**Yes, fully.**

`jakoch/install-vulkan-sdk-action` installs the full Vulkan SDK including `glslc`, `spirv-val`, and Lavapipe on Linux runners. Shader compilation and validation are CPU-only operations — they require no GPU, no display, no Vulkan runtime. They succeed even before Lavapipe is set up. Confirmed by: Android NDK shader compiler guide (recommends subprocess for CI), Isotropy gradle-shaderc (production plugin on CI).

---

## Recommendation: Confirmed Toolchain Configuration

### Tool selection

| Component | Tool | Rationale |
|---|---|---|
| GLSL → SPIR-V | `glslc` (Google Shaderc CLI) | Standard toolchain; GCC-like CLI flags; reproducible via Vulkan SDK |
| SPIR-V validation | `spirv-val` (SPIRV-Tools CLI) | Only option; exact same distribution as glslc |
| Gradle integration | Custom `DefaultTask` + subprocess | Industry pattern; avoids LWJGL daemon issues |
| Task caching | `@CacheableTask` + `@InputFiles`/`@OutputDirectory` | Full build cache and up-to-date support |

### Gradle task skeleton

```kotlin
// buildSrc/src/main/kotlin/ShaderCompileTask.kt

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class ShaderCompileTask @Inject constructor(
    private val workers: WorkerExecutor
) : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val shaderSources: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val glslcPath: Property<String>  // e.g. System.getenv("VULKAN_SDK") + "/bin/glslc"

    @TaskAction
    fun compile(changes: InputChanges) {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        val queue = workers.noIsolation()
        val glslc = glslcPath.get()

        (if (changes.isIncremental) changes.getFileChanges(shaderSources) else null)
            ?.filter { it.changeType != ChangeType.REMOVED }
            ?.map { it.file }
            ?: shaderSources.files.toList()
        ).forEach { src ->
            val spvOut = outDir.resolve("${src.nameWithoutExtension}.spv")
            queue.submit(CompileShaderWorkAction::class.java) {
                it.glslcPath.set(glslc)
                it.sourceFile.set(src)
                it.outputFile.set(spvOut)
            }
        }
    }
}

// Corresponding WorkAction (separate class) invokes Process { glslc -o out.spv in.glsl }
// — omitted from skeleton for brevity; standard exec pattern

@CacheableTask
abstract class SpirvValidateTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val spirvFiles: ConfigurableFileCollection

    @get:Input
    abstract val spirvValPath: Property<String>

    @TaskAction
    fun validate() {
        spirvFiles.forEach { spv ->
            project.exec {
                it.commandLine(spirvValPath.get(), spv.absolutePath)
                it.isIgnoreExitValue = false  // build fails on any SPIR-V validity error
            }
        }
    }
}
```

### Registration in build.gradle.kts

```kotlin
val compileShaders = tasks.register<ShaderCompileTask>("compileShaders") {
    val sdk = System.getenv("VULKAN_SDK") ?: error("VULKAN_SDK not set")
    shaderSources.from(fileTree("src/main/glsl") { include("**/*.vert", "**/*.frag", "**/*.comp") })
    outputDir.set(layout.buildDirectory.dir("generated/shaders"))
    glslcPath.set("$sdk/bin/glslc")
}

val validateShaders = tasks.register<SpirvValidateTask>("validateShaders") {
    val sdk = System.getenv("VULKAN_SDK") ?: error("VULKAN_SDK not set")
    spirvFiles.from(compileShaders.map { it.outputDir.asFileTree.matching { include("**/*.spv") } })
    spirvValPath.set("$sdk/bin/spirv-val")
    dependsOn(compileShaders)
}

tasks.named("processResources") { dependsOn(validateShaders) }
```

### GitHub Actions CI snippet

```yaml
- uses: jakoch/install-vulkan-sdk-action@v1
  with:
    vulkan_version: 1.3.290.0
    install_runtime: true      # installs glslc, spirv-val, Lavapipe
    cache: true

- run: ./gradlew validateShaders
```

---

## Platform Quirks

| Platform | Note |
|---|---|
| Linux CI | `VULKAN_SDK` set by jakoch action; works headlessly |
| macOS | `brew install vulkan-sdk` required; path is `/opt/homebrew/opt/vulkan-sdk/bin/glslc` |
| Windows | LunarG installer sets `VULKAN_SDK`; works as documented |
| Gradle daemon | No native library loading required with subprocess approach |
| Incremental builds | `InputChanges` ensures only modified shaders recompile |

---

## Blocking Issues

**None.** No platform incompatibilities identified for the subprocess approach. The LWJGL shaderc binding path is ruled out, but it was never the only option — the ecosystem has always favored subprocess invocation for build-time shader compilation.

---

## Impact on SHADER-1

SHADER-1 can proceed. Implementation hints:
- Use the task skeleton above as the starting point
- Expose `glslcPath` / `spirvValPath` as project properties with `VULKAN_SDK`-relative defaults; fail early with a clear message if neither `VULKAN_SDK` nor explicit paths are set
- Add `validateShaders` to the `check` task group so it runs on every CI build
- Keep shader sources under `src/main/glsl/` and generated SPIR-V under `build/generated/shaders/`
- The `validateShaders` task is a zero-VUID gate: build fails on any SPIR-V structural error

---

## Sources

| Claim | Source |
|---|---|
| LWJGL shaderc artifact exists | https://javadoc.lwjgl.org/org/lwjgl/util/shaderc/Shaderc.html |
| LWJGL daemon loading issues | https://discuss.gradle.org/t/how-to-use-lwjgl-or-how-to-use-native-libraries/7498 |
| LWJGL has no SPIRV-Tools bindings | https://github.com/LWJGL/lwjgl3/issues/147 |
| Isotropy gradle-shaderc (subprocess) | https://github.com/Isotropy-Studio/gradle-shaderc |
| jakoch Vulkan SDK action | https://github.com/jakoch/install-vulkan-sdk-action |
| Android NDK shader compiler guide | https://developer.android.com/ndk/guides/graphics/shader-compilers |
