//================================================================================//
// Esta clase es porque Room no guarda Set<String> directamente, hay que convertirlo //
//================================================================================//

package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.RegistroApnea
import com.berterogarcia.appnea.dominio.modelo.TipoFase
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import androidx.room.TypeConverter
import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
class Converters {
    @TypeConverter
    fun fromSet(value: Set<String>): String = value.joinToString(",")

    @TypeConverter
    fun toSet(value: String): Set<String> =
        if (value.isBlank()) emptySet() else value.split(",").toSet()

    // Factores de entrenamiento <-> claves estables separadas por coma ("DURMIO_BIEN,ESTRES")
    @TypeConverter
    fun fromFactores(value: Set<FactorEntrenamiento>): String = value.joinToString(",") { it.name }

    @TypeConverter
    fun toFactores(value: String): Set<FactorEntrenamiento> =
        value.split(",").mapNotNull { FactorEntrenamiento.desdeClave(it.trim()) }.toSet()

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

    // TipoInmersion <-> nombre ("BUCEO" / "APNEA")
    @TypeConverter
    fun fromTipoInmersion(value: TipoInmersion): String = value.name

    @TypeConverter
    fun toTipoInmersion(value: String): TipoInmersion = TipoInmersion.desdeNombre(value)

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

    // Visibilidad <-> nombre (null si no se indicó o no se reconoce)
    @TypeConverter
    fun fromVisibilidad(value: Visibilidad?): String? = value?.name

    @TypeConverter
    fun toVisibilidad(value: String?): Visibilidad? = Visibilidad.desdeNombre(value)

    // RegistroApnea <-> JSON (null en buceo)
    @TypeConverter
    fun fromRegistroApnea(value: RegistroApnea?): String? = value?.aJson()

    @TypeConverter
    fun toRegistroApnea(value: String?): RegistroApnea? = value?.let { RegistroApnea.desdeJson(it) }
}
