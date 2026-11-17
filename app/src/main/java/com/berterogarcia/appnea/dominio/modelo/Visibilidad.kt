package com.berterogarcia.appnea.dominio.modelo

/**
 * Visibilidad subjetiva del agua. Se guarda por NOMBRE (ver Converters): si algún día se
 * renombra una constante, el registro se lee sin visibilidad (null) en vez de crashear.
 */
enum class Visibilidad {
    NULA,
    MALA,
    BUENA,
    MUY_BUENA,
    EXCELENTE;

    companion object {
        fun desdeNombre(nombre: String?): Visibilidad? = entries.firstOrNull { it.name == nombre }
    }
}
