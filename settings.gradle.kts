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
    // Using enableFeaturePreview() to safely declare we want to use the incubating features
    enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
    // We directly manage repositories in the settings file instead of using repositoriesMode
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "project Asteria"
include(":app")
