package com.berterogarcia.appnea.data.local

import android.content.Context
import androidx.core.content.edit
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Datos de ejemplo para el usuario de prueba "Franco" (login: Franco / Mamalesa21).
 *
 * Carga algunas inmersiones, sesiones de entrenamiento y salidas de pesca para que
 * las pantallas (sobre todo las estadísticas de Cuenta) no arranquen vacías.
 * Se ejecuta UNA sola vez por base de datos: si la base se regenera (cambio de versión),
 * AppDatabase reinicia la marca y se vuelve a sembrar.
 */
object SeedDatos {
    private const val PREFS = "seed"
    private const val CLAVE_SEMBRADO = "datos_franco_v1"
    const val USUARIO_PRUEBA = "Franco"
    const val PASSWORD_PRUEBA = "Mamalesa21"

    /** Se llama desde el callback onCreate de la base para que se vuelva a sembrar. */
    fun reiniciarMarca(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(CLAVE_SEMBRADO) }
    }

    suspend fun sembrarSiHaceFalta(context: Context, db: AppDatabase) {
        // Primero tocamos la base: si es nueva, esto dispara onCreate (que reinicia la marca)
        // antes de que leamos la marca.
        val existeFranco = db.usuarioDao().existeUsuario(USUARIO_PRUEBA)

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CLAVE_SEMBRADO, false)) return

        if (!existeFranco) {
            db.usuarioDao().insertar(UsuarioEntity(USUARIO_PRUEBA, hashearPassword(PASSWORD_PRUEBA)))
        }

        inmersiones().zip(CLIMA_INMERSIONES) { inmersion, clima ->
            inmersion.copy(condiciones = clima.aRegistro(inmersion.fecha.atTime(inmersion.horaEntrada)))
        }.forEach { db.inmersionDao().insertar(it) }
        sesionesApnea().forEach { db.sesionApneaDao().insertar(it) }
        salidasPesca().zip(CLIMA_SALIDAS) { salida, clima ->
            salida.copy(condiciones = clima.aRegistro(salida.fecha.atTime(10, 0)))
        }.forEach { db.salidaPescaDao().insertar(it) }

        // Perfil de ejemplo (datos personales, certificados, equipo, contacto). Es idempotente.
        runCatching { SeedPerfil.sembrarPerfilFranco(db) }
        prefs.edit { putBoolean(CLAVE_SEMBRADO, true) }
    }

    /** Clima de ejemplo para cada registro sembrado (lo astronómico se calcula de verdad). */
    private data class ClimaEjemplo(
        val lat: Double, val lon: Double,
        val tempC: Double, val vientoKmh: Double, val dirViento: Double, val rafagasKmh: Double,
        val presionHpa: Double, val codigo: Int, val olaM: Double, val periodoS: Double,
        val aguaC: Double, val marea: String, val aptitud: String
    ) {
        fun aRegistro(momento: java.time.LocalDateTime): CondicionesRegistro =
            CondicionesRepository.registroAstronomico(lat, lon, momento).copy(
                temperaturaC = tempC,
                vientoKmh = vientoKmh,
                direccionVientoGrados = dirViento,
                rafagasKmh = rafagasKmh,
                presionHpa = presionHpa,
                codigoClima = codigo,
                olaM = olaM,
                periodoOlaS = periodoS,
                temperaturaAguaC = aguaC,
                estadoMarea = marea,
                aptitudAgua = aptitud,
                conDatosOnline = true
            )
    }

    private val CLIMA_INMERSIONES = listOf(
        ClimaEjemplo(-45.75, -67.37, 8.0, 12.0, 270.0, 22.0, 1016.0, 1, 0.4, 7.0, 9.1, "Subiendo hacia pleamar 12:40", "Buenas"),
        ClimaEjemplo(-45.93, -67.55, 11.0, 18.0, 250.0, 30.0, 1011.0, 2, 0.7, 6.0, 9.8, "Bajando hacia bajamar 17:05", "Buenas"),
        ClimaEjemplo(-45.12, -66.53, 12.0, 9.0, 340.0, 17.0, 1019.0, 0, 0.3, 8.0, 10.2, "Subiendo hacia pleamar 11:55", "Buenas")
    )

    private val CLIMA_SALIDAS = listOf(
        ClimaEjemplo(-45.95, -67.53, 10.0, 14.0, 260.0, 26.0, 1013.0, 1, 0.5, 7.0, 9.6, "Bajando hacia bajamar 12:10", "Buenas"),
        ClimaEjemplo(-45.75, -67.37, 9.0, 28.0, 270.0, 46.0, 1006.0, 3, 0.9, 5.0, 9.9, "Subiendo hacia pleamar 13:30", "Regulares")
    )

    private fun inmersiones() = listOf(
        InmersionEntity(
            fecha = LocalDate.of(2026, 7, 18),
            lugar = "Caleta Córdova",
            profundidadMetros = 14f,
            paradaSeguridadMin = 3f,
            tiempoFondo = 38.minutes,
            horaEntrada = LocalTime.of(10, 30),
            horaSalida = LocalTime.of(11, 15),
            caracteristicasAgua = setOf("Salada", "Bote"),
            lastreKg = 10f,
            proteccionMm = 7f,
            equipamiento = setOf("Semi seco", "Guantes", "Botas", "Casco"),
            temperaturaAireC = 8f,
            temperaturaAguaC = 9f,
            visibilidadMetros = 6f,
            actividad = "Buceo recreativo",
            comentarios = "Mucha vida en el fondo, lobos marinos cerca del bote."
        ),
        InmersionEntity(
            fecha = LocalDate.of(2026, 8, 22),
            lugar = "Rada Tilly - Punta Piedras",
            profundidadMetros = 8.5f,
            paradaSeguridadMin = 0f,
            tiempoFondo = 105.seconds,
            horaEntrada = LocalTime.of(15, 0),
            horaSalida = LocalTime.of(16, 20),
            caracteristicasAgua = setOf("Salada", "Orilla", "Olas"),
            lastreKg = 6f,
            proteccionMm = 7f,
            equipamiento = setOf("Dos piezas", "Guantes", "Botas", "Casco"),
            temperaturaAireC = 11f,
            temperaturaAguaC = 10f,
            visibilidadMetros = 4f,
            actividad = "Apnea",
            comentarios = "Series de peso constante con boya y compañero en superficie."
        ),
        InmersionEntity(
            fecha = LocalDate.of(2026, 9, 12),
            lugar = "Bahía Bustamante",
            profundidadMetros = 18f,
            paradaSeguridadMin = 3f,
            tiempoFondo = 42.minutes,
            horaEntrada = LocalTime.of(9, 45),
            horaSalida = LocalTime.of(10, 40),
            caracteristicasAgua = setOf("Salada", "Bote", "Corrientes"),
            lastreKg = 11f,
            proteccionMm = 7f,
            equipamiento = setOf("Semi seco", "Guantes", "Botas", "Casco"),
            temperaturaAireC = 12f,
            temperaturaAguaC = 10f,
            visibilidadMetros = 9f,
            actividad = "Buceo recreativo",
            comentarios = "Corriente leve de norte a sur, buena visibilidad."
        )
    )

    private fun sesionesApnea() = listOf(
        SesionApneaEntity(
            fecha = LocalDate.of(2026, 9, 2),
            nombreTabla = "Tabata 4 x (90s apnea / 60s descanso)",
            completada = true,
            faseAlcanzada = 8,
            totalFases = 8,
            marcaSegundos = 90,
            factores = setOf("Dormí bien", "Entrené acompañado"),
            sensaciones = "Relajado, las contracciones llegaron recién en la última."
        ),
        SesionApneaEntity(
            fecha = LocalDate.of(2026, 9, 9),
            nombreTabla = "Pirámide 5 escalones",
            completada = false,
            faseAlcanzada = 8,
            totalFases = 10,
            marcaSegundos = 75,
            factores = setOf("Estrés / ansiedad", "Cansancio físico"),
            sensaciones = "Día largo de trabajo, corté antes del último escalón."
        ),
        SesionApneaEntity(
            fecha = LocalDate.of(2026, 9, 19),
            nombreTabla = "Progresiva 3-2-1-2-1-3",
            completada = true,
            faseAlcanzada = 6,
            totalFases = 6,
            marcaSegundos = 180,
            factores = setOf("Dormí bien", "Comí liviano", "Buena hidratación"),
            sensaciones = "Mejor marca: 3 minutos en la última apnea."
        )
    )

    private fun salidasPesca() = listOf(
        SalidaPescaEntity(
            fecha = LocalDate.of(2026, 8, 30),
            lugar = "Punta Marqués",
            latitud = -45.9500,
            longitud = -67.5300,
            capturas = listOf(
                CapturaPesca("Salmón de mar", 1, 3.2),
                CapturaPesca("Róbalo", 2, 0.9)
            ),
            fotoPath = null,
            comentario = "Agua clara con marea bajando."
        ),
        SalidaPescaEntity(
            fecha = LocalDate.of(2026, 9, 14),
            lugar = "Caleta Córdova",
            latitud = -45.7500,
            longitud = -67.3700,
            capturas = listOf(
                CapturaPesca("Mero", 2, 1.8)
            ),
            fotoPath = null,
            comentario = "Pocos peces, mucho viento del oeste."
        )
    )
}
