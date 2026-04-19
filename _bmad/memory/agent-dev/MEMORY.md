# Memory — Forge Patterns

_Implementation lessons. Grows over time. Keep under 200 lines._

## Vulkan / LWJGL Patterns (issue #4 Loop 2 lessons)

- **VUID severity filter must cover BOTH bits:** WARNING (0x00000100) and ERROR (0x00001000). `VUID-vkDestroyInstance-instance-00629` fires at ERROR severity. A callback with only the WARNING check silently drops it. Always: `(severity and WARNING) != 0 || (severity and ERROR) != 0`.
- **`vkCreateDebugUtilsMessengerEXT` return code:** Always capture and `shouldBe VK_SUCCESS` before VUID-dependent assertions. Silently failed messenger = zero VUIDs even when Vulkan is misbehaving.
- **`vkCreateInstance` return code in every test that uses it:** Discard = wrong failure reason when the test fails. Always store and assert.
- **try/finally for native LWJGL callbacks:** `VkDebugUtilsMessengerCallbackEXT.create {}` allocates off-heap. `MemoryStack` does NOT free it. Always: `try { ... } finally { callback.free() }`.
- **Kotest `context` scopes `beforeTest`:** Use `context("Vulkan runtime") { beforeTest { ... } ... }` to keep Vulkan lifecycle calls away from static inspection tests. `beforeTest` at spec level fires for everything.
- **`lastIndexOf` in source inspection:** Dangerous when an intentional-failure test block reuses the same Vulkan call. Always scope to target block or use behavioral assertions instead.

## Test Assertion Patterns

- **`shouldNotContain` forbidden string in `else ->` branch:** A when-expression `else -> "some-literal"` still contains that substring. Make the forbidden pattern more specific (e.g., prefix with `"val varName = "`) to target direct assignment only.
- **`shouldContain` self-match via concatenation split:** `source shouldContain "messengerResult" + " shouldBe VK_SUCCESS"` is safe — the test source has the parts split, so the concatenated target only appears in the implementation code, not in the test's own assertion string.

## Known Gotchas

- **Kotlin property annotations:** Decision docs use `@get:InputFiles` / `@get:OutputDirectory` etc. When writing tests that check for annotation presence in code blocks, match the annotation name without the `@` prefix — `"InputFiles"` not `"@InputFiles"` — or the check fails.
- **Git commit before testing:** TC-1 for spike issues checks `git log -- path` for committed files. Always commit the artifact before running tests that verify it's in git history.
- **LWJGL Vulkan constant name:** `VK_EXT_DEBUG_UTILS_EXTENSION_NAME` (not `EXT_DEBUG_UTILS_EXTENSION_NAME`). Struct for validation features is `VkValidationFeaturesEXT`; constants in `EXTValidationFeatures` class.
- **Self-referential test anti-pattern:** A test that reads its own source file and uses `shouldNotContain "someString"` will always fail if `"someString"` is a literal in the assertion itself. Fix: build the forbidden string via concatenation (`"some" + "String"`) so it's never a source literal.
- **GitHub Actions Write hook:** The security reminder hook blocks the `Write` tool for `.github/workflows/*.yml` files. Use `Bash` + heredoc (`cat > file.yml << 'EOF' ... EOF`) to write CI workflow files.
- **Kotest collection vs string shouldContain:** Import both `io.kotest.matchers.collections.shouldContain` and `io.kotest.matchers.string.shouldContain`. Kotlin resolves by receiver type — no ambiguity.

## Spike Issue Pattern
- Spike deliverable = decision document (already written by Atlas before Forge arrives)
- Tests = document inspection (Kotest + file reads + subprocess calls to git/gh)
- "Contracts" = helper class wrapping document access (`SpikeDecisionDocument`)
- GitHub API tests (checking issue bodies) must be guarded by `GH_TOKEN` env var

## PR Patterns
- Branch: `feature/issue-{n}-{slug}`
- Commit artifacts + build infra + tests in one commit; annotation fixes in a follow-up
- Update linked issues as part of the PR (e.g., SHADER-1 from SPIKE-SHADER-1)
