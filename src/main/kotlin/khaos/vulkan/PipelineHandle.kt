package khaos.vulkan

/**
 * Vulkan pipeline handle carrying both the native handle and a multi-draw reuse flag.
 *
 * This is a regular data class rather than an `@JvmInline value class` because pipeline handles
 * require two fields: the native `handle` (Long) and `reusable` (Boolean). Bit-packing the flag
 * into the high bits of the Long was rejected — encoding metadata in the native opaque pointer
 * risks corrupting the value when it passes through JNI to the Vulkan driver. Separate typed
 * variants (e.g., ReusablePipelineHandle / SingleUsePipelineHandle) were rejected to avoid
 * doubling the pipeline API surface for VK-3 through VK-8.
 *
 * Cost: one heap allocation per instance that the other inline handle types avoid.
 */
data class PipelineHandle(val handle: Long, val reusable: Boolean)
