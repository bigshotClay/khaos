# Test Plan — Issue #29: DIST-1: Maven Central publishing configuration

## Scope

Gradle Maven Central publishing in `khaos-gradle` and all publishable modules. GPG signing, correct metadata, pre-release versioning. Tests are build/integration verification — not unit tests.

## Test Cases

### Acceptance

- **TC-1: maven-publish plugin configured for all modules** — `maven-publish` plugin applied in: `khaos-core`, `khaos-memory`, `khaos-shader`, `khaos-graph`, `khaos-cmd`. Confirmed by grep for `maven-publish` in each module's `build.gradle.kts`.
- **TC-2: Group ID and artifact IDs correct** — Inspect generated POM or `publishing { publications { ... } }` block. Assert `groupId = "dev.khaos"`. Assert artifact IDs: `khaos-core`, `khaos-memory`, `khaos-shader`, `khaos-graph`, `khaos-cmd`.
- **TC-3: POM metadata complete** — Generated POM contains: `<description>`, `<licenses>`, `<scm><url>`, `<developers><developer>`. Assert all four present (Maven Central requirement). Confirmed by running `./gradlew generatePomFileFor...` and inspecting output.
- **TC-4: GPG signing via Gradle properties** — `signing { ... }` block reads key from Gradle properties (`signing.keyId`, `signing.password`, `signing.secretKeyRingFile` or in-memory key). Assert key is NOT committed to the repo (grep: no `gpg.key` or `signing.secretKey` literal in any tracked file).
- **TC-5: publishToMavenCentral task exists** — `./gradlew publishToMavenCentral --dry-run` exits 0 and shows the publish task in the plan. Assert no build failures in dry-run mode.
- **TC-6: Pre-release versioning** — Assert version string in `build.gradle.kts` uses `-alpha.N` or `-beta.N` suffix format. Assert no `-SNAPSHOT` version present anywhere. Confirmed by grep: no `SNAPSHOT` in any `build.gradle.kts`.
- **TC-7: Gradle wrapper version pinned** — `gradle/wrapper/gradle-wrapper.properties` contains a specific Gradle version (not `latest`). Assert version is documented in README.
- **TC-8: khaos-gradle published separately** — The Gradle plugin artifact is published as a separate artifact, not bundled into `khaos-core`. Confirmed by `publishing` block inspection.

### Design Contract

- **TC-9: Staging target only** — Assert `./gradlew publishToMavenCentral` publishes to the staging repository — not directly to release. Confirmed by Nexus Publish plugin or Central Portal API configuration: `nexusPublishing { transitionCheckOptions { delayBetween = ... } }` and no auto-promotion step.
- **TC-10: Signing key in CI secrets** — In CI YAML, GPG signing key is injected via `secrets.*` environment variables — not stored in any tracked file. Confirmed by CI YAML inspection.

### Failure Paths

- **TC-11: Publish without signing key** — Run `./gradlew publishToMavenCentral` without GPG signing properties set. Assert Gradle fails with a clear message about missing signing configuration — not a cryptic Nexus API error.
- **TC-12: Wrong artifact ID** — Temporarily set artifact ID to `khaos_core` (underscore). Assert POM generation produces the wrong ID. (This is a sanity check that the configuration is being read, not defaulted.) Restore correct ID after test.
