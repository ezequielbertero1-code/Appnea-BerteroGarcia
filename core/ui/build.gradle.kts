// Tema, componentes compartidos (botones, campos, listas, barras...) y etiquetas de los datos.
plugins {
    alias(libs.plugins.appnea.android.library)
    alias(libs.plugins.appnea.android.compose)
}

android {
    namespace = "com.berterogarcia.appnea.core.ui"
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
