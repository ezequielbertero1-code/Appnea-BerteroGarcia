package com.berterogarcia.appnea.data.local

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
        SalidaPescaEntity::class
    ],
    version = 5, // v5: condiciones guardadas en inmersiones y salidas de pesca
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inmersionDao(): InmersionDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun tablaPersonalizadaDao(): TablaPersonalizadaDao
    abstract fun sesionApneaDao(): SesionApneaDao
    abstract fun salidaPescaDao(): SalidaPescaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Usuario de prueba: se crea solo la primera vez que se genera la base
        // (instalación nueva o cuando se borra por un cambio de versión/esquema).
        // Login: Franco / Mamalesa21
        // Además reinicia la marca de SeedDatos para que se vuelvan a cargar los datos de ejemplo.
        private fun seedCallback(appContext: Context) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT INTO usuarios (nombreUsuario, password) VALUES (?, ?)",
                    arrayOf(SeedDatos.USUARIO_PRUEBA, hashearPassword(SeedDatos.PASSWORD_PRUEBA))
                )
                SeedDatos.reiniciarMarca(appContext)
            }
        }

        // Elimina la BD si no la encuentra, es para ahora que estamos en produccion
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(seedCallback(context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
