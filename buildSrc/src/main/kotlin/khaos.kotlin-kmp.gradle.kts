// TODO(issue-N): wire JaCoCo for jvmTest task in KMP modules — deferred from Issue #3 (design A6)
plugins {
    kotlin("multiplatform")
}

val catalogLibs = the<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)
    jvm()
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(catalogLibs.findLibrary("kotest-runner-junit5").orElseThrow { GradleException("Catalog entry 'kotest-runner-junit5' not found in libs catalog") })
            implementation(catalogLibs.findLibrary("kotest-assertions-core").orElseThrow { GradleException("Catalog entry 'kotest-assertions-core' not found in libs catalog") })
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
