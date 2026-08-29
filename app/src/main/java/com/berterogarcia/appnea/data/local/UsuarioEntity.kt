package com.berterogarcia.appnea.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val nombreUsuario: String,
    val password: String
)