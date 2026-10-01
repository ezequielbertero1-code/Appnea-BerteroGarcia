//================================================================================//
// Converters de los enums del perfil. Room ya sabe guardar enums solo, pero si    //
// mañana se renombra una constante, el converter automático tira una excepción    //
// al leer registros viejos. Con estos, un valor desconocido se lee como OTRO.     //
//                                                                                 //
// Las fechas (LocalDate?) usan los converters que ya existen en Converters.kt:     //
// Room (2.5+ con KSP) maneja solo los null cuando el converter es no-nulo.        //
//================================================================================//

package com.berterogarcia.appnea.data.local

import androidx.room.TypeConverter
@Suppress("unused")
class PerfilConverters {
    @TypeConverter
    fun fromCategoriaEquipo(value: CategoriaEquipo): String = value.name

    @TypeConverter
    fun toCategoriaEquipo(value: String): CategoriaEquipo = CategoriaEquipo.desdeNombre(value)

    @TypeConverter
    fun fromTipoCertificado(value: TipoCertificado): String = value.name

    @TypeConverter
    fun toTipoCertificado(value: String): TipoCertificado = TipoCertificado.desdeNombre(value)
}
