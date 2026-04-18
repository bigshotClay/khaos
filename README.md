# khaos

A Vulkan-first 3D rendering engine for Kotlin Multiplatform.

> **Status:** Pre-alpha — kernel in development.

## What

Khaos gives Kotlin developers a first-class path to Vulkan without C++. The core thesis: treat Vulkan's verbosity as invariant surface, not friction. Every explicit field is a place to hang a type, a test, or an assertion.

- All handles as `@JvmInline value class` — mismatched handles are compile errors
- All results as `VulkanOutcome` sealed hierarchy — no unchecked exceptions
- Render graph as pure data — compiled by a pure function, testable without a GPU
- Command recording enforced by context type — `draw()` outside a `RecordingScope` is a compile error

## Project Status

Working toward the kernel gate. See [milestones](../../milestones) for current scope.

## License

TBD
