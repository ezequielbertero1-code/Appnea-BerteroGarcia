package com.berterogarcia.appnea.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [InmersionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inmersionDao(): InmersionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        //fun getDatabase(context: Context): AppDatabase {
        //    return INSTANCE ?: synchronized(this) {
        //        Room.databaseBuilder(
        //            context.applicationContext,
        //            AppDatabase::class.java,
        //            "app_database"
        //        ).build().also { INSTANCE = it }
        //    }
        //}

        // Elimina la BD si no la encuentra, es para ahora que estamos en produccion
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}