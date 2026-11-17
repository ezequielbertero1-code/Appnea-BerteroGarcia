// Implementaciones: Room, repositorios, Open-Meteo, GPS, sesión, exportación y contraseñas.
plugins {
    alias(libs.plugins.appnea.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.berterogarcia.appnea.core.data"

    defaultConfig {
        // Open-Meteo pide identificar al cliente
        val versionApp = providers.gradleProperty("appnea.versionName").get()
        buildConfigField("String", "USER_AGENT", "\"AppneaMe/$versionApp (Android)\"")
    }
    buildFeatures {
        buildConfig = true
    }
    sourceSets {
        // Los esquemas exportados de Room: los usa MigrationTestHelper en los tests instrumentados
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
}

ksp {
    // Esquema de cada versión de la base. Esta carpeta SE COMMITEA (hace falta para las migraciones)
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    // api: AppContainer (en :app) arma los repositorios con AppDatabase, que es un RoomDatabase
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
