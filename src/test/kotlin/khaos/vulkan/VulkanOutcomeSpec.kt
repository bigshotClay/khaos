@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package khaos.vulkan

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf

class VulkanOutcomeSpec : FreeSpec({

    // Tests AC1
    "a when expression covering all four VulkanOutcome variants compiles without an else branch" {
        fun dispatch(outcome: VulkanOutcome<String>): String = when (outcome) {
            is VulkanOutcome.Success -> "success"
            is VulkanOutcome.Degraded -> "degraded"
            is VulkanOutcome.RecoverableFailure -> "recoverable"
            is VulkanOutcome.UnrecoverableFailure -> "unrecoverable"
        }
        dispatch(VulkanOutcome.Success("r")) shouldBe "success"
        dispatch(VulkanOutcome.Degraded(SwapchainStatus.SUBOPTIMAL)) shouldBe "degraded"
        dispatch(VulkanOutcome.RecoverableFailure(VulkanError.OUT_OF_POOL_MEMORY)) shouldBe "recoverable"
        dispatch(VulkanOutcome.UnrecoverableFailure(VulkanError.DEVICE_LOST)) shouldBe "unrecoverable"
    }

    "a when expression on VulkanOutcome omitting UnrecoverableFailure without an else branch fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("NonExhaustive.kt", """
                import khaos.vulkan.VulkanOutcome
                fun dispatch(o: VulkanOutcome<String>): String = when (o) {
                    is VulkanOutcome.Success -> "s"
                    is VulkanOutcome.Degraded -> "d"
                    is VulkanOutcome.RecoverableFailure -> "r"
                }
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC2
    "when a when expression omits a VulkanOutcome variant without else, compiler messages contain exhaustiveness wording" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("MissingVariant.kt", """
                import khaos.vulkan.VulkanOutcome
                fun dispatch(o: VulkanOutcome<String>): String = when (o) {
                    is VulkanOutcome.Success -> "s"
                    is VulkanOutcome.Degraded -> "d"
                    is VulkanOutcome.RecoverableFailure -> "r"
                }
            """))
            inheritClassPath = true
        }
        val result = compilation.compile()
        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages.lowercase() shouldContain "exhaustive"
    }

    "a fully exhaustive when expression over all variants produces no exhaustiveness-related error in diagnostics" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Exhaustive.kt", """
                import khaos.vulkan.VulkanOutcome
                fun dispatch(o: VulkanOutcome<String>): String = when (o) {
                    is VulkanOutcome.Success -> "s"
                    is VulkanOutcome.Degraded -> "d"
                    is VulkanOutcome.RecoverableFailure -> "r"
                    is VulkanOutcome.UnrecoverableFailure -> "u"
                }
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldNotBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC3
    "after an is VulkanOutcome.Success check, .resource is statically typed as T and not Any?" {
        val outcome: VulkanOutcome<String> = VulkanOutcome.Success("vk-texture-handle")
        if (outcome is VulkanOutcome.Success) {
            val resource: String = outcome.resource
            resource shouldBe "vk-texture-handle"
        } else {
            fail("Expected Success variant")
        }
    }

    "smart-cast on Success<Int> yields an Int-typed resource, not Any?" {
        val outcome: VulkanOutcome<Int> = VulkanOutcome.Success(42)
        if (outcome is VulkanOutcome.Success) {
            val resource: Int = outcome.resource
            resource shouldBe 42
        } else {
            fail("Expected Success variant")
        }
    }

    "star-projecting VulkanOutcome.Success without a type argument yields resource as Any? causing a compile error on typed assignment" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("StarProjection.kt", """
                import khaos.vulkan.VulkanOutcome
                fun bad(o: VulkanOutcome<String>): String {
                    val success = o as VulkanOutcome.Success<*>
                    return success.resource
                }
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC4
    "Degraded.indicator returns the SwapchainStatus value passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.Degraded(SwapchainStatus.SUBOPTIMAL)
        if (outcome is VulkanOutcome.Degraded) {
            outcome.indicator shouldBe SwapchainStatus.SUBOPTIMAL
            outcome.indicator.shouldBeInstanceOf<SwapchainStatus>()
        } else {
            fail("Expected Degraded variant")
        }
    }

    "accessing .indicator on a bare VulkanOutcome reference fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("IndicatorBase.kt", """
                import khaos.vulkan.VulkanOutcome
                fun bad(o: VulkanOutcome<String>) = o.indicator
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC5
    "RecoverableFailure.cause returns the VulkanError passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.RecoverableFailure(VulkanError.OUT_OF_POOL_MEMORY)
        if (outcome is VulkanOutcome.RecoverableFailure) {
            outcome.cause shouldBe VulkanError.OUT_OF_POOL_MEMORY
            outcome.cause.shouldBeInstanceOf<VulkanError>()
        } else {
            fail("Expected RecoverableFailure variant")
        }
    }

    "RecoverableFailure.cause is accessible at the call site to drive a retry decision" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.RecoverableFailure(VulkanError.TIMEOUT)
        if (outcome is VulkanOutcome.RecoverableFailure) {
            val shouldRetry = outcome.cause == VulkanError.TIMEOUT
            shouldRetry shouldBe true
        } else {
            fail("Expected RecoverableFailure variant")
        }
    }

    "accessing .cause on a bare VulkanOutcome reference fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("CauseBase.kt", """
                import khaos.vulkan.VulkanOutcome
                fun bad(o: VulkanOutcome<String>) = o.cause
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC6
    "UnrecoverableFailure.cause returns the VulkanError passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.UnrecoverableFailure(VulkanError.DEVICE_LOST)
        if (outcome is VulkanOutcome.UnrecoverableFailure) {
            outcome.cause shouldBe VulkanError.DEVICE_LOST
            outcome.cause.shouldBeInstanceOf<VulkanError>()
        } else {
            fail("Expected UnrecoverableFailure variant")
        }
    }

    "UnrecoverableFailure is not an instance of RecoverableFailure — it carries no retry path" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.UnrecoverableFailure(VulkanError.SURFACE_LOST)
        outcome.shouldNotBeInstanceOf<VulkanOutcome.RecoverableFailure>()
    }
})
