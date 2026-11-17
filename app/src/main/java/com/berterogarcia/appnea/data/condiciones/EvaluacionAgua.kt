package com.berterogarcia.appnea.data.condiciones

import com.berterogarcia.appnea.dominio.modelo.Aptitud
import com.berterogarcia.appnea.dominio.modelo.AptitudAgua
import com.berterogarcia.appnea.dominio.modelo.HoraClima
import com.berterogarcia.appnea.dominio.modelo.HoraMar
import com.berterogarcia.appnea.dominio.modelo.MotivoAptitud
import com.berterogarcia.appnea.dominio.modelo.TipoMotivo
import kotlin.math.roundToInt

/** Semáforo para entrar al agua (buceo y apnea) con los datos de una hora. */
fun evaluarAptitudAgua(clima: HoraClima?, mar: HoraMar?): AptitudAgua {
    var nivel = Aptitud.BUENA
    val motivos = mutableListOf<MotivoAptitud>()
    fun empeorar(a: Aptitud, motivo: MotivoAptitud) {
        if (a.ordinal > nivel.ordinal) nivel = a
        motivos += motivo
    }

    val ola = mar?.olaM
    if (ola != null) {
        val motivo = MotivoAptitud(TipoMotivo.OLAS, "%.1f".format(ola))
        if (ola >= 1.5) empeorar(Aptitud.MALA, motivo)
        else if (ola >= 0.8) empeorar(Aptitud.REGULAR, motivo)
    }
    val viento = clima?.vientoKmh
    if (viento != null) {
        val motivo = MotivoAptitud(TipoMotivo.VIENTO, viento.roundToInt().toString())
        if (viento >= 30) empeorar(Aptitud.MALA, motivo)
        else if (viento >= 20) empeorar(Aptitud.REGULAR, motivo)
    }
    val rafagas = clima?.rafagasKmh
    if (rafagas != null) {
        val motivo = MotivoAptitud(TipoMotivo.RAFAGAS, rafagas.roundToInt().toString())
        if (rafagas >= 50) empeorar(Aptitud.MALA, motivo)
        else if (rafagas >= 35) empeorar(Aptitud.REGULAR, motivo)
    }
    val codigo = clima?.codigoClima
    if (codigo != null) {
        if (codigo >= 95) empeorar(Aptitud.MALA, MotivoAptitud(TipoMotivo.TORMENTA))
        else if (codigo in 61..67 || codigo in 80..82) empeorar(Aptitud.REGULAR, MotivoAptitud(TipoMotivo.LLUVIA))
    }
    val visibilidad = clima?.visibilidadM
    if (visibilidad != null && visibilidad < 1000) {
        empeorar(Aptitud.REGULAR, MotivoAptitud(TipoMotivo.NIEBLA))
    }
    val corriente = mar?.corrienteKmh
    if (corriente != null && corriente >= 2.5) {
        empeorar(Aptitud.REGULAR, MotivoAptitud(TipoMotivo.CORRIENTE, "%.1f".format(corriente)))
    }
    if (motivos.isEmpty()) motivos += MotivoAptitud(TipoMotivo.TRANQUILO)
    return AptitudAgua(nivel, motivos)
}
