import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // rutas tipadas de Navigation (@Serializable)
    alias(libs.plugins.ksp)
}

// Client ID "web" de Google Cloud (se guarda en local.properties: client_web=...). Sin él, el botón de Google avisa que falta configurar.
val clientWebGoogle: String = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}.getProperty("client_web", "").trim()

android {
    namespace = "com.berterogarcia.appnea"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.berterogarcia.appnea"
        minSdk = 24
        targetSdk = 37
        // Subir versionCode en cada APK que se distribuya (Android no instala uno menor encima)
        versionCode = 2
        versionName = "1.1.0"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$clientWebGoogle\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        // android.util.Log y similares devuelven valores por defecto en los tests de JVM (no fallan)
        unitTests.isReturnDefaultValues = true
    }
    sourceSets {
        // Los esquemas exportados de Room: los usa MigrationTestHelper en los tests instrumentados
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.core)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json) // org.json real: el de android.jar en los tests de JVM es un stub
    androidTestImplementation(libs.androidx.room.testing)
    // Rutas tipadas. Va en 1.8.1 porque room-testing (MigrationTestHelper) lo necesita y el APK de
    // test usa la versión de la app; Navigation trae 1.7.3 y 1.8 es compatible hacia atrás.
    implementation(libs.kotlinx.serialization.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}