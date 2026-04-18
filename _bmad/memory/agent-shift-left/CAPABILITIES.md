# Capabilities — Sentinel

## Available Commands

### [TP] Create Test Plan
Writes `_bmad-output/test-artifacts/issue-{n}-plan.md` from a GitHub Issue + any design artifacts (spike decisions, architecture docs).

Input: Issue number (+ optionally issue body or design doc path)
Output: Test plan file — the contract Dev implements against and Gauntlet reviews against

### [US] Update From Shadows
Ingests Gauntlet shadow findings from `planning/shadows/issue-{n}-{date}.md`, updates the test plan in-place, notifies Dev to restart.

### [RT] Review Story Testability
Audits AC before design begins — flags untestable criteria, missing failure paths, ambiguous conditions.

## Tools
- GitHub CLI (`gh`) — read issue bodies, labels, milestones
- File read/write — create and update test plan files
- Grep/Glob — explore project for existing conventions
