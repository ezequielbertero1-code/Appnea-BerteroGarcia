package com.berterogarcia.appnea.ui.perfil.comun

import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.textoUi
import java.time.LocalDate

/**
 * Validaciones de formularios del perfil. Cada función devuelve el MENSAJE de error
 * (como [TextoUi], lo resuelve la pantalla) o null si el valor es válido.
 *
 * Son Kotlin puro (sin android.util.Patterns) para poder testearlas con JUnit común.
 */
object Validaciones {
    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_TELEFONO = Regex("^\\+?[0-9 ()-]+$")
    private const val MIN_DIGITOS_TELEFONO = 6
    private const val MAX_DIGITOS_TELEFONO = 15 // máximo E.164

    fun obligatorio(valor: String, nombreCampo: TextoUi): TextoUi? =
        if (valor.isBlank()) textoUi(R.string.val_ingresa, nombreCampo) else null

    fun largoMaximo(valor: String, maximo: Int): TextoUi? =
        if (valor.trim().length > maximo) textoUi(R.string.val_maximo_caracteres, maximo) else null

    fun email(valor: String): TextoUi? {
        val v = valor.trim()
        return if (v.isEmpty() || REGEX_EMAIL.matches(v)) null else textoUi(R.string.val_email_invalido)
    }

    fun telefono(valor: String, obligatorio: Boolean): TextoUi? {
        val v = valor.trim()
        if (v.isEmpty()) return if (obligatorio) textoUi(R.string.val_telefono_obligatorio) else null
        val digitos = v.count(Char::isDigit)
        return when {
            !REGEX_TELEFONO.matches(v) -> textoUi(R.string.val_telefono_caracteres)
            digitos < MIN_DIGITOS_TELEFONO -> textoUi(R.string.val_telefono_corto)
            digitos > MAX_DIGITOS_TELEFONO -> textoUi(R.string.val_telefono_largo)
            else -> null
        }
    }

    fun noFutura(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): TextoUi? =
        if (fecha != null && fecha.isAfter(hoy)) textoUi(R.string.val_fecha_futura) else null

    /** Para pares desde/hasta (emisión/vencimiento, compra/service). */
    fun noAnterior(fecha: LocalDate?, referencia: LocalDate?, mensaje: TextoUi): TextoUi? =
        if (fecha != null && referencia != null && fecha.isBefore(referencia)) mensaje else null
}
