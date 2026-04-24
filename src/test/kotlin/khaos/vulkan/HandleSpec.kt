@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package khaos.vulkan

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.lang.reflect.Modifier
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

class HandleSpec : FreeSpec({

    val allInlineHandleClasses = listOf(
        InstanceHandle::class,
        DeviceHandle::class,
        PhysicalDeviceHandle::class,
        QueueHandle::class,
        SurfaceHandle::class,
        SwapchainHandle::class,
        ImageHandle::class,
        ImageViewHandle::class,
        RenderPassHandle::class,
        FramebufferHandle::class,
        CommandPoolHandle::class,
        CommandBufferHandle::class,
        SemaphoreHandle::class,
        FenceHandle::class,
        PipelineLayoutHandle::class,
        DescriptorSetLayoutHandle::class,
        BufferHandle::class,
        DeviceMemoryHandle::class,
    )

    // Tests AC1
    "AC1-P1: ImageHandle accepted where ImageHandle expected — compiles" {
        fun useImageHandle(h: ImageHandle): ImageHandle = h
        val result = useImageHandle(ImageHandle.NULL)
        result shouldBe ImageHandle.NULL
    }

    "AC1-P2: SemaphoreHandle accepted where SemaphoreHandle expected — compiles" {
        fun useSemaphoreHandle(h: SemaphoreHandle): SemaphoreHandle = h
        val result = useSemaphoreHandle(SemaphoreHandle.NULL)
        result shouldBe SemaphoreHandle.NULL
    }

    "AC1-N1 (negative): ImageHandle passed where ImageViewHandle expected — fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("WrongHandle.kt", """
                import khaos.vulkan.ImageHandle
                import khaos.vulkan.ImageViewHandle
                fun useImageView(h: ImageViewHandle): ImageViewHandle = h
                val bad = useImageView(ImageHandle.NULL)
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    "AC1-N2 (negative): DeviceHandle passed where InstanceHandle expected — fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("WrongHandle2.kt", """
                import khaos.vulkan.DeviceHandle
                import khaos.vulkan.InstanceHandle
                fun useInstance(h: InstanceHandle): InstanceHandle = h
                val bad = useInstance(DeviceHandle.NULL)
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC2
    "AC2-P1: all 18 Long-wrapping handle types report KClass.isValue == true" {
        for (klass in allInlineHandleClasses) {
            klass.isValue shouldBe true
        }
    }

    "AC2-P2: all 18 Long-wrapping handle types have exactly one Kotlin member property" {
        for (klass in allInlineHandleClasses) {
            klass.memberProperties.count() shouldBe 1
        }
    }

    "AC2-N1 (negative): PipelineHandle.isValue returns false" {
        PipelineHandle::class.isValue shouldBe false
    }

    // Tests AC3
    "AC3-P1: no public method on any handle class has a Long-typed parameter" {
        val allClasses = allInlineHandleClasses.map { it.java } + listOf(PipelineHandle::class.java)
        for (klass in allClasses) {
            // Exclude: Object-inherited infrastructure, JVM-synthetic methods, property
            // accessors (get*/is*), and data class infrastructure (copy, componentN).
            // These carry Long due to JVM backing representation, not Vulkan API design.
            val apiMethods = klass.declaredMethods.filter { m ->
                Modifier.isPublic(m.modifiers) &&
                !m.isSynthetic &&
                m.declaringClass != Object::class.java &&
                !m.name.startsWith("get") &&
                !m.name.startsWith("is") &&
                m.name != "copy" &&
                !m.name.startsWith("component")
            }
            for (method in apiMethods) {
                for (paramType in method.parameterTypes) {
                    paramType shouldNotBe Long::class.javaPrimitiveType
                    paramType shouldNotBe Long::class.javaObjectType
                }
            }
        }
    }

    "AC3-P2: no public method on any handle class has a Long return type" {
        val allClasses = allInlineHandleClasses.map { it.java } + listOf(PipelineHandle::class.java)
        for (klass in allClasses) {
            // Same scope restriction as AC3-P1: exclude infrastructure methods.
            val apiMethods = klass.declaredMethods.filter { m ->
                Modifier.isPublic(m.modifiers) &&
                !m.isSynthetic &&
                m.declaringClass != Object::class.java &&
                !m.name.startsWith("get") &&
                !m.name.startsWith("is") &&
                m.name != "copy" &&
                !m.name.startsWith("component")
            }
            for (method in apiMethods) {
                method.returnType shouldNotBe Long::class.javaPrimitiveType
                method.returnType shouldNotBe Long::class.javaObjectType
            }
        }
    }

    "AC3-N1 (negative): passing raw Long where InstanceHandle expected fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("RawLong.kt", """
                import khaos.vulkan.InstanceHandle
                fun useInstance(h: InstanceHandle): InstanceHandle = h
                val bad = useInstance(0L)
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    // Tests AC4
    "AC4-P1: InstanceHandle.NULL has handle value 0L" {
        InstanceHandle.NULL.handle shouldBe 0L
    }

    "AC4-P2: DeviceHandle.NULL has handle value 0L" {
        DeviceHandle.NULL.handle shouldBe 0L
    }

    "AC4-P3: all 18 Long-wrapping handle types expose a NULL companion constant with handle == 0L" {
        for (klass in allInlineHandleClasses) {
            val companion = klass.companionObjectInstance
                ?: error("${klass.simpleName} has no companion object")
            val companionKlass = klass.companionObject
                ?: error("${klass.simpleName} has no companion KClass")
            val nullProp = companionKlass.memberProperties
                .first { it.name == "NULL" }
            val nullValue = nullProp.getter.call(companion)
                ?: error("${klass.simpleName}.NULL returned null")
            val handleProp = klass.memberProperties.first { it.name == "handle" }
            val handleValue = handleProp.getter.call(nullValue)
            handleValue shouldBe 0L
        }
    }

    "AC4-N1 (negative): directly constructing InstanceHandle(0L) fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("DirectConstruct.kt", """
                import khaos.vulkan.InstanceHandle
                val h = InstanceHandle(0L)
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }

    "AC4-N2 (negative): all 18 Long-wrapping handle types have no public primary constructor" {
        for (klass in allInlineHandleClasses) {
            val ctors = klass.java.declaredConstructors
            for (ctor in ctors) {
                Modifier.isPublic(ctor.modifiers) shouldBe false
            }
        }
    }

    // Tests AC5
    "AC5-P1: PipelineHandle construction with handle and reusable succeeds" {
        val p = PipelineHandle(handle = 0x1000L, reusable = true)
        p shouldNotBe null
    }

    "AC5-P2: PipelineHandle.handle returns the Long value passed at construction" {
        val p = PipelineHandle(handle = 0x2000L, reusable = false)
        p.handle shouldBe 0x2000L
    }

    "AC5-P3: PipelineHandle.reusable returns the Boolean value passed at construction" {
        PipelineHandle(handle = 0L, reusable = true).reusable shouldBe true
        PipelineHandle(handle = 0L, reusable = false).reusable shouldBe false
    }

    "AC5-P4: PipelineHandle source file contains KDoc explaining bit-packing rejection and JNI rationale" {
        val projectRoot = System.getProperty("user.dir")
        val sourceFile = java.io.File("$projectRoot/src/main/kotlin/khaos/vulkan/PipelineHandle.kt")
        val content = sourceFile.readText().lowercase()
        val hasBitPack = "bit-pack" in content || "bit pack" in content
        val hasJni = "jni" in content
        val hasVk3 = "vk-3" in content || "vk3" in content
        hasBitPack shouldBe true
        hasJni shouldBe true
        hasVk3 shouldBe true
    }

    "AC5-N1 (negative): PipelineHandle.isValue returns false — not @JvmInline" {
        PipelineHandle::class.isValue shouldBe false
    }

    "AC5-N2 (negative): passing PipelineHandle where ImageHandle expected fails to compile" {
        val compilation = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("PipelineAsImage.kt", """
                import khaos.vulkan.ImageHandle
                import khaos.vulkan.PipelineHandle
                fun useImage(h: ImageHandle): ImageHandle = h
                val bad = useImage(PipelineHandle(handle = 0L, reusable = false))
            """))
            inheritClassPath = true
        }
        compilation.compile().exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
    }
})
