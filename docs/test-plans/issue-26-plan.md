# Test Plan — Issue #26: TEST-4: Golden image test — triangle under Lavapipe

## Scope

Golden image test in `khaos-test-harness`: renders triangle offscreen under Lavapipe, compares against stored golden via SSIM. This is kernel gate condition #3.

## Preconditions

- GRAPH-5 (triangle) complete.
- Lavapipe present on CI runner.
- SSIM library on test classpath.
- Stored golden image checked into repo.

## Test Cases

### Acceptance

- **TC-1: Offscreen render** — Triangle renders to an offscreen framebuffer (no GLFW window). Assert render completes with `VulkanOutcome.Success`. [VUID gate]
- **TC-2: PNG capture** — Output image is captured as a PNG file. Assert file exists and is a valid PNG (non-zero byte size, decodable header).
- **TC-3: SSIM comparison** — Compare output PNG against stored golden image using SSIM. Assert SSIM score is above the configured threshold.
- **TC-4: SSIM threshold in config** — Threshold value is read from a named config file (e.g., `test-config.properties` or `golden-image.toml`). Assert it is NOT hardcoded in the test source. Confirmed by inspection.
- **TC-5: Fail below threshold** — Assert the test fails (not warns, not skips) when SSIM falls below threshold. Confirmed by a contrived negative test: compare against a deliberately wrong golden and assert `AssertionError` is thrown.
- **TC-6: CI task integration** — Test runs via `./gradlew test` on the CI Lavapipe runner. Assert the task passes in the CI matrix.
- **TC-7: Failure artifact upload** — When the test fails, the output PNG is stored as a CI artifact for visual diff inspection. Confirmed by CI YAML: `uses: actions/upload-artifact` step triggered on failure.

### Design Contract

- **TC-8: Lavapipe identity** — Before running the render, assert the selected physical device is Lavapipe (`llvmpipe` in `deviceName`). Assert the test does not silently run on a different GPU.
- **TC-9: SSIM threshold review comment** — A PR that changes the threshold value in the config file must include a comment explaining why. This is a process gate, not an automated test. Documented in CONTRIBUTING.md (see TEST-5).

### Failure Paths

- **TC-10: Golden image missing** — If the stored golden image file does not exist, the test fails with a message directing the developer to run the golden-update task — not a `FileNotFoundException` with no context.
- **TC-11: Render produces all-black image** — If the triangle fails to render (black output), SSIM against a non-black golden fails clearly. Assert the SSIM failure message includes the actual SSIM score.
- **TC-12: SSIM library unavailable** — If the SSIM dependency is missing, the test fails at class-loading time with a clear "missing dependency" message — not a `NoClassDefFoundError` swallowed by the test runner.
