# Test Plan — Issue #31: DIST-3: Public API freeze — KDoc and semver tag

## Scope

API surface completeness, KDoc coverage, binary compatibility baseline, and semver tag. This is kernel gate condition #5 — closes only after GRAPH-5, TEST-3, and TEST-4 are all green.

## Preconditions

- Issues #22 (GRAPH-5), #25 (TEST-3), #26 (TEST-4) all green before this issue closes.

## Test Cases

### Acceptance

- **TC-1: KDoc on every public type** — Run `./gradlew dokkaHtml` (or equivalent). Assert zero undocumented public API warnings. Confirmed by Dokka's `reportUndocumented = true` option or a lint rule that fails on missing KDoc.
- **TC-2: WHY lines on invariant types** — For types with non-obvious invariants (`PipelineHandle.reusable`, `RecordingScope`, `VulkanOutcome.SwapchainOutOfDate`, `BarrierSpec`), assert KDoc contains a "WHY" explanation — not just a description of WHAT the type is. Confirmed by manual review checklist.
- **TC-3: No internal types leaking** — Run the Kotlin binary compatibility validator (`./gradlew apiCheck`). Assert no `internal` or `private` types appear in the public API dump. Assert no public API surface that should be `internal`.
- **TC-4: Binary compatibility baseline established** — `api/` directory exists with `.api` dump files for all publishable modules. Assert files are non-empty and up to date (`./gradlew apiCheck` passes).
- **TC-5: Semver tag cut** — Git tag `v0.1.0-alpha.1` exists in the repository. Assert `git tag -l "v0.1.0-alpha.1"` returns the tag. Assert the tag is on a commit where all gate conditions (GRAPH-5, TEST-3, TEST-4) are green.
- **TC-6: CHANGELOG.md created** — `CHANGELOG.md` exists at repo root with an initial entry for `v0.1.0-alpha.1`. Assert entry contains date and a summary of what is included in the release.
- **TC-7: API freeze for one cycle** — This is a process gate: the API is held frozen for at least one development cycle without pressure to change it. Confirmed by issue tracking (no API-breaking changes merged between tag and next milestone). Not an automated test.

### Design Contract

- **TC-8: apiCheck passes before tag** — `./gradlew apiCheck` must pass on the commit that is tagged `v0.1.0-alpha.1`. Assert CI runs apiCheck on the tag commit.
- **TC-9: Kotlin binary compatibility validator plugin applied** — `binary-compatibility-validator` plugin is applied in root `build.gradle.kts`. Confirmed by grep.

### Failure Paths

- **TC-10: Missing KDoc on one public type fails build** — `dokkaHtml` with `reportUndocumented = true` fails if any public type is missing KDoc. Assert this gate is active in CI — not just on local runs.
- **TC-11: API change without apiCheck fails CI** — Add a new public function to any module without regenerating the `.api` dump. Assert `./gradlew apiCheck` fails with a message identifying the change. Confirmed by a test: manually delete one line from an `.api` file and assert `apiCheck` catches the delta.
