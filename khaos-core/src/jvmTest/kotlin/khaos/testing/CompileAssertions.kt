@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package khaos.testing

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain

/**
 * Asserts that [this] compilation fails, and that it fails for a *language* reason.
 *
 * The negative type-safety tests are the only proof that the typed handle layer does what it
 * claims — that passing a `BufferHandle` where an `ImageHandle` is expected cannot compile. A bare
 * `exitCode shouldBe COMPILATION_ERROR` cannot distinguish that from the compilation failing
 * because the embedded compiler could not read our classes at all, which makes the whole suite
 * pass while proving nothing. That is not hypothetical: kctfork pins an older
 * kotlin-compiler-embeddable, and when the project's Kotlin version outran it, every one of these
 * tests kept passing on metadata errors alone.
 *
 * So we reject the known vacuity mode explicitly rather than trusting the exit code.
 */
fun KotlinCompilation.shouldFailToCompile(): JvmCompilationResult {
    val result = compile()
    withClue(
        "Compilation failed on a Kotlin metadata version mismatch, not on the type error under " +
            "test — the embedded compiler cannot read classes built by this project's Kotlin " +
            "version. Align kotlin-compiler-embeddable with the 'kotlin' catalog version. " +
            "Messages:\n${result.messages}",
    ) {
        result.messages shouldNotContain "incompatible version"
    }
    result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    return result
}
