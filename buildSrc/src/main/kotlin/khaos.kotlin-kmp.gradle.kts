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
            implementation(catalogLibs.findLibrary("kotest-runner-junit5").get())
            implementation(catalogLibs.findLibrary("kotest-assertions-core").get())
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
