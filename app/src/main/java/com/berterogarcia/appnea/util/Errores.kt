package com.berterogarcia.appnea.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Reemplazo de `runCatching` para operaciones que pueden fallar (base, red, GPS, archivos).
 *
 * Diferencias con runCatching:
 *  - Deja pasar la [CancellationException]: si la corrutina se canceló (ej. se cerró la pantalla),
 *    tiene que cortarse, no seguir como si hubiera sido un error común.
 *  - Registra el error en el log con [tag], así un fallo nunca queda escondido detrás de un
 *    mensaje genérico (pasó con el 403 de Open-Meteo: la app solo decía "revisá tu conexión").
 *
 * No usar para validar texto del usuario (ej. LocalTime.parse de un campo): ahí fallar es lo
 * normal y llenaría el log de ruido. Para eso sigue estando runCatching.
 */
inline fun <T> intentar(tag: String, bloque: () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Registro.w(tag, "Operación fallida: ${e.message}", e)
        Result.failure(e)
    }
