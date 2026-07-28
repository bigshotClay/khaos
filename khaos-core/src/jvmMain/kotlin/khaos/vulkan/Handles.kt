package khaos.vulkan

@JvmInline
value class InstanceHandle private constructor(val handle: Long) {
    companion object {
        val NULL = InstanceHandle(0L)
    }
}

@JvmInline
value class DeviceHandle private constructor(val handle: Long) {
    companion object {
        val NULL = DeviceHandle(0L)
    }
}

@JvmInline
value class PhysicalDeviceHandle private constructor(val handle: Long) {
    companion object {
        val NULL = PhysicalDeviceHandle(0L)
    }
}

@JvmInline
value class QueueHandle private constructor(val handle: Long) {
    companion object {
        val NULL = QueueHandle(0L)
    }
}

@JvmInline
value class SurfaceHandle private constructor(val handle: Long) {
    companion object {
        val NULL = SurfaceHandle(0L)
    }
}

@JvmInline
value class SwapchainHandle private constructor(val handle: Long) {
    companion object {
        val NULL = SwapchainHandle(0L)
    }
}

@JvmInline
value class ImageHandle private constructor(val handle: Long) {
    companion object {
        val NULL = ImageHandle(0L)
    }
}

@JvmInline
value class ImageViewHandle private constructor(val handle: Long) {
    companion object {
        val NULL = ImageViewHandle(0L)
    }
}

@JvmInline
value class RenderPassHandle private constructor(val handle: Long) {
    companion object {
        val NULL = RenderPassHandle(0L)
    }
}

@JvmInline
value class FramebufferHandle private constructor(val handle: Long) {
    companion object {
        val NULL = FramebufferHandle(0L)
    }
}

@JvmInline
value class CommandPoolHandle private constructor(val handle: Long) {
    companion object {
        val NULL = CommandPoolHandle(0L)
    }
}

@JvmInline
value class CommandBufferHandle private constructor(val handle: Long) {
    companion object {
        val NULL = CommandBufferHandle(0L)
    }
}

@JvmInline
value class SemaphoreHandle private constructor(val handle: Long) {
    companion object {
        val NULL = SemaphoreHandle(0L)
    }
}

@JvmInline
value class FenceHandle private constructor(val handle: Long) {
    companion object {
        val NULL = FenceHandle(0L)
    }
}

@JvmInline
value class PipelineLayoutHandle private constructor(val handle: Long) {
    companion object {
        val NULL = PipelineLayoutHandle(0L)
    }
}

@JvmInline
value class DescriptorSetLayoutHandle private constructor(val handle: Long) {
    companion object {
        val NULL = DescriptorSetLayoutHandle(0L)
    }
}

@JvmInline
value class BufferHandle private constructor(val handle: Long) {
    companion object {
        val NULL = BufferHandle(0L)
    }
}

@JvmInline
value class DeviceMemoryHandle private constructor(val handle: Long) {
    companion object {
        val NULL = DeviceMemoryHandle(0L)
    }
}
