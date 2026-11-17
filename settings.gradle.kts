pluginManagement {
    // Plugins de convención (appnea.android.library, appnea.jvm.library...)
    includeBuild("build-logic")
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
include(":core:model")    // modelos, Kotlin puro
include(":core:domain")   // interfaces y casos de uso, Kotlin puro
include(":core:data")     // Room, repositorios, red, GPS
include(":core:ui")       // tema y componentes de Compose
include(":core:testing")  // fakes para los tests
