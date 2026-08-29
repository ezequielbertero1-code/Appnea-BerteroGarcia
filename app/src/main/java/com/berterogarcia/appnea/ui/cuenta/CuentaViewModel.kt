package com.berterogarcia.appnea.ui.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.SessionManager

class CuentaViewModel(private val sessionManager: SessionManager) : ViewModel() {
    val usuario: String? = sessionManager.obtenerUsuario()

    fun cerrarSesion() {
        sessionManager.cerrarSesion()
    }

    companion object {
        fun Factory(sessionManager: SessionManager) = viewModelFactory {
            initializer { CuentaViewModel(sessionManager) }
        }
    }
}