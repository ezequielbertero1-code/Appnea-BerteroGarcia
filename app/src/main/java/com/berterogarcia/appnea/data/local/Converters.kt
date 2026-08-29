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
}