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

// Feature modules. Each depends only on :framework and is registered in
// app/src/main/java/edu/sdwu/teaching/modules/InstalledModules.kt.
include(":modules:study-timer")
include(":modules:lesson-formatter")
