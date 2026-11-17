// Fakes de los repositorios y puertos, datos de ejemplo y la regla del dispatcher Main.
// La usan los tests de JVM de la app y de los módulos, y los instrumentados.
plugins {
    alias(libs.plugins.appnea.jvm.library)
}

dependencies {
    api(project(":core:domain"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
