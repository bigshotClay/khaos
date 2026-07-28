plugins { id("khaos.kotlin-kmp") }

// Native classifier for the LWJGL runtime artifacts, resolved from the host running the build.
// The JVM tests load the native Vulkan loader in-process, so the classifier must match the
// machine executing them rather than a fixed target.
val lwjglNatives = when {
    System.getProperty("os.name").startsWith("Windows") -> "natives-windows"
    System.getProperty("os.name") == "Mac OS X" ->
        if (System.getProperty("os.arch") == "aarch64") "natives-macos-arm64" else "natives-macos"
    else -> "natives-linux"
}

// Several specs assert on repo-level files (ci.yml) and on their own source text, resolving them
// through System.getProperty("user.dir"). Gradle defaults that to the module directory, so pin it
// to the repo root and let the specs address module files by explicit relative path.
tasks.withType<Test>().configureEach {
    workingDir = rootProject.projectDir
}

kotlin {
    sourceSets {
        jvmTest.dependencies {
            implementation(kotlin("reflect"))   // kotlin.reflect.full.* in HandleSpec
            implementation(libs.kctfork.core)
            // Must come with kctfork — see the catalog note on kotlin-compiler-embeddable.
            implementation(libs.kotlin.compiler.embeddable)
            implementation(project.dependencies.platform(libs.lwjgl.bom))
            implementation(libs.lwjgl.core)
            implementation(libs.lwjgl.vulkan)
            // String notation, not variantOf(): that helper is on DependencyHandler and is not
            // in scope inside a KMP source-set dependencies block. Version is left empty so the
            // LWJGL BOM above supplies it. See D6 in planning/designs/issue-3-design.md.
            runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
        }
    }
}
