package com.berterogarcia.appnea.ui.historial

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.ui.components.ChipsSeleccionUnica
import com.berterogarcia.appnea.ui.components.TarjetaInmersion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.VeloMedio
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.berterogarcia.appnea.ui.components.TituloPantalla
import com.berterogarcia.appnea.ui.components.BotonVerEstadisticas
import androidx.compose.ui.res.stringResource

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_MES_ANIO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ES)

/**
 * Bitácora de inmersiones: buscador por lugar, filtro por período, lista agrupada por mes
 * y botón flotante para registrar. Toda la tarjeta abre el detalle.
 */
@Composable
fun HistorialScreen(
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel,
    onAgregarClick: () -> Unit,
    onVerDetalle: (Long) -> Unit,
    onVerEstadisticas: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val periodo by viewModel.periodo.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val tipo by viewModel.tipo.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    // El botón se achica a solo "+" cuando scrolleás, así tapa menos
    val botonExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Fijo arriba: título, buscador y filtros ----
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Encabezado(totalGeneral = estado.totalGeneral, onVerEstadisticas = onVerEstadisticas)
                Spacer(Modifier.height(12.dp))
                Buscador(texto = busqueda, onTextoChange = viewModel::onBusquedaChange)
                Spacer(Modifier.height(10.dp))
                ChipsSeleccionUnica(
                    opciones = listOf(null) + TipoInmersion.entries,
                    seleccionada = tipo,
                    etiqueta = { t -> t?.let { "${it.emoji} ${it.etiqueta}" } ?: "Todos" },
                    onSeleccion = viewModel::onTipoChange
                )
                Spacer(Modifier.height(6.dp))
                ChipsSeleccionUnica(
                    opciones = PeriodoFiltro.entries,
                    seleccionada = periodo,
                    etiqueta = { it.etiqueta },
                    onSeleccion = viewModel::onPeriodoChange
                )
            }

            // ---- Lista ----
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Acento)
                }
                estado.error -> MensajeCentrado(
                    titulo = "No se pudieron cargar tus inmersiones",
                    detalle = "Probá cerrar y volver a abrir la app."
                )
                estado.totalGeneral == 0 -> MensajeCentrado(
                    titulo = "Todavía no registraste inmersiones",
                    detalle = "Tocá \"Nueva inmersión\" para cargar la primera."
                )
                estado.cantidadFiltrada == 0 -> MensajeCentrado(
                    titulo = "No hay inmersiones para este filtro",
                    detalle = "Probá con otro tipo, período o búsqueda.",
                    accion = "Ver todas",
                    onAccion = viewModel::limpiarFiltros
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Abajo deja lugar para que el botón flotante no tape la última tarjeta
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
                ) {
                    item(key = "resumen") {
                        Text(
                            text = textoResumen(estado.cantidadFiltrada, periodo, busqueda, tipo),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    estado.grupos.forEach { grupo ->
                        item(key = "mes_${grupo.mes}") { TituloMes(grupo.mes) }
                        items(grupo.inmersiones, key = { it.id }) { inmersion ->
                            TarjetaInmersion(inmersion = inmersion, onClick = { onVerDetalle(inmersion.id) })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            text = { Text("Nueva inmersión", fontWeight = FontWeight.Normal) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_agregar),
                    contentDescription = "Nueva inmersión",
                    modifier = Modifier.size(26.dp)
                )
            },
            onClick = onAgregarClick,
            expanded = botonExpandido,
            shape = RoundedCornerShape(16.dp),
            containerColor = FondoTarjetaResaltada,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .border(1.dp, Acento.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
        )
    }
}

// ================================ ENCABEZADO Y FILTROS ================================

@Composable
private fun Encabezado(totalGeneral: Int, onVerEstadisticas: () -> Unit) {
    TituloPantalla(
        titulo = stringResource(R.string.titulo_inmersiones),
        subtitulo = if (totalGeneral > 0) stringResource(R.string.inmersiones_total, totalGeneral) else null,
        accion = { BotonVerEstadisticas(onClick = onVerEstadisticas) }
    )
}

@Composable
private fun Buscador(texto: String, onTextoChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Buscar por lugar") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTextoChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VeloMedio,
            unfocusedContainerColor = VeloMedio,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Acento,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedPlaceholderColor = TextoSecundario,
            unfocusedPlaceholderColor = TextoSecundario,
            focusedLeadingIconColor = TextoSecundario,
            unfocusedLeadingIconColor = TextoSecundario,
            focusedTrailingIconColor = TextoSecundario,
            unfocusedTrailingIconColor = TextoSecundario
        )
    )
}

@Composable
private fun TituloMes(mes: YearMonth) {
    Text(
        text = mes.format(FORMATO_MES_ANIO).replaceFirstChar { it.titlecase(LOCALE_ES) },
        style = MaterialTheme.typography.labelLarge,
        color = TextoSecundario,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun MensajeCentrado(
    titulo: String,
    detalle: String,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center
        )
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(accion, color = Acento) }
        }
    }
}

// ==================================== FORMATOS ====================================
private fun textoResumen(cantidad: Int, periodo: PeriodoFiltro, busqueda: String, tipo: TipoInmersion?): String {
    val sustantivo = if (cantidad == 1) "inmersión" else "inmersiones"
    val deTipo = tipo?.let { " de ${it.etiqueta.lowercase()}" }.orEmpty()
    val conBusqueda = busqueda.trim().takeIf { it.isNotEmpty() }?.let { " con \"$it\"" }.orEmpty()
    return "$cantidad $sustantivo$deTipo$conBusqueda ${periodo.descripcion}"
}