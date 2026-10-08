package com.berterogarcia.appnea.data.local

import androidx.room.withTransaction
import com.berterogarcia.appnea.data.condiciones.CondicionesRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Usuario de DEMO con un perfil completo para probar la app con muchos datos
 * (sobre todo Estadísticas, filtros de Inmersiones y Entrenar).
 *
 * Login: Demo / Demo1234
 *
 * - Perfil completo: datos personales, 4 certificados, equipo (uno retirado), 2 contactos.
 * - 20 inmersiones distintas (buceo y apnea, mar y lago, con y sin condiciones guardadas)
 *   repartidas en los últimos ~7 meses.
 * - 3 tablas de entrenamiento propias y 30 sesiones con una progresión de marcas y factores
 *   que muestran algo en "Qué influye en completar la tabla".
 *
 * Las fechas son RELATIVAS al día en que se siembra, así los filtros de 1 mes / 3 meses tienen datos.
 * Se siembra solo si el usuario Demo no existe, y todo en una transacción: si algo falla
 * no queda a medias (y se vuelve a intentar en el próximo arranque).
 */
object SeedDemo {
    const val USUARIO = "Demo"
    const val PASSWORD = "Demo1234"

    suspend fun sembrarSiHaceFalta(db: AppDatabase) {
        if (db.usuarioDao().existeUsuario(USUARIO)) return
        val hoy = LocalDate.now()

        db.withTransaction {
            db.usuarioDao().insertar(UsuarioEntity(USUARIO, SeguridadPassword.hashear(PASSWORD)))
            sembrarPerfil(db, hoy)
            inmersiones(hoy).forEach { db.inmersionDao().insertar(it) }
            TABLAS.forEach { db.tablaPersonalizadaDao().insertar(it.copy(nombreUsuario = USUARIO)) }
            sesiones(hoy).forEach { db.sesionApneaDao().insertar(it) }
        }
    }

    // ================================ Perfil ================================

    private val EQUIPO = listOf(
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.MASCARA, marca = "Omer", modelo = "Alien"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.SNORKEL, marca = "Cressi", modelo = "Corsica"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.ALETAS, marca = "Leaderfins", modelo = "Pure Carbon", talle = "40-41"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.ALETAS, marca = "Mares", modelo = "Avanti Quattro+", talle = "40-41"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.TRAJE, marca = "Elios", modelo = "Apnea", talle = "M · 5 mm"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.TRAJE, marca = "Mares", modelo = "Flexa semi seco", talle = "M · 7 mm"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.GUANTES_ESCARPINES, marca = "Omer", modelo = "Guantes 5 mm"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.LASTRE, marca = "Omer", modelo = "Cinturón marsellés", talle = "5 kg"),
        EquipoEntity(
            nombreUsuario = USUARIO, categoria = CategoriaEquipo.COMPUTADORA, marca = "Suunto", modelo = "D5",
            proximoMantenimiento = LocalDate.now().minusDays(10), notas = "Cambio de batería"
        ),
        EquipoEntity(
            nombreUsuario = USUARIO, categoria = CategoriaEquipo.REGULADOR, marca = "Aqualung", modelo = "Core",
            proximoMantenimiento = LocalDate.now().plusDays(25), notas = "Service anual"
        ),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.CHALECO, marca = "Mares", modelo = "Prestige", talle = "M"),
        EquipoEntity(nombreUsuario = USUARIO, categoria = CategoriaEquipo.LINTERNA, marca = "Tovatec", modelo = "Fusion 530"),
        // Retirado: queda en el historial pero no se ofrece en el formulario
        EquipoEntity(
            nombreUsuario = USUARIO, categoria = CategoriaEquipo.ALETAS, marca = "Cressi", modelo = "Rondinella",
            talle = "40-41", enUso = false, notas = "Vendidas"
        )
    )

    /** Igual que etiquetaInmersion (NuevaInmersionViewModel): así el formulario pre-marca este equipo. */
    private fun etiqueta(e: EquipoEntity): String =
        (if (e.talle.isBlank()) e.descripcion else "${e.descripcion} (${e.talle})").replace(',', ' ')

    private fun equipo(vararg modelos: String): Set<String> =
        EQUIPO.filter { it.modelo in modelos }.map(::etiqueta).toSet()

    private val EQUIPO_APNEA = equipo("Alien", "Corsica", "Pure Carbon", "Apnea", "Guantes 5 mm", "Cinturón marsellés", "D5")
    private val EQUIPO_BUCEO = equipo("Alien", "Avanti Quattro+", "Flexa semi seco", "Guantes 5 mm", "D5", "Core", "Prestige")
    private val EQUIPO_BUCEO_NOCTURNO = EQUIPO_BUCEO + equipo("Fusion 530")

    private suspend fun sembrarPerfil(db: AppDatabase, hoy: LocalDate) {
        db.perfilUsuarioDao().guardar(
            PerfilUsuarioEntity(
                nombreUsuario = USUARIO,
                nombre = "Lucía",
                apellido = "Ferreyra",
                email = "lucia.ferreyra@appnea.com.ar",
                telefono = "+54 297 455-1023",
                fechaNacimiento = LocalDate.of(1998, 7, 22),
                ciudad = "Rada Tilly, Chubut",
                companeroHabitual = "Franco García"
            )
        )
        listOf(
            CertificadoEntity(
                nombreUsuario = USUARIO, tipo = TipoCertificado.BUCEO, titulo = "Buceador 1 Estrella",
                entidad = "CMAS", escuelaInstructor = "Escuela de Buceo Zeus",
                numero = "ARG-CMAS-2022-01377", fechaEmision = LocalDate.of(2022, 3, 12)
            ),
            CertificadoEntity(
                nombreUsuario = USUARIO, tipo = TipoCertificado.BUCEO, titulo = "Buceador 2 Estrellas",
                entidad = "CMAS", escuelaInstructor = "Escuela de Buceo Zeus",
                numero = "ARG-CMAS-2024-02219", fechaEmision = LocalDate.of(2024, 4, 6)
            ),
            CertificadoEntity(
                nombreUsuario = USUARIO, tipo = TipoCertificado.APNEA, titulo = "AIDA 2",
                entidad = "AIDA", escuelaInstructor = "Apnea Patagonia", fechaEmision = LocalDate.of(2025, 2, 15)
            ),
            // Por vencer: así se ve la insignia de vencimiento en Cuenta
            CertificadoEntity(
                nombreUsuario = USUARIO, tipo = TipoCertificado.APTO_MEDICO, titulo = "Apto médico",
                fechaEmision = hoy.minusYears(1).plusDays(20), fechaVencimiento = hoy.plusDays(20)
            )
        ).forEach { db.certificadoDao().insertar(it) }

        EQUIPO.forEach { db.equipoDao().insertar(it) }

        db.contactoEmergenciaDao().guardar(
            ContactoEmergenciaEntity(
                nombreUsuario = USUARIO, nombre = "Marta Ferreyra", relacion = "Madre",
                telefono = "+54 297 446-7781", esPrincipal = true
            )
        )
        db.contactoEmergenciaDao().guardar(
            ContactoEmergenciaEntity(
                nombreUsuario = USUARIO, nombre = "Franco García", relacion = "Compañero de buceo",
                telefono = "+54 297 415-2231"
            )
        )
    }

    // ================================ Inmersiones ================================

    /** Clima de ejemplo. [marea] null = lago (sin marea). */
    private data class Clima(
        val tempC: Double, val vientoKmh: Double, val dirViento: Double, val codigo: Int,
        val olaM: Double?, val aguaC: Double, val marea: String?, val aptitud: String
    )

    /** Cómo quedaron guardadas las condiciones (para probar los 3 casos de la app). */
    private enum class Registro { CON_INTERNET, SIN_INTERNET, SIN_DATOS }

    private data class Sitio(val nombre: String, val lat: Double, val lon: Double)

    private val CALETA_CORDOVA = Sitio("Caleta Córdova", -45.749, -67.373)
    private val RADA_TILLY = Sitio("Rada Tilly - Punta Piedras", -45.930, -67.553)
    private val PUNTA_MARQUES = Sitio("Punta Marqués", -45.957, -67.574)
    private val BUSTAMANTE = Sitio("Bahía Bustamante", -45.125, -66.530)
    private val CAMARONES = Sitio("Camarones", -44.797, -65.709)
    private val MADRYN = Sitio("Puerto Madryn - Parque Submarino", -42.787, -65.006)
    private val PIRAMIDES = Sitio("Puerto Pirámides", -42.572, -64.282)
    private val CALETA_OLIVIA = Sitio("Caleta Olivia", -46.441, -67.518)
    private val LAGO_MUSTERS = Sitio("Lago Musters", -45.425, -69.188)

    private fun inmersion(
        hoy: LocalDate,
        diasAtras: Long,
        sitio: Sitio,
        tipo: TipoInmersion,
        profundidad: Float,
        fondo: Duration,
        entrada: LocalTime,
        minutosEnAgua: Long,
        agua: Set<String>,
        tempAire: Float,
        tempAgua: Float,
        visibilidad: Float,
        actividad: String,
        comentarios: String,
        clima: Clima?,
        registro: Registro = Registro.CON_INTERNET,
        equipamiento: Set<String> = if (tipo == TipoInmersion.APNEA) EQUIPO_APNEA else EQUIPO_BUCEO,
        lastre: Float = if (tipo == TipoInmersion.APNEA) 5f else 10f,
        proteccion: Float = if (tipo == TipoInmersion.APNEA) 5f else 7f,
        conUbicacion: Boolean = true
    ): InmersionEntity {
        val fecha = hoy.minusDays(diasAtras)
        val momento = LocalDateTime.of(fecha, entrada)
        val condiciones = when (registro) {
            Registro.SIN_DATOS -> null
            Registro.SIN_INTERNET -> CondicionesRepository.registroAstronomico(sitio.lat, sitio.lon, momento)
            Registro.CON_INTERNET -> clima?.let { c ->
                CondicionesRepository.registroAstronomico(sitio.lat, sitio.lon, momento).copy(
                    temperaturaC = c.tempC,
                    vientoKmh = c.vientoKmh,
                    direccionVientoGrados = c.dirViento,
                    rafagasKmh = c.vientoKmh * 1.6,
                    presionHpa = 1013.0,
                    codigoClima = c.codigo,
                    olaM = c.olaM,
                    periodoOlaS = c.olaM?.let { 7.0 },
                    temperaturaAguaC = c.aguaC,
                    estadoMarea = c.marea,
                    aptitudAgua = c.aptitud,
                    conDatosOnline = true
                )
            }
        }
        return InmersionEntity(
            fecha = fecha,
            lugar = sitio.nombre,
            profundidadMetros = profundidad,
            paradaSeguridadMin = if (tipo == TipoInmersion.APNEA) 0f else 3f,
            tiempoFondo = fondo,
            horaEntrada = entrada,
            horaSalida = entrada.plusMinutes(minutosEnAgua),
            caracteristicasAgua = agua,
            lastreKg = lastre,
            proteccionMm = proteccion,
            equipamiento = equipamiento,
            temperaturaAireC = tempAire,
            temperaturaAguaC = tempAgua,
            visibilidadMetros = visibilidad,
            actividad = actividad,
            comentarios = comentarios,
            condiciones = condiciones,
            nombreUsuario = USUARIO,
            tipo = tipo,
            latitud = if (conUbicacion) sitio.lat else null,
            longitud = if (conUbicacion) sitio.lon else null
        )
    }

    private fun inmersiones(hoy: LocalDate): List<InmersionEntity> {
        val buceo = TipoInmersion.BUCEO
        val apnea = TipoInmersion.APNEA
        return listOf(
            inmersion(hoy, 3, PUNTA_MARQUES, buceo, 24f, 41.minutes, LocalTime.of(10, 15), 52,
                setOf("Salada", "Bote", "Corrientes"), 13f, 11f, 9f, "Buceo profundo",
                "Récord personal. Pared con anémonas a 22 m.",
                Clima(13.0, 14.0, 250.0, 1, 0.6, 11.2, "Subiendo hacia pleamar 12:10", "Buenas")),
            inmersion(hoy, 6, RADA_TILLY, apnea, 12f, 110.seconds, LocalTime.of(16, 0), 75,
                setOf("Salada", "Orilla"), 15f, 11f, 6f, "Peso constante",
                "Series de 8 a 12 m con boya. Compensación cómoda.",
                Clima(15.0, 9.0, 300.0, 0, 0.3, 11.0, "Bajando hacia bajamar 18:40", "Buenas")),
            inmersion(hoy, 10, CALETA_CORDOVA, buceo, 16f, 44.minutes, LocalTime.of(9, 30), 55,
                setOf("Salada", "Bote"), 11f, 10f, 7f, "Buceo recreativo",
                "Lobos marinos curiosos en el último tramo.",
                Clima(11.0, 18.0, 270.0, 2, 0.8, 10.4, "Pleamar 10:05", "Regulares")),
            inmersion(hoy, 14, PUNTA_MARQUES, apnea, 15f, 125.seconds, LocalTime.of(15, 30), 80,
                setOf("Salada", "Bote"), 14f, 10f, 8f, "Peso constante",
                "Mejor inmersión de apnea del año: 15 m sin forzar.",
                Clima(14.0, 7.0, 320.0, 0, 0.2, 10.6, "Subiendo hacia pleamar 17:20", "Buenas")),
            inmersion(hoy, 19, MADRYN, buceo, 18f, 48.minutes, LocalTime.of(11, 0), 60,
                setOf("Salada", "Bote"), 16f, 12f, 12f, "Buceo en naufragio",
                "Parque submarino: muy buena visibilidad, cardúmenes de salmón de mar.",
                Clima(16.0, 10.0, 200.0, 1, 0.4, 12.1, "Bajando hacia bajamar 14:30", "Buenas")),
            inmersion(hoy, 22, PIRAMIDES, apnea, 9f, 95.seconds, LocalTime.of(10, 45), 70,
                setOf("Salada", "Bote", "Olas"), 15f, 12f, 10f, "Apnea recreativa",
                "Lobos marinos jugando alrededor. Mucho oleaje en superficie.",
                Clima(15.0, 22.0, 230.0, 2, 1.1, 12.0, "Pleamar 11:15", "Regulares")),
            inmersion(hoy, 28, CALETA_OLIVIA, buceo, 10f, 36.minutes, LocalTime.of(14, 15), 45,
                setOf("Salada", "Orilla", "Resaca"), 12f, 10f, 3f, "Buceo de orilla",
                "Poca visibilidad por la resaca. Salida corta.",
                Clima(12.0, 26.0, 250.0, 3, 1.3, 10.0, "Bajando hacia bajamar 16:00", "Malas")),
            inmersion(hoy, 35, LAGO_MUSTERS, buceo, 8f, 33.minutes, LocalTime.of(12, 0), 45,
                setOf("Dulce", "Orilla"), 10f, 8f, 5f, "Buceo en lago",
                "Agua dulce y fría. Hay que ajustar el lastre (−2 kg).",
                Clima(10.0, 30.0, 270.0, 1, null, 8.0, null, "Regulares"),
                lastre = 8f),
            inmersion(hoy, 41, RADA_TILLY, apnea, 10f, 100.seconds, LocalTime.of(17, 0), 60,
                setOf("Salada", "Orilla", "Olas"), 12f, 9f, 4f, "Apnea dinámica",
                "Práctica de giros y compensación Frenzel.",
                null, registro = Registro.SIN_INTERNET),
            inmersion(hoy, 48, BUSTAMANTE, buceo, 21f, 46.minutes, LocalTime.of(9, 15), 58,
                setOf("Salada", "Bote", "Corrientes"), 9f, 9f, 11f, "Buceo profundo",
                "Corriente de norte a sur, buena visibilidad en el fondo.",
                Clima(9.0, 15.0, 340.0, 0, 0.5, 9.2, "Subiendo hacia pleamar 11:55", "Buenas")),
            inmersion(hoy, 56, CALETA_CORDOVA, buceo, 12f, 52.minutes, LocalTime.of(20, 30), 65,
                setOf("Salada", "Bote"), 7f, 9f, 5f, "Buceo nocturno",
                "Nocturna: pulpos y centollas con la linterna.",
                Clima(7.0, 8.0, 280.0, 0, 0.3, 9.0, "Pleamar 21:10", "Buenas"),
                equipamiento = EQUIPO_BUCEO_NOCTURNO),
            inmersion(hoy, 63, PUNTA_MARQUES, apnea, 13f, 115.seconds, LocalTime.of(14, 0), 75,
                setOf("Salada", "Bote"), 10f, 9f, 7f, "Peso constante",
                "Primera vez a 13 m. Compañero en superficie todo el tiempo.",
                Clima(10.0, 12.0, 260.0, 1, 0.4, 9.1, "Bajando hacia bajamar 15:45", "Buenas")),
            inmersion(hoy, 72, CAMARONES, buceo, 15f, 40.minutes, LocalTime.of(10, 30), 50,
                setOf("Salada", "Bote"), 8f, 8f, 8f, "Buceo recreativo",
                "Bosque de cachiyuyos, mucha vida entre las algas.",
                null, registro = Registro.SIN_DATOS),
            inmersion(hoy, 81, RADA_TILLY, buceo, 7f, 30.minutes, LocalTime.of(11, 30), 40,
                setOf("Salada", "Orilla"), 9f, 8f, 4f, "Práctica de habilidades",
                "Práctica de flotabilidad y vaciado de máscara.",
                Clima(9.0, 20.0, 240.0, 3, 0.9, 8.4, "Pleamar 12:00", "Regulares"),
                conUbicacion = false),
            inmersion(hoy, 90, CALETA_CORDOVA, apnea, 8f, 85.seconds, LocalTime.of(15, 0), 60,
                setOf("Salada", "Bote"), 8f, 8f, 6f, "Apnea recreativa",
                "Agua fría, corté temprano.",
                Clima(8.0, 16.0, 290.0, 2, 0.5, 8.2, "Bajando hacia bajamar 17:30", "Regulares"),
                proteccion = 7f),
            inmersion(hoy, 104, BUSTAMANTE, buceo, 19f, 42.minutes, LocalTime.of(9, 45), 55,
                setOf("Salada", "Bote", "Corrientes"), 8f, 8f, 10f, "Buceo profundo",
                "Corriente leve, muy buena visibilidad.",
                Clima(8.0, 11.0, 330.0, 0, 0.4, 8.6, "Subiendo hacia pleamar 11:20", "Buenas")),
            inmersion(hoy, 120, MADRYN, apnea, 11f, 105.seconds, LocalTime.of(12, 30), 70,
                setOf("Salada", "Bote"), 12f, 10f, 13f, "Apnea con lobos",
                "Visibilidad increíble en Madryn.",
                Clima(12.0, 9.0, 210.0, 0, 0.3, 10.5, "Pleamar 13:05", "Buenas")),
            inmersion(hoy, 140, PUNTA_MARQUES, buceo, 17f, 39.minutes, LocalTime.of(10, 0), 50,
                setOf("Salada", "Bote", "Corrientes"), 14f, 11f, 6f, "Buceo recreativo",
                "Mucho plancton en el agua.",
                Clima(14.0, 17.0, 260.0, 1, 0.7, 11.5, "Bajando hacia bajamar 13:20", "Buenas")),
            inmersion(hoy, 165, RADA_TILLY, apnea, 6f, 70.seconds, LocalTime.of(16, 30), 50,
                setOf("Salada", "Orilla"), 18f, 12f, 5f, "Apnea recreativa",
                "Primera salida de apnea de la temporada.",
                Clima(18.0, 13.0, 280.0, 0, 0.4, 12.4, "Subiendo hacia pleamar 18:00", "Buenas")),
            inmersion(hoy, 190, CALETA_CORDOVA, buceo, 13f, 37.minutes, LocalTime.of(11, 15), 48,
                setOf("Salada", "Bote"), 20f, 13f, 7f, "Buceo recreativo",
                "Agua más templada que de costumbre.",
                Clima(20.0, 19.0, 250.0, 1, 0.6, 13.0, "Pleamar 11:40", "Buenas"))
        )
    }

    // ================================ Entrenamiento ================================

    private fun fases(vararg pares: Pair<TipoFase, Int>) = pares.map { (tipo, seg) -> Fase(tipo, seg) }

    private val P = TipoFase.PREPARACION
    private val A = TipoFase.APNEA
    private val D = TipoFase.DESCANSO

    /** 3 tablas propias, de más fácil a más exigente (las sesiones avanzan en ese orden). */
    private val TABLAS = listOf(
        TablaPersonalizadaEntity(
            nombre = "CO₂ inicial",
            fases = fases(P to 120, A to 60, D to 105, A to 60, D to 90, A to 60, D to 75, A to 60, D to 60, A to 60)
        ),
        TablaPersonalizadaEntity(
            nombre = "O₂ progresiva",
            fases = fases(P to 120, A to 60, D to 120, A to 75, D to 120, A to 90, D to 120, A to 105, D to 120, A to 120)
        ),
        TablaPersonalizadaEntity(
            nombre = "Estática larga",
            fases = fases(P to 180, A to 120, D to 150, A to 150, D to 180, A to 180)
        )
    )

    private val SENSACIONES_BIEN = listOf(
        "Muy relajada, las contracciones llegaron tarde.",
        "Buena respiración previa, todo fluyó.",
        "Me sentí cómoda toda la tabla.",
        "Mejor concentración que la vez pasada."
    )
    private val SENSACIONES_MAL = listOf(
        "Me costó relajarme desde el principio.",
        "Contracciones fuertes desde la mitad, preferí cortar.",
        "Día pesado, no era el momento.",
        "Corté a tiempo, sin forzar."
    )

    /**
     * 30 sesiones en los últimos ~4 meses. Usa un Random con semilla fija: siempre genera lo mismo.
     * Completar depende de los factores (dormir bien ayuda, el estrés no), así la sección
     * "Qué influye" muestra diferencias reales.
     */
    private fun sesiones(hoy: LocalDate): List<SesionApneaEntity> {
        val random = Random(2026)
        val total = 30
        return (0 until total).map { i ->
            // Primeras 10 con la tabla fácil, las 10 del medio con la intermedia, las últimas con la larga
            val tabla = TABLAS[i * TABLAS.size / total]
            val diasAtras = (total - i) * 4L - random.nextInt(0, 3)

            val factores = buildSet {
                if (random.nextFloat() < 0.6f) add("Dormí bien")
                if (random.nextFloat() < 0.45f) add("Comí liviano")
                if (random.nextFloat() < 0.5f) add("Buena hidratación")
                if (random.nextFloat() < 0.3f) add("Estrés / ansiedad")
                if (random.nextFloat() < 0.25f) add("Cansancio físico")
                if (random.nextFloat() < 0.7f) add("Entrené acompañado")
            }
            var chance = 0.6f
            if ("Dormí bien" in factores) chance += 0.25f
            if ("Estrés / ansiedad" in factores) chance -= 0.35f
            if ("Cansancio físico" in factores) chance -= 0.25f
            if ("Buena hidratación" in factores) chance += 0.05f
            val completada = random.nextFloat() < chance.coerceIn(0.1f, 0.95f)

            val (faseAlcanzada, marca) = if (completada) {
                tabla.fases.size to tabla.fases.filter { it.tipo == A }.maxOf { it.duracionSegundos }
            } else {
                abortada(tabla.fases, random)
            }

            SesionApneaEntity(
                fecha = hoy.minusDays(diasAtras.coerceAtLeast(0)),
                nombreTabla = tabla.nombre,
                completada = completada,
                faseAlcanzada = faseAlcanzada,
                totalFases = tabla.fases.size,
                marcaSegundos = marca,
                factores = factores,
                sensaciones = (if (completada) SENSACIONES_BIEN else SENSACIONES_MAL).random(random),
                nombreUsuario = USUARIO
            )
        }
    }

    /**
     * Simula que se cortó en una apnea de la segunda mitad de la tabla:
     * devuelve (fase alcanzada 1-based, apnea más larga sostenida).
     */
    private fun abortada(fases: List<Fase>, random: Random): Pair<Int, Int> {
        val apneas = fases.indices.filter { fases[it].tipo == A }
        val indice = apneas.drop(apneas.size / 2).random(random)
        val completas = fases.take(indice).filter { it.tipo == A }.maxOfOrNull { it.duracionSegundos } ?: 0
        val parcial = (fases[indice].duracionSegundos * random.nextDouble(0.5, 0.9)).toInt()
        return (indice + 1) to maxOf(completas, parcial)
    }
}
