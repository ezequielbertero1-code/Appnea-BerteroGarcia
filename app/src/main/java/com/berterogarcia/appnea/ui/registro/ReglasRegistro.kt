package com.berterogarcia.appnea.ui.registro

/** Qué tan fuerte es una contraseña (solo informativo, se muestra con una barra). */
enum class FuerzaPassword(val etiqueta: String, val nivel: Int) {
    VACIA("", 0),
    DEBIL("Débil", 1),
    MEDIA("Media", 2),
    FUERTE("Fuerte", 3)
}

/**
 * Reglas del registro. Kotlin puro (sin Android) para poder testearlas con JUnit común.
 * Cada validación devuelve el mensaje de error, o null si está bien.
 */
object ReglasRegistro {
    const val PASSWORD_MIN = 8
    private const val PASSWORD_MAX = 64
    private val REGEX_USUARIO = Regex("^[A-Za-z0-9._-]{3,20}$")

    fun usuario(valor: String): String? = when {
        valor.isBlank() -> "Elegí un nombre de usuario."
        !REGEX_USUARIO.matches(valor) -> "De 3 a 20 caracteres: letras, números, punto, guion o guion bajo (sin espacios)."
        else -> null
    }

    fun password(valor: String): String? = when {
        valor.isEmpty() -> "Elegí una contraseña."
        valor.length < PASSWORD_MIN -> "Tiene que tener al menos $PASSWORD_MIN caracteres."
        valor.length > PASSWORD_MAX -> "Máximo $PASSWORD_MAX caracteres."
        !valor.any { it.isLetter() } || !valor.any { it.isDigit() } -> "Tiene que tener letras y números."
        else -> null
    }

    fun confirmacion(password: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> "Repetí la contraseña."
        confirmacion != password -> "Las contraseñas no coinciden."
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