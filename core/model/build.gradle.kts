// Modelos del dominio (Inmersion, SesionApnea, Ubicacion...) y utilidades comunes.
// Kotlin puro: no puede depender de Android.
plugins {
    alias(libs.plugins.appnea.jvm.library)
}

dependencies {
    // org.json viene con Android: acá solo para compilar (y real en los tests de JVM)
    compileOnly(libs.org.json)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
