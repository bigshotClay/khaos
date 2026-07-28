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
data class PipelineHandle(val handle: Long, val reusable: Boolean) {
    companion object {
        /**
         * The absent pipeline — `VK_NULL_HANDLE`, matching the `NULL` constant every other handle
         * type exposes. Use it instead of a nullable `PipelineHandle?` so the absent case stays in
         * the type rather than in every call site's null check.
         *
         * `reusable` is `false` because reuse is a promise about a real pipeline object surviving
         * across draws. There is no object here to survive, and declaring the null handle reusable
         * would let a multi-draw path accept it as a legitimate binding.
         */
        val NULL = PipelineHandle(handle = 0L, reusable = false)
    }
}
