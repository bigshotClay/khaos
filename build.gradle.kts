// NOTE: alias(libs.plugins.kotlin.multiplatform) and alias(libs.plugins.kotlin.jvm) cannot be
// declared here — buildSrc puts kotlin-gradle-plugin on the classpath and Gradle rejects a
// re-declaration ("already on classpath with unknown version"). D5 approved deviation.
// See _bmad/memory/agent-dev/MEMORY.md: "Kotlin plugin + buildSrc classpath conflict".
plugins {
    alias(libs.plugins.ksp) apply false
}

allprojects {
    group = "dev.khaos"
}
