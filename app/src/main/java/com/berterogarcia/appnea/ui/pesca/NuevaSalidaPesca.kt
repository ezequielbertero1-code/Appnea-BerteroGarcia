package com.berterogarcia.appnea.ui.pesca

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.ui.components.DatePickerField

@Composable
fun NuevaSalidaPescaScreen(
    modifier: Modifier = Modifier,
    viewModel: NuevaSalidaPescaViewModel,
    onGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.guardadoOk) {
        if (uiState.guardadoOk) onGuardado()
    }

    val lanzadorPermisoUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.usarUbicacionActual()
    }

    val lanzadorFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onFotoSeleccionada(uri)
    }

    var especie by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var peso by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Nueva salida de pesca submarina",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        DatePickerField(
            label = "Fecha",
            valor = uiState.fecha,
            onValorChange = viewModel::onFechaChange,
            isError = uiState.errores.containsKey(CampoSalida.FECHA),
            mensajeError = uiState.errores[CampoSalida.FECHA],
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = uiState.lugar,
            onValueChange = viewModel::onLugarChange,
            label = { Text("Lugar") },
            isError = uiState.errores.containsKey(CampoSalida.LUGAR),
            supportingText = { uiState.errores[CampoSalida.LUGAR]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { lanzadorPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.latitud != null) "Ubicación guardada ✓ (tocar para actualizar)"
                else "Usar mi ubicación actual"
            )
        }
        uiState.mensajeUbicacion?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(20.dp))
        Text("Especies pescadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (uiState.errores.containsKey(CampoSalida.CAPTURAS)) {
            Text(
                uiState.errores[CampoSalida.CAPTURAS]!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = especie,
                onValueChange = { especie = it },
                label = { Text("Especie") },
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it.filter { c -> c.isDigit() } },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(110.dp)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = peso,
                onValueChange = { peso = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Peso kg (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.agregarCaptura(especie, cantidad.toIntOrNull() ?: 0, peso.toDoubleOrNull())
                especie = ""
                cantidad = "1"
                peso = ""
            }) { Text("Agregar") }
        }

        if (uiState.capturas.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            uiState.capturas.forEachIndexed { i, captura ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pesoTexto = captura.pesoKg?.let { " — ${"%.1f".format(it)} kg" } ?: ""
                    Text("${captura.cantidad}x ${captura.especie}$pesoTexto")
                    TextButton(onClick = { viewModel.quitarCaptura(i) }) { Text("Quitar") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Foto (opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        uiState.fotoPath?.let { path ->
            val bitmap = remember(path) { runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto elegida",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        OutlinedButton(
            onClick = {
                lanzadorFoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (uiState.fotoPath != null) "Cambiar foto" else "Elegir foto") }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.comentario,
            onValueChange = viewModel::onComentarioChange,
            label = { Text("Comentario (opcional)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(onClick = { viewModel.guardar() }, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar salida")
        }
    }
}
