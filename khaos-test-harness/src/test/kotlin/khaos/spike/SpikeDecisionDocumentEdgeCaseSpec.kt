package khaos.spike

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class SpikeDecisionDocumentEdgeCaseSpec : ShouldSpec({

    // TC-29: committedToGit() must handle edge-case paths without throwing
    should("TC-29: committedToGit() returns false for bare filename with null parent") {
        val doc = SpikeDecisionDocument(Path.of("bare-file.md"))
        withClue("SpikeDecisionDocument with a bare filename (path.parent == null) must return false, not throw NPE") {
            doc.committedToGit() shouldBe false
        }
    }

    should("TC-29: committedToGit() returns false for non-existent parent directory") {
        val doc = SpikeDecisionDocument(Path.of("/nonexistent/dir/file.md"))
        withClue("SpikeDecisionDocument with a non-existent parent directory must return false, not throw IOException") {
            doc.committedToGit() shouldBe false
        }
    }
})
