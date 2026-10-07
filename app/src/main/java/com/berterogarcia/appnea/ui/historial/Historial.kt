package com.berterogarcia.appnea.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.CondicionesRegistro
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.remote.emojiClima
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.condiciones.direccion
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

private val TextoChip = Color(0xFFE3F2F7)
private val FondoTranslucido = Color.White.copy(alpha = 0.08f)
private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_MES_ANIO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ES)
private val FORMATO_MES_CORTO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", LOCALE_ES)

/**
 * Bitácora de inmersiones: buscador por lugar, filtro por período, lista agrupada por mes
 * y botón flotante para registrar. Toda la tarjeta abre el detalle.
 */
@Composable
fun HistorialScreen(
    modifier: Modifier = Modifier,
    viewModel: HistorialViewModel,
    onAgregarClick: () -> Unit,
    onVerDetalle: (Long) -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val periodo by viewModel.periodo.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    // El botón se achica a solo "+" cuando scrolleás, así tapa menos
    val botonExpandido by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Fijo arriba: título, buscador y filtros ----
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Encabezado(totalGeneral = estado.totalGeneral)
                Spacer(Modifier.height(12.dp))
                Buscador(texto = busqueda, onTextoChange = viewModel::onBusquedaChange)
                Spacer(Modifier.height(10.dp))
                FiltrosPeriodo(seleccionado = periodo, onSeleccion = viewModel::onPeriodoChange)
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
                    detalle = "Probá con otro período u otra búsqueda.",
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
                            text = textoResumen(estado.cantidadFiltrada, periodo, busqueda),
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
            text = { Text("Nueva inmersión", fontWeight = FontWeight.Bold) },
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
private fun Encabezado(totalGeneral: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            "Inmersiones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (totalGeneral > 0) {
            Text("$totalGeneral en total", style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
        }
    }
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
            focusedContainerColor = FondoTranslucido,
            unfocusedContainerColor = FondoTranslucido,
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
private fun FiltrosPeriodo(seleccionado: PeriodoFiltro, onSeleccion: (PeriodoFiltro) -> Unit) {
    // Scroll horizontal por si la pantalla es angosta o la letra está agrandada
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PeriodoFiltro.entries.forEach { periodo ->
            val elegido = periodo == seleccionado
            FilterChip(
                selected = elegido,
                onClick = { onSeleccion(periodo) },
                label = { Text(periodo.etiqueta, fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = TextoChip,
                    selectedContainerColor = FondoTarjetaResaltada,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = elegido,
                    borderColor = Color.White.copy(alpha = 0.18f),
                    selectedBorderColor = Acento.copy(alpha = 0.55f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
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

// ==================================== TARJETA ====================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaInmersion(inmersion: InmersionEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fecha en bloque
                Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d".format(inmersion.fecha.dayOfMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(mesCorto(inmersion.fecha), style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        inmersion.lugar.ifBlank { "Sin lugar" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    primeraLinea(inmersion.actividad)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "%.1f m".format(inmersion.profundidadMetros),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Acento
                )
            }

            // Datos de la inmersión
            FlowRow(
                modifier = Modifier.padding(top = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ChipDato("⏱ Fondo ${formatoDuracion(inmersion.tiempoFondo)}")
                ChipDato("🌡️ Agua ${numeroCorto(inmersion.temperaturaAguaC)} °C")
                if (inmersion.visibilidadMetros > 0f) {
                    ChipDato("👁 Visib. ${numeroCorto(inmersion.visibilidadMetros)} m")
                }
            }

            // Condiciones guardadas al registrar (las inmersiones viejas no tienen: no se muestra nada)
            inmersion.condiciones?.let { LineaCondiciones(it) }
        }
    }
}

@Composable
private fun ChipDato(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = TextoChip,
        modifier = Modifier
            .background(FondoTranslucido, RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineaCondiciones(c: CondicionesRegistro) {
    val datos = buildList {
        if (c.codigoClima != null || c.temperaturaC != null) {
            val emoji = c.codigoClima?.let { emojiClima(it) } ?: "🌡️"
            add("$emoji ${c.temperaturaC?.let { "${it.roundToInt()} °C" }.orEmpty()}".trim())
        }
        c.vientoKmh?.let { v ->
            // direccion() devuelve "-" si no hay dato: se recorta para que no quede "12 km/h -"
            add("💨 ${v.roundToInt()} km/h ${direccion(c.direccionVientoGrados)}".trimEnd('-', ' '))
        }
        c.olaM?.let { add("🌊 ${"%.1f".format(it)} m") }
        c.faseLunar.takeIf { it.isNotBlank() }?.let { add("🌙 $it") }
    }
    if (datos.isEmpty()) return

    HorizontalDivider(modifier = Modifier.padding(top = 9.dp), color = Color.White.copy(alpha = 0.1f))
    FlowRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        datos.forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = TextoSecundario) }
    }
}

// ==================================== FORMATOS ====================================

private fun textoResumen(cantidad: Int, periodo: PeriodoFiltro, busqueda: String): String {
    val sustantivo = if (cantidad == 1) "inmersión" else "inmersiones"
    val conBusqueda = busqueda.trim().takeIf { it.isNotEmpty() }?.let { " con \"$it\"" }.orEmpty()
    return "$cantidad $sustantivo$conBusqueda ${periodo.descripcion}"
}

/** "oct." → "oct" (según la versión de Android, el mes abreviado puede venir con punto). */
private fun mesCorto(fecha: LocalDate): String = fecha.format(FORMATO_MES_CORTO).removeSuffix(".")

/** La actividad es texto libre de varias líneas: en la tarjeta va solo la primera. */
private fun primeraLinea(texto: String): String? =
    texto.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }

/** 11.0 → "11", 11.5 → "11,5" */
private fun numeroCorto(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

/** mm:ss */
private fun formatoDuracion(d: Duration): String =
    d.toComponents { minutos, segundos, _ -> "%d:%02d".format(minutos, segundos) }