package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.BajadaApnea
import com.berterogarcia.appnea.dominio.modelo.CondicionesRegistro
import com.berterogarcia.appnea.dominio.modelo.RegistroApnea
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import com.berterogarcia.appnea.util.intentar
import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Datos de ejemplo para el usuario de prueba "Franco" (login: Franco / Prueba1234).
 *
 * SOLO EN DEBUG (src/debug): el APK release usa el SeedDatos vacío de src/release, así no
 * viajan usuarios de prueba ni sus contraseñas a los usuarios reales.
 *
 * Carga algunas inmersiones y sesiones de entrenamiento para que
 * las pantallas (sobre todo las estadísticas de Cuenta) no arranquen vacías.
 * Se ejecuta UNA sola vez por base de datos: si la base se regenera (cambio de versión),
 * AppDatabase reinicia la marca y se vuelve a sembrar.
 */
object SeedDatos {
    private const val PREFS = "seed"
    private const val CLAVE_SEMBRADO = "datos_franco_v1"
    const val USUARIO_PRUEBA = "Franco"
    const val PASSWORD_PRUEBA = "Prueba1234"

    /** Se llama desde el callback onCreate de la base para que se vuelva a sembrar. */
    fun reiniciarMarca(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(CLAVE_SEMBRADO) }
    }

    suspend fun sembrarSiHaceFalta(context: Context, db: AppDatabase) {
        // Primero tocamos la base: si es nueva, esto dispara onCreate (que reinicia la marca)
        // antes de que leamos la marca.
        val existeFranco = db.usuarioDao().existeUsuario(USUARIO_PRUEBA)

        // Usuario Demo con muchos datos (independiente de la marca de Franco: se siembra
        // solo si el usuario Demo no existe, así también aparece en instalaciones viejas).
        intentar("SeedDatos") { SeedDemo.sembrarSiHaceFalta(db) }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CLAVE_SEMBRADO, false)) return

        // Todo o nada: si algo falla a mitad de camino no quedan datos de ejemplo a medias
        // (y como la marca no se guarda, se reintenta en el próximo arranque).
        val hash = SeguridadPassword.hashear(PASSWORD_PRUEBA)
        db.withTransaction {
            if (!existeFranco) db.usuarioDao().insertar(UsuarioEntity(USUARIO_PRUEBA, hash))
            inmersiones().zip(CLIMA_INMERSIONES) { inmersion, clima ->
                inmersion.copy(condiciones = clima.aRegistro(inmersion.fecha.atTime(inmersion.horaEntrada)))
            }.forEach { db.inmersionDao().insertar(it.copy(nombreUsuario = USUARIO_PRUEBA)) }
            sesionesApnea().forEach { db.sesionApneaDao().insertar(it.copy(nombreUsuario = USUARIO_PRUEBA)) }
            // Perfil de ejemplo (datos personales, certificados, equipo, contacto). Es idempotente.
            SeedPerfil.sembrarPerfilFranco(db)
        }
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
            visibilidad = Visibilidad.BUENA,
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
            lastreKg = null,       // apnea: sin lastre extra
            proteccionMm = 7f,
            equipamiento = setOf("Dos piezas", "Guantes", "Botas", "Casco"),
            temperaturaAireC = 11f,
            temperaturaAguaC = 10f,
            visibilidad = Visibilidad.BUENA,
            visibilidadMetros = 4f,
            actividad = "Apnea",
            tipo = TipoInmersion.APNEA,
            apnea = RegistroApnea(
                cantidadBajadas = 4,
                profundidadMaximaMetros = 8.5f,
                sincope = false,
                bajadas = listOf(BajadaApnea(5f, 25), BajadaApnea(7f, 30), BajadaApnea(8f, 20), BajadaApnea(8.5f, 30))
            ),
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
            visibilidad = Visibilidad.MUY_BUENA,
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
            factores = setOf(FactorEntrenamiento.DURMIO_BIEN, FactorEntrenamiento.ACOMPANADO),
            sensaciones = "Relajado, las contracciones llegaron recién en la última."
        ),
        SesionApneaEntity(
            fecha = LocalDate.of(2026, 9, 9),
            nombreTabla = "Pirámide 5 escalones",
            completada = false,
            faseAlcanzada = 8,
            totalFases = 10,
            marcaSegundos = 75,
            factores = setOf(FactorEntrenamiento.ESTRES, FactorEntrenamiento.CANSANCIO),
            sensaciones = "Día largo de trabajo, corté antes del último escalón."
        ),
        SesionApneaEntity(
            fecha = LocalDate.of(2026, 9, 19),
            nombreTabla = "Progresiva 3-2-1-2-1-3",
            completada = true,
            faseAlcanzada = 6,
            totalFases = 6,
            marcaSegundos = 180,
            factores = setOf(FactorEntrenamiento.DURMIO_BIEN, FactorEntrenamiento.COMIO_LIVIANO, FactorEntrenamiento.HIDRATACION),
            sensaciones = "Mejor marca: 3 minutos en la última apnea."
        )
    )
}
