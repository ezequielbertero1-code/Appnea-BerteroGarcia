package com.berterogarcia.appnea.ui.registro

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi

/** Qué tan fuerte es una contraseña (solo informativo, se muestra con una barra). */
/** Fuerza de la contraseña. El nombre que se muestra sale de strings.xml (ver Etiquetas.kt). */
enum class FuerzaPassword(val nivel: Int) {
    VACIA(0),
    DEBIL(1),
    MEDIA(2),
    FUERTE(3)
}

/**
 * Reglas del registro. Kotlin puro (sin Android) para poder testearlas con JUnit común.
 * Cada validación devuelve el mensaje de error, o null si está bien.
 */
object ReglasRegistro {
    const val PASSWORD_MIN = 8
    private const val PASSWORD_MAX = 64
    private val REGEX_USUARIO = Regex("^[A-Za-z0-9._-]{3,20}$")

    fun usuario(valor: String): TextoUi? = when {
        valor.isBlank() -> textoUi(R.string.val_usuario_vacio)
        !REGEX_USUARIO.matches(valor) -> textoUi(R.string.val_usuario_formato)
        else -> null
    }

    fun password(valor: String): TextoUi? = when {
        valor.isEmpty() -> textoUi(R.string.val_password_vacia)
        valor.length < PASSWORD_MIN -> textoUi(R.string.val_password_minimo, PASSWORD_MIN)
        valor.length > PASSWORD_MAX -> textoUi(R.string.val_maximo_caracteres, PASSWORD_MAX)
        !valor.any { it.isLetter() } || !valor.any { it.isDigit() } -> textoUi(R.string.val_password_letras_numeros)
        else -> null
    }

    fun confirmacion(password: String, confirmacion: String): TextoUi? = when {
        confirmacion.isEmpty() -> textoUi(R.string.val_confirmacion_vacia)
        confirmacion != password -> textoUi(R.string.val_confirmacion_distinta)
        else -> null
    }

    /** Puntaje: largo (8+ y 12+), mayúsculas y minúsculas, números y símbolos. Menos de 8 = débil. */
    fun fuerza(password: String): FuerzaPassword {
        if (password.isEmpty()) return FuerzaPassword.VACIA
        var puntos = 0
        if (password.length >= PASSWORD_MIN) puntos++
        if (password.length >= 12) puntos++
        if (password.any { it.isLowerCase() } && password.any { it.isUpperCase() }) puntos++
        if (password.any { it.isDigit() }) puntos++
        if (password.any { !it.isLetterOrDigit() }) puntos++
        return when {
            password.length < PASSWORD_MIN || puntos <= 2 -> FuerzaPassword.DEBIL
            puntos == 3 -> FuerzaPassword.MEDIA
            else -> FuerzaPassword.FUERTE
        }
    }
}