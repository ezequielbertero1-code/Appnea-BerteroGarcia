package com.berterogarcia.appnea.ui.registro

import com.berterogarcia.appnea.ui.components.pluralUi
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.FuerzaPassword
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi

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
        valor.length < PASSWORD_MIN -> pluralUi(R.plurals.val_password_minimo, PASSWORD_MIN, PASSWORD_MIN)
        valor.length > PASSWORD_MAX -> pluralUi(R.plurals.val_maximo_caracteres, PASSWORD_MAX, PASSWORD_MAX)
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
