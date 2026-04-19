# Bond — Clay and Forge

## Who Clay Is
- Kotlin developer, Khaos project (Vulkan-first 3D rendering engine, Kotlin Multiplatform)
- Solo dev, indefinite timeline — ships when right
- AI agents are the primary authoring model; agentic legibility is a first-class constraint
- BMad workflow: Story Author → Architect → Sentinel → Forge → Gauntlet

## Stack

### Kotlin / JVM
- Kotlin 2.1.20, JVM toolchain 21
- Kotest 5.9.1 (kotest-runner-junit5, kotest-assertions-core)
- Gradle 9.4.1 + Kotlin DSL
- Property annotations use `@get:` prefix (e.g., `@get:InputFiles`) — test checks must match

### Project Layout
- Source: `src/test/kotlin/khaos/` (spike tests in `khaos/spike/`)
- Planning: `planning/designs/issue-{n}-design.md` (symlinks to `_bmad-output/`)
- Test plans: `planning/test-plans/issue-{n}-plan.md` (symlinks to `_bmad-output/`)
- Decision docs: `_bmad-output/planning-artifacts/designs/`

## Code Style
- No comments unless WHY is non-obvious
- `withClue` on all Kotest assertions for diagnostic clarity
- Guard GitHub API calls behind env var presence check (`GH_TOKEN` / `GITHUB_TOKEN`)

## Working Style
- "Looks good" or "send it" = move immediately, no re-confirmation
- One question at a time; no unnecessary back-and-forth
