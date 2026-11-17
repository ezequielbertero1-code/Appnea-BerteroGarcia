package com.berterogarcia.appnea.dominio.modelo

/**
 * Factores que el usuario marca después de una tabla (para ver en Estadísticas cuáles ayudan).
 *
 * En la base se guarda el NOMBRE del enum (clave estable), no el texto visible: así se puede
 * cambiar o traducir la etiqueta sin perder las sesiones viejas (el texto visible está en
 * ui/components/Etiquetas.kt). [textoV10] es como se guardaba
 * hasta la versión 10 de la base (lo usa la migración 10 → 11).
 */
enum class FactorEntrenamiento(val textoV10: String) {
    DURMIO_BIEN("Dormí bien"),
    COMIO_LIVIANO("Comí liviano"),
    HIDRATACION("Buena hidratación"),
    ESTRES("Estrés / ansiedad"),
    CANSANCIO("Cansancio físico"),
    ACOMPANADO("Entrené acompañado");

    companion object {
        /** Clave guardada -> factor. Una clave desconocida (versión futura) se ignora. */
        fun desdeClave(clave: String): FactorEntrenamiento? = entries.firstOrNull { it.name == clave }
    }
}
