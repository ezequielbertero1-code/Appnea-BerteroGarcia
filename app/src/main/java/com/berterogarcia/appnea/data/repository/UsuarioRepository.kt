package com.berterogarcia.appnea.data.repository

import com.berterogarcia.appnea.data.local.UsuarioDao
import com.berterogarcia.appnea.data.local.UsuarioEntity
import com.berterogarcia.appnea.data.local.hashearPassword

class UsuarioRepository(private val dao: UsuarioDao) {

    suspend fun registrar(nombreUsuario: String, password: String): Boolean {
        if (dao.existeUsuario(nombreUsuario)) return false
        dao.insertar(UsuarioEntity(nombreUsuario, hashearPassword(password)))
        return true
    }

    suspend fun validar(nombreUsuario: String, password: String): Boolean {
        val usuario = dao.obtenerPorUsuario(nombreUsuario) ?: return false
        return usuario.password == hashearPassword(password)
    }

}