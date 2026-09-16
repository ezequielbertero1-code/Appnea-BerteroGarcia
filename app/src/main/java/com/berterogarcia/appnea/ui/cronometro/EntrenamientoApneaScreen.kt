package com.berterogarcia.appnea.ui.cronometro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.SesionApneaEntity
import com.berterogarcia.appnea.data.local.TablaApnea
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.ui.components.CheckboxGroup

@Composable
fun EntrenamientoApneaScreen(
    modifier: Modifier = Modifier,
    viewModel: EntrenamientoApneaViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AvisoSeguridadApnea()

        when (uiState.etapa) {
            EtapaEntrenamiento.SELECCION -> SeleccionTablaContenido(uiState, viewModel)
            EtapaEntrenamiento.EN_CURSO -> EnCursoContenido(uiState, viewModel)
            EtapaEntrenamiento.GUARDANDO -> GuardandoContenido(uiState, viewModel)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        TextButton(onClick = { viewModel.onMostrarHistorial(!uiState.mostrarHistorial) }) {
            Icon(
                painter = painterResource(R.drawable.ic_calendario),
                contentDescription = null,
                modifier = Modifier.width(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(if (uiState.mostrarHistorial) "Ocultar historial" else "Ver historial de sesiones")
        }
        if (uiState.mostrarHistorial) {
            HistorialSesiones(uiState.sesiones)
        }
    }
}

@Composable
private fun SeleccionTablaContenido(
    uiState: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Tablas predefinidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        uiState.tablasPredefinidas.forEach { tabla ->
            TablaOpcionCard(
                tabla = tabla,
                seleccionada = uiState.tablaSeleccionada == tabla,
                onClick = { viewModel.onSeleccionarTabla(tabla) }
            )
        }

        if (uiState.tablasPersonalizadas.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Mis tablas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            uiState.tablasPersonalizadas.forEach { entidad ->
                val tabla = entidad.aTablaApnea()
                TablaOpcionCard(
                    tabla = tabla,
                    seleccionada = uiState.tablaSeleccionada == tabla,
                    onClick = { viewModel.onSeleccionarTabla(tabla) },
                    onEliminar = { viewModel.onEliminarTablaPersonalizada(entidad) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        CreadorDeTablas(onTablaCreada = { viewModel.onGuardarTablaPersonalizada(it) })

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::onIniciar,
            enabled = uiState.tablaSeleccionada != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Iniciar entrenamiento") }
    }
}

@Composable
private fun TablaOpcionCard(
    tabla: TablaApnea,
    seleccionada: Boolean,
    onClick: () -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    val duracionTotal = tabla.fases.sumOf { it.duracionSegundos }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = if (seleccionada) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tabla.nombre, fontWeight = FontWeight.Bold)
                Text(
                    "${tabla.fases.size} fases · ${formatearDuracion(duracionTotal)} total",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (onEliminar != null) {
                IconButton(onClick = onEliminar) {
                    Icon(painterResource(R.drawable.ic_borrar), contentDescription = "Eliminar tabla")
                }
            }
        }
    }
}

@Composable
private fun EnCursoContenido(
    uiState: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel
) {
    val fase = uiState.faseActual ?: return
    val etiquetaFase = when (fase.tipo) {
        TipoFase.PREPARACION -> "Preparate"
        TipoFase.APNEA -> "Apnea"
        TipoFase.DESCANSO -> "Descanso"
    }
    val progreso = if (fase.duracionSegundos > 0) {
        1f - (uiState.segundosRestantes.toFloat() / fase.duracionSegundos.toFloat())
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fase ${uiState.indiceFase + 1} de ${uiState.totalFases}", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Text(etiquetaFase, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(formatearDuracion(uiState.segundosRestantes), fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progreso.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::onAbortar,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) { Text("Abortar") }
    }
}

@Composable
private fun GuardandoContenido(
    uiState: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (uiState.guardadoOk) {
            Text("¡Sesión guardada!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::onVolverASeleccion, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }
        } else {
            Text(
                if (uiState.completada) "¡Completaste la tabla!" else "Abortaste en la fase ${uiState.faseAlcanzada} de ${uiState.totalFases}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text("Mejor apnea sostenida: ${formatearDuracion(uiState.marcaSegundos)}")

            Spacer(Modifier.height(16.dp))
            CheckboxGroup(
                titulo = "¿Qué pudo haber influido hoy?",
                opciones = FACTORES_DISPONIBLES,
                seleccionadas = uiState.factoresSeleccionados,
                onCambio = viewModel::onFactorToggle
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.sensaciones,
                onValueChange = viewModel::onSensacionesChange,
                label = { Text("¿Cómo te sentiste?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row {
                Button(onClick = viewModel::onGuardarSesion) { Text("Guardar") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = viewModel::onVolverASeleccion) { Text("Descartar") }
            }
        }
    }
}

@Composable
private fun HistorialSesiones(sesiones: List<SesionApneaEntity>) {
    if (sesiones.isEmpty()) {
        Text("Todavía no guardaste ninguna sesión.", style = MaterialTheme.typography.bodyMedium)
        return
    }
    Column {
        sesiones.forEach { sesion -> SesionCard(sesion) }
    }
}

@Composable
private fun SesionCard(sesion: SesionApneaEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${sesion.fecha} — ${sesion.nombreTabla}", fontWeight = FontWeight.Bold)
                Text(if (sesion.completada) "Completa" else "Abortada (${sesion.faseAlcanzada}/${sesion.totalFases})")
            }
            Text("Marca: ${formatearDuracion(sesion.marcaSegundos)}")
            if (sesion.factores.isNotEmpty()) {
                Text(
                    "Factores: ${sesion.factores.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (sesion.sensaciones.isNotBlank()) {
                Text("\"${sesion.sensaciones}\"", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
