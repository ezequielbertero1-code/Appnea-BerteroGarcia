package com.berterogarcia.appnea

import android.app.Application
import com.berterogarcia.appnea.data.local.AppDatabase
import com.berterogarcia.appnea.data.repository.InmersionRepository

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val inmersionRepository by lazy { InmersionRepository(database.inmersionDao()) }
}