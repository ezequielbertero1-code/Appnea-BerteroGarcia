package com.berterogarcia.appnea.dominio.modelo

/**
 * Tipo de inmersión. Se guarda por NOMBRE (ver Converters): si algún día se renombra una
 * constante, los registros viejos se leen como BUCEO en vez de crashear.
 */
enum class TipoInmersion(val etiqueta: String) {
    BUCEO("Buceo"),
    APNEA("Apnea");

    companion object {
        fun desdeNombre(nombre: String?): TipoInmersion =
            entries.firstOrNull { it.name == nombre } ?: BUCEO
    }
}