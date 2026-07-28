# Test Plan — Issue #33: DOCS-2: Getting started guide — blank project → window → triangle

## Scope

GitHub Wiki guide for a Kotlin developer with no Vulkan experience. Covers platform setup through a rendered triangle. This is kernel gate condition #4. Verification is manual + process-based.

## Test Cases

### Acceptance

- **TC-1: Guide lives in GitHub Wiki** — The guide is accessible via the GitHub Wiki tab of the repository (not a `docs/` markdown file in the repo). Confirmed by navigating to `github.com/bigshotClay/khaos/wiki`.
- **TC-2: Chapter 0 — platform setup** — Guide contains a Chapter 0 covering: macOS (`brew install vulkan-sdk`, MoltenVK), Linux (`apt`-based Vulkan SDK), Windows (LunarG installer). Assert all three platforms are documented. Confirmed by inspecting wiki page sections.
- **TC-3: Verification test before rendering** — Chapter 0 includes a verification step the reader can run to confirm platform setup is correct before writing any Khaos code (e.g., `vulkaninfo` output check, or a one-line test script). Assert the verification step is present.
- **TC-4: Time estimates per chapter** — Every chapter includes a time estimate ("This section takes about X minutes"). Assert time estimates are present for all chapters. Confirmed by grep/inspection.
- **TC-5: Resume landmarks** — Each chapter includes a "Stopped here? Your project should look like: ..." landmark so a reader returning to the guide can orient themselves. Assert landmark is present in each chapter.
- **TC-6: Guide works for target reader** — A Kotlin developer with no Vulkan experience can follow the guide from Chapter 0 to a working triangle without hitting an undocumented wall. Confirmed by manual walkthrough on a fresh machine (at least one platform). This is a manual gate — not automated.
- **TC-7: Links to starter template** — Guide references the starter template (DIST-2) as the starting point. Confirmed by link in Chapter 1 or equivalent.

### Design Contract

- **TC-8: Khaos terminology introduced before use** — Guide defines `RecordingScope`, `RenderGraphSpec`, `VulkanOutcome`, and `BarrierSpec` in plain English before asking the reader to use them. No Vulkan jargon used before the guide explains what it means.
- **TC-9: Chapters are independent checkpoints** — Each chapter ends with a "your app should do X now" check. Readers can start from any chapter if they have the prior chapter's code.

### Failure Paths

- **TC-10: Missing platform step causes build failure** — If the guide omits a required platform setup step (e.g., `VULKAN_SDK` env var on Windows), a reader following the guide exactly would encounter a build failure. Assert the guide is reviewed against the CI YAML for each platform to ensure parity.
- **TC-11: Time estimates are realistic** — Guide does not promise 5-minute setup if MoltenVK installation takes 30 minutes. Assert time estimates are reviewed by actually following the steps. Manual gate.
