// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
}

// Análisis estático de Kotlin sobre todos los módulos. Como con lint, lo que ya había queda en
// el baseline y ./gradlew detekt falla solo por problemas NUEVOS.
// Regenerar el baseline: ./gradlew detektBaseline
detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("config/detekt/detekt.yml"))
    baseline = file("config/detekt/baseline.xml")
    source.setFrom(
        listOf("app", "core/model", "core/domain", "core/data", "core/ui", "core/testing").flatMap { modulo ->
            listOf("main", "test", "androidTest").map { "$modulo/src/$it/java" }
        }
    )
}
