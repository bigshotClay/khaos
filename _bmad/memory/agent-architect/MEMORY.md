# Memory — Recurring Patterns and Decisions

## CI / Vulkan Test Patterns (settled 2026-04-19, Issue #4)

- **VUID callback:** `AtomicReference<String?>` set inside `VkDebugUtilsMessengerCallbackEXT`, checked after test body — never throw across native boundary
- **Layer activation:** Always programmatic (`ppEnabledLayerNames` + `VkValidationFeaturesEXT` struct) — sync validation cannot be env-var enabled
- **Lavapipe device name:** `"llvmpipe (LLVM X.Y.Z, ...)"` — not `"lavapipe"`. Assert `deviceNameString().contains("llvmpipe")`.
- **lwjgl-vulkan natives:** No `:natives-linux` classifier — Vulkan loader is OS-provided (Mesa apt package). `lwjgl` core still needs `:natives-linux` for MemoryUtil etc.

## Shader Pipeline (settled 2026-04-18, Issues #1 + #2)

- **Compilation:** `glslc` subprocess via `@CacheableTask` — NOT LWJGL shaderc JNI (daemon loading issues; no ecosystem precedent)
- **Validation:** `spirv-val` subprocess — no JVM bindings exist (LWJGL #147 unimplemented)
- **Binding codegen:** Standalone Gradle `@CacheableTask`, NOT KSP — KSP can't declare external file inputs to Gradle; incremental builds break silently
- **Reflection:** `spirv-cross --reflect` → JSON; `spirv-reflect-kt` is a pure-JVM alternative for later
- **Task chain:** `compileShaders` → `reflectShaders` → `generateShaderBindings` → `compileKotlin`
- **Generated types:** `@JvmInline value class` per binding, `sealed interface` for vertex attributes, `data class` for push constants — all in `commonMain`
