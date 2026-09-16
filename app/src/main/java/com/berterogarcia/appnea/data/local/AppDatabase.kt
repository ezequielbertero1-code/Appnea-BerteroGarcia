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
        SesionApneaEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inmersionDao(): InmersionDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun tablaPersonalizadaDao(): TablaPersonalizadaDao
    abstract fun sesionApneaDao(): SesionApneaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Usuario de prueba: se crea solo la primera vez que se genera la base
        // (instalación nueva o cuando se borra por un cambio de versión/esquema).
        // Login: Franco / Mamalesa21
        private val SEED_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT INTO usuarios (nombreUsuario, password) VALUES (?, ?)",
                    arrayOf("Franco", hashearPassword("Mamalesa21"))
                )
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
                    .addCallback(SEED_CALLBACK)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
