package com.berterogarcia.appnea

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.local.Migraciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Corre las migraciones sobre una base real con los esquemas de app/schemas y verifica que
 * (1) Room acepte el esquema final y (2) los datos del usuario no se pierdan.
 * Usa una base aparte ("test-migraciones"): no toca los datos de la app instalada.
 */
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val nombreBase = "test-migraciones"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val todas = Migraciones.todas { null }.filter { it.startVersion >= 7 }.toTypedArray()

    @Test
    fun de7a10ConservaLasInmersiones() {
        helper.createDatabase(nombreBase, 7).use { db ->
            db.execSQL("INSERT INTO usuarios (nombreUsuario, password) VALUES ('ana', 'x')")
            db.execSQL(
                "INSERT INTO inmersiones (id, fecha, lugar, profundidadMetros, paradaSeguridadMin, tiempoFondo, " +
                    "horaEntrada, horaSalida, caracteristicasAgua, lastreKg, proteccionMm, equipamiento, " +
                    "temperaturaAireC, temperaturaAguaC, visibilidadMetros, actividad, comentarios, condiciones, nombreUsuario) " +
                    "VALUES (5, 20000, 'Rada Tilly', 12.0, 0, 600, 36000, 39600, 'Calma', 2.0, 5.0, 'Aletas', " +
                    "20.0, 14.0, 8.0, 'Apnea estática', 'ok', NULL, 'ana')"
            )
            db.execSQL(
                "INSERT INTO inmersiones (id, fecha, lugar, profundidadMetros, paradaSeguridadMin, tiempoFondo, " +
                    "horaEntrada, horaSalida, caracteristicasAgua, lastreKg, proteccionMm, equipamiento, " +
                    "temperaturaAireC, temperaturaAguaC, visibilidadMetros, actividad, comentarios, condiciones, nombreUsuario) " +
                    "VALUES (6, 20001, 'Punta Marqués', 18.0, 3, 1800, 36000, 39600, '', 4.0, 7.0, '', " +
                    "18.0, 12.0, 10.0, 'Buceo recreativo', '', NULL, 'ana')"
            )
        }

        val db = helper.runMigrationsAndValidate(nombreBase, 12, true, *todas)

        db.query("SELECT id, tipo, lastreKg, visibilidad, apnea, lugar FROM inmersiones ORDER BY id").use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals(5L, c.getLong(0))
            assertEquals("APNEA", c.getString(1))
            assertEquals(2.0, c.getDouble(2), 1e-6)
            assertTrue(c.isNull(3))
            assertTrue(c.isNull(4))
            assertEquals("Rada Tilly", c.getString(5))
            c.moveToNext()
            assertEquals("BUCEO", c.getString(1))
        }
        db.close()
    }

    @Test
    fun de7a8BorraPescaYConvierteCertificados() {
        helper.createDatabase(nombreBase, 7).use { db ->
            db.execSQL("INSERT INTO usuarios (nombreUsuario, password) VALUES ('ana', 'x')")
            db.execSQL(
                "INSERT INTO certificados (nombreUsuario, tipo, titulo, entidad, escuelaInstructor, numero, fechaEmision, fechaVencimiento, notas) " +
                    "VALUES ('ana', 'PESCA_SUBMARINA', 'Pesca', '', '', '', NULL, NULL, '')"
            )
        }
        val db = helper.runMigrationsAndValidate(nombreBase, 8, true, *todas)
        db.query("SELECT tipo FROM certificados").use { c ->
            c.moveToFirst(); assertEquals("OTRO", c.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE name = 'salidas_pesca'").use { assertEquals(0, it.count) }
        db.close()
    }

    @Test
    fun de10a11PasaLosFactoresAClaves() {
        helper.createDatabase(nombreBase, 10).use { db ->
            db.execSQL("INSERT INTO usuarios (nombreUsuario, password) VALUES ('ana', 'x')")
            listOf(1 to "Dormí bien,Entrené acompañado", 2 to "", 3 to "Estrés / ansiedad", 4 to "Cansancio físico,Comí liviano,Buena hidratación")
                .forEach { (id, f) ->
                    db.execSQL(
                        "INSERT INTO sesiones_apnea (id, fecha, nombreTabla, completada, faseAlcanzada, totalFases, " +
                            "marcaSegundos, factores, sensaciones, nombreUsuario) VALUES (?, 20000, 'T', 1, 3, 3, 60, ?, '', 'ana')",
                        arrayOf<Any>(id, f)
                    )
                }
        }
        val db = helper.runMigrationsAndValidate(nombreBase, 11, true, Migraciones.De10a11)
        val esperado = listOf("DURMIO_BIEN,ACOMPANADO", "", "ESTRES", "CANSANCIO,COMIO_LIVIANO,HIDRATACION")
        db.query("SELECT factores FROM sesiones_apnea ORDER BY id").use { c ->
            val leidos = buildList { while (c.moveToNext()) add(c.getString(0)) }
            assertEquals(esperado, leidos)
        }
    }

    @Test
    fun de11a12AgregaHoraYDuracionEIntentos() {
        helper.createDatabase(nombreBase, 11).use { db ->
            db.execSQL("INSERT INTO usuarios (nombreUsuario, password) VALUES ('ana', 'x')")
            db.execSQL(
                "INSERT INTO sesiones_apnea (id, fecha, nombreTabla, completada, faseAlcanzada, totalFases, " +
                    "marcaSegundos, factores, sensaciones, nombreUsuario) VALUES (7, 20000, 'T', 1, 3, 3, 60, 'ESTRES', 'bien', 'ana')"
            )
        }
        val db = helper.runMigrationsAndValidate(nombreBase, 12, true, Migraciones.De11a12)
        // La sesión vieja se conserva, sin hora ni duración (no se sabían)
        db.query("SELECT nombreTabla, factores, horaInicio, duracionSegundos FROM sesiones_apnea WHERE id = 7").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("T", c.getString(0))
            assertEquals("ESTRES", c.getString(1))
            assertTrue(c.isNull(2))
            assertTrue(c.isNull(3))
        }
        // La tabla de intentos existe y respeta la FK con usuarios
        db.execSQL("INSERT INTO intentos_apnea (fecha, hora, duracionMs, nombreUsuario) VALUES (20000, 36000, 95000, 'ana')")
        db.query("SELECT COUNT(*) FROM intentos_apnea").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
    }
}
