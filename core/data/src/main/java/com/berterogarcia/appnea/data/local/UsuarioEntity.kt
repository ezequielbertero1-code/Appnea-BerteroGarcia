package com.berterogarcia.appnea.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.berterogarcia.appnea.dominio.modelo.TipoCuenta

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val nombreUsuario: String,
    /** Hash PBKDF2 (ver SeguridadPassword). Vacío en las cuentas de Google. */
    val password: String,
    /** Desde la v13 (antes las cuentas de Google se marcaban con la contraseña "!google"). */
    @ColumnInfo(defaultValue = "LOCAL") val tipoCuenta: TipoCuenta = TipoCuenta.LOCAL
)
