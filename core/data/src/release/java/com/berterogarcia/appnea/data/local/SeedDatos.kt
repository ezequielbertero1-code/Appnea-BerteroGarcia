package com.berterogarcia.appnea.data.local

import android.content.Context

/**
 * Versión release: no siembra nada. Los usuarios y datos de prueba (Franco, Demo) viven solo
 * en src/debug, así no llegan al APK que se distribuye.
 */
object SeedDatos {
    @Suppress("UNUSED_PARAMETER")
    fun reiniciarMarca(context: Context) = Unit

    @Suppress("UNUSED_PARAMETER", "RedundantSuspendModifier")
    suspend fun sembrarSiHaceFalta(context: Context, db: AppDatabase) = Unit
}
