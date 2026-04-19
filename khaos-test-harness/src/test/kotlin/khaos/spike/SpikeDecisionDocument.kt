package khaos.spike

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText

class SpikeDecisionDocument(private val path: Path) {

    val exists: Boolean get() = path.exists()

    val text: String by lazy(LazyThreadSafetyMode.NONE) {
        check(exists) { "Decision document not found at $path" }
        path.readText()
    }

    fun committedToGit(): Boolean {
        val result = ProcessBuilder("git", "log", "--oneline", "--", path.toString())
            .directory(path.parent.toFile())
            .redirectErrorStream(true)
            .start()
        val output = result.inputStream.bufferedReader().readText()
        val exited = result.waitFor(30, TimeUnit.SECONDS)
        if (!exited) {
            result.destroyForcibly()
            return false
        }
        if (result.exitValue() != 0) return false
        return output.isNotBlank()
    }

    fun hasSection(header: Regex): Boolean = text.contains(header)

    fun hasSection(header: String): Boolean = text.contains(header, ignoreCase = true)

    fun hasCodeBlock(content: String): Boolean {
        val codeBlockPattern = Regex("```[\\s\\S]*?```")
        return codeBlockPattern.findAll(text).any { it.value.contains(content) }
    }

    fun hasCodeBlockInSection(anchorText: String, content: String): Boolean {
        val anchorIdx = text.indexOf(anchorText)
        if (anchorIdx < 0) return false
        val afterAnchor = text.substring(anchorIdx)
        val codeBlockPattern = Regex("```[\\s\\S]*?```")
        return codeBlockPattern.findAll(afterAnchor).firstOrNull()?.value?.contains(content) == true
    }
}
