package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.dominio.modelo.FactorEntrenamiento
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones de la base. A partir de la v6, CADA cambio de versión tiene que tener su
 * migración acá (o una AutoMigration en AppDatabase) para no perder los datos del usuario.
 *
 * Reglas para escribir una migración a mano:
 *  1. El SQL final tiene que dejar las tablas EXACTAMENTE como las espera Room. Room lo
 *     verifica al abrir la base y, si algo no coincide, la app se cierra con
 *     "Migration didn't properly handle...".
 *  2. El CREATE TABLE correcto se copia del JSON que genera Room en app/schemas/<versión>.json
 *     (campo "createSql") o de AppDatabase_Impl (build/generated/ksp/...).
 *  3. SQLite no permite agregar una FOREIGN KEY a una tabla existente, así que se hace el
 *     procedimiento recomendado: crear tabla nueva → copiar datos → borrar vieja → renombrar.
 *     Ref: https://www.sqlite.org/lang_altertable.html#otheralter
 */
object Migraciones {

    /** Todas las migraciones, para pasarlas juntas a addMigrations(). */
    fun todas(usuarioSesion: () -> String?): Array<Migration> = arrayOf(
        De6a7(usuarioSesion),
        De7a8,
        De8a9,
        De9a10,
        De10a11,
        De11a12,
        De12a13
    )

    /**
     * v6 → v7: inmersiones, sesiones de apnea, salidas de pesca y tablas personalizadas pasan
     * a pertenecer a un usuario (columna `nombreUsuario` + FK a `usuarios` + índice).
     *
     * ¿De quién son los datos que ya existían? No hay forma de saber quién los cargó, así que:
     *  1. del usuario con la sesión iniciada en el dispositivo (lo más probable), si existe;
     *  2. si no, del primer usuario registrado (normalmente Franco, el de prueba);
     *  3. si no hubiera ningún usuario (no debería pasar), esas filas no se copian.
     *
     * @param usuarioSesion lee la sesión guardada (SharedPreferences). Es una función para que
     * se lea recién cuando corre la migración y para poder testearlo.
     */
    class De6a7(private val usuarioSesion: () -> String?) : Migration(6, 7) {

        override fun migrate(db: SupportSQLiteDatabase) {
            val duenio = elegirDuenio(db)

            recrearConDuenio(
                db = db,
                tabla = "inmersiones",
                columnasViejas = "`id`, `fecha`, `lugar`, `profundidadMetros`, `paradaSeguridadMin`, " +
                    "`tiempoFondo`, `horaEntrada`, `horaSalida`, `caracteristicasAgua`, `lastreKg`, " +
                    "`proteccionMm`, `equipamiento`, `temperaturaAireC`, `temperaturaAguaC`, " +
                    "`visibilidadMetros`, `actividad`, `comentarios`, `condiciones`",
                crearTabla = "CREATE TABLE IF NOT EXISTS `inmersiones_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `lugar` TEXT NOT NULL, `profundidadMetros` REAL NOT NULL, `paradaSeguridadMin` REAL NOT NULL, `tiempoFondo` INTEGER NOT NULL, `horaEntrada` INTEGER NOT NULL, `horaSalida` INTEGER NOT NULL, `caracteristicasAgua` TEXT NOT NULL, `lastreKg` REAL NOT NULL, `proteccionMm` REAL NOT NULL, `equipamiento` TEXT NOT NULL, `temperaturaAireC` REAL NOT NULL, `temperaturaAguaC` REAL NOT NULL, `visibilidadMetros` REAL NOT NULL, `actividad` TEXT NOT NULL, `comentarios` TEXT NOT NULL, `condiciones` TEXT, `nombreUsuario` TEXT NOT NULL, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) ON UPDATE CASCADE ON DELETE CASCADE )",
                duenio = duenio
            )

            recrearConDuenio(
                db = db,
                tabla = "sesiones_apnea",
                columnasViejas = "`id`, `fecha`, `nombreTabla`, `completada`, `faseAlcanzada`, " +
                    "`totalFases`, `marcaSegundos`, `factores`, `sensaciones`",
                crearTabla = "CREATE TABLE IF NOT EXISTS `sesiones_apnea_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `nombreTabla` TEXT NOT NULL, `completada` INTEGER NOT NULL, `faseAlcanzada` INTEGER NOT NULL, `totalFases` INTEGER NOT NULL, `marcaSegundos` INTEGER NOT NULL, `factores` TEXT NOT NULL, `sensaciones` TEXT NOT NULL, `nombreUsuario` TEXT NOT NULL, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) ON UPDATE CASCADE ON DELETE CASCADE )",
                duenio = duenio
            )

            recrearConDuenio(
                db = db,
                tabla = "salidas_pesca",
                columnasViejas = "`id`, `fecha`, `lugar`, `latitud`, `longitud`, `capturas`, " +
                    "`fotoPath`, `comentario`, `condiciones`",
                crearTabla = "CREATE TABLE IF NOT EXISTS `salidas_pesca_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `lugar` TEXT NOT NULL, `latitud` REAL, `longitud` REAL, `capturas` TEXT NOT NULL, `fotoPath` TEXT, `comentario` TEXT NOT NULL, `condiciones` TEXT, `nombreUsuario` TEXT NOT NULL, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) ON UPDATE CASCADE ON DELETE CASCADE )",
                duenio = duenio
            )

            recrearConDuenio(
                db = db,
                tabla = "tablas_personalizadas",
                columnasViejas = "`id`, `nombre`, `fases`",
                crearTabla = "CREATE TABLE IF NOT EXISTS `tablas_personalizadas_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `fases` TEXT NOT NULL, `nombreUsuario` TEXT NOT NULL, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) ON UPDATE CASCADE ON DELETE CASCADE )",
                duenio = duenio
            )
        }

        /** Usuario de la sesión si existe en la base; si no, el primero registrado; si no, null. */
        private fun elegirDuenio(db: SupportSQLiteDatabase): String? {
            val sesion = runCatching { usuarioSesion() }.getOrNull()
            if (sesion != null) {
                db.query("SELECT nombreUsuario FROM usuarios WHERE nombreUsuario = ?", arrayOf<Any?>(sesion))
                    .use { if (it.moveToFirst()) return it.getString(0) }
            }
            db.query("SELECT nombreUsuario FROM usuarios ORDER BY rowid LIMIT 1")
                .use { return if (it.moveToFirst()) it.getString(0) else null }
        }

        /**
         * Crea `<tabla>_nueva` con la columna nombreUsuario + FK, copia todas las filas
         * (mismos id) asignándoles [duenio], borra la vieja, renombra, restaura el contador de
         * AUTOINCREMENT y crea el índice.
         * Room corre todas las migraciones dentro de UNA transacción: si algo falla, no queda
         * nada a medias.
         */
        private fun recrearConDuenio(
            db: SupportSQLiteDatabase,
            tabla: String,
            columnasViejas: String,
            crearTabla: String,
            duenio: String?
        ) {
            val nueva = "${tabla}_nueva"
            // Último id usado por AUTOINCREMENT: se conserva para que no se reutilicen ids de
            // registros borrados (al recrear la tabla, SQLite arrancaría desde el id más alto copiado).
            val ultimoId = db.query("SELECT seq FROM sqlite_sequence WHERE name = ?", arrayOf<Any?>(tabla))
                .use { if (it.moveToFirst()) it.getLong(0) else null }

            db.execSQL(crearTabla)
            if (duenio != null) {
                db.execSQL(
                    "INSERT INTO `$nueva` ($columnasViejas, `nombreUsuario`) " +
                        "SELECT $columnasViejas, ? FROM `$tabla`",
                    arrayOf<Any?>(duenio)
                )
            }
            db.execSQL("DROP TABLE `$tabla`")
            db.execSQL("ALTER TABLE `$nueva` RENAME TO `$tabla`")
            if (ultimoId != null) {
                db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf<Any?>(tabla))
                db.execSQL("INSERT INTO sqlite_sequence (name, seq) VALUES (?, ?)", arrayOf<Any?>(tabla, ultimoId))
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${tabla}_nombreUsuario` ON `$tabla` (`nombreUsuario`)")
        }
    }

    /**
     * v7 → v8: se saca pesca submarina.
     *  - Se borra la tabla `salidas_pesca` (las salidas cargadas se pierden: es intencional).
     *    DROP TABLE también borra su índice.
     *  - Los certificados de tipo PESCA_SUBMARINA pasan a OTRO: se conservan título, número,
     *    fechas, etc. (sin esto también caerían en OTRO al leerlos, pero así queda prolijo en la base).
     * Las condiciones guardadas en inmersiones (JSON) no se tocan: los campos de pesca que tengan
     * se ignoran al leerlas.
     */
    object De7a8 : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `salidas_pesca`")
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'salidas_pesca'")
            db.execSQL("UPDATE `certificados` SET `tipo` = 'OTRO' WHERE `tipo` = 'PESCA_SUBMARINA'")
        }
    }
    /**
     * v8 → v9: formulario de inmersión nuevo.
     *  - inmersiones.tipo (BUCEO / APNEA). Las existentes quedan en APNEA si la actividad dice
     *    "apnea"; el resto, en BUCEO.
     *  - inmersiones.latitud / longitud: lugar marcado en el mapa (null en las viejas).
     *  - Equipamiento: la categoría ARPON (resto de pesca) pasa a OTRO.
     * Alcanza con ALTER TABLE ADD COLUMN: no cambian claves ni foreign keys.
     */
    object De8a9 : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // SQLite exige DEFAULT para agregar una columna NOT NULL a una tabla con datos
            db.execSQL("ALTER TABLE `inmersiones` ADD COLUMN `tipo` TEXT NOT NULL DEFAULT 'BUCEO'")
            db.execSQL("ALTER TABLE `inmersiones` ADD COLUMN `latitud` REAL")
            db.execSQL("ALTER TABLE `inmersiones` ADD COLUMN `longitud` REAL")
            db.execSQL("UPDATE `inmersiones` SET `tipo` = 'APNEA' WHERE LOWER(`actividad`) LIKE '%apnea%'")
            db.execSQL("UPDATE `equipamiento` SET `categoria` = 'OTRO' WHERE `categoria` = 'ARPON'")
        }
    }

    /**
     * v9 → v10: lastreKg, proteccionMm y visibilidadMetros pasan a aceptar null (SQLite no
     * permite quitar el NOT NULL de una columna, así que se recrea la tabla) y se agregan
     * `visibilidad` (escala) y `apnea` (JSON). Las inmersiones existentes conservan todos sus
     * datos; las nuevas columnas quedan en null.
     */
    object De9a10 : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `inmersiones_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `lugar` TEXT NOT NULL, `profundidadMetros` REAL NOT NULL, `paradaSeguridadMin` REAL NOT NULL, `tiempoFondo` INTEGER NOT NULL, `horaEntrada` INTEGER NOT NULL, `horaSalida` INTEGER NOT NULL, `caracteristicasAgua` TEXT NOT NULL, `lastreKg` REAL, `proteccionMm` REAL, `equipamiento` TEXT NOT NULL, `temperaturaAireC` REAL NOT NULL, `temperaturaAguaC` REAL NOT NULL, `visibilidad` TEXT, `visibilidadMetros` REAL, `actividad` TEXT NOT NULL, `comentarios` TEXT NOT NULL, `condiciones` TEXT, `nombreUsuario` TEXT NOT NULL, `tipo` TEXT NOT NULL, `latitud` REAL, `longitud` REAL, `apnea` TEXT, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) ON UPDATE CASCADE ON DELETE CASCADE )"
            )
            val columnas = "`id`, `fecha`, `lugar`, `profundidadMetros`, `paradaSeguridadMin`, `tiempoFondo`, " +
                "`horaEntrada`, `horaSalida`, `caracteristicasAgua`, `lastreKg`, `proteccionMm`, " +
                "`equipamiento`, `temperaturaAireC`, `temperaturaAguaC`, `visibilidadMetros`, `actividad`, " +
                "`comentarios`, `condiciones`, `nombreUsuario`, `tipo`, `latitud`, `longitud`"
            db.execSQL("INSERT INTO `inmersiones_nueva` ($columnas) SELECT $columnas FROM `inmersiones`")
            db.execSQL("DROP TABLE `inmersiones`")
            db.execSQL("ALTER TABLE `inmersiones_nueva` RENAME TO `inmersiones`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_inmersiones_nombreUsuario` ON `inmersiones` (`nombreUsuario`)")
        }
    }

    /**
     * v10 → v11: los factores de las sesiones de apnea se guardaban con el texto visible
     * ("Dormí bien"); pasan a la clave del enum (DURMIO_BIEN). El esquema no cambia.
     * Se reemplaza cada texto entre comas (",x," sobre ",lista,") para no tocar subcadenas.
     */
    object De10a11 : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE `sesiones_apnea` SET `factores` = ',' || `factores` || ','")
            FactorEntrenamiento.entries.forEach { f ->
                db.execSQL(
                    "UPDATE `sesiones_apnea` SET `factores` = REPLACE(`factores`, ?, ?)",
                    arrayOf(",${f.textoV10},", ",${f.name},")
                )
            }
            // Saca las comas de los extremos (",A,B," -> "A,B"; ",," -> "")
            db.execSQL("UPDATE `sesiones_apnea` SET `factores` = TRIM(`factores`, ',')")
        }
    }

    /**
     * v11 → v12:
     *  - `sesiones_apnea` gana `horaInicio` y `duracionSegundos` (null en las sesiones viejas: no se sabe).
     *  - Tabla nueva `intentos_apnea` para los intentos del cronómetro libre.
     */
    object De11a12 : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `sesiones_apnea` ADD COLUMN `horaInicio` INTEGER")
            db.execSQL("ALTER TABLE `sesiones_apnea` ADD COLUMN `duracionSegundos` INTEGER")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `intentos_apnea` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`fecha` INTEGER NOT NULL, `hora` INTEGER NOT NULL, `duracionMs` INTEGER NOT NULL, " +
                    "`nombreUsuario` TEXT NOT NULL, FOREIGN KEY(`nombreUsuario`) REFERENCES `usuarios`(`nombreUsuario`) " +
                    "ON UPDATE CASCADE ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_intentos_apnea_nombreUsuario` ON `intentos_apnea` (`nombreUsuario`)")
        }
    }

    /**
     * v12 → v13: columna `tipoCuenta` en `usuarios`. Las cuentas de Google tenían la contraseña
     * "!google" como marca: pasan a GOOGLE y la contraseña queda vacía.
     */
    object De12a13 : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `usuarios` ADD COLUMN `tipoCuenta` TEXT NOT NULL DEFAULT 'LOCAL'")
            db.execSQL("UPDATE `usuarios` SET `tipoCuenta` = 'GOOGLE', `password` = '' WHERE `password` = '!google'")
        }
    }
}
