package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.ui.components.CheckboxGroup
import com.berterogarcia.appnea.ui.components.DatePickerField
import com.berterogarcia.appnea.ui.components.TimePickerField

private val OPCIONES_AGUA = listOf(
    "Dulce", "Salada", "Orilla", "Bote", "Olas", "Corrientes", "Resaca"
)

private val OPCIONES_EQUIPAMIENTO = listOf(
    "Ninguna", "Corto", "Largo", "Dos piezas", "Semi seco", "Seco",
    "Guantes", "Botas", "Casco"
)

private val tiempoFondoVisualTransformation = VisualTransformation { texto ->
    val digitos = texto.text
    val formateado = buildString {
        digitos.forEachIndexed { index, c ->
            if (index == 2) append(':')
            append(c)
        }
    }
    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int) =
            if (offset <= 2) offset else offset + 1
        override fun transformedToOriginal(offset: Int) =
            if (offset <= 2) offset else offset - 1
    }
    TransformedText(AnnotatedString(formateado), offsetMapping)
}

@Composable
fun NuevaInmersionScreen(
    modifier: Modifier = Modifier,
    viewModel: NuevaInmersionViewModel,
    onGuardado: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.guardadoOk) {
        if (uiState.guardadoOk) onGuardado()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // FECHA
        DatePickerField(
            label = "Fecha",
            valor = uiState.fecha,
            onValorChange = viewModel::onFechaChange,
            isError = uiState.errores.containsKey(Campo.FECHA),
            mensajeError = uiState.errores[Campo.FECHA],
            modifier = Modifier.fillMaxWidth()
        )
        // LUGAR
        OutlinedTextField(
            value = uiState.lugar,
            onValueChange = viewModel::onLugarChange,
            label = { Text("Lugar") },
            isError = uiState.errores.containsKey(Campo.LUGAR),
            supportingText = {
                uiState.errores[Campo.LUGAR]?.let { Text(it) }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // PROFUNDIDAD MAXIMA
        OutlinedTextField(
            value = uiState.profundidadMetros,
            onValueChange = viewModel::onProfundidadChange,
            label = { Text("Profundidad máxima (m)") },
            isError = uiState.errores.containsKey(Campo.PROFUNDIDAD),
            supportingText = {
                uiState.errores[Campo.PROFUNDIDAD]?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // PARADA DE SEGURIDAD
        OutlinedTextField(
            value = uiState.paradaSeguridadMin,
            onValueChange = viewModel::onParadaChange,
            label = { Text("Parada de seguridad (min)") },
            isError = uiState.errores.containsKey(Campo.PARADA),
            supportingText = {
                uiState.errores[Campo.PARADA]?.let { Text(it)}
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // TIEMPO DE FONDO
        OutlinedTextField(
            value = uiState.tiempoFondo,
            onValueChange = viewModel::onTiempoFondoChange,
            label = { Text("Tiempo de fondo (mm:ss)") },
            visualTransformation = tiempoFondoVisualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = uiState.errores.containsKey(Campo.TIEMPO_FONDO),
            supportingText = { uiState.errores[Campo.TIEMPO_FONDO]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // HORA DE ENTRADA
        TimePickerField(
            label = "Hora de entrada",
            valor = uiState.horaEntrada,
            onValorChange = viewModel::onHoraEntradaChange,
            isError = uiState.errores.containsKey(Campo.HORA_ENTRADA),
            mensajeError = uiState.errores[Campo.HORA_ENTRADA],
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // HORA DE SALIDA
        TimePickerField(
            label = "Hora de salida",
            valor = uiState.horaSalida,
            onValorChange = viewModel::onHoraSalidaChange,
            isError = uiState.errores.containsKey(Campo.HORA_SALIDA),
            mensajeError = uiState.errores[Campo.HORA_SALIDA],
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // CARACTERISTICAS DEL AGUA
        CheckboxGroup(
            titulo = "Características del agua",
            opciones = OPCIONES_AGUA,
            seleccionadas = uiState.caracteristicasAgua,
            onCambio = viewModel::onAguaToggle
        )
        // LASTRE
        OutlinedTextField(
            value = uiState.lastreKg,
            onValueChange = viewModel::onLastreChange,
            label = { Text("Lastre (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = uiState.errores.containsKey(Campo.LASTRE),
            supportingText = { uiState.errores[Campo.LASTRE]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        // PROTECCION
        OutlinedTextField(
            value = uiState.proteccionMm,
            onValueChange = viewModel::onProteccionMmChange,
            label = { Text("Protección (mm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = uiState.errores.containsKey(Campo.PROTECCION),
            supportingText = { uiState.errores[Campo.PROTECCION]?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // EQUIPAMIENTO
        CheckboxGroup(
            titulo = "Equipamiento",
            opciones = OPCIONES_EQUIPAMIENTO,
            seleccionadas = uiState.equipamiento,
            onCambio = viewModel::onEquipamientoToggle
        )
        // TEMPERATURA DEL AIRE
        OutlinedTextField(
            value = uiState.temperaturaAireC,
            onValueChange = viewModel::onTempAireChange,
            isError = uiState.errores.containsKey(Campo.TEMP_AIRE),
            supportingText = {
                uiState.errores[Campo.TEMP_AIRE]?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Temperatura del aire (°C)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // TEMPERATURA DEL AGUA
        OutlinedTextField(
            value = uiState.temperaturaAguaC,
            onValueChange = viewModel::onTempAguaChange,
            isError = uiState.errores.containsKey(Campo.TEMP_AGUA),
            supportingText = {
                uiState.errores[Campo.TEMP_AGUA]?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Temperatura del agua (°C)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // VISIBILIDAD
        OutlinedTextField(
            value = uiState.visibilidadMetros,
            onValueChange = viewModel::onVisibilidadChange,
            isError = uiState.errores.containsKey(Campo.VISIBILIDAD),
            supportingText = {
                uiState.errores[Campo.VISIBILIDAD]?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("Visibilidad (m)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // ACTIVIDAD
        OutlinedTextField(
            value = uiState.actividad,
            onValueChange = viewModel::onActividadChange,
            label = { Text("Actividad") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            minLines = 3
        )
        // COMENTARIOS
        OutlinedTextField(
            value = uiState.comentarios,
            onValueChange = viewModel::onComentariosChange,
            label = { Text("Comentarios") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            minLines = 3
        )
        // GUARDAR
        Button(
            onClick = viewModel::guardar,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Guardar inmersión")
        }
    }
}