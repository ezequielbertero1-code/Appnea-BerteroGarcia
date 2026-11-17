package com.berterogarcia.appnea.dominio.modelo

/** Cómo entra el usuario a la app. */
enum class TipoCuenta {
    /** Usuario y contraseña creados en la app. */
    LOCAL,
    /** "Iniciar sesión con Google": no tiene contraseña en la app. */
    GOOGLE
}
