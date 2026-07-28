# Test Plan — Issue #32: DOCS-1: KDoc pass — every public type documented

## Scope

KDoc completeness across all kernel public types. This is a correctness requirement — KDoc bridges Vulkan spec to plain English. Verification is automated where possible, manual where not.

## Test Cases

### Acceptance

- **TC-1: KDoc on every public class and interface** — `./gradlew dokkaHtml` with `reportUndocumented = true` exits 0. Zero undocumented public classes, interfaces, sealed classes, or top-level functions. Confirmed by CI: dokka warning count == 0.
- **TC-2: Vulkan-term types have plain-English first line** — For types that wrap Vulkan concepts (`KhaosRenderPass`, `BarrierSpec`, `RecordingScope`, `VulkanOutcome`, `KhaosSwapchain`), assert KDoc starts with a plain-English explanation before any Vulkan spec reference. Confirmed by manual review checklist (at least 5 types spot-checked).
- **TC-3: BarrierSpec Barrier Cookbook warning** — `BarrierSpec` KDoc contains the Barrier Cookbook warning: stage flags are not interchangeable; copying from another spec without checking the access pattern is the primary source of vendor-specific bugs. Confirmed by grep for "cookbook" or "interchangeable" in `BarrierSpec` KDoc.
- **TC-4: PipelineHandle.reusable KDoc note** — `PipelineHandle.reusable` field has a KDoc note explaining the multi-draw pipeline contract (not just "true if the pipeline can be reused"). Confirmed by inspection.
- **TC-5: KDoc on all properties in data classes** — Every public `val` or `var` in a public `data class` has either a KDoc comment or an `@param` in the parent class KDoc. Confirmed by automated check (dokka with `reportUndocumented = true`).

### Design Contract

- **TC-6: Plain English before spec links** — KDoc follows the pattern: plain English explanation first, then optional `@see` or spec link. Assert no KDoc that is purely a Vulkan spec reference with no English explanation. Confirmed by manual spot-check of 10 types.
- **TC-7: WHY lines where non-obvious** — Types with non-obvious invariants (`RecordingScope`, `FrameIndex`, `VulkanOutcome.SwapchainOutOfDate`) have a `@note` or dedicated paragraph explaining the constraint. Confirmed by manual checklist.

### Failure Paths

- **TC-8: New public type without KDoc fails CI** — Adding a new public type without KDoc causes `./gradlew dokkaHtml` to fail. Assert this gate is active in CI. Confirmed by test: add an undocumented public class and verify CI fails.
- **TC-9: KDoc that doesn't compile fails build** — A KDoc `@param` referencing a non-existent parameter causes a Kotlin compiler warning or Dokka error. Confirmed by build: rename a parameter and assert the stale `@param` is flagged.
