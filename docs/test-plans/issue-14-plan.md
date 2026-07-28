# Test Plan — Issue #14: MEM-2: FrameIndex type and resource lifetime scope encoding

## Scope

`FrameIndex` value class and `ResourceLifetime` sealed hierarchy in `khaos-memory`. Pure Kotlin — GPU-free. All tests run headlessly.

## Test Cases

### Acceptance

- **TC-1: FrameIndex is @JvmInline** — Assert `FrameIndex` is a `@JvmInline value class` wrapping `UInt`. Verify via reflection: `FrameIndex::class.java.annotations` contains `JvmInline`. Confirm it cannot be passed where a raw `Int` or `UInt` is expected without explicit conversion.
- **TC-2: FrameIndex arithmetic — modular** — `FrameIndex(3u).mod(framesInFlight = 3u)` returns `FrameIndex(0u)`. Property-based test: `forAll(Arb.uint()) { n -> FrameIndex(n).mod(3u).raw < 3u }`.
- **TC-3: FrameIndex arithmetic — increment** — `FrameIndex(2u).next(framesInFlight = 3u)` returns `FrameIndex(0u)` (wraps correctly). Assert `FrameIndex(0u).next(3u) == FrameIndex(1u)`.
- **TC-4: ResourceLifetime.Persistent** — Construct `ResourceLifetime.Persistent`. Assert it carries no frame count. Assert it is a sealed class member.
- **TC-5: ResourceLifetime.FrameScoped construction** — Construct `ResourceLifetime.FrameScoped(frames = 2)`. Assert `frames == 2`. Assert it is a sealed class member distinct from `Persistent`.
- **TC-6: KDoc on ResourceLifetime** — `ResourceLifetime` has KDoc explaining the deferred deletion queue contract (references MEM-3). Confirmed by documentation check.

### Design Contract

- **TC-7: FrameIndex is not interchangeable with UInt** — A function accepting `FrameIndex` must not accept a raw `UInt` directly. Compile-time enforcement via type system. Confirmed by API inspection.
- **TC-8: FrameScoped frame count is accessible** — `ResourceLifetime.FrameScoped(frames = 2).frames` is readable at runtime. The frame count is not erased.
- **TC-9: Sealed when exhaustive** — A `when` on `ResourceLifetime` without `else` must compile exhaustively. Adding a new subtype at compile time causes an error at the `when` site.

### Failure Paths

- **TC-10: Zero framesInFlight in mod** — `FrameIndex(5u).mod(0u)` throws `ArithmeticException` or returns a typed error — not silent division-by-zero undefined behavior.
- **TC-11: FrameScoped with negative frames** — `ResourceLifetime.FrameScoped(frames = -1)` is rejected at construction time (runtime check or compile-time enforcement via `UInt` parameter). Assert an explicit `IllegalArgumentException` or type error.
