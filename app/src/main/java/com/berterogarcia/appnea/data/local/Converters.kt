//================================================================================//
// Esta clase es porque Room no guarda Set<String> directamente, hay que convertirlo //
//================================================================================//

package com.berterogarcia.appnea.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class Converters {
    @TypeConverter
    fun fromSet(value: Set<String>): String = value.joinToString(",")

    @TypeConverter
    fun toSet(value: String): Set<String> =
        if (value.isBlank()) emptySet() else value.split(",").toSet()

    // LocalDate <-> epoch day (Long): ordena bien numéricamente en SQL
    @TypeConverter
    fun fromLocalDate(value: LocalDate): Long = value.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    // LocalTime <-> segundos desde medianoche (Int)
    @TypeConverter
    fun fromLocalTime(value: LocalTime): Int = value.toSecondOfDay()

    @TypeConverter
    fun toLocalTime(value: Int): LocalTime = LocalTime.ofSecondOfDay(value.toLong())

    // Duration <-> segundos (Long)
    @TypeConverter
    fun fromDuration(value: Duration): Long = value.inWholeSeconds

    @TypeConverter
    fun toDuration(value: Long): Duration = value.seconds

    // List<Fase> <-> "TIPO:segundos;TIPO:segundos;..." (para las tablas de apnea guardadas)
    @TypeConverter
    fun fromFases(value: List<Fase>): String =
        value.joinToString(";") { "${it.tipo.name}:${it.duracionSegundos}" }

    @TypeConverter
    fun toFases(value: String): List<Fase> =
        if (value.isBlank()) emptyList()
        else value.split(";").map { parte ->
            val (tipo, segundos) = parte.split(":")
            Fase(TipoFase.valueOf(tipo), segundos.toInt())
        }

    // CondicionesRegistro <-> JSON (puede ser null en registros viejos o sin datos)
    @TypeConverter
    fun fromCondiciones(value: CondicionesRegistro?): String? = value?.aJson()

    @TypeConverter
    fun toCondiciones(value: String?): CondicionesRegistro? =
        value?.let { CondicionesRegistro.desdeJson(it) }

    // List<CapturaPesca> <-> "especie,cantidad,peso;especie,cantidad,peso;..." (peso vacío si no se cargó)
    @TypeConverter
    fun fromCapturas(value: List<CapturaPesca>): String =
        value.joinToString(";") { "${it.especie}\u0001${it.cantidad}\u0001${it.pesoKg ?: ""}" }

    @TypeConverter
    fun toCapturas(value: String): List<CapturaPesca> =
        if (value.isBlank()) emptyList()
        else value.split(";").map { parte ->
            val partes = parte.split("\u0001")
            CapturaPesca(
                especie = partes[0],
                cantidad = partes[1].toInt(),
                pesoKg = partes.getOrNull(2)?.takeIf { it.isNotBlank() }?.toDouble()
            )
        }
}
