// Interfaces de repositorios, puertos (ubicación, condiciones...), sesión y casos de uso.
// Kotlin puro: los ViewModels y los tests dependen de esto, no de Room ni de Android.
plugins {
    alias(libs.plugins.appnea.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.org.json)
}
