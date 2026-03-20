pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Checklist"

// Platform Framework
include(":app")

// iOS Umbrella module
include(":framework:umbrella")

// Modules
include(":framework:core:common")
include(":framework:core:data")
include(":framework:feature:task")
