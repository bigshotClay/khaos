package khaos.spike

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class SpikeDecisionDocument(private val path: Path) {

    val exists: Boolean get() = path.exists()

    val text: String by lazy {
        check(exists) { "Decision document not found at $path" }
        path.readText()
    }

    fun committedToGit(): Boolean {
        val result = ProcessBuilder("git", "log", "--oneline", "--", path.toString())
            .directory(path.parent.toFile())
            .redirectErrorStream(true)
            .start()
        val output = result.inputStream.bufferedReader().readText()
        result.waitFor()
        return output.isNotBlank()
    }

    fun hasSection(header: Regex): Boolean = text.contains(header)

    fun hasSection(header: String): Boolean = text.contains(header, ignoreCase = true)

    fun hasCodeBlock(content: String): Boolean {
        val codeBlockPattern = Regex("```[\\s\\S]*?```", RegexOption.DOT_MATCHES_ALL)
        return codeBlockPattern.findAll(text).any { it.value.contains(content) }
    }
}
