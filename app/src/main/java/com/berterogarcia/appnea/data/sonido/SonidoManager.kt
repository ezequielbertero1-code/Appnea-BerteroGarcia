package com.berterogarcia.appnea.data.sonido

import com.berterogarcia.appnea.dominio.modelo.TipoFase
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.berterogarcia.appnea.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Avisos sonoros del entrenamiento de apnea estática:
 * - Inicio de cada fase (preparación / apnea / descanso)
 * - Mitad del tiempo de la fase
 * - Últimos 15 segundos de la fase
 * - Cuenta regresiva final (últimos 3 segundos)
 * - Fin de la tabla completa
 *
 * Vive en AppneaApp (dura toda la vida de la app), así que no hace
 * falta liberar el SoundPool manualmente entre pantallas.
 */
class SonidoManager(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val idCorto = soundPool.load(context, R.raw.beep_corto, 1)
    private val idApnea = soundPool.load(context, R.raw.beep_apnea, 1)
    private val idDescanso = soundPool.load(context, R.raw.beep_descanso, 1)
    private val idFin = soundPool.load(context, R.raw.beep_fin, 1)

    private fun reproducir(id: Int) = soundPool.play(id, 1f, 1f, 1, 0, 1f)

    /** Suena al arrancar una fase: distinto según sea preparación, apnea o descanso. */
    fun avisarInicioFase(tipo: TipoFase) {
        reproducir(
            when (tipo) {
                TipoFase.APNEA -> idApnea
                TipoFase.DESCANSO -> idDescanso
                TipoFase.PREPARACION -> idCorto
            }
        )
    }

    /** Beep simple: mitad del tiempo de la fase actual. */
    fun avisarMitadDeFase() = reproducir(idCorto)

    /** Doble beep: quedan 15 segundos para que termine la fase actual. */
    fun avisarQuedan15Segundos() {
        reproducir(idCorto)
        scope.launch {
            delay(250)
            reproducir(idCorto)
        }
    }

    /** Beep de cuenta regresiva, para los últimos segundos de cada fase. */
    fun beepCuentaAtras() = reproducir(idCorto)

    /** Suena una sola vez, al terminar la última fase de la tabla. */
    fun avisarFinTabla() = reproducir(idFin)
}
