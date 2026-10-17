package com.berterogarcia.appnea.data.remote

/** Convierte grados (0-360) a un punto cardinal en español, para viento y oleaje. */
fun direccionCardinal(grados: Double): String {
    val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val indice = (((grados % 360) + 360) % 360 / 45.0).let { Math.round(it).toInt() % 8 }
    return puntos[indice]
}
