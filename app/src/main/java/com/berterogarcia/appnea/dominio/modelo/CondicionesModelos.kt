package com.berterogarcia.appnea.dominio.modelo

import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Una hora del pronóstico atmosférico (Open-Meteo Forecast). Cualquier valor puede faltar. */
data class HoraClima(
    val hora: LocalDateTime,
    val temperaturaC: Double?,
    val sensacionTermicaC: Double?,
    val humedadPorc: Int?,
    val puntoRocioC: Double?,
    val probabilidadLluviaPorc: Int?,
    val precipitacionMm: Double?,
    val codigoClima: Int?,
    val presionHpa: Double?,
    val nubosidadPorc: Int?,
    val visibilidadM: Double?,
    val vientoKmh: Double?,
    val direccionVientoGrados: Double?,
    val rafagasKmh: Double?,
    val indiceUv: Double?,
    val esDeDia: Boolean?
)

/** Una hora del pronóstico marino (Open-Meteo Marine). No todos los puntos tienen datos. */
data class HoraMar(
    val hora: LocalDateTime,
    val olaM: Double?,
    val direccionOlaGrados: Double?,
    val periodoOlaS: Double?,
    val olaVientoM: Double?,
    val marDeFondoM: Double?,
    val direccionMarDeFondoGrados: Double?,
    val periodoMarDeFondoS: Double?,
    val nivelMarM: Double?,
    val temperaturaAguaC: Double?,
    val corrienteKmh: Double?,
    val direccionCorrienteGrados: Double?
)

/** Resumen diario del pronóstico. */
data class DiaClima(
    val fecha: LocalDate,
    val codigoClima: Int?,
    val maximaC: Double?,
    val minimaC: Double?,
    val lluviaMm: Double?,
    val probabilidadLluviaPorc: Int?,
    val vientoMaxKmh: Double?,
    val rafagasMaxKmh: Double?,
    val direccionVientoGrados: Double?,
    val uvMax: Double?,
    val olaMaxM: Double?
)

/** Pleamar o bajamar. */
data class ExtremoMarea(
    val hora: LocalDateTime,
    val alturaM: Double,
    val esPleamar: Boolean
)

data class EstadoMarea(
    val alturaM: Double,
    val subiendo: Boolean,
    val anterior: ExtremoMarea?,
    val siguiente: ExtremoMarea?
)

data class DiaAstro(
    val fecha: LocalDate,
    val amanecer: LocalDateTime?,
    val atardecer: LocalDateTime?,
    val inicioCrepusculo: LocalDateTime?,
    val finCrepusculo: LocalDateTime?,
    val mediodiaSolar: LocalDateTime?,
    val alturaMaximaSol: Double,
    val salidaLuna: LocalDateTime?,
    val puestaLuna: LocalDateTime?,
    val transitoLuna: LocalDateTime?,
    val antitransitoLuna: LocalDateTime?,
    val elongacion: Double,
    val iluminacionPorc: Int,
    val edadLunaDias: Double,
    val fase: String,
    val proximaLunaNueva: LocalDate,
    val proximaLunaLlena: LocalDate,
)

/** [texto] se guarda en CondicionesRegistro.aptitudAgua: es una clave, no se traduce (ver Etiquetas.kt). */
enum class Aptitud(val texto: String) {
    BUENA("Buenas"),
    REGULAR("Regulares"),
    MALA("Malas")
}

/** Semáforo de condiciones para entrar al agua (buceo y apnea). */
data class AptitudAgua(
    val nivel: Aptitud,
    val motivos: List<MotivoAptitud>
)

/** Qué empeoró el semáforo. El texto lo arma la pantalla (strings.xml); acá solo va el dato. */
enum class TipoMotivo { OLAS, VIENTO, RAFAGAS, TORMENTA, LLUVIA, NIEBLA, CORRIENTE, TRANQUILO }

/** Un motivo del semáforo con su [valor] ya formateado ("1,2" m, "25" km/h), si corresponde. */
data class MotivoAptitud(val tipo: TipoMotivo, val valor: String? = null)

/** Todo lo que muestra la pantalla de Condiciones para una ubicación. */
data class Condiciones(
    val ubicacion: Ubicacion,
    val zona: ZoneId,
    val ahora: LocalDateTime,
    val climaActual: HoraClima?,
    val marActual: HoraMar?,
    val horasClima: List<HoraClima>,
    val horasMar: List<HoraMar>,
    val dias: List<DiaClima>,
    val mareas: List<ExtremoMarea>,
    val estadoMarea: EstadoMarea?,
    val tendenciaPresionHpa3h: Double?,
    val astroDias: List<DiaAstro>,
    val aptitud: AptitudAgua,
    val marDisponible: Boolean
) {
    val astroHoy: DiaAstro get() = astroDias.first()
}
