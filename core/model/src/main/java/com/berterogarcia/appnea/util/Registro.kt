package com.berterogarcia.appnea.util

/**
 * Único punto de la app que escribe en el log. Es Kotlin puro (lo usan el dominio y los datos):
 * a dónde va cada mensaje lo decide [destino], que la app conecta con el log de Android al
 * arrancar (ver RegistroAndroid). Sin conectar (tests de JVM, tests de un módulo) no escribe nada.
 *
 * - [d] arma el mensaje solo si el destino acepta depuración: los de ubicación llevan
 *   coordenadas, que no tienen que quedar en el logcat de una build publicada.
 * - [w] y [e] son fallos que conviene ver siempre.
 */
object Registro {

    enum class Nivel { DEPURACION, AVISO, ERROR }

    interface Destino {
        /** false en release: los mensajes de depuración ni se arman. */
        val depuracion: Boolean
        fun escribir(nivel: Nivel, tag: String, mensaje: String, error: Throwable?)
    }

    private object Nada : Destino {
        override val depuracion = false
        override fun escribir(nivel: Nivel, tag: String, mensaje: String, error: Throwable?) = Unit
    }

    @Volatile var destino: Destino = Nada

    fun d(tag: String, mensaje: () -> String) {
        val salida = destino
        if (salida.depuracion) salida.escribir(Nivel.DEPURACION, tag, mensaje(), null)
    }

    fun w(tag: String, mensaje: String, error: Throwable? = null) = destino.escribir(Nivel.AVISO, tag, mensaje, error)

    fun e(tag: String, mensaje: String, error: Throwable? = null) = destino.escribir(Nivel.ERROR, tag, mensaje, error)
}
