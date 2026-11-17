package com.berterogarcia.appnea.data.local

import com.berterogarcia.appnea.data.sesion.SessionManager
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InmersionEntity::class,
        UsuarioEntity::class,
        TablaPersonalizadaEntity::class,
        SesionApneaEntity::class,
        PerfilUsuarioEntity::class,
        EquipoEntity::class,
        CertificadoEntity::class,
        ContactoEmergenciaEntity::class,
        IntentoApneaEntity::class
    ],
    // v5: condiciones guardadas en inmersiones y salidas de pesca
    // v6: perfil, equipamiento, certificados y contactos de emergencia
    // v7: inmersiones, sesiones, salidas de pesca y tablas pasan a ser de cada usuario
    // v10: lastre/protección/visibilidad opcionales, escala de visibilidad y datos de apnea
    // v11: factores de entrenamiento con clave estable
    // v12: hora y duración en las sesiones; intentos del cronómetro libre
    version = 12,
    // Room guarda el esquema de cada versión en app/schemas/ (configurado en app/build.gradle.kts).
    // Hace falta para escribir migraciones y para los tests de migración. Esa carpeta SE COMMITEA.
    exportSchema = true
)
@TypeConverters(Converters::class, PerfilConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inmersionDao(): InmersionDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun tablaPersonalizadaDao(): TablaPersonalizadaDao
    abstract fun sesionApneaDao(): SesionApneaDao
    abstract fun perfilUsuarioDao(): PerfilUsuarioDao
    abstract fun equipoDao(): EquipoDao
    abstract fun certificadoDao(): CertificadoDao
    abstract fun contactoEmergenciaDao(): ContactoEmergenciaDao
    abstract fun intentoApneaDao(): IntentoApneaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Versiones viejas (de antes de tener migraciones) que se pueden borrar al actualizar.
         * NO agregar versiones nuevas acá: desde la v6 cada cambio lleva su migración.
         */
        private val VERSIONES_SIN_MIGRACION = intArrayOf(1, 2, 3, 4, 5)

        // Base nueva (instalación nueva o recreada): reinicia la marca de SeedDatos para que se
        // vuelvan a cargar los datos de ejemplo. En release SeedDatos no hace nada.
        private fun seedCallback(appContext: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                SeedDatos.reiniciarMarca(appContext)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                INSTANCE ?: Room.databaseBuilder(appContext, AppDatabase::class.java, "app_database")
                    // Actualizar NO borra los datos:
                    //  - v6 → v7 (y las que vengan) usan las migraciones de Migraciones.kt.
                    //  - Solo las versiones 1 a 5 (anteriores a las migraciones) se recrean de cero.
                    //  - Si alguien sube la versión y se olvida la migración, la app falla al abrir
                    //    (en desarrollo se ve enseguida) en vez de borrar los datos sin avisar.
                    .addMigrations(*Migraciones.todas { SessionManager.leerUsuarioGuardado(appContext) })
                    .fallbackToDestructiveMigrationFrom(true, *VERSIONES_SIN_MIGRACION)
                    // Instalar un APK más viejo encima de uno más nuevo (pasa al cambiar de rama
                    // con Franco) borra la base en vez de cerrar la app. Solo afecta a desarrollo.
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .addCallback(seedCallback(appContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
