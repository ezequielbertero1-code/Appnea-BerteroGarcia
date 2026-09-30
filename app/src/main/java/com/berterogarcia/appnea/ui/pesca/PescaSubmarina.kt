package com.berterogarcia.appnea.ui.pesca

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.CapturaPesca
import com.berterogarcia.appnea.data.local.SalidaPescaEntity
import com.berterogarcia.appnea.ui.condiciones.ResumenCondicionesRegistro

@Composable
fun PescaSubmarinaScreen(
    modifier: Modifier = Modifier,
    viewModel: PescaSubmarinaViewModel,
    onAgregarClick: () -> Unit
) {
    val salidas by viewModel.salidas.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onAgregarClick, modifier = Modifier.fillMaxWidth()) {
            Text("Cargar nueva salida")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (salidas.isEmpty()) {
            Text(
                text = "Todavía no cargaste ninguna salida.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn {
            items(salidas, key = { it.id }) { salida ->
                SalidaCard(salida, onEliminar = { viewModel.eliminar(salida) })
            }
        }
    }
}

@Composable
private fun SalidaCard(
    salida: SalidaPescaEntity,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                salida.fotoPath?.let { path ->
                    val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto de la captura",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${salida.fecha} — ${salida.lugar}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(resumenCapturas(salida.capturas), style = MaterialTheme.typography.bodyMedium)
                    if (salida.comentario.isNotBlank()) {
                        Text(salida.comentario, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            salida.condiciones?.let { ResumenCondicionesRegistro(it) }

            Row(modifier = Modifier.fillMaxWidth()) {
                if (salida.latitud != null && salida.longitud != null) {
                    TextButton(onClick = { abrirEnElMapa(context, salida.latitud, salida.longitud, salida.lugar) }) {
                        Text("Ver en el mapa 🗺️")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEliminar) { Text("Eliminar") }
            }
        }
    }
}

private fun resumenCapturas(capturas: List<CapturaPesca>): String =
    capturas.joinToString(", ") { captura ->
        val peso = captura.pesoKg?.let { " (${"%.1f".format(it)} kg)" } ?: ""
        "${captura.cantidad}x ${captura.especie}$peso"
    }

private fun abrirEnElMapa(context: Context, latitud: Double, longitud: Double, lugar: String) {
    val uri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(lugar)})")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }
}
