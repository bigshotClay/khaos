# Test Plan — Issue #27: TEST-5: Golden image governance workflow

## Scope

Golden image update process: explicit human approval required, diff artifact attached. Uncontrolled auto-updates are prohibited. Deliverable is primarily process + tooling, not runtime code.

## Test Cases

### Acceptance

- **TC-1: No CI auto-update** — Inspect all CI YAML files. Assert no job runs `updateGoldenImages` or equivalent task unconditionally. Assert no `if: failure()` or `if: always()` step that auto-commits golden image changes. Confirmed by grep: `update.*golden\|golden.*update` not present in non-explicit CI steps.
- **TC-2: Explicit update task** — A Gradle task `updateGoldenImages` (or named equivalent) exists. Assert it requires a named flag (e.g., `--update-goldens` or `-PupdateGoldens=true`) to run — not callable with plain `./gradlew updateGoldenImages`. Assert running without the flag fails with a clear message.
- **TC-3: Diff artifact generated** — Running the update task generates a diff artifact alongside the new golden: side-by-side PNG comparison or SSIM score report. Assert the artifact file exists after a simulated golden update.
- **TC-4: CONTRIBUTING.md process documented** — `CONTRIBUTING.md` exists and contains a section on golden image updates covering: (a) when it is acceptable to update, (b) what the diff artifact must show, (c) what the PR description must include. Confirmed by grep for "golden" in `CONTRIBUTING.md`.
- **TC-5: SSIM threshold in config file** — The threshold value lives in a named config file (not hardcoded). Assert a PR changing the threshold triggers CI to re-run the golden comparison with the new value. Confirmed by verifying the test reads from the config file dynamically.
- **TC-6: PR template checklist** — The PR template (`.github/PULL_REQUEST_TEMPLATE.md` or equivalent) contains a checklist item for golden image updates. Assert grep finds the item.

### Design Contract

- **TC-7: Update requires human approval** — The update workflow requires Clay to approve the change on the PR — the tooling generates the diff; the human decides. This is a policy gate, not an automated test. Confirmed by CONTRIBUTING.md wording.
- **TC-8: Old golden archived, not overwritten silently** — When the update task runs, the previous golden is archived (renamed or moved to a diff directory) — not silently overwritten. Assert the archive file exists after update.

### Failure Paths

- **TC-9: Update task without flag prints clear guidance** — Run `./gradlew updateGoldenImages` without the required flag. Assert output contains the correct flag name and a one-line description of what the task does — not a Gradle build error with no message.
- **TC-10: Diff artifact on CI failure** — In CI, when the golden comparison fails (SSIM below threshold), the output image is uploaded as an artifact automatically. Confirmed by CI YAML inspection (same gate as TEST-4 TC-7).
