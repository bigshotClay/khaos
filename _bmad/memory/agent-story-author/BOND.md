# Bond

## Basics
- **Name:** Clay
- **Call them:** Clay
- **Language:** English

## Their Projects

### khaos
- **What it is:** Vulkan-first 3D rendering engine written in Kotlin Multiplatform — a runtime library
- **Stack:** Kotlin 2.2+ (context parameters load-bearing), LWJGL 3 (Vulkan/GLFW/shaderc/VMA bindings), Gradle + Kotlin DSL, KSP processor for shader binding generation, Lavapipe for CI, kotest for property/unit tests
- **Repo type:** Greenfield. Not yet started — this is pre-implementation planning
- **Distribution:** Maven Central under `dev.khaos` group ID
- **Key module proposals:** `core`, `memory`, `shader`, `graph`, `cmd`, `test-harness` (from design brief open questions)
- **PRD location:** `_bmad-output/planning-artifacts/prd.md`
- **Design brief location:** `docs/engine-design-brief.md`

## Label Taxonomy

**Type** (always exactly one per issue):
- `spike` — time-boxed investigation; produces a decision, not production code
- `feature` — new capability from the PRD
- `infra` — build, CI, tooling, project scaffolding
- `docs` — documentation and guides
- `bug` — defect (post-implementation)

**Area** (always exactly one per issue):
- `area: wrapper` — Vulkan handle, result, and scope layer
- `area: memory` — VMA, allocator, resource lifetimes
- `area: shader` — GLSL pipeline, KSP processor, SPIR-V
- `area: graph` — render graph, compiler, pipeline state
- `area: testing` — test infrastructure, golden images, harnesses
- `area: distribution` — Maven Central, versioning, starter template
- `area: foundation` — project structure, CI pipeline

**Status** (optional, applied as needed):
- `blocked` — waiting on a dependency before work can start
- `needs-decision` — open question must be resolved before implementation

## Milestone Conventions
_Not yet discovered. Will populate during First Breath._

## Acceptance Criteria Style
Checkboxes. Each item is a discrete, testable condition. No Given/When/Then, no "should" statements.

## Story Detail Level
Behavior plus implementation hints. State what the behavior is, then add brief technical context (which artifact, which mechanism, what it hooks into). Not full implementation specs — enough that a developer or agent knows the intended approach without having to infer it.

## Things They've Asked Me to Remember
_Nothing yet._

## Things to Avoid
_Nothing flagged yet._
