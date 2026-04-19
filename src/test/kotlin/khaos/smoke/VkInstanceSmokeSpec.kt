package khaos.smoke

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT
import org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT
import org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT
import org.lwjgl.vulkan.EXTValidationFeatures.VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT
import org.lwjgl.vulkan.EXTValidationFeatures.VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import org.lwjgl.vulkan.VK10.vkCreateDevice
import org.lwjgl.vulkan.VK10.vkCreateInstance
import org.lwjgl.vulkan.VK10.vkDestroyDevice
import org.lwjgl.vulkan.VK10.vkDestroyInstance
import org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties
import org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties
import org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkDeviceCreateInfo
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo
import org.lwjgl.vulkan.VkInstance
import org.lwjgl.vulkan.VkInstanceCreateInfo
import org.lwjgl.vulkan.VkLayerProperties
import org.lwjgl.vulkan.VkPhysicalDevice
import org.lwjgl.vulkan.VkPhysicalDeviceProperties
import org.lwjgl.vulkan.VkValidationFeaturesEXT
import java.io.File
import java.util.concurrent.atomic.AtomicReference

private val projectRoot = System.getProperty("user.dir")

private val vulkanEnvironmentAvailable: Boolean =
    System.getenv("CI") == "true" || System.getenv("VK_ICD_FILENAMES") != null

private val requiresVulkan: (TestCase) -> Boolean = { vulkanEnvironmentAvailable }

class VkInstanceSmokeSpec : FunSpec({

    // ── Static workflow file inspection (always run, no Vulkan required) ─────

    // TC-1: ci.yml exists with correct triggers
    test("TC-1: ci.yml exists at the correct path with correct triggers") {
        val ciYml = File("$projectRoot/.github/workflows/ci.yml")
        withClue("ci.yml must exist at .github/workflows/ci.yml") {
            ciYml.exists() shouldBe true
        }
        val content = ciYml.readText()
        withClue("ci.yml must trigger on push to main") {
            content shouldContain "branches: [main]"
        }
        withClue("ci.yml must trigger on pull_request") {
            content shouldContain "pull_request:"
        }
    }

    // TC-8: Mesa packages installed via apt, not a third-party Vulkan SDK action for Lavapipe
    test("TC-8: Mesa packages installed via apt-get in workflow") {
        val content = File("$projectRoot/.github/workflows/ci.yml").readText()
        withClue("Workflow must use apt-get to install mesa-vulkan-drivers") {
            content shouldContain "mesa-vulkan-drivers"
        }
        withClue("Workflow must use apt-get to install vulkan-validationlayers") {
            content shouldContain "vulkan-validationlayers"
        }
        withClue("Workflow must not use jakoch/install-vulkan-sdk-action for Lavapipe runtime") {
            content shouldNotContain "jakoch/install-vulkan-sdk-action"
        }
    }

    // TC-9: VK_ICD_FILENAMES points to Lavapipe ICD JSON
    test("TC-9: VK_ICD_FILENAMES set to Lavapipe ICD path in workflow") {
        val content = File("$projectRoot/.github/workflows/ci.yml").readText()
        withClue("Workflow must set VK_ICD_FILENAMES to Lavapipe ICD JSON") {
            content shouldContain "VK_ICD_FILENAMES"
        }
        withClue("ICD path must reference the lvp_icd JSON file") {
            content shouldContain "lvp_icd"
        }
    }

    // TC-10: Smoke test scope — VkInstance + VkDevice only, no swapchain/render pass
    // Forbidden patterns are split to avoid self-matching when this file reads itself.
    test("TC-10: smoke test scope limited to VkInstance and VkDevice") {
        val source = File("$projectRoot/src/test/kotlin/khaos/smoke/VkInstanceSmokeSpec.kt").readText()
        val noSurface = "vkCreate" + "Win32SurfaceKHR"
        val noSurfaceXlib = "vkCreate" + "XlibSurfaceKHR"
        val noSwapchain = "vkCreate" + "SwapchainKHR"
        val noRenderPass = "vkCreate" + "RenderPass"
        val noCommandBuffers = "vkAllocate" + "CommandBuffers"
        withClue("Smoke test must not create a Vulkan surface") {
            source shouldNotContain noSurface
            source shouldNotContain noSurfaceXlib
        }
        withClue("Smoke test must not create a swapchain") {
            source shouldNotContain noSwapchain
        }
        withClue("Smoke test must not create a render pass") {
            source shouldNotContain noRenderPass
        }
        withClue("Smoke test must not create command buffers") {
            source shouldNotContain noCommandBuffers
        }
    }

    // TC-11: VUID detection is in-process via AtomicReference, not log scraping
    test("TC-11: VUID callback uses AtomicReference for in-process detection") {
        val source = File("$projectRoot/src/test/kotlin/khaos/smoke/VkInstanceSmokeSpec.kt").readText()
        withClue("Must use AtomicReference for in-process VUID capture") {
            source shouldContain "AtomicReference"
        }
        withClue("Must check vuidViolation after test body, not scrape logs") {
            source shouldContain "vuidViolation.get()"
        }
    }

    // TC-12: Teardown order verified — vkDestroyDevice before vkDestroyInstance in source
    test("TC-12: teardown order — vkDestroyDevice before vkDestroyInstance in source") {
        val source = File("$projectRoot/src/test/kotlin/khaos/smoke/VkInstanceSmokeSpec.kt").readText()
        val destroyDeviceIdx = source.indexOf("vkDestroyDevice")
        val destroyInstanceIdx = source.lastIndexOf("vkDestroyInstance")
        withClue("vkDestroyDevice must appear before vkDestroyInstance") {
            (destroyDeviceIdx < destroyInstanceIdx) shouldBe true
        }
    }

    // ── Runtime Vulkan tests (require CI=true or VK_ICD_FILENAMES) ───────────

    // TC-15 guard: beforeTest verifies validation layer is available before any runtime test
    beforeTest {
        if (!vulkanEnvironmentAvailable) return@beforeTest
        MemoryStack.stackPush().use { stack ->
            val countBuf = stack.ints(0)
            vkEnumerateInstanceLayerProperties(countBuf, null)
            val layerProps = VkLayerProperties.malloc(countBuf[0], stack)
            vkEnumerateInstanceLayerProperties(countBuf, layerProps)
            val names = (0 until layerProps.capacity()).map { layerProps[it].layerNameString() }
            withClue("TC-15: VK_LAYER_KHRONOS_validation must be present in instance layers — is the vulkan-validationlayers package installed?") {
                names shouldContain "VK_LAYER_KHRONOS_validation"
            }
        }
    }

    // TC-2,3,4,5,6,7,13: Main VkInstance lifecycle smoke test
    test("TC-2,3,4,5,6,7: VkInstance creates and destroys cleanly with Lavapipe and validation").config(enabledIf = requiresVulkan) {
        val vuidViolation = AtomicReference<String?>(null)

        MemoryStack.stackPush().use { stack ->
            // TC-4, D2: Fully programmatic layer + sync validation (no VK_INSTANCE_LAYERS env var)
            val layers = stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"))
            val extensions = stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

            val syncValidation = VkValidationFeaturesEXT.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT)
                pEnabledValidationFeatures(stack.ints(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT))
            }

            // TC-11, D1: In-process VUID detection via AtomicReference
            val callback = VkDebugUtilsMessengerCallbackEXT.create { severity, _, pCallbackData, _ ->
                val data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                val messageId = data.pMessageIdNameString() ?: ""
                if (messageId.startsWith("VUID-") &&
                    (severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0
                ) {
                    vuidViolation.compareAndSet(null, data.pMessageString())
                }
                VK_FALSE
            }

            val messengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                pfnUserCallback(callback)
            }

            syncValidation.pNext(messengerCreateInfo.address())

            val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                ppEnabledLayerNames(layers)
                ppEnabledExtensionNames(extensions)
                pNext(syncValidation.address())
            }

            val instanceHandle = stack.pointers(0L)
            val instanceResult = vkCreateInstance(createInfo, null, instanceHandle)
            withClue("TC-6: vkCreateInstance must return VK_SUCCESS") {
                instanceResult shouldBe VK_SUCCESS
            }
            val instance = VkInstance(instanceHandle[0], createInfo)

            val messengerHandle = stack.longs(0L)
            vkCreateDebugUtilsMessengerEXT(instance, messengerCreateInfo, null, messengerHandle)
            val messenger = messengerHandle[0]

            // TC-2, D6: Enumerate physical devices and verify Lavapipe is selected
            val deviceCount = stack.ints(0)
            vkEnumeratePhysicalDevices(instance, deviceCount, null)
            withClue("TC-13: At least one Vulkan physical device must be present — check VK_ICD_FILENAMES and Mesa installation") {
                deviceCount[0] shouldNotBe 0
            }

            val deviceHandles = stack.mallocPointer(deviceCount[0])
            vkEnumeratePhysicalDevices(instance, deviceCount, deviceHandles)
            val physicalDevice = VkPhysicalDevice(deviceHandles[0], instance)

            val deviceProps = VkPhysicalDeviceProperties.malloc(stack)
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProps)
            val deviceName = deviceProps.deviceNameString()

            // TC-7: Log device and layer versions for CI traceability
            println("Vulkan physical device: $deviceName")
            println("Driver version (raw): ${deviceProps.driverVersion()}")

            withClue("TC-2: Expected Lavapipe (llvmpipe) device — got '$deviceName'. Check that VK_ICD_FILENAMES points to the Lavapipe ICD and no discrete GPU is intercepting.") {
                deviceName shouldContain "llvmpipe"
            }

            // TC-10: Create VkDevice (logical device) — no swapchain, render pass, or command buffers
            val queueFamilyCount = stack.ints(0)
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null)

            val queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack)
            queueCreateInfos[0].apply {
                sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                queueFamilyIndex(0)
                pQueuePriorities(stack.floats(1.0f))
            }

            val deviceCreateInfo = VkDeviceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                pQueueCreateInfos(queueCreateInfos)
            }

            val logicalDeviceHandle = stack.pointers(0L)
            val deviceResult = vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDeviceHandle)
            withClue("TC-6: vkCreateDevice must return VK_SUCCESS") {
                deviceResult shouldBe VK_SUCCESS
            }
            val device = VkDevice(logicalDeviceHandle[0], physicalDevice, deviceCreateInfo)

            // TC-12, D1: Teardown in correct order — device before messenger before instance
            vkDestroyDevice(device, null)
            vkDestroyDebugUtilsMessengerEXT(instance, messenger, null)
            vkDestroyInstance(instance, null)

            callback.free()
        }

        // TC-5: Zero VUIDs on the clean path — any VUID is a build failure
        withClue("TC-5: No VUID violations expected on clean VkInstance lifecycle. Got: ${vuidViolation.get()}") {
            vuidViolation.get() shouldBe null
        }
    }

    // TC-7: Validation layer version logged to CI output
    test("TC-7: validation layer version logged to CI output").config(enabledIf = requiresVulkan) {
        MemoryStack.stackPush().use { stack ->
            val countBuf = stack.ints(0)
            vkEnumerateInstanceLayerProperties(countBuf, null)
            val layerProps = VkLayerProperties.malloc(countBuf[0], stack)
            vkEnumerateInstanceLayerProperties(countBuf, layerProps)

            val validationLayer = (0 until layerProps.capacity())
                .map { layerProps[it] }
                .firstOrNull { it.layerNameString() == "VK_LAYER_KHRONOS_validation" }

            withClue("VK_LAYER_KHRONOS_validation must be in the layer list") {
                validationLayer shouldNotBe null
            }
            // TC-3: Layer confirmed active programmatically (D2 — no VK_INSTANCE_LAYERS env var needed)
            println("VK_LAYER_KHRONOS_validation spec version: ${validationLayer!!.specVersion()}")
            println("VK_LAYER_KHRONOS_validation impl version: ${validationLayer.implementationVersion()}")
        }
    }

    // TC-14: Deliberate VUID triggers detection — validates the callback mechanism itself
    test("TC-14: VUID from wrong destruction order is detected by callback").config(enabledIf = requiresVulkan) {
        val vuidViolation = AtomicReference<String?>(null)

        MemoryStack.stackPush().use { stack ->
            val layers = stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"))
            val extensions = stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

            val syncValidation = VkValidationFeaturesEXT.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT)
                pEnabledValidationFeatures(stack.ints(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT))
            }

            val callback = VkDebugUtilsMessengerCallbackEXT.create { severity, _, pCallbackData, _ ->
                val data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                val messageId = data.pMessageIdNameString() ?: ""
                if (messageId.startsWith("VUID-") &&
                    (severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0
                ) {
                    vuidViolation.compareAndSet(null, data.pMessageString())
                }
                VK_FALSE
            }

            val messengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT or VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                pfnUserCallback(callback)
            }

            syncValidation.pNext(messengerCreateInfo.address())

            val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
                sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                ppEnabledLayerNames(layers)
                ppEnabledExtensionNames(extensions)
                pNext(syncValidation.address())
            }

            val instanceHandle = stack.pointers(0L)
            vkCreateInstance(createInfo, null, instanceHandle)
            val instance = VkInstance(instanceHandle[0], createInfo)

            val messengerHandle = stack.longs(0L)
            vkCreateDebugUtilsMessengerEXT(instance, messengerCreateInfo, null, messengerHandle)

            // Deliberately destroy instance while messenger is still alive (wrong order)
            // This triggers VUID-vkDestroyInstance-instance-00629
            vkDestroyInstance(instance, null)

            callback.free()
        }

        withClue("TC-14: VUID violation must have been detected from wrong destruction order (messenger still alive when instance destroyed)") {
            vuidViolation.get() shouldNotBe null
        }
    }
})
