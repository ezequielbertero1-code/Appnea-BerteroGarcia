package com.berterogarcia.appnea.data.export

import android.content.Context
import androidx.core.net.toUri
import com.berterogarcia.appnea.dominio.ExportadorInmersiones
import com.berterogarcia.appnea.dominio.ProveedorUsuario
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.util.intentar
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Escribe el UDDF con el ContentResolver de Android (el ViewModel no necesita Context ni Uri). */
class ExportadorUddf(
    private val context: Context,
    private val sesion: ProveedorUsuario,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ExportadorInmersiones {
    override suspend fun exportar(inmersion: Inmersion, destino: String): Boolean = withContext(io) {
        val contenido = UddfExporter.generar(inmersion, autor = sesion.usuarioActual.value.orEmpty())
        intentar("ExportadorUddf") {
            context.contentResolver.openOutputStream(destino.toUri())
                ?.use { it.write(contenido.toByteArray(Charsets.UTF_8)) }
                ?: error("No se pudo abrir el archivo")
        }.isSuccess
    }
}
