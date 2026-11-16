package com.berterogarcia.appnea.data.condiciones

import com.berterogarcia.appnea.dominio.FuenteCondiciones
import com.berterogarcia.appnea.util.intentar
import com.berterogarcia.appnea.data.local.CondicionesRegistro
import com.berterogarcia.appnea.data.ubicacion.Ubicacion
import com.berterogarcia.appnea.data.remote.OpenMeteoService
import com.berterogarcia.appnea.data.remote.PronosticoClima
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Junta toodo: pronóstico (Open-Meteo), mar y mareas (Open-Meteo Marine) y astronomía
 * (cálculo propio).
 */
object CondicionesRepository : FuenteCondiciones {

    private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override suspend fun obtener(ubicacion: Ubicacion): Result<Condiciones> {
        val pronostico = OpenMeteoService.obtenerPronostico(ubicacion.latitud, ubicacion.longitud)
            .getOrElse { return Result.failure(it) }
        val mar = OpenMeteoService.obtenerMar(ubicacion.latitud, ubicacion.longitud).getOrNull()
        return intentar("CondicionesRepository") { armar(ubicacion, pronostico, mar.orEmpty(), mar != null) }
    }

    private fun armar(
        ubicacion: Ubicacion,
        pronostico: PronosticoClima,
        mar: List<HoraMar>,
        marDisponible: Boolean
    ): Condiciones {
        val zona = pronostico.zona
        val ahora = LocalDateTime.now(zona)
        val horaActual = ahora.truncatedTo(ChronoUnit.HOURS)
        val hoy = ahora.toLocalDate()

        val climaPorHora = pronostico.horas.associateBy { it.hora }
        val marPorHora = mar.associateBy { it.hora }

        val climaActual = climaPorHora[horaActual]
        val marActual = marPorHora[horaActual]
        val tendencia = climaActual?.presionHpa?.let { p ->
            climaPorHora[horaActual.minusHours(3)]?.presionHpa?.let { p - it }
        }

        val extremos = Mareas.extremos(mar)
        val estado = Mareas.estado(mar, extremos, ahora)

        val astroDias = (0L..6L).map {
            Astronomia.calcularDia(hoy.plusDays(it), ubicacion.latitud, ubicacion.longitud, zona)
        }

        val olaMaxPorDia = mar.groupBy { it.hora.toLocalDate() }
            .mapValues { (_, horas) -> horas.mapNotNull { it.olaM }.maxOrNull() }
        val dias = pronostico.dias
            .filter { !it.fecha.isBefore(hoy) }
            .map { it.copy(olaMaxM = olaMaxPorDia[it.fecha]) }

        return Condiciones(
            ubicacion = ubicacion,
            zona = zona,
            ahora = ahora,
            climaActual = climaActual,
            marActual = marActual,
            horasClima = pronostico.horas.filter { !it.hora.isBefore(hoy.atStartOfDay()) },
            horasMar = mar.filter { !it.hora.isBefore(hoy.atStartOfDay()) },
            dias = dias,
            mareas = extremos,
            estadoMarea = estado,
            tendenciaPresionHpa3h = tendencia,
            astroDias = astroDias,
            aptitud = evaluarAptitudAgua(climaActual, marActual),
            marDisponible = marDisponible
        )
    }

    /**
     * Condiciones para guardar junto a una inmersión.
     * Nunca falla: si no hay internet guarda solo lo astronómico (luna).
     */
    override suspend fun registroPara(latitud: Double, longitud: Double, momento: LocalDateTime): CondicionesRegistro {
        val fecha: LocalDate = momento.toLocalDate()
        val pronostico = OpenMeteoService.obtenerPronostico(latitud, longitud, fecha).getOrNull()
        val mar = OpenMeteoService.obtenerMar(latitud, longitud, fecha).getOrNull().orEmpty()
        val zona: ZoneId = pronostico?.zona ?: ZoneId.systemDefault()

        val hora = momento.truncatedTo(ChronoUnit.HOURS)
        val clima = pronostico?.horas?.firstOrNull { it.hora == hora }
        val marHora = mar.firstOrNull { it.hora == hora }
        val extremos = Mareas.extremos(mar)
        val estado = Mareas.estado(mar, extremos, momento)
        val astro = Astronomia.calcularDia(fecha, latitud, longitud, zona)

        val textoMarea = estado?.let { e ->
            val destino = e.siguiente?.let {
                " hacia ${if (it.esPleamar) "pleamar" else "bajamar"} ${it.hora.format(HH_MM)}"
            } ?: ""
            (if (e.subiendo) "Subiendo" else "Bajando") + destino
        }

        return CondicionesRegistro(
            temperaturaC = clima?.temperaturaC,
            sensacionTermicaC = clima?.sensacionTermicaC,
            humedadPorc = clima?.humedadPorc,
            presionHpa = clima?.presionHpa,
            vientoKmh = clima?.vientoKmh,
            direccionVientoGrados = clima?.direccionVientoGrados,
            rafagasKmh = clima?.rafagasKmh,
            codigoClima = clima?.codigoClima,
            nubosidadPorc = clima?.nubosidadPorc,
            olaM = marHora?.olaM,
            periodoOlaS = marHora?.periodoOlaS,
            direccionOlaGrados = marHora?.direccionOlaGrados,
            marDeFondoM = marHora?.marDeFondoM,
            temperaturaAguaC = marHora?.temperaturaAguaC,
            corrienteKmh = marHora?.corrienteKmh,
            alturaMareaM = estado?.alturaM,
            estadoMarea = textoMarea,
            faseLunar = astro.fase,
            iluminacionLunaPorc = astro.iluminacionPorc,
            aptitudAgua = if (clima != null || marHora != null) evaluarAptitudAgua(clima, marHora).nivel.texto else null,
            conDatosOnline = clima != null
        )
    }

    /** Solo lo astronómico (sin internet). Se usa en los datos de ejemplo. */
    fun registroAstronomico(latitud: Double, longitud: Double, momento: LocalDateTime): CondicionesRegistro {
        val astro = Astronomia.calcularDia(momento.toLocalDate(), latitud, longitud, ZoneId.systemDefault())
        return CondicionesRegistro(
            faseLunar = astro.fase,
            iluminacionLunaPorc = astro.iluminacionPorc,
            conDatosOnline = false
        )
    }
}