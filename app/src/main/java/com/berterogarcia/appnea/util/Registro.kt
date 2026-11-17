package com.berterogarcia.appnea.util

import android.annotation.SuppressLint
import android.util.Log
import com.berterogarcia.appnea.BuildConfig

/**
 * Único punto de la app que escribe en el log de Android.
 *
 * - [d] solo escribe en las builds de debug, y arma el mensaje recién ahí: los de ubicación
 *   llevan coordenadas, que no tienen que quedar en el logcat de una build publicada.
 * - [w] y [e] se escriben siempre (son fallos que conviene ver en un reporte).
 *
 * Si algún día se suma Crashlytics o Timber, se cambia solo acá.
 */
@SuppressLint("LogNotTimber")
object Registro {
    fun d(tag: String, mensaje: () -> String) {
        if (BuildConfig.DEBUG) Log.d(tag, mensaje())
    }

    fun w(tag: String, mensaje: String, error: Throwable? = null) {
        Log.w(tag, mensaje, error)
    }

    fun e(tag: String, mensaje: String, error: Throwable? = null) {
        Log.e(tag, mensaje, error)
    }
}
