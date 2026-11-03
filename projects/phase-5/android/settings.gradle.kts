pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TeachingFramework"
include(":app")

// The framework: contracts every module implements (no app internals, no Room, no Hilt).
include(":framework")

// Feature modules: EVERY folder in modules/ that contains a build.gradle.kts is included
// automatically. Adding a module = adding a folder (use ./gradlew newModule). No list to edit.
file("modules").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":modules:${it.name}") }
