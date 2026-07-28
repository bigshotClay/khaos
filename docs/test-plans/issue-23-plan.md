# Test Plan — Issue #23: TEST-1: Property tests for math primitives

## Scope

Kotest property tests for math types (vectors, matrices, quaternions) in `khaos-core` test sources. GPU-free. All tests run in `./gradlew test` under 500ms total.

## Test Cases

### Acceptance

- **TC-1: Vector arithmetic coverage** — Property tests exist for: vector addition (`a + b == b + a` commutativity), subtraction, scalar multiplication (`(a * s).magnitude == a.magnitude * |s|`), dot product (`a · b == b · a`), cross product (`a × b == -(b × a)`), magnitude (`|a| >= 0.0`).
- **TC-2: Matrix arithmetic coverage** — Property tests exist for: multiplication associativity (`(A * B) * C == A * (B * C)` within float tolerance), identity matrix (`A * I == A`), transpose (`(A^T)^T == A`), determinant sign consistency (not necessarily exact value due to float error).
- **TC-3: Quaternion coverage** — Property tests exist for: composition (rotation chaining), normalization (`|q.normalize()| ≈ 1.0`), slerp edge cases (identity quaternion, 180° rotation without flip).
- **TC-4: GPU-free execution** — `./gradlew test` passes with no GPU, no GLFW window, no Vulkan instance loaded. Assert no `UnsatisfiedLinkError` or `VulkanException` in test output.
- **TC-5: Runtime under 500ms** — Tests complete in under 500ms on the CI runner. Assert via CI timing artifact or Gradle test report.
- **TC-6: Arb-based generation** — All property tests use `Arb.float()` (or `Arb.double()`, `Arb.numericFloat()`) — not a fixed handful of literal test cases. Assert at least one `checkAll(Arb.float(), Arb.float()) { ... }` pattern in each property test.

### Design Contract

- **TC-7: Float tolerance in matrix tests** — Matrix multiplication associativity assertions use an epsilon tolerance (not `==`). Assert `|result - expected| < epsilon` where epsilon is a named constant.
- **TC-8: No Kotest FunSpec nesting error** — Property tests use `FreeSpec` or `StringSpec` with `checkAll` — not a manual test loop. Confirmed by source inspection.

### Failure Paths

- **TC-9: NaN input propagation** — Property test includes `Arb.float()` which may generate NaN. Assert that math operations on NaN inputs either produce NaN (correct IEEE behavior) or are explicitly filtered with `.filter { !it.isNaN() }` with a comment explaining the decision.
- **TC-10: Division by zero in normalization** — Normalizing a zero vector is tested explicitly: assert either `NaN` (IEEE behavior) or a typed `ZeroVector` error — not a crash or unhandled exception.
