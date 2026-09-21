package com.berterogarcia.appnea.data.remote

/** Clima atmosférico actual en un punto, tal como lo devuelve Open-Meteo. */
data class ClimaActual(
    val temperaturaC: Double,
    val sensacionTermicaC: Double?,
    val humedadPorc: Int?,
    val vientoKmh: Double,
    val direccionVientoGrados: Double,
    val codigoClima: Int
)

/** Estado del mar actual en un punto (API marina de Open-Meteo). No todos los puntos lo tienen. */
data class MarActual(
    val alturaOlaM: Double,
    val direccionOlaGrados: Double?,
    val periodoOlaS: Double?
)

/**
 * Traduce el "weather code" (estándar WMO, el que usa Open-Meteo) a una descripción corta
 * en español. No cubre todos los códigos posibles, solo los más comunes.
 */
fun descripcionClima(codigo: Int): String = when (codigo) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55 -> "Llovizna"
    56, 57 -> "Llovizna helada"
    61, 63, 65 -> "Lluvia"
    66, 67 -> "Lluvia helada"
    71, 73, 75 -> "Nieve"
    77 -> "Granizo fino"
    80, 81, 82 -> "Chubascos"
    85, 86 -> "Chubascos de nieve"
    95 -> "Tormenta"
    96, 99 -> "Tormenta con granizo"
    else -> "Sin datos"
}

/** Convierte grados (0-360) a un punto cardinal en español, para viento y oleaje. */
fun direccionCardinal(grados: Double): String {
    val puntos = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val indice = (((grados % 360) + 360) % 360 / 45.0).let { Math.round(it).toInt() % 8 }
    return puntos[indice]
}
