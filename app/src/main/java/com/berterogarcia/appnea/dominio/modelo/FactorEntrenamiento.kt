package com.berterogarcia.appnea.dominio.modelo

import androidx.annotation.StringRes
import com.berterogarcia.appnea.R

/**
 * Factores que el usuario marca después de una tabla (para ver en Estadísticas cuáles ayudan).
 *
 * En la base se guarda el NOMBRE del enum (clave estable), no el texto visible: así se puede
 * cambiar o traducir la etiqueta sin perder las sesiones viejas. [textoV10] es como se guardaba
 * hasta la versión 10 de la base (lo usa la migración 10 → 11).
 */
enum class FactorEntrenamiento(@StringRes val etiqueta: Int, val textoV10: String) {
    DURMIO_BIEN(R.string.factor_durmio_bien, "Dormí bien"),
    COMIO_LIVIANO(R.string.factor_comio_liviano, "Comí liviano"),
    HIDRATACION(R.string.factor_hidratacion, "Buena hidratación"),
    ESTRES(R.string.factor_estres, "Estrés / ansiedad"),
    CANSANCIO(R.string.factor_cansancio, "Cansancio físico"),
    ACOMPANADO(R.string.factor_acompanado, "Entrené acompañado");

    companion object {
        /** Clave guardada -> factor. Una clave desconocida (versión futura) se ignora. */
        fun desdeClave(clave: String): FactorEntrenamiento? = entries.firstOrNull { it.name == clave }
    }
}
