package com.berterogarcia.appnea

import android.annotation.SuppressLint
import android.util.Log
import com.berterogarcia.appnea.util.Registro

/** Conecta [Registro] con el log de Android. Los mensajes de depuración solo en debug. */
@SuppressLint("LogNotTimber")
class RegistroAndroid(override val depuracion: Boolean = BuildConfig.DEBUG) : Registro.Destino {
    override fun escribir(nivel: Registro.Nivel, tag: String, mensaje: String, error: Throwable?) {
        when (nivel) {
            Registro.Nivel.DEPURACION -> Log.d(tag, mensaje, error)
            Registro.Nivel.AVISO -> Log.w(tag, mensaje, error)
            Registro.Nivel.ERROR -> Log.e(tag, mensaje, error)
        }
    }
}
