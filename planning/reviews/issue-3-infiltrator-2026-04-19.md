{
  "reviewer": "infiltrator",
  "findings": [
    {
      "id": "INF-01",
      "title": "git subprocess inherits full parent environment — credential and proxy leakage",
      "severity": "high",
      "category": "secrets",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:18",
      "description": "The ProcessBuilder for 'git log' does not sanitize the child process environment. It inherits every variable from the test JVM: GH_TOKEN, GITHUB_TOKEN, SSH_AUTH_SOCK, AWS_*, ANTHROPIC_API_KEY, HTTP_PROXY, HTTPS_PROXY, and any other secret injected into the CI environment. The gh subprocess in TC-5b correctly calls it.clear() before setting only three named vars; the git subprocess does nothing equivalent. Any git operation that triggers a credential helper, proxy, or hook will execute with the full secret environment exposed to that helper or hook binary.",
      "evidence": "ProcessBuilder('git', 'log', ...).directory(path.parent.toFile()).redirectErrorStream(true).start() — no environment.clear() call present. Contrast with SpikeShader1DecisionSpec.kt:107 which does pb.environment().also { it.clear() ... }.",
      "impact": "A malicious or compromised .git/hooks/post-checkout or a credential helper configured in the user's global gitconfig gains access to every secret in the CI environment. On GitHub Actions, this exposes GITHUB_TOKEN to arbitrary binaries reachable via the git credential chain."
    },
    {
      "id": "INF-02",
      "title": "CompletableFuture.get() called unconditionally after cancel(true) — CancellationException hides timeout context",
      "severity": "medium",
      "category": "trust",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt:116-122",
      "description": "When the gh subprocess times out (waitFor returns false), the code calls bodyFuture.cancel(true) and then falls through to bodyFuture.get(5, TimeUnit.SECONDS) with no early return. A cancelled CompletableFuture throws CancellationException on .get(), not a controlled test failure. The real timeout is buried; what surfaces is an unintelligible CancellationException stack trace with no clue linking it to the 30-second gh timeout. Identical issue in SpikeShader2DecisionSpec.kt:107-113.",
      "evidence": "if (!exited) { result.destroyForcibly(); bodyFuture.cancel(true) } /* no return */ ... val body = bodyFuture.get(5, TimeUnit.SECONDS) — SpikeShader1DecisionSpec.kt lines 116-122 and SpikeShader2DecisionSpec.kt lines 107-113.",
      "impact": "Timeout failures produce undiagnosable test output on CI. An attacker controlling network latency (e.g., SSRF toward the GitHub API, or a slow proxy) can force a confusing non-assertion failure that obscures the actual cause. Maintainers may re-run blindly rather than investigate."
    },
    {
      "id": "INF-03",
      "title": "path.parent silently returns null for root or bare filename paths — NullPointerException in committedToGit()",
      "severity": "medium",
      "category": "trust",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:19",
      "description": "path.parent returns null when path has no parent component (e.g., Path.of('file.md') or Path.of('/')). Calling .toFile() on null produces a NullPointerException that propagates uncaught from committedToGit(), crashing the test with a JVM exception rather than returning false as the method's contract implies. The path is caller-supplied via the SpikeDecisionDocument constructor with no normalization or root-check guard.",
      "evidence": "`.directory(path.parent.toFile())` — path.parent is not null-checked before .toFile() is invoked. TC-18 exercises the /tmp path (which has a parent) but does not exercise a bare filename or root path.",
      "impact": "Any caller constructing a SpikeDecisionDocument with a relative bare filename (e.g., 'file.md') or '/' triggers a hard NPE instead of a clean false. TC-18 does not catch this gap. A confused developer adding a new spec could unknowingly introduce this path and get an opaque crash."
    },
    {
      "id": "INF-04",
      "title": "KSP version '2.3.6' does not exist and does not match Kotlin 2.3.20 — phantom dependency poisons build",
      "severity": "medium",
      "category": "platform",
      "location": "gradle/libs.versions.toml:3",
      "description": "The version catalog pins kotlin = '2.3.20' and ksp = '2.3.6'. KSP versions for Kotlin 2.x follow the format '<kotlinVersion>-<kspRelease>' (e.g., '2.0.21-1.0.25'). A standalone version of '2.3.6' does not match this scheme and almost certainly does not exist on Maven Central or the Gradle Plugin Portal. The plugin id 'com.google.devtools.ksp' at version '2.3.6' will fail to resolve at build time. Since ksp is declared apply false at the root and no submodule applies it yet, this failure is silent during current builds — but will detonate the moment any module adds `alias(libs.plugins.ksp)`.",
      "evidence": "gradle/libs.versions.toml line 3: ksp = '2.3.6'. KSP's own release page (github.com/google/ksp/releases) shows no '2.3.6' release; valid Kotlin-2.x-era releases are '2.0.x-1.0.y' series.",
      "impact": "Any developer who adds ksp to a submodule triggers an immediate build failure that is misattributed to their change rather than the catalog. In a worst-case supply-chain scenario, if a malicious actor registers 'com.google.devtools.ksp:2.3.6' on Maven Central before the team corrects the version, that artifact would be resolved and executed as a Gradle plugin with full build-script privileges."
    },
    {
      "id": "INF-05",
      "title": "Gradle wrapper distribution URL has no SHA-256 checksum — MITM can substitute arbitrary Gradle distribution",
      "severity": "high",
      "category": "platform",
      "location": "gradle/wrapper/gradle-wrapper.properties:3",
      "description": "The wrapper properties file specifies distributionUrl over HTTPS and sets validateDistributionUrl=true, but omits distributionSha256Sum. Without a pinned checksum, Gradle only validates that the URL is an HTTPS URL; it does not verify the integrity of the downloaded zip. A network-level attacker (compromised CDN, MITM on a corporate proxy, or a poisoned DNS entry for services.gradle.org) can substitute a trojaned Gradle distribution that executes arbitrary code with the permissions of the build user on every developer machine and CI runner.",
      "evidence": "gradle/wrapper/gradle-wrapper.properties: distributionUrl=https://services.gradle.org/distributions/gradle-9.4.1-bin.zip — no distributionSha256Sum line present. The official Gradle documentation mandates distributionSha256Sum for production use.",
      "impact": "Full build-host code execution. The replaced Gradle daemon runs with the credentials of the CI service account, has access to all secrets injected into the build environment, and can exfiltrate source code or push malicious artifacts to any connected registry."
    },
    {
      "id": "INF-06",
      "title": "user.dir system property used as trusted project root — CI environment can redirect file reads anywhere",
      "severity": "medium",
      "category": "injection",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt:12",
      "description": "Both spec files resolve the decision document path as Paths.get(System.getProperty('user.dir')).resolve('_bmad-output/...'). The user.dir system property is the JVM process working directory, which is explicitly overridden in khaos-test-harness/build.gradle.kts to rootProject.projectDir. However, on CI or in IDEs, user.dir is controlled by whoever launches the test process. If an attacker can set a JVM system property (e.g., via JAVA_TOOL_OPTIONS=-Duser.dir=/), the resolve() call walks relative segments and can load any file on the filesystem as the 'decision document'. The spec files also pass path.toString() directly to git subprocess as the file argument.",
      "evidence": "SpikeShader1DecisionSpec.kt:12: private val projectRoot = Paths.get(System.getProperty('user.dir')). khaos-test-harness/build.gradle.kts:4: workingDir = rootProject.projectDir — these are not the same value in all execution contexts.",
      "impact": "Arbitrary file read: an attacker who controls JAVA_TOOL_OPTIONS on the CI runner can redirect the decision document path to any readable file (e.g., /proc/self/environ, ~/.ssh/id_rsa) and cause its contents to be evaluated against test assertions, potentially leaking content through assertion error messages."
    },
    {
      "id": "INF-07",
      "title": "LazyThreadSafetyMode.NONE on shared mutable document text — data race under parallel spec execution",
      "severity": "low",
      "category": "trust",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeDecisionDocument.kt:12",
      "description": "The text property is initialized with lazy(LazyThreadSafetyMode.NONE), which performs no synchronization. SpikeDecisionDocument instances are created as file-level private vals, meaning they are class-loader-level singletons shared across all test functions within a spec. If Kotest is configured for parallel test execution within a spec (e.g., via spec-level concurrency settings or a project-level KotestProjectConfig), two tests accessing .text simultaneously will both enter the lazy initializer. NONE provides no memory barrier, so both threads may see a partially initialized or double-initialized String value. The project has no KotestProjectConfig that enforces sequential isolation.",
      "evidence": "SpikeDecisionDocument.kt:12: val text: String by lazy(LazyThreadSafetyMode.NONE). The decisionDoc instances are declared private val at file scope in each spec, making them shared across all should-blocks in the class.",
      "impact": "Under parallel Kotest execution, concurrent tests could observe a torn read of a partially constructed String or trigger a double file read. The realistic threat is a false test pass: one coroutine reads a stale empty string and 'shouldContain' passes vacuously if shouldContain has a bug, or produces a confusing failure if not."
    },
    {
      "id": "INF-08",
      "title": "GH_TOKEN falls back to GITHUB_TOKEN without precedence documentation — token scope confusion",
      "severity": "low",
      "category": "auth",
      "location": "khaos-test-harness/src/test/kotlin/khaos/spike/SpikeShader1DecisionSpec.kt:97",
      "description": "The token resolution is System.getenv('GH_TOKEN') ?: System.getenv('GITHUB_TOKEN'). In GitHub Actions, GITHUB_TOKEN is the built-in ephemeral token with a default permission scope determined by the workflow's permissions block. GH_TOKEN is typically a fine-grained PAT with broader or narrower scope depending on who set it. The code uses whichever is set without logging which token source was selected or asserting its minimum required scope. If GITHUB_TOKEN has been granted write:issues or admin:org by a permissive workflow config, passing it to the gh CLI as GH_TOKEN enables privilege escalation — the test could mutate issues or repos rather than only reading them.",
      "evidence": "val token = System.getenv('GH_TOKEN') ?: System.getenv('GITHUB_TOKEN') — no scope assertion, no logging of which variable was resolved.",
      "impact": "If the higher-privilege GITHUB_TOKEN is selected in an environment where both are set, or if GH_TOKEN is over-scoped, the gh subprocess can write to the repository. A test that is supposed to only read issue #16 or #17 could mutate them if gh CLI is invoked with a write-scoped token and the command is modified in a future commit."
    },
    {
      "id": "INF-09",
      "title": "findLibrary().get() without isPresent check — silent NoSuchElementException during convention plugin application",
      "severity": "low",
      "category": "platform",
      "location": "buildSrc/src/main/kotlin/khaos.kotlin-jvm.gradle.kts:16",
      "description": "Both convention plugins call catalogLibs.findLibrary('kotest-runner-junit5').get() and catalogLibs.findLibrary('kotest-assertions-core').get() without checking Optional.isPresent() first. If the version catalog is renamed, the TOML entry is deleted, or the buildSrc catalog import path changes (e.g., the ../gradle/libs.versions.toml relative path breaks), findLibrary() returns an empty Optional and .get() throws NoSuchElementException. This fails every submodule that applies the convention plugin with a cryptic JVM exception rather than a Gradle-level build failure with actionable context.",
      "evidence": "khaos.kotlin-jvm.gradle.kts:16: catalogLibs.findLibrary('kotest-runner-junit5').get() — identical pattern in khaos.kotlin-kmp.gradle.kts:18-19.",
      "impact": "Any catalog refactor silently breaks the entire build with a NoSuchElementException stack trace that points into Gradle internals rather than the TOML file. A developer who renames a catalog entry triggers a confusing build-wide failure that is hard to attribute to the catalog change."
    },
    {
      "id": "INF-10",
      "title": "-Xcontext-parameters experimental flag applied globally without opt-in annotation — unstable API leaks into all modules",
      "severity": "low",
      "category": "platform",
      "location": "buildSrc/src/main/kotlin/khaos.kotlin-kmp.gradle.kts:11",
      "description": "Both convention plugins add '-Xcontext-parameters' to freeCompilerArgs for every module that applies the convention. This is an experimental Kotlin compiler feature (context receivers / context parameters) that is not stabilized. Enabling it project-wide without requiring an @OptIn annotation means any contributor can unknowingly use context parameter syntax in API-facing code. When the experimental feature is eventually dropped or its syntax changes between Kotlin versions, every module breaks simultaneously rather than failing at the single module that deliberately opted in.",
      "evidence": "khaos.kotlin-kmp.gradle.kts:11: freeCompilerArgs.add('-Xcontext-parameters'). khaos.kotlin-jvm.gradle.kts:11: same. Applied to all six submodules via the convention plugins.",
      "impact": "A Kotlin upgrade that alters context-parameter syntax triggers a project-wide compilation failure. More immediately: any external contributor PR that uses context parameter syntax passes CI because the flag is silently active, dragging experimental language constructs into stable modules without explicit design intent."
    }
  ]
}
