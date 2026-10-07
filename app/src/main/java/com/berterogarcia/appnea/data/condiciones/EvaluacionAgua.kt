package com.berterogarcia.appnea.data.condiciones

import kotlin.math.roundToInt

/** Semáforo para entrar al agua (buceo y apnea) con los datos de una hora. */
fun evaluarAptitudAgua(clima: HoraClima?, mar: HoraMar?): AptitudAgua {
    var nivel = Aptitud.BUENA
    val motivos = mutableListOf<String>()
    fun empeorar(a: Aptitud, motivo: String) {
        if (a.ordinal > nivel.ordinal) nivel = a
        motivos += motivo
    }

    val ola = mar?.olaM
    if (ola != null) {
        if (ola >= 1.5) empeorar(Aptitud.MALA, "Olas de ${"%.1f".format(ola)} m")
        else if (ola >= 0.8) empeorar(Aptitud.REGULAR, "Olas de ${"%.1f".format(ola)} m")
    }
    val viento = clima?.vientoKmh
    if (viento != null) {
        if (viento >= 30) empeorar(Aptitud.MALA, "Viento de ${viento.roundToInt()} km/h")
        else if (viento >= 20) empeorar(Aptitud.REGULAR, "Viento de ${viento.roundToInt()} km/h")
    }
    val rafagas = clima?.rafagasKmh
    if (rafagas != null) {
        if (rafagas >= 50) empeorar(Aptitud.MALA, "Ráfagas de ${rafagas.roundToInt()} km/h")
        else if (rafagas >= 35) empeorar(Aptitud.REGULAR, "Ráfagas de ${rafagas.roundToInt()} km/h")
    }
    val codigo = clima?.codigoClima
    if (codigo != null) {
        if (codigo >= 95) empeorar(Aptitud.MALA, "Tormenta eléctrica")
        else if (codigo in 61..67 || codigo in 80..82) empeorar(Aptitud.REGULAR, "Lluvia")
    }
    val visibilidad = clima?.visibilidadM
    if (visibilidad != null && visibilidad < 1000) {
        empeorar(Aptitud.REGULAR, "Poca visibilidad en superficie (niebla)")
    }
    val corriente = mar?.corrienteKmh
    if (corriente != null && corriente >= 2.5) {
        empeorar(Aptitud.REGULAR, "Corriente de ${"%.1f".format(corriente)} km/h")
    }
    if (motivos.isEmpty()) motivos += "Viento y mar tranquilos"
    return AptitudAgua(nivel, motivos)
}