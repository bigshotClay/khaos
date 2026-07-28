# Test Plan — Issue #30: DIST-2: Starter template — minimal project

## Scope

A minimal starter project (separate repo or `template/` directory) that compiles and runs on Linux, macOS (Apple Silicon), and Windows. CI-tested on all three platforms.

## Test Cases

### Acceptance

- **TC-1: Template location** — Starter template exists as a separate repository or a `template/` directory in this repo with clear instructions in its README. Assert the location is documented in the main repo README.
- **TC-2: Module count** — Template has at most 3 Gradle modules: `app` (+ any minimal support modules). Assert by counting `include(...)` entries in `settings.gradle.kts`.
- **TC-3: main.kt under 50 lines** — `main.kt` is under 50 lines. Assert via `wc -l main.kt < 50`. Assert every non-obvious line has a `// why` comment.
- **TC-4: Dependency pinned to specific version** — `build.gradle.kts` references a specific Khaos version (e.g., `"dev.khaos:khaos-core:0.1.0-alpha.1"`). Assert no `+` version or `LATEST` version. Confirmed by grep.
- **TC-5: CI matrix — Linux/macOS/Windows** — CI matrix passes `./gradlew build` on: Linux (x64), macOS (Apple Silicon), Windows (x64). Assert all three pass (CI badge or job success artifact).
- **TC-6: macOS MoltenVK in SETUP.md** — `SETUP.md` contains macOS setup instructions including MoltenVK installation (e.g., `brew install molten-vk`). Assert section exists. Confirmed by grep for `MoltenVK` or `molten-vk` in `SETUP.md`.
- **TC-7: Fresh clone compiles and runs** — Clone the template to a fresh directory. Follow `SETUP.md` exactly. Run `./gradlew run`. Assert the program starts without manual configuration beyond what `SETUP.md` describes. (Manual verification — CI matrix confirms this on each platform.)
- **TC-8: README links to template** — Main Khaos repo README contains a link to the starter template. Confirmed by grep for `starter` or `template` in `README.md`.

### Design Contract

- **TC-9: Embarrassingly simple** — `main.kt` contains no mesh loading, no asset pipeline, no shader hot-reload, no configuration system. Assert by code review: only window creation + render graph + triangle. Nothing else.
- **TC-10: macOS is the hardest path** — If the template works on macOS Apple Silicon (MoltenVK), it works on Linux and Windows. Assert macOS CI job is not skipped or marked as allowed-failure.

### Failure Paths

- **TC-11: Clone without SETUP.md steps fails with clear guidance** — Run `./gradlew build` without completing SETUP.md platform steps (e.g., no MoltenVK on macOS). Assert the build fails with a message directing the developer to `SETUP.md` — not a cryptic native library error.
- **TC-12: Wrong Khaos version** — Reference a non-existent Khaos version in `build.gradle.kts`. Assert `./gradlew build` fails with a clear "artifact not found" message — not a runtime crash.
