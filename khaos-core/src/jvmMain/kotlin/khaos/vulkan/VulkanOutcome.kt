package khaos.vulkan

sealed class VulkanOutcome<out T> {
    data class Success<out T>(val resource: T) : VulkanOutcome<T>()
    data class Degraded(val indicator: SwapchainStatus) : VulkanOutcome<Nothing>()
    data class RecoverableFailure(val cause: VulkanError) : VulkanOutcome<Nothing>()
    data class UnrecoverableFailure(val cause: VulkanError) : VulkanOutcome<Nothing>()
}
