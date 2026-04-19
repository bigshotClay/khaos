plugins {
    kotlin("jvm")
    jacoco
}

val catalogLibs = the<VersionCatalogsExtension>().named("libs")

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    testImplementation(catalogLibs.findLibrary("kotest-runner-junit5").orElseThrow { GradleException("Catalog entry 'kotest-runner-junit5' not found in libs catalog") })
    testImplementation(catalogLibs.findLibrary("kotest-assertions-core").orElseThrow { GradleException("Catalog entry 'kotest-assertions-core' not found in libs catalog") })
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        xml.outputLocation = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml")
    }
}
