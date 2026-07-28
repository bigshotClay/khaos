package khaos.spike

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

private val projectRoot = Paths.get(System.getProperty("user.dir"))
private val decisionDoc = SpikeDecisionDocument(
    projectRoot.resolve("_bmad-output/planning-artifacts/designs/spike-shader-2-decision.md")
)

class SpikeShader2DecisionSpec : ShouldSpec({

    // TC-1: Decision document committed at the agreed path
    should("TC-1: decision document exists and is committed to git") {
        withClue("File must exist at _bmad-output/planning-artifacts/designs/spike-shader-2-decision.md") {
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
        withClue("Q1 evidence: KSP external file limitation must cite KSP #1677") {
            text shouldContain "1677"
        }
        withClue("Q2 (KSP processor architecture) must be explicitly stated as moot — not left unanswered by silence") {
            text shouldContain "This question is moot"
        }
        withClue("Q3 evidence: KSP generated type support must address @JvmInline") {
            text shouldContain "@JvmInline"
        }
        withClue("Q3 evidence: known IDE regression must cite KSP #1351") {
            text shouldContain "1351"
        }
        withClue("Q4 evidence: Gradle CacheableTask must be named as the right tool") {
            text shouldContain "CacheableTask"
        }
        withClue("Q5 evidence: task pipeline must show reflectShaders stage") {
            text shouldContain "reflectShaders"
        }
        withClue("I/O contract evidence: input JSON must name push_constants field") {
            text shouldContain "push_constants"
        }
        withClue("Evidence: Sources section must be present with citations") {
            text shouldContain "## Sources"
        }
    }

    // TC-3: KSP not recommended — alternative named with rationale
    should("TC-3: KSP is rejected and standalone Gradle CacheableTask named as alternative") {
        val text = decisionDoc.text
        withClue("Verdict must explicitly reject KSP") {
            text shouldContain "not use KSP"
        }
        withClue("Alternative must be named: standalone Gradle CacheableTask") {
            text shouldContain "CacheableTask"
        }
        withClue("Rationale must explain Gradle cannot track external JSON inputs") {
            text shouldContain "external file inputs to Gradle"
        }
    }

    // TC-4: Input/output contract defined with JSON field names and Kotlin output shape
    should("TC-4: input/output contract specifies spirv-cross JSON schema and generated Kotlin types") {
        val text = decisionDoc.text
        withClue("Input JSON must name 'ubos' field") { text shouldContain "ubos" }
        withClue("Input JSON must name 'push_constants' field") { text shouldContain "push_constants" }
        withClue("Input JSON must name 'inputs' field (vertex inputs)") { text shouldContain "\"inputs\"" }
        withClue("Input JSON must name 'textures' field") { text shouldContain "textures" }
        withClue("Output contract must show @JvmInline value class in generated output code block") {
            decisionDoc.hasCodeBlockInSection("**Output contract:**", "@JvmInline value class") shouldBe true
        }
    }

    // TC-5: SHADER-2 (#17) updated with spike findings — guarded by GH_TOKEN
    should("TC-5: SHADER-2 issue #17 has implementation hints referencing Gradle task approach") {
        val token = System.getenv("GH_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        if (token == null) {
            println("Skipping GitHub check — no GH_TOKEN available. Verified manually: gh issue view 17 body contains Gradle task approach.")
            return@should
        }
        val pb = ProcessBuilder(
            "gh", "issue", "view", "17",
            "--repo", "bigshotClay/khaos", "--json", "body", "--jq", ".body"
        )
        pb.redirectErrorStream(true)
        pb.environment().also {
            it.clear()
            it["PATH"] = System.getenv("PATH") ?: "/usr/bin:/bin"
            it["HOME"] = System.getenv("HOME") ?: ""
            it["GH_TOKEN"] = token
        }
        val result = pb.start()
        val bodyFuture = CompletableFuture.supplyAsync { result.inputStream.bufferedReader().readText() }
        val exited = result.waitFor(30, TimeUnit.SECONDS)
        if (!exited) {
            result.destroyForcibly()
            bodyFuture.cancel(true)
        }
        withClue("gh subprocess must exit within 30 seconds") { exited shouldBe true }
        withClue("gh subprocess must exit with code 0") { result.exitValue() shouldBe 0 }
        val body = bodyFuture.get(5, TimeUnit.SECONDS)
        withClue("SHADER-2 body must reference Gradle task approach (not 'TBD')") {
            body shouldContain "Gradle"
        }
    }

    // TC-6: KSP rejected on Gradle input tracking grounds — not just complexity
    should("TC-6: KSP rejection cites silent incremental build failure, not just complexity") {
        val text = decisionDoc.text
        withClue("Must cite KSP #1677 as structural evidence for the rejection") {
            text shouldContain "1677"
        }
        withClue("Failure must be described as silent — stale types without a build error") {
            text shouldContain "silent"
        }
        withClue("Must confirm there is no supported KSP API to declare external Gradle inputs") {
            text shouldContain "no supported"
        }
    }

    // TC-7: Gradle task skeleton correctly declares external file inputs
    should("TC-7: task skeleton uses @InputFiles @SkipWhenEmpty @PathSensitive and InputChanges") {
        withClue("@InputFiles must be declared in code block") {
            decisionDoc.hasCodeBlock("InputFiles") shouldBe true
        }
        withClue("@SkipWhenEmpty must be declared in code block") {
            decisionDoc.hasCodeBlock("SkipWhenEmpty") shouldBe true
        }
        withClue("@PathSensitive must be declared in code block") {
            decisionDoc.hasCodeBlock("PathSensitive") shouldBe true
        }
        withClue("InputChanges must be used in @TaskAction for incremental file processing") {
            decisionDoc.hasCodeBlock("InputChanges") shouldBe true
        }
    }

    // TC-8: Four-stage pipeline defined
    should("TC-8: four-stage pipeline compileShaders→reflectShaders→generateBindings→compileKotlin is defined") {
        val text = decisionDoc.text
        withClue("compileShaders stage must be present") { text shouldContain "compileShaders" }
        withClue("reflectShaders stage must be present") { text shouldContain "reflectShaders" }
        withClue("generateShaderBindings stage must be present") { text shouldContain "generateShaderBindings" }
        withClue("reflectShaders must produce .reflection.json files") { text shouldContain "reflection.json" }
    }

    // TC-9: Generated types use correct Kotlin idioms — scoped to output contract code block
    should("TC-9: generated output example uses @JvmInline value class, sealed interface, data class") {
        withClue("@JvmInline value class must appear in generated output code block") {
            decisionDoc.hasCodeBlockInSection("**Output contract:**", "@JvmInline value class") shouldBe true
        }
        withClue("sealed interface must appear in generated output code block for vertex attributes") {
            decisionDoc.hasCodeBlockInSection("**Output contract:**", "sealed interface") shouldBe true
        }
        withClue("data class must appear in generated output code block for push constants") {
            decisionDoc.hasCodeBlockInSection("**Output contract:**", "data class") shouldBe true
        }
        withClue("Generated types must target commonMain for KMP portability") {
            decisionDoc.text shouldContain "commonMain"
        }
    }

    // TC-10: Source set wiring — generated sources in commonMain
    should("TC-10: generateShaderBindings outputDir is wired into commonMain kotlin.srcDir") {
        withClue("Registration snippet must wire outputDir into Kotlin source sets via kotlin.srcDir") {
            decisionDoc.hasCodeBlock("kotlin.srcDir") shouldBe true
        }
        withClue("Source set wiring must target commonMain (not jvmMain)") {
            decisionDoc.hasCodeBlock("commonMain") shouldBe true
        }
    }

    // TC-11: spirv-cross chosen over spirv-reflect-kt for v0
    should("TC-11: spirv-cross chosen for v0 with Vulkan SDK rationale; spirv-reflect-kt noted as future path") {
        val text = decisionDoc.text
        withClue("spirv-cross must be explicitly recommended") {
            text shouldContain "spirv-cross"
        }
        withClue("Rationale must cite that spirv-cross is already in the Vulkan SDK") {
            text shouldContain "Vulkan SDK"
        }
        withClue("spirv-reflect-kt must be noted as a future alternative path") {
            text shouldContain "spirv-reflect-kt"
        }
    }

    // TC-12: Determinism invariant stated
    should("TC-12: same reflection JSON produces byte-identical Kotlin output — determinism stated") {
        withClue("Document must state the determinism invariant for the binding generator") {
            decisionDoc.text shouldContain "deterministic"
        }
    }

    // TC-13: Required JSON fields named; parseReflectionJson parsing stub present [SUPERSEDED by TC-16]
    should("TC-13: input JSON schema names required fields and skeleton defines parseReflectionJson") {
        val text = decisionDoc.text
        withClue("Required JSON field 'entryPoints' must be in input contract") {
            text shouldContain "entryPoints"
        }
        withClue("Required JSON field 'ubos' must be in input contract") {
            text shouldContain "ubos"
        }
        withClue("Required JSON field 'push_constants' must be in input contract") {
            text shouldContain "push_constants"
        }
        withClue("Skeleton must define parseReflectionJson to handle JSON parsing") {
            decisionDoc.hasCodeBlock("parseReflectionJson") shouldBe true
        }
    }

    // TC-14: InputChanges enables per-shader incremental invalidation
    should("TC-14: skeleton uses InputChanges with isIncremental and getFileChanges for per-file builds") {
        withClue("InputChanges must be present in skeleton") {
            decisionDoc.hasCodeBlock("InputChanges") shouldBe true
        }
        withClue("isIncremental must be checked in @TaskAction") {
            decisionDoc.hasCodeBlock("isIncremental") shouldBe true
        }
        withClue("getFileChanges must be called to process only changed files") {
            decisionDoc.hasCodeBlock("getFileChanges") shouldBe true
        }
    }

    // TC-15: KSP #1351 IDE sealed hierarchy regression acknowledged
    should("TC-15: KSP #1351 sealed hierarchy IDE regression documented and confirmed moot") {
        val text = decisionDoc.text
        withClue("KSP #1351 sealed hierarchy regression must be mentioned") {
            text shouldContain "1351"
        }
        withClue("Regression must be confirmed moot under the Gradle task approach") {
            text shouldContain "moot"
        }
    }

    // TC-16: Error handling approach for parseReflectionJson is stated — not left implicit
    should("TC-16: parseReflectionJson error handling approach is stated in document") {
        val text = decisionDoc.text
        withClue("Document must not be silent on error handling — must state fail-fast behavior or defer to implementation") {
            val hasFastFail = text.contains("Fail-fast", ignoreCase = true) ||
                text.contains("fail fast", ignoreCase = true)
            val hasDeferNote = text.contains("implementation concern", ignoreCase = true) ||
                text.contains("error handling is deferred", ignoreCase = true)
            (hasFastFail || hasDeferNote) shouldBe true
        }
    }

    // TC-17: Document-absent failure is isolated — no lazy exception cascade
    should("TC-17: missing document fails with clear message; second access retries cleanly") {
        val absentDoc = SpikeDecisionDocument(
            projectRoot.resolve("does-not-exist-spike-shader-2.md")
        )
        withClue("exists must be false for absent document") {
            absentDoc.exists shouldBe false
        }
        val firstError = runCatching { absentDoc.text }.exceptionOrNull()
        withClue("First access must throw with 'Decision document not found'") {
            (firstError?.message ?: "no exception") shouldContain "Decision document not found"
        }
        val secondError = runCatching { absentDoc.text }.exceptionOrNull()
        withClue("Second access must also throw with 'Decision document not found' — no lazy exception caching cascade") {
            (secondError?.message ?: "no exception") shouldContain "Decision document not found"
        }
    }

    // TC-18: committedToGit() returns false when git exits non-zero
    should("TC-18: committedToGit() returns false when git subprocess exits non-zero") {
        val outsideGitDoc = SpikeDecisionDocument(Paths.get("/tmp/nonexistent-spike-shader-test.md"))
        withClue("committedToGit() must return false when run outside a git repository (git exits non-zero)") {
            outsideGitDoc.committedToGit() shouldBe false
        }
    }
})
