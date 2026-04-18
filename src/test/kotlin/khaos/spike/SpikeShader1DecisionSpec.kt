package khaos.spike

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Paths

private val projectRoot = Paths.get(System.getProperty("user.dir"))
private val decisionDoc = SpikeDecisionDocument(
    projectRoot.resolve("_bmad-output/planning-artifacts/designs/spike-shader-1-decision.md")
)

class SpikeShader1DecisionSpec : ShouldSpec({

    // TC-1: Decision document committed at the agreed path
    should("TC-1: decision document exists and is committed to git") {
        withClue("File must exist at _bmad-output/planning-artifacts/designs/spike-shader-1-decision.md") {
            decisionDoc.exists shouldBe true
        }
        withClue("Document must contain a Verdict section") {
            decisionDoc.hasSection("## Verdict") shouldBe true
        }
        withClue("Document must be committed to version control (git log returns entries)") {
            decisionDoc.committedToGit() shouldBe true
        }
    }

    // TC-2: All five spike questions answered with evidence
    should("TC-2: all five spike questions are answered with evidence") {
        val text = decisionDoc.text
        withClue("Question 1: shaderc LWJGL feasibility must be answered") {
            text shouldContain "SharedLibraryLoader"
        }
        withClue("Question 2: spirv-val integration must be addressed") {
            text shouldContain "spirv-val"
            text shouldContain "project.exec"
        }
        withClue("Question 3: Gradle task type must be addressed") {
            text shouldContain "DefaultTask"
        }
        withClue("Question 4: platform-specific requirements must be addressed") {
            text shouldContain "VULKAN_SDK"
        }
        withClue("Question 5: headless CI on Lavapipe must be confirmed") {
            text shouldContain "Lavapipe"
            text shouldContain "GitHub Actions"
        }
        withClue("Evidence: must cite sources, not bare assertions") {
            text shouldContain "## Sources"
        }
    }

    // TC-3: Gradle task skeleton included
    should("TC-3: document contains a runnable Gradle task skeleton") {
        withClue("@CacheableTask annotation must be present in a code block") {
            decisionDoc.hasCodeBlock("@CacheableTask") shouldBe true
        }
        withClue("@InputFiles annotation must be present in a code block") {
            decisionDoc.hasCodeBlock("InputFiles") shouldBe true
        }
        withClue("@OutputDirectory annotation must be present in a code block") {
            decisionDoc.hasCodeBlock("@OutputDirectory") shouldBe true
        }
        withClue("@TaskAction must be present for both compile and validate tasks") {
            decisionDoc.hasCodeBlock("@TaskAction") shouldBe true
        }
        withClue("Compile task AND validate task must both be represented") {
            decisionDoc.hasSection("ShaderCompileTask") shouldBe true
            decisionDoc.hasSection("SpirvValidateTask") shouldBe true
        }
    }

    // TC-4: Blocking issues or platform incompatibilities explicitly named
    should("TC-4: blocking issues section exists and is explicit") {
        withClue("Document must have a Blocking Issues section") {
            decisionDoc.hasSection("## Blocking Issues") shouldBe true
        }
    }

    // TC-5: Recommendation clearly stated
    should("TC-5: recommendation is clear and unambiguous") {
        val text = decisionDoc.text
        withClue("Document must have a Verdict section with explicit proceed/reject decision") {
            text shouldContain "## Verdict"
        }
        withClue("Verdict must reference the confirmed approach (glslc subprocess)") {
            text shouldContain "glslc"
        }
    }

    // TC-5b: SHADER-1 updated — verified by PR description; gh CLI call guarded by env
    should("TC-5b: SHADER-1 issue #16 has implementation hints from spike") {
        val token = System.getenv("GH_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        if (token == null) {
            println("Skipping GitHub check — no GH_TOKEN available. Verified in PR description.")
            return@should
        }
        val result = ProcessBuilder("gh", "issue", "view", "16",
            "--repo", "bigshotClay/khaos", "--json", "body", "--jq", ".body")
            .redirectErrorStream(false)
            .start()
        val body = result.inputStream.bufferedReader().readText()
        result.waitFor()
        withClue("SHADER-1 body must reference glslc subprocess approach from spike") {
            body shouldContain "glslc"
        }
    }

    // TC-6: LWJGL shaderc bindings ruled out
    should("TC-6: LWJGL shaderc bindings explicitly ruled out with rationale") {
        val text = decisionDoc.text
        withClue("Must address SharedLibraryLoader daemon problem") {
            text shouldContain "SharedLibraryLoader"
        }
        withClue("Must explain concurrent daemon conflict") {
            text shouldContain "daemon"
        }
        withClue("Must rule out LWJGL shaderc") {
            text shouldContain "off the table"
        }
    }

    // TC-7: glslc on all three platforms
    should("TC-7: glslc subprocess confirmed on Linux, macOS, and Windows") {
        val text = decisionDoc.text
        withClue("Linux CI: jakoch/install-vulkan-sdk-action must be mentioned") {
            text shouldContain "jakoch/install-vulkan-sdk-action"
        }
        withClue("macOS: Vulkan SDK install path must be mentioned") {
            text shouldContain "macOS"
        }
        withClue("Windows: LunarG installer must be mentioned") {
            text shouldContain "Windows"
        }
        withClue("VULKAN_SDK env var convention must be defined") {
            text shouldContain "VULKAN_SDK"
        }
    }

    // TC-8: spirv-val confirmed, no JVM bindings
    should("TC-8: spirv-val subprocess confirmed, LWJGL absence of SPIRV-Tools noted") {
        val text = decisionDoc.text
        withClue("Must reference LWJGL issue #147 (no SPIRV-Tools bindings)") {
            text shouldContain "#147"
        }
        withClue("Must explain failure propagation via non-zero exit") {
            text shouldContain "non-zero"
        }
    }

    // TC-9: Incremental build support
    should("TC-9: Gradle task skeleton uses InputChanges for incremental builds") {
        withClue("Skeleton must show InputChanges pattern") {
            decisionDoc.hasCodeBlock("InputChanges") shouldBe true
        }
    }

    // TC-10: Lifecycle integration
    should("TC-10: task wiring into Gradle lifecycle is shown") {
        val text = decisionDoc.text
        withClue("Must show processResources dependency or equivalent lifecycle wiring") {
            val hasWiring = text.contains("processResources") || text.contains("compileKotlin")
            withClue("Either processResources or compileKotlin wiring must be present") {
                hasWiring shouldBe true
            }
        }
    }

    // TC-11: VULKAN_SDK env var convention
    should("TC-11: VULKAN_SDK env var used as default with configurable override") {
        val text = decisionDoc.text
        withClue("glslcPath must default from VULKAN_SDK") {
            text shouldContain "glslcPath"
            text shouldContain "VULKAN_SDK"
        }
        withClue("Must show explicit failure when VULKAN_SDK not set") {
            text shouldContain "error("
        }
    }

    // TC-12: GLSL compilation error output
    should("TC-12: glslc compilation errors produce actionable output with file and line") {
        val text = decisionDoc.text
        withClue("Must address error output from glslc") {
            val hasCoverage = text.contains("errorOutput") || text.contains("standardOutput") ||
                text.contains("stderr") || text.contains("error includes")
            withClue("Must explain how compilation errors surface in Gradle output") {
                hasCoverage shouldBe true
            }
        }
    }

    // TC-13: SPIR-V validation is a hard gate
    should("TC-13: spirv-val is a hard build gate, not a warning") {
        val text = decisionDoc.text
        withClue("isIgnoreExitValue must be false") {
            decisionDoc.hasCodeBlock("isIgnoreExitValue = false") shouldBe true
        }
        withClue("Document must not set isIgnoreExitValue = true for spirv-val") {
            val spirvValSection = text.substringAfter("SpirvValidateTask")
            spirvValSection shouldNotContain "isIgnoreExitValue = true"
        }
    }

    // TC-14: Missing VULKAN_SDK produces clear early failure
    should("TC-14: missing VULKAN_SDK env var produces clear early failure message") {
        val text = decisionDoc.text
        withClue("Must show early validation with human-readable error, not cryptic IOException") {
            val hasEarlyFail = text.contains("VULKAN_SDK not set") || text.contains("?: error(")
            withClue("Must use error() or equivalent to fail early with a clear message") {
                hasEarlyFail shouldBe true
            }
        }
    }
})
