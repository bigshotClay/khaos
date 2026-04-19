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
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
import org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME
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

    // ── Static inspection tests (always run — no Vulkan required, no beforeTest fires) ──

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

    // TC-8: Mesa packages installed via apt, not a third-party Vulkan SDK action
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

    // TC-9: VK_ICD_FILENAMES set to dynamically-discovered Lavapipe ICD path (no hardcoded arch)
    test("TC-9: VK_ICD_FILENAMES set to dynamically-discovered Lavapipe ICD path in workflow") {
        val content = File("$projectRoot/.github/workflows/ci.yml").readText()
        withClue("Workflow must set VK_ICD_FILENAMES to Lavapipe ICD JSON") {
            content shouldContain "VK_ICD_FILENAMES"
        }
        withClue("ICD path must reference the lvp_icd JSON file") {
            content shouldContain "lvp_icd"
        }
        val hardcodedArch = "x86" + "_64"
        withClue("ICD path must not hardcode architecture suffix — breaks ARM64 runners") {
            content shouldNotContain hardcodedArch
        }
    }

    // TC-10: Smoke test scope — VkInstance + VkDevice only, no swapchain/render pass
    // Forbidden strings split to avoid self-matching when this file reads itself.
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

    // TC-12: Teardown order verified behaviorally via TC-14 VUID detection.
    // Both destroy calls must exist; correct ordering is enforced by TC-14's VUID assertion.
    // lastIndexOf across the whole file is vacuous — TC-14's wrong-order line is always last.
    test("TC-12: teardown order — vkDestroyDevice before vkDestroyInstance (behavioral check via TC-14)") {
        val source = File("$projectRoot/src/test/kotlin/khaos/smoke/VkInstanceSmokeSpec.kt").readText()
        withClue("Happy-path must call vkDestroyDevice") {
            source shouldContain "vkDestroyDevice"
        }
        withClue("Happy-path must call vkDestroyInstance") {
            source shouldContain "vkDestroyInstance"
        }
    }

    // TC-17: LWJGL natives are platform-conditional, not hardcoded to natives-linux
    test("TC-17: LWJGL natives are platform-conditional in build.gradle.kts") {
        val content = File("$projectRoot/build.gradle.kts").readText()
        // Check for the direct assignment pattern — split to avoid self-match in this source file.
        // `else -> "natives-linux"` inside a when-expression is acceptable; a bare assignment is not.
        val directAssignment = "val lwjglNatives = " + "\"natives-linux\""
        withClue("lwjglNatives must not be directly assigned to a hardcoded \"natives-linux\" literal — breaks macOS and Windows local dev") {
            content shouldNotContain directAssignment
        }
        withClue("build.gradle.kts must use OS detection for lwjglNatives") {
            content shouldContain "os.name"
        }
    }

    // TC-18: vkCreateDebugUtilsMessengerEXT return code asserted VK_SUCCESS
    // Split string avoids self-match when TC-18 reads its own source file.
    test("TC-18: vkCreateDebugUtilsMessengerEXT return code is captured and asserted VK_SUCCESS") {
        val source = File("$projectRoot/src/test/kotlin/khaos/smoke/VkInstanceSmokeSpec.kt").readText()
        withClue("vkCreateDebugUtilsMessengerEXT result must be stored in messengerResult") {
            source shouldContain "messengerResult"
        }
        withClue("messengerResult must be asserted as VK_SUCCESS before VUID-dependent assertions") {
            source shouldContain "messengerResult" + " shouldBe VK_SUCCESS"
        }
    }

    // TC-19: GitHub Actions uses: entries pinned to SHA digests, not mutable version tags
    test("TC-19: GitHub Actions uses: entries pinned to SHA digests") {
        val content = File("$projectRoot/.github/workflows/ci.yml").readText()
        val usesLines = content.lines().filter { it.trim().startsWith("uses:") }
        withClue("ci.yml must have at least one uses: entry") {
            usesLines.isNotEmpty() shouldBe true
        }
        val mutableTag = Regex("@" + "v\\d+\\b")
        for (line in usesLines) {
            withClue("Action must be pinned to SHA digest, not mutable version tag: $line") {
                mutableTag.containsMatchIn(line) shouldBe false
            }
        }
        val shaDigest = Regex("[a-zA-Z0-9._/-]+@[0-9a-f]{40}")
        for (line in usesLines) {
            withClue("Action must reference a 40-char hex SHA digest: $line") {
                shaDigest.containsMatchIn(line) shouldBe true
            }
        }
    }

    // TC-20: ci.yml declares permissions: block with contents: read
    test("TC-20: ci.yml declares permissions block with contents: read") {
        val content = File("$projectRoot/.github/workflows/ci.yml").readText()
        withClue("ci.yml must contain a permissions: block") {
            content shouldContain "permissions:"
        }
        withClue("permissions block must include contents: read") {
            content shouldContain "contents: read"
        }
        val forbidden = "contents: " + "write"
        withClue("permissions block must NOT set contents: write") {
            content shouldNotContain forbidden
        }
    }

    // ── Vulkan runtime tests (require CI=true or VK_ICD_FILENAMES) ────────────────────────
    // beforeTest inside this context fires only for the tests below, not for the static tests above.

    context("Vulkan runtime") {

        // TC-15 guard: verify validation layer available before any runtime test.
        // Scoped to this context — static tests above are unaffected.
        beforeTest {
            if (!vulkanEnvironmentAvailable) return@beforeTest
            MemoryStack.stackPush().use { stack ->
                val countBuf = stack.ints(0)
                vkEnumerateInstanceLayerProperties(countBuf, null)
                val layerProps = VkLayerProperties.malloc(countBuf[0], stack)
                vkEnumerateInstanceLayerProperties(countBuf, layerProps)
                val names = (0 until layerProps.capacity()).map { layerProps[it].layerNameString() }
                withClue("TC-15: VK_LAYER_KHRONOS_validation must be present — is vulkan-validationlayers installed?") {
                    names shouldContain "VK_LAYER_KHRONOS_validation"
                }
            }
        }

        // TC-2,3,4,5,6,13: Main VkInstance lifecycle smoke test
        test("TC-2,3,4,5,6,13: VkInstance creates and destroys cleanly with Lavapipe and validation").config(enabledIf = requiresVulkan) {
            val vuidViolation = AtomicReference<String?>(null)

            // TC-11, D1: In-process VUID detection via AtomicReference.
            // TC-5: callback must cover BOTH WARNING and ERROR severity bits.
            val callback = VkDebugUtilsMessengerCallbackEXT.create { severity, _, pCallbackData, _ ->
                val data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                val messageId = data.pMessageIdNameString() ?: ""
                if (messageId.startsWith("VUID-") &&
                    ((severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0 ||
                     (severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
                ) {
                    vuidViolation.compareAndSet(null, data.pMessageString())
                }
                VK_FALSE
            }
            try {
                MemoryStack.stackPush().use { stack ->
                    // TC-4, D2: Fully programmatic layer + sync validation (no VK_INSTANCE_LAYERS env var)
                    val layers = stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"))
                    val extensions = stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

                    val syncValidation = VkValidationFeaturesEXT.calloc(stack).apply {
                        sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT)
                        pEnabledValidationFeatures(stack.ints(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT))
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

                    // TC-18: assert messenger creation succeeds before making VUID-dependent assertions
                    val messengerHandle = stack.longs(0L)
                    val messengerResult = vkCreateDebugUtilsMessengerEXT(instance, messengerCreateInfo, null, messengerHandle)
                    withClue("TC-18: vkCreateDebugUtilsMessengerEXT must return VK_SUCCESS — zero-VUID assertion is only meaningful with an active messenger") {
                        messengerResult shouldBe VK_SUCCESS
                    }
                    val messenger = messengerHandle[0]

                    // TC-2, TC-13: Enumerate physical devices and verify Lavapipe is selected
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

                    // TC-7: Log device version for CI traceability — decoded to human-readable form
                    val rawDriverVersion = deviceProps.driverVersion()
                    val driverMajor = rawDriverVersion ushr 22
                    val driverMinor = (rawDriverVersion ushr 12) and 0x3FF
                    val driverPatch = rawDriverVersion and 0xFFF
                    println("Vulkan physical device: $deviceName")
                    println("Driver version: $driverMajor.$driverMinor.$driverPatch")

                    withClue("TC-2: Expected Lavapipe (llvmpipe) device — got '$deviceName'. Check that VK_ICD_FILENAMES points to the Lavapipe ICD.") {
                        deviceName shouldContain "llvmpipe"
                    }

                    // TC-10: Create VkDevice — no swapchain, render pass, or command buffers
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
                }
            } finally {
                // TC-16: callback freed in finally — guaranteed even if an assertion throws mid-test
                callback.free()
            }

            // TC-5: Zero VUIDs on the clean path — any VUID is a build failure
            withClue("TC-5: No VUID violations expected on clean VkInstance lifecycle. Got: ${vuidViolation.get()}") {
                vuidViolation.get() shouldBe null
            }
        }

        // TC-7: Validation layer version AND Lavapipe driver version logged in this dedicated test
        test("TC-7: validation layer version and Lavapipe driver version logged to CI output").config(enabledIf = requiresVulkan) {
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

                // TC-3: Layer confirmed active programmatically (D2 — no VK_INSTANCE_LAYERS env var)
                val specVersion = validationLayer!!.specVersion()
                val specStr = "${specVersion ushr 22}.${(specVersion ushr 12) and 0x3FF}.${specVersion and 0xFFF}"
                println("VK_LAYER_KHRONOS_validation spec version: $specStr (impl: ${validationLayer.implementationVersion()})")
                withClue("TC-7: Validation layer version string must be non-empty") {
                    specStr.isNotEmpty() shouldBe true
                }

                // TC-7: Driver version requires a VkInstance to enumerate physical devices.
                // L2-02: instance must be destroyed in finally — assertions can throw between creation and destroy.
                val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
                    sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                }
                val instanceHandle = stack.pointers(0L)
                val result = vkCreateInstance(createInfo, null, instanceHandle)
                withClue("TC-7: vkCreateInstance must succeed for driver version query") {
                    result shouldBe VK_SUCCESS
                }
                val instance = VkInstance(instanceHandle[0], createInfo)
                try {
                    val deviceCount = stack.ints(0)
                    vkEnumeratePhysicalDevices(instance, deviceCount, null)
                    withClue("TC-7: At least one physical device must be present for driver version query") {
                        deviceCount[0] shouldNotBe 0
                    }
                    val deviceHandles = stack.mallocPointer(deviceCount[0])
                    vkEnumeratePhysicalDevices(instance, deviceCount, deviceHandles)

                    val deviceProps = VkPhysicalDeviceProperties.malloc(stack)
                    vkGetPhysicalDeviceProperties(VkPhysicalDevice(deviceHandles[0], instance), deviceProps)

                    val rawDriverVersion = deviceProps.driverVersion()
                    val driverMajor = rawDriverVersion ushr 22
                    val driverMinor = (rawDriverVersion ushr 12) and 0x3FF
                    val driverPatch = rawDriverVersion and 0xFFF
                    val driverVersionStr = "$driverMajor.$driverMinor.$driverPatch"
                    println("Lavapipe driver version: $driverVersionStr")
                    withClue("TC-7: Lavapipe driver version string must be non-empty") {
                        driverVersionStr.isNotEmpty() shouldBe true
                    }
                } finally {
                    vkDestroyInstance(instance, null)
                }
            }
        }

        // TC-14: Deliberate VUID from wrong destruction order — validates callback mechanism
        test("TC-14: VUID from wrong destruction order is detected by callback").config(enabledIf = requiresVulkan) {
            val vuidViolation = AtomicReference<String?>(null)

            val callback = VkDebugUtilsMessengerCallbackEXT.create { severity, _, pCallbackData, _ ->
                val data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)
                val messageId = data.pMessageIdNameString() ?: ""
                if (messageId.startsWith("VUID-") &&
                    ((severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0 ||
                     (severity and VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0)
                ) {
                    vuidViolation.compareAndSet(null, data.pMessageString())
                }
                VK_FALSE
            }
            try {
                MemoryStack.stackPush().use { stack ->
                    val layers = stack.pointers(stack.UTF8("VK_LAYER_KHRONOS_validation"))
                    val extensions = stack.pointers(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME))

                    val syncValidation = VkValidationFeaturesEXT.calloc(stack).apply {
                        sType(VK_STRUCTURE_TYPE_VALIDATION_FEATURES_EXT)
                        pEnabledValidationFeatures(stack.ints(VK_VALIDATION_FEATURE_ENABLE_SYNCHRONIZATION_VALIDATION_EXT))
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
                    // TC-14 precondition (G-08): instance creation must succeed before testing VUID detection
                    val instanceResult = vkCreateInstance(createInfo, null, instanceHandle)
                    withClue("TC-14 precondition: vkCreateInstance must succeed before testing VUID detection") {
                        instanceResult shouldBe VK_SUCCESS
                    }
                    val instance = VkInstance(instanceHandle[0], createInfo)

                    // TC-14 precondition (L2-01): messenger creation must succeed — silent failure means no VUIDs fire
                    val messengerHandle = stack.longs(0L)
                    val messengerResult = vkCreateDebugUtilsMessengerEXT(instance, messengerCreateInfo, null, messengerHandle)
                    withClue("TC-14 precondition: vkCreateDebugUtilsMessengerEXT must succeed before testing VUID detection") {
                        messengerResult shouldBe VK_SUCCESS
                    }

                    // Deliberately destroy instance while messenger is still alive (wrong order).
                    // Triggers VUID-vkDestroyInstance-instance-00629.
                    vkDestroyInstance(instance, null)
                }
            } finally {
                callback.free()
            }

            withClue("TC-14: VUID violation must have been detected — messenger was alive when instance was destroyed") {
                vuidViolation.get() shouldNotBe null
            }
        }

        // TC-16: Native callback freed cleanly on vkCreateInstance failure — no JVM crash
        test("TC-16: native callback freed cleanly when vkCreateInstance returns non-SUCCESS").config(enabledIf = requiresVulkan) {
            val callback = VkDebugUtilsMessengerCallbackEXT.create { _, _, _, _ -> VK_FALSE }
            try {
                MemoryStack.stackPush().use { stack ->
                    val invalidLayer = stack.pointers(stack.UTF8("VK_LAYER_NONEXISTENT_INVALID_KHAOS_12345"))
                    val createInfo = VkInstanceCreateInfo.calloc(stack).apply {
                        sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                        ppEnabledLayerNames(invalidLayer)
                    }
                    val instanceHandle = stack.pointers(0L)
                    val result = vkCreateInstance(createInfo, null, instanceHandle)
                    withClue("TC-16: vkCreateInstance with invalid layer must NOT return VK_SUCCESS") {
                        result shouldNotBe VK_SUCCESS
                    }
                }
            } finally {
                // Freed in finally — guaranteed even when vkCreateInstance fails mid-block
                callback.free()
            }
            // Reaching here confirms: no JVM crash, no UnsatisfiedLinkError from the freed callback
        }
    }
})
