package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.ui.components.CampoTexto
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
        CampoTexto(
            valor = uiState.lugar,
            onValorChange = viewModel::onLugarChange,
            etiqueta = "Lugar",
            esError = uiState.errores.containsKey(Campo.LUGAR),
            mensajeError = uiState.errores[Campo.LUGAR] ?: "",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // PROFUNDIDAD MAXIMA
        CampoTexto(
            valor = uiState.profundidadMetros,
            onValorChange = viewModel::onProfundidadChange,
            etiqueta = "Profundidad máxima (m)",
            esError = uiState.errores.containsKey(Campo.PROFUNDIDAD),
            mensajeError = uiState.errores[Campo.PROFUNDIDAD] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // PARADA DE SEGURIDAD
        CampoTexto(
            valor = uiState.paradaSeguridadMin,
            onValorChange = viewModel::onParadaChange,
            etiqueta = "Parada de seguridad (min)",
            esError = uiState.errores.containsKey(Campo.PARADA),
            mensajeError = uiState.errores[Campo.PARADA] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // TIEMPO DE FONDO
        CampoTexto(
            valor = uiState.tiempoFondo,
            onValorChange = viewModel::onTiempoFondoChange,
            etiqueta = "Tiempo de fondo (mm:ss)",
            visualTransformation = tiempoFondoVisualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            esError = uiState.errores.containsKey(Campo.TIEMPO_FONDO),
            mensajeError = uiState.errores[Campo.TIEMPO_FONDO] ?: "",
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
        CampoTexto(
            valor = uiState.lastreKg,
            onValorChange = viewModel::onLastreChange,
            etiqueta = "Lastre (kg)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            esError = uiState.errores.containsKey(Campo.LASTRE),
            mensajeError = uiState.errores[Campo.LASTRE] ?: "",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // PROTECCION
        CampoTexto(
            valor = uiState.proteccionMm,
            onValorChange = viewModel::onProteccionMmChange,
            etiqueta = "Protección (mm)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            esError = uiState.errores.containsKey(Campo.PROTECCION),
            mensajeError = uiState.errores[Campo.PROTECCION] ?: "",
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
        CampoTexto(
            valor = uiState.temperaturaAireC,
            onValorChange = viewModel::onTempAireChange,
            etiqueta = "Temperatura del aire (°C)",
            esError = uiState.errores.containsKey(Campo.TEMP_AIRE),
            mensajeError = uiState.errores[Campo.TEMP_AIRE] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // TEMPERATURA DEL AGUA
        CampoTexto(
            valor = uiState.temperaturaAguaC,
            onValorChange = viewModel::onTempAguaChange,
            etiqueta = "Temperatura del agua (°C)",
            esError = uiState.errores.containsKey(Campo.TEMP_AGUA),
            mensajeError = uiState.errores[Campo.TEMP_AGUA] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // VISIBILIDAD
        CampoTexto(
            valor = uiState.visibilidadMetros,
            onValorChange = viewModel::onVisibilidadChange,
            etiqueta = "Visibilidad (m)",
            esError = uiState.errores.containsKey(Campo.VISIBILIDAD),
            mensajeError = uiState.errores[Campo.VISIBILIDAD] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // ACTIVIDAD
        CampoTexto(
            valor = uiState.actividad,
            onValorChange = viewModel::onActividadChange,
            etiqueta = "Actividad",
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // COMENTARIOS
        CampoTexto(
            valor = uiState.comentarios,
            onValorChange = viewModel::onComentariosChange,
            etiqueta = "Comentarios",
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        // GUARDAR
        Button(
            onClick = viewModel::guardar,
            enabled = !uiState.guardando,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text(if (uiState.guardando) "Guardando y obteniendo condiciones…" else "Guardar inmersión")
        }
        Text(
            "Al guardar se registran solas las condiciones del momento de entrada: clima, olas, marea y luna.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}