package com.berterogarcia.appnea.ui.perfil.comun

import java.time.LocalDate

/**
 * Validaciones de formularios del perfil. Cada función devuelve el MENSAJE de error
 * o null si el valor es válido, así se arma fácil el Map<Campo, String> de errores.
 *
 * Son Kotlin puro (sin android.util.Patterns) para poder testearlas con JUnit común.
 */
object Validaciones {
    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_TELEFONO = Regex("^\\+?[0-9 ()-]+$")
    private const val MIN_DIGITOS_TELEFONO = 6
    private const val MAX_DIGITOS_TELEFONO = 15 // máximo E.164

    fun obligatorio(valor: String, nombreCampo: String): String? =
        if (valor.isBlank()) "Ingresá $nombreCampo." else null

    fun largoMaximo(valor: String, maximo: Int): String? =
        if (valor.trim().length > maximo) "Máximo $maximo caracteres." else null

    fun email(valor: String): String? {
        val v = valor.trim()
        return if (v.isEmpty() || REGEX_EMAIL.matches(v)) null else "El email no es válido."
    }

    fun telefono(valor: String, obligatorio: Boolean): String? {
        val v = valor.trim()
        if (v.isEmpty()) return if (obligatorio) "Ingresá un teléfono." else null
        val digitos = v.count(Char::isDigit)
        return when {
            !REGEX_TELEFONO.matches(v) -> "Solo números, espacios, +, - y paréntesis."
            digitos < MIN_DIGITOS_TELEFONO -> "El teléfono es muy corto."
            digitos > MAX_DIGITOS_TELEFONO -> "El teléfono es muy largo."
            else -> null
        }
    }

    fun noFutura(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): String? =
        if (fecha != null && fecha.isAfter(hoy)) "La fecha no puede ser futura." else null

    /** Para pares desde/hasta (emisión/vencimiento, compra/service). */
    fun noAnterior(fecha: LocalDate?, referencia: LocalDate?, mensaje: String): String? =
        if (fecha != null && referencia != null && fecha.isBefore(referencia)) mensaje else null
}
