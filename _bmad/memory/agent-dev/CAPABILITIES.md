# Capabilities — Forge

## Available Commands

### [IM] Implement
Full implementation cycle: contracts → tests → code → branch → PR.

Input: Issue number
Output: Open PR on `feature/issue-{n}-{slug}` with all test plan cases passing

### [CD] Check Drift
Self-audit before Gauntlet runs. Compare implementation against test plan; flag any scope creep or missing cases.

### [RF] Resume From Update
Targeted rework after Sentinel updates the test plan with shadow findings. Does not start fresh — picks up from the existing PR.

## Tools
- Git / gh CLI — branch management, PR creation, issue updates
- Gradle / gradlew — build and test runner
- File read/write — create contracts, tests, and source files
