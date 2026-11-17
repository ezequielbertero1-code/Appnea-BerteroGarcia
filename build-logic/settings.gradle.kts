// Plugins de convención: la configuración que comparten los módulos (SDK, Java, Compose...)
// vive acá una sola vez. Se incluye desde el settings.gradle.kts de la raíz.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
