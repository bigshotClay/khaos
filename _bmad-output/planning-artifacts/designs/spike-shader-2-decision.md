# SPIKE-SHADER-2: Validate KSP Processor for SPIR-V Reflection → Typed Kotlin Bindings

**Issue:** #2  
**Date:** 2026-04-18  
**Status:** Decision ready  
**Author:** Atlas (agent-architect)

---

## Verdict

**Do not use KSP. Use a standalone Gradle code-generation task.**

The spike hypothesis was "KSP is the right mechanism for consuming SPIR-V reflection output and emitting typed Kotlin bindings." The investigation invalidates that framing: KSP is a Kotlin symbol processor — it is designed to consume Kotlin source symbols, not external files. When the primary input is a JSON file from a SPIR-V reflection tool, KSP has no mechanism to declare that dependency to Gradle's input tracking system, which breaks incremental builds in a way that cannot be fixed without misusing the KSP API. A standalone Gradle `@CacheableTask` with declarative `@InputFiles`/`@OutputDirectory` is the correct tool. It is simpler, more legible to agents, has full incremental build support, and produces the same `@JvmInline value class` and sealed hierarchy output that KSP would.

---

## Questions Answered

> **Q2 — What is the right KSP processor architecture for this use case?** This question is moot: KSP is rejected as the wrong tool for this problem. There is no right KSP processor architecture; the correct answer is not to use KSP at all.

### 1. Can a KSP processor consume external files (SPIR-V JSON from spirv-cross or spirv-reflect) as primary input?

**Technically yes, architecturally wrong.**

A KSP `SymbolProcessor` runs inside the Kotlin compiler. It can call `File(path).readText()` to read arbitrary disk files. So it is physically possible to read a spirv-cross JSON file from inside a KSP processor.

The problem is Gradle's task model. KSP runs as a compiler plugin — from Gradle's perspective, its inputs are the Kotlin source files. If the SPIR-V JSON file changes, Gradle has no way to know that the KSP output is stale. The only record Gradle has is the Kotlin source file set. Changing a `.json` file will not trigger a KSP re-run. Your developers will edit a shader, recompile, and see no change in the generated Kotlin types — a silent correctness failure, not a loud build error.

There is no supported mechanism in KSP to declare external file inputs to Gradle. KSP issue #1677 ("Generate files that are neither source code nor resources") illustrates the broader problem of KSP's lack of first-class support for non-standard I/O. This is structural, not a fixable workaround.

**Conclusion:** Using KSP as the consumer of SPIR-V JSON is architecturally unsound. It inverts the tool's design: KSP processes Kotlin, not JSON.

### 2. Can KSP generate `@JvmInline value class` or sealed hierarchies?

**Yes to both, with a known IDE caveat on sealed types.**

KSP generators write arbitrary Kotlin source text to output files in `build/generated/ksp/`. There is no restriction on what source can be generated — `@JvmInline value class`, `sealed interface`, `sealed class`, data classes, all work.

Known caveat: KSP-generated sealed hierarchies are sometimes not recognized as sealed by the IDE (IntelliJ/Android Studio). The exhaustive `when` expression check fails in the editor even though compilation succeeds. This is a tracked issue (KSP #1351). At build time the types are correct; the IDE experience is degraded.

This caveat is moot for the standalone Gradle task approach, since generated files are added to the source set as regular Kotlin sources. The Kotlin compiler and IDE see them identically to hand-written files.

### 3. Is KSP the right tool, or would a standalone Gradle code-generation task be simpler and more appropriate?

**Standalone Gradle task is the right tool.**

| Dimension | KSP | Gradle `@CacheableTask` |
|---|---|---|
| Primary input | Kotlin source symbols | Any file — `@InputFiles` |
| Incremental build | Automatic for Kotlin inputs | Automatic via `@InputFiles` + `InputChanges` |
| External file dependency | No built-in support; breaks incremental | First-class; Gradle tracks JSON file changes |
| Up-to-date checks | Implicit; tied to Kotlin compilation | Explicit; Gradle manages precisely |
| Output → source set wiring | `build/generated/ksp/` auto-added | Manually add `outputDir` to source set (two lines) |
| Complexity | Compiler plugin lifecycle, `Resolver` API | Regular Kotlin class + Gradle APIs |
| Agent legibility | Requires understanding KSP internals | One task class, readable in isolation |
| IDE sealed hierarchy recognition | Known regression (KSP #1351) | None — normal source file |

The Gradle task is simpler, more correct, and more legible.

### 4. Does the processor need to run in the same Gradle task graph as shaderc compilation, or can it be a separate downstream task?

**Separate downstream task — this is the natural and correct structure.**

The full pipeline is a three-stage task chain:

```
compileShaders     (Exec: glslc)              → build/generated/shaders/*.spv
    ↓
reflectShaders     (Exec: spirv-cross)         → build/generated/shaders/*.reflection.json
    ↓
generateBindings   (DefaultTask: Kotlin codegen) → build/generated/shader-bindings/**/*.kt
    ↓
compileKotlin      (automatic dependency)
```

Each stage declares its predecessor as a task dependency via Gradle's `dependsOn` / input wiring. This gives:
- Clean separation of concerns: compilation, reflection, codegen are independently cacheable
- Each task is fast and incremental — only changed shaders trigger downstream reprocessing
- The binding generator has no knowledge of glslc; it only sees JSON

### 5. What is the input/output contract for the binding generator?

**Input contract:** One `*.reflection.json` file per shader, produced by `spirv-cross --reflect`. JSON structure (authoritative source: [spirv-cross Reflection API guide](https://github.com/KhronosGroup/SPIRV-Cross/wiki/Reflection-API-user-guide)):

```json
{
  "entryPoints": [{ "name": "main", "mode": "vert" }],
  "inputs": [
    { "type": "vec4", "name": "inPosition", "location": 0 }
  ],
  "ubos": [
    {
      "type": "block", "name": "UBO", "set": 0, "binding": 0,
      "block_size": 64,
      "members": [{ "name": "mvp", "type": "mat4", "offset": 0 }]
    }
  ],
  "push_constants": [
    {
      "type": "block", "name": "Push",
      "members": [{ "name": "objectIndex", "type": "int", "offset": 0 }]
    }
  ],
  "textures": [
    { "type": "sampler2D", "name": "albedo", "set": 0, "binding": 1 }
  ]
}
```

**Output contract:** One generated Kotlin file per shader, in package `com.khaos.shader.generated`:

```kotlin
// Generated from vertex.vert.reflection.json — do not edit

package com.khaos.shader.generated

import com.khaos.vulkan.DescriptorBinding
import com.khaos.vulkan.PushConstantLayout
import com.khaos.vulkan.VertexInput

object VertexShader {

    // Descriptor set 0, binding 0 — uniform buffer
    @JvmInline value class UboBinding(val set: Int = 0, val binding: Int = 0) : DescriptorBinding
    val ubo: UboBinding = UboBinding()

    // Descriptor set 0, binding 1 — sampled image
    @JvmInline value class AlbedoBinding(val set: Int = 0, val binding: Int = 1) : DescriptorBinding
    val albedo: AlbedoBinding = AlbedoBinding()

    // Push constant block — total size 4 bytes
    data class PushConstants(val objectIndex: Int)
    val pushLayout: PushConstantLayout = PushConstantLayout(size = 4)

    // Vertex inputs
    sealed interface VertexAttributes {
        @JvmInline value class InPosition(val location: Int = 0) : VertexAttributes
    }
}
```

---

## Recommendation: Confirmed Approach

### Toolchain

| Component | Tool | Rationale |
|---|---|---|
| SPIR-V reflection | `spirv-cross --reflect` (CLI) | Standard Khronos tool; structured JSON output |
| Kotlin binding codegen | Custom Gradle `@CacheableTask` | External file input; full Gradle tracking; simple |
| SPIR-V byte-level reflection (optional) | `spirv-reflect-kt` (JVM library) | Pure JVM alternative if CLI invocation is undesirable |

### Gradle binding generator task skeleton

```kotlin
// buildSrc/src/main/kotlin/ShaderBindingGenTask.kt

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges

@CacheableTask
abstract class ShaderBindingGenTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val reflectionJsonFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate(changes: InputChanges) {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        val toProcess = if (changes.isIncremental)
            changes.getFileChanges(reflectionJsonFiles)
                .filter { it.changeType != ChangeType.REMOVED }
                .map { it.file }
        else
            reflectionJsonFiles.files.toList()

        toProcess.forEach { jsonFile ->
            val reflection = parseReflectionJson(jsonFile)
            val kotlinSource = generateBindings(reflection)
            val outFile = outDir.resolve("${reflection.shaderName.capitalize()}Shader.kt")
            outFile.writeText(kotlinSource)
        }
    }

    private fun parseReflectionJson(file: File): ShaderReflection {
        // Fail-fast: validate required fields (entryPoints, inputs, ubos, push_constants, textures);
        // throw IllegalArgumentException naming the missing field and shader file on malformed JSON.
        /* JSON parsing */
    }
    private fun generateBindings(r: ShaderReflection): String { /* emit Kotlin source */ }
}
```

### Registration in build.gradle.kts

```kotlin
val reflectShaders = tasks.register<ReflectShadersTask>("reflectShaders") {
    val sdk = System.getenv("VULKAN_SDK") ?: error("VULKAN_SDK not set")
    spirvFiles.from(compileShaders.map { it.outputDir.asFileTree })
    outputDir.set(layout.buildDirectory.dir("generated/shaders/reflection"))
    spirvCrossPath.set("$sdk/bin/spirv-cross")
    dependsOn(compileShaders)
}

val generateShaderBindings = tasks.register<ShaderBindingGenTask>("generateShaderBindings") {
    reflectionJsonFiles.from(reflectShaders.map { it.outputDir.asFileTree.matching { include("**/*.json") } })
    outputDir.set(layout.buildDirectory.dir("generated/shader-bindings/kotlin"))
    dependsOn(reflectShaders)
}

kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(generateShaderBindings.map { it.outputDir })
}
```

---

## Open Question: spirv-cross or spirv-reflect?

Two tools provide SPIR-V reflection for this pipeline:

| Tool | Form | Output | Recommendation |
|---|---|---|---|
| `spirv-cross --reflect` | CLI binary (Vulkan SDK) | JSON — descriptor bindings, push constants, vertex inputs | **Use this** — already installed with Vulkan SDK alongside glslc |
| `spirv-reflect-kt` | Pure JVM library (`zmichaels11/spirv-reflect-kt`) | Kotlin API over raw SPIR-V bytecode | Use if you want to eliminate the CLI step entirely |

Recommend: `spirv-cross` for v0 — it's already in the Vulkan SDK, no additional dependency, and its JSON output is the documented Khronos reflection format. `spirv-reflect-kt` is a viable migration path if you want to eliminate subprocess calls at the cost of taking a dependency on a small, less-maintained library.

---

## Impact on SHADER-2

SHADER-2 can proceed. Implementation hints:
- Do NOT use KSP for this. The Gradle task approach is the answer.
- The `ReflectShadersTask` (spirv-cross invocation) follows the same `@CacheableTask` + subprocess pattern as `ShaderCompileTask` in Spike 1 — implement both in the same `buildSrc` module.
- The `ShaderBindingGenTask` is pure Kotlin: JSON parsing → Kotlin source text. Use `kotlinx.serialization` for JSON parsing (already on the KMP classpath).
- Add `generateShaderBindings.map { it.outputDir }` to `commonMain.kotlin.srcDirs` — generated bindings live in common so all targets see them.
- The generated file for each shader is deterministic: same reflection JSON → same Kotlin source → same binary. This is a testable invariant.
- Consider naming convention: `VertexShader.kt`, `FragmentShader.kt` etc. — one file per shader stage, named after the GLSL source file.

---

## Sources

| Claim | Source |
|---|---|
| KSP cannot declare external file inputs to Gradle | https://github.com/google/ksp/issues/1677 |
| KSP sealed hierarchy IDE regression | https://github.com/google/ksp/issues/1351 |
| KSP incremental build docs | https://kotlinlang.org/docs/ksp-incremental.html |
| spirv-cross reflection API | https://github.com/KhronosGroup/SPIRV-Cross/wiki/Reflection-API-user-guide |
| spirv-reflect-kt | https://github.com/zmichaels11/spirv-reflect-kt |
| KSP overview | https://kotlinlang.org/docs/ksp-overview.html |
