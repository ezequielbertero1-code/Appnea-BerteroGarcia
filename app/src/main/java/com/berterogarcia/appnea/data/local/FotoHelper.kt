package com.berterogarcia.appnea.data.local

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copia una imagen elegida desde el selector de fotos del sistema al almacenamiento
 * interno de la app, para poder seguir accediendo a ella más adelante (la Uri que
 * entrega el selector de fotos no siempre sigue siendo válida después de reiniciar la app).
 *
 * Devuelve la ruta absoluta del archivo copiado, o null si algo falló.
 */
object FotoHelper {

    fun copiarACarpetaInterna(context: Context, uriOrigen: Uri): String? {
        return runCatching {
            val carpeta = File(context.filesDir, "capturas_pesca").apply { mkdirs() }
            val archivoDestino = File(carpeta, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uriOrigen)?.use { entrada ->
                archivoDestino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            archivoDestino.absolutePath
        }.getOrNull()
    }
}
