package com.berterogarcia.appnea.dominio.modelo

/** Datos de la cuenta de Google que eligió el usuario. */
data class CuentaGoogle(
    val email: String,
    val nombre: String,
    val apellido: String
)

sealed interface ResultadoGoogle {
    data class Ok(val cuenta: CuentaGoogle) : ResultadoGoogle
    /** El usuario cerró el selector de cuentas: no es un error, no se muestra nada. */
    data object Cancelado : ResultadoGoogle
    /** No hay ninguna cuenta de Google en el dispositivo. */
    data object SinCuentas : ResultadoGoogle
    /** Falta el Client ID en local.properties (client_web=...). */
    data object SinConfigurar : ResultadoGoogle
    data object Error : ResultadoGoogle
}
