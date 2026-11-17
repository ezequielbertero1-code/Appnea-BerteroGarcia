package com.berterogarcia.appnea.ui.components

/**
 * Qué tan fuerte es una contraseña (solo informativo, se muestra con una barra).
 * La calcula ReglasRegistro; el nombre que se muestra sale de strings.xml (ver Etiquetas.kt).
 */
enum class FuerzaPassword(val nivel: Int) {
    VACIA(0),
    DEBIL(1),
    MEDIA(2),
    FUERTE(3)
}
