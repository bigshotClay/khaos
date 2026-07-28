# Test Plan — Issue #34: VUID Coverage Tracker

## Scope

Governance of the VUID tracker issue: accuracy, completeness, and update process. This is a living documentation artifact, not a code deliverable. Verification is process-based.

## Test Cases

### Acceptance

- **TC-1: Tracker structure** — The issue body contains at minimum: a Legend section (✅ Documented / 🔄 In Progress / ❌ Missing), and at least three category sections (Synchronization, Image Layout, Descriptor/Pipeline). Confirmed by reading issue #34 body.
- **TC-2: No entries marked ✅ without a wiki page** — Every VUID marked ✅ Documented has a corresponding GitHub Wiki page or linked doc. Assert no ✅ entry without a link. (Initially the table is empty — this gate activates when VUIDs are added.)
- **TC-3: VUIDs encountered during development are added** — When a VUID fires during development (CI, local dev), it is added to the tracker before the PR merges. Confirmed by PR process: PRs that touch Vulkan code include a CONTRIBUTING.md checklist item checking for new VUIDs.
- **TC-4: CONTRIBUTING.md references the tracker** — `CONTRIBUTING.md` explains the VUID tracker: what it is, how to add entries, and the documentation requirement. Confirmed by grep for "VUID" in `CONTRIBUTING.md`.
- **TC-5: Tracker is up-to-date at kernel gate** — By the time kernel gate conditions are met (GRAPH-5, TEST-3, TEST-4 green), all VUIDs encountered during v0 development are documented or marked 🔄 In Progress. Confirmed at gate review.

### Design Contract

- **TC-6: Plain-English explanations** — Each documented VUID entry links to a wiki page with: (a) plain-English explanation of what triggers it, (b) correct fix, (c) at least one code example of the correct pattern. Confirmed by reading the linked page.
- **TC-7: Wiki page before ✅** — A VUID is not marked ✅ until its wiki page is complete (all three items from TC-6 present). Process gate: PR adding ✅ status must link the completed wiki page.

### Failure Paths

- **TC-8: Missing VUID category** — If a new VUID category is encountered (e.g., "Render Pass compatibility") that doesn't have a section in the tracker, assert a new section is added before the VUID entry. Confirmed by process: the tracker issue is updated as part of the PR that introduced the code triggering the new VUID.
- **TC-9: Stale ✅ entry** — A VUID is marked ✅ but the linked wiki page is deleted or inaccessible. Assert a CI or periodic check (e.g., link checker) flags broken links in the wiki. Confirmed by CI YAML or manual quarterly review.
