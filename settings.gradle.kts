rootProject.name = "khaos"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":khaos-core",
    ":khaos-memory",
    ":khaos-shader",
    ":khaos-graph",
    ":khaos-cmd",
    ":khaos-test-harness",
)
