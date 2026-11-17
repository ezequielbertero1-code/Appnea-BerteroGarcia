import java.util.Properties

plugins {
    // SDK, Java y desugaring: build-logic/ (igual para todos los módulos)
    alias(libs.plugins.appnea.android.application)
    alias(libs.plugins.appnea.android.compose)
    alias(libs.plugins.kotlin.serialization) // rutas tipadas de Navigation (@Serializable)
}

// Client ID "web" de Google Cloud (se guarda en local.properties: client_web=...). Sin él, el botón de Google avisa que falta configurar.
val clientWebGoogle: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("client_web", "").trim()

android {
    namespace = "com.berterogarcia.appnea"

    defaultConfig {
        applicationId = "com.berterogarcia.appnea"
        // En gradle.properties (appnea.versionCode / appnea.versionName)
        versionCode = providers.gradleProperty("appnea.versionCode").get().toInt()
        versionName = providers.gradleProperty("appnea.versionName").get()

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$clientWebGoogle\"")
    }

    buildTypes {
        release {
            // R8: reduce y ofusca el código y saca los recursos que no se usan (APK más chico)
            optimization {
                enable = true
            }
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint {
        // Los problemas que ya existían quedan en lint-baseline.xml: falla solo por problemas NUEVOS
        baseline = file("lint-baseline.xml")
        abortOnError = true
        checkDependencies = false
        // Traducciones: la app es solo en español por ahora
        disable += "MissingTranslation"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.core)
    testImplementation(project(":core:testing"))
    testImplementation(libs.turbine)
    testImplementation(libs.org.json) // org.json real: el de android.jar en los tests de JVM es un stub
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(project(":core:testing"))
    // Rutas tipadas de Navigation (@Serializable)
    implementation(libs.kotlinx.serialization.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.maplibre.android)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)
}
