package com.berterogarcia.appnea.ui.historial

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonFlotante
import com.berterogarcia.appnea.ui.components.ChipFiltro
import com.berterogarcia.appnea.ui.inmersion.FilaInmersion
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.SuperficieTonalAlta
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")
private val FORMATO_MES_ANIO: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ES)

/**
 * Bitácora de inmersiones (estilo "Appnea circular"): barra superior con acceso a Estadísticas,
 * buscador en pastilla, chips de tipo + período, lista agrupada por mes y botón flotante para
 * registrar. Toda la fila abre el detalle.
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
            BarraSuperior(
                titulo = stringResource(R.string.titulo_inmersiones),
                acciones = {
                    IconButton(onClick = onVerEstadisticas) {
                        Icon(
                            painterResource(R.drawable.ic_ms_bar_chart),
                            contentDescription = stringResource(R.string.accion_estadisticas)
                        )
                    }
                }
            )

            // ---- Fijo arriba: buscador y filtros ----
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Buscador(texto = busqueda, onTextoChange = viewModel::onBusquedaChange)
                Filtros(
                    tipo = tipo,
                    onTipo = viewModel::onTipoChange,
                    periodo = periodo,
                    onPeriodo = viewModel::onPeriodoChange
                )
            }

            // ---- Lista ----
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Acento)
                }
                estado.error -> MensajeCentrado(
                    titulo = stringResource(R.string.historial_error_titulo),
                    detalle = stringResource(R.string.historial_error_detalle)
                )
                estado.totalGeneral == 0 -> MensajeCentrado(
                    titulo = stringResource(R.string.historial_vacio_titulo),
                    detalle = stringResource(R.string.historial_vacio_detalle)
                )
                estado.cantidadFiltrada == 0 -> MensajeCentrado(
                    titulo = stringResource(R.string.historial_sin_resultados_titulo),
                    detalle = stringResource(R.string.historial_sin_resultados_detalle),
                    accion = stringResource(R.string.inicio_ver_todas),
                    onAccion = viewModel::limpiarFiltros
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Abajo deja lugar para que el botón flotante no tape la última fila
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
                ) {
                    item(key = "resumen") {
                        Text(
                            text = textoResumen(estado.cantidadFiltrada, periodo, busqueda, tipo),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuperficieVariante,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    estado.grupos.forEach { grupo ->
                        item(key = "mes_${grupo.mes}") { TituloMes(grupo.mes, grupo.inmersiones.size) }
                        itemsIndexed(grupo.inmersiones, key = { _, it -> it.id }) { indice, inmersion ->
                            FilaInmersion(
                                inmersion = inmersion,
                                indice = indice,
                                total = grupo.inmersiones.size,
                                onClick = { onVerDetalle(inmersion.id) },
                                conTemperaturaAgua = true,
                                // Mismo espacio de 2 dp que GrupoLista entre filas
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        BotonFlotante(
            texto = stringResource(R.string.historial_nueva),
            icono = rememberVectorPainter(Icons.Rounded.Add),
            onClick = onAgregarClick,
            expandido = botonExpandido,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

// ================================ BUSCADOR Y FILTROS ================================

/** Buscador en pastilla (velo 11 %), como la SearchBar de Material 3 sin la vista expandida. */
@Composable
private fun Buscador(texto: String, onTextoChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.historial_buscar)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTextoChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.historial_borrar_busqueda))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = SuperficieTonalAlta,
            unfocusedContainerColor = SuperficieTonalAlta,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Acento,
            focusedTextColor = TextoSuperficie,
            unfocusedTextColor = TextoSuperficie,
            focusedPlaceholderColor = TextoSuperficieVariante,
            unfocusedPlaceholderColor = TextoSuperficieVariante,
            focusedLeadingIconColor = TextoSuperficieVariante,
            unfocusedLeadingIconColor = TextoSuperficieVariante,
            focusedTrailingIconColor = TextoSuperficieVariante,
            unfocusedTrailingIconColor = TextoSuperficieVariante
        )
    )
}

/** Una fila: Todas | Buceo | Apnea y, al final, el período como chip con menú. */
@Composable
private fun Filtros(
    tipo: TipoInmersion?,
    onTipo: (TipoInmersion?) -> Unit,
    periodo: PeriodoFiltro,
    onPeriodo: (PeriodoFiltro) -> Unit
) {
    // Scroll horizontal por si la pantalla es angosta o la letra está agrandada
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChipFiltro(
            texto = stringResource(R.string.historial_filtro_todas),
            elegido = tipo == null,
            onClick = { onTipo(null) }
        )
        TipoInmersion.entries.forEach { t ->
            ChipFiltro(texto = t.nombre(), elegido = tipo == t, onClick = { onTipo(t) })
        }
        ChipPeriodo(periodo = periodo, onPeriodo = onPeriodo)
    }
}

/** Chip con flechita que abre un menú con los períodos; queda "elegido" si filtra algo. */
@Composable
private fun ChipPeriodo(periodo: PeriodoFiltro, onPeriodo: (PeriodoFiltro) -> Unit) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    val filtra = periodo != PeriodoFiltro.TODAS
    Box {
        FilterChip(
            selected = filtra,
            onClick = { abierto = true },
            label = { Text(stringResource(periodo.etiqueta)) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = TextoSuperficieVariante,
                iconColor = TextoSuperficieVariante,
                selectedContainerColor = ContenedorSecundario,
                selectedLabelColor = TextoSobreContenedorSecundario,
                selectedTrailingIconColor = TextoSobreContenedorSecundario
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = filtra,
                borderColor = ContornoFuerte,
                selectedBorderColor = Color.Transparent,
                borderWidth = 1.dp,
                selectedBorderWidth = 0.dp
            )
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            PeriodoFiltro.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(stringResource(opcion.etiqueta)) },
                    onClick = {
                        onPeriodo(opcion)
                        abierto = false
                    },
                    trailingIcon = if (opcion == periodo) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    colors = MenuDefaults.itemColors(textColor = TextoSuperficie, trailingIconColor = Acento)
                )
            }
        }
    }
}

// ================================ LISTA ================================

/** "Octubre 2026 · 3 inmersiones" en cian, como los encabezados de grupo del resto de la app. */
@Composable
private fun TituloMes(mes: YearMonth, cantidad: Int) {
    Text(
        text = pluralStringResource(
            R.plurals.historial_mes_cantidad,
            cantidad,
            mes.format(FORMATO_MES_ANIO).replaceFirstChar { it.titlecase(LOCALE_ES) },
            cantidad
        ),
        style = MaterialTheme.typography.titleSmall,
        color = Acento,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
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
            color = TextoSuperficie,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuperficieVariante,
            textAlign = TextAlign.Center
        )
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(accion, color = Acento) }
        }
    }
}

// ==================================== FORMATOS ====================================

/** "3 inmersiones de buceo con “pirámides” en el último mes" */
@Composable
private fun textoResumen(cantidad: Int, periodo: PeriodoFiltro, busqueda: String, tipo: TipoInmersion?): String {
    val partes = buildList {
        add(pluralStringResource(R.plurals.historial_cantidad, cantidad, cantidad))
        tipo?.let { add(stringResource(R.string.historial_resumen_tipo, it.nombre().lowercase(LOCALE_ES))) }
        busqueda.trim().takeIf { it.isNotEmpty() }?.let { add(stringResource(R.string.historial_resumen_busqueda, it)) }
        add(stringResource(periodo.descripcion))
    }
    return partes.joinToString(" ")
}
