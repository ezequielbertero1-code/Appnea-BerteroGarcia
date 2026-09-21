package com.berterogarcia.appnea.data.local

/**
 * Una pieza (o grupo de piezas de la misma especie) pescada en una salida de pesca submarina.
 */
data class CapturaPesca(
    val especie: String,
    val cantidad: Int,
    val pesoKg: Double? = null
)
