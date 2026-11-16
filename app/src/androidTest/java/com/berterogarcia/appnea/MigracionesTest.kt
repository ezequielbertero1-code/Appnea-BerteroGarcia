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

        val db = helper.runMigrationsAndValidate(nombreBase, 10, true, *todas)

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
}
