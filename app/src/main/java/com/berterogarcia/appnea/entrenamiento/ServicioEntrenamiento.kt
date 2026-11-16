package com.berterogarcia.appnea.entrenamiento

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.berterogarcia.appnea.MainActivity
import com.berterogarcia.appnea.MyApplication
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.TipoFase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Mantiene viva la app mientras corre una tabla de apnea.
 *
 * Sin esto, al bloquear el teléfono o cambiar de app Android puede frenar o matar el proceso
 * (en Xiaomi/MIUI pasa enseguida) y se cortan la cuenta regresiva y los avisos sonoros: en un
 * entrenamiento de apnea eso es un problema de seguridad.
 *
 * - Foreground service con notificación (fase actual + cuenta regresiva del sistema).
 * - Wake lock parcial: con la pantalla apagada la CPU sigue despierta para el timer y los sonidos.
 * - El que manda es [MotorEntrenamiento]: el servicio solo lo observa y se apaga cuando la tabla termina.
 */
class ServicioEntrenamiento : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    private val motor get() = (application as MyApplication).motorEntrenamiento

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanal(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground tiene que llamarse enseguida (Android da unos segundos)
        val n = notificacion(motor.estado.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(ID_NOTIFICACION, n, tipoServicio())
        else startForeground(ID_NOTIFICACION, n)
        tomarWakeLock(motor.estado.value)
        scope.launch {
            motor.estado
                .map { Triple(it.corriendo, it.indiceFase, it.tabla) }
                .distinctUntilChanged()
                .collect { (corriendo, _, _) ->
                    if (!corriendo) {
                        detener()
                    } else {
                        NotificationManagerCompat.from(this@ServicioEntrenamiento)
                            .notifySiSePuede(ID_NOTIFICACION, notificacion(motor.estado.value))
                    }
                }
        }
        // Si Android mata el servicio, no lo recrea: el motor ya no existiría (murió el proceso)
        return START_NOT_STICKY
    }

    private fun detener() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun tomarWakeLock(estado: EstadoMotor) {
        val totalMs = estado.tabla?.fases?.sumOf { it.duracionSegundos }?.times(1000L) ?: 0L
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppneaMe:entrenamiento").apply {
            setReferenceCounted(false)
            // Con tope: si algo falla, el lock se suelta solo (la tabla + 1 minuto de margen)
            acquire(totalMs + 60_000L)
        }
    }

    private fun notificacion(estado: EstadoMotor): Notification {
        val tabla = estado.tabla
        val fase = tabla?.fases?.getOrNull(estado.indiceFase)
        val abrirApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Cuenta regresiva que dibuja el sistema: no hace falta actualizar la notificación cada segundo
        val finFaseReloj = System.currentTimeMillis() + (estado.finFaseMs - SystemClock.elapsedRealtime())
        return NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_ms_timer)
            .setContentTitle(tabla?.nombre ?: getString(R.string.app_name))
            .setContentText(
                if (fase == null) getString(R.string.notif_entrenamiento_en_curso)
                else getString(R.string.notif_entrenamiento_fase, nombreFase(fase.tipo), estado.indiceFase + 1, tabla.fases.size)
            )
            .setContentIntent(abrirApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true) // los avisos los hace SonidoManager
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setUsesChronometer(fase != null)
            .setChronometerCountDown(true)
            .setWhen(finFaseReloj)
            .build()
    }

    private fun nombreFase(tipo: TipoFase): String = getString(
        when (tipo) {
            TipoFase.PREPARACION -> R.string.fase_preparacion
            TipoFase.APNEA -> R.string.fase_apnea
            TipoFase.DESCANSO -> R.string.fase_descanso
        }
    )

    private fun tipoServicio(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0

    @SuppressLint("MissingPermission")
    private fun NotificationManagerCompat.notifySiSePuede(id: Int, n: Notification) {
        // Sin permiso de notificaciones (Android 13+) el servicio igual corre; solo no se actualiza el texto
        if (areNotificationsEnabled()) notify(id, n)
    }

    companion object {
        private const val CANAL = "entrenamiento"
        private const val ID_NOTIFICACION = 1001

        private fun crearCanal(context: Context) {
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.notif_canal_entrenamiento))
                    .setShowBadge(false)
                    .build()
            )
        }

        /** Lo llama el motor (vía AppContainer) cuando una tabla arranca o termina. */
        fun actualizar(context: Context, corriendo: Boolean) {
            val intent = Intent(context, ServicioEntrenamiento::class.java)
            if (corriendo) {
                // Se arranca al tocar "Iniciar" (app en primer plano): Android lo permite
                runCatching { ContextCompat.startForegroundService(context, intent) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
