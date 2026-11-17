package com.berterogarcia.appnea.dominio.modelo

/**
 * Resultado simple de una ubicación: latitud, longitud y (opcionalmente) un nombre legible.
 */
data class Ubicacion(
    val latitud: Double,
    val longitud: Double,
    val nombre: String? = null
)

/** Comodoro Rivadavia, Chubut. Se usa como ubicación por defecto para el Clima. */
val UBICACION_COMODORO_RIVADAVIA = Ubicacion(
    latitud = -45.8641,
    longitud = -67.4966,
    nombre = "Comodoro Rivadavia"
)
