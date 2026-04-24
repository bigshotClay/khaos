@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package khaos.vulkan

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldNotBeInstanceOf

class VulkanOutcomeSpec : FreeSpec({

    // Tests AC1
    "a when expression covering all four VulkanOutcome variants resolves without an else branch" {
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
    "Success<String>.resource returns the exact produced value passed at construction" {
        val outcome: VulkanOutcome<String> = VulkanOutcome.Success("vk-texture-handle")
        val success = outcome as VulkanOutcome.Success
        success.resource shouldBe "vk-texture-handle"
    }

    "Success<Int>.resource returns the produced integer value with correct runtime type" {
        val outcome: VulkanOutcome<Int> = VulkanOutcome.Success(42)
        val success = outcome as VulkanOutcome.Success
        success.resource shouldBe 42
        success.resource.shouldBeInstanceOf<Int>()
    }

    "accessing .resource on a bare VulkanOutcome reference fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("ResourceBase.kt", """
                import khaos.vulkan.VulkanOutcome
                fun bad(o: VulkanOutcome<String>) = o.resource
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC3
    "Degraded.indicator returns the SwapchainStatus value passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.Degraded(SwapchainStatus.SUBOPTIMAL)
        val degraded = outcome as VulkanOutcome.Degraded
        degraded.indicator shouldBe SwapchainStatus.SUBOPTIMAL
        degraded.indicator.shouldBeInstanceOf<SwapchainStatus>()
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

    // Tests AC4
    "RecoverableFailure.cause returns the VulkanError passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.RecoverableFailure(VulkanError.OUT_OF_POOL_MEMORY)
        val failure = outcome as VulkanOutcome.RecoverableFailure
        failure.cause shouldBe VulkanError.OUT_OF_POOL_MEMORY
        failure.cause.shouldBeInstanceOf<VulkanError>()
    }

    "RecoverableFailure.cause is accessible at the call site to drive a retry decision" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.RecoverableFailure(VulkanError.TIMEOUT)
        val failure = outcome as VulkanOutcome.RecoverableFailure
        val shouldRetry = failure.cause == VulkanError.TIMEOUT
        shouldRetry shouldBe true
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

    // Tests AC5
    "UnrecoverableFailure.cause returns the VulkanError passed at construction" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.UnrecoverableFailure(VulkanError.DEVICE_LOST)
        val failure = outcome as VulkanOutcome.UnrecoverableFailure
        failure.cause shouldBe VulkanError.DEVICE_LOST
        failure.cause.shouldBeInstanceOf<VulkanError>()
    }

    "UnrecoverableFailure is not an instance of RecoverableFailure — carries no retry path" {
        val outcome: VulkanOutcome<Nothing> = VulkanOutcome.UnrecoverableFailure(VulkanError.SURFACE_LOST)
        outcome.shouldNotBeInstanceOf<VulkanOutcome.RecoverableFailure>()
    }
})
