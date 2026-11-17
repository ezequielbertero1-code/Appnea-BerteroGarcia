package com.berterogarcia.appnea.ui.detalleinmersion

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Inmersion
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Duration as JDuration

internal val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
internal val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
internal val PESTANIAS = listOf(
    R.string.detalle_tab_general,
    R.string.detalle_tab_grafico,
    R.string.detalle_tab_equipo,
    R.string.detalle_tab_condiciones
)

/**
 * Detalle de una inmersión (estilo "Appnea circular"): barra con volver, editar y menú ⋮
 * (editar, exportar UDDF, eliminar en rojo), pestañas desplazables y el contenido de cada una.
 */
@Composable
fun DetalleInmersionScreen(
    modifier: Modifier = Modifier,
    viewModel: DetalleInmersionViewModel,
    onEliminada: () -> Unit,
    onEditar: (Long) -> Unit = {},
    onVolver: () -> Unit = {}
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }
    val inmersion by viewModel.inmersion.collectAsStateWithLifecycle()
    val perfil by viewModel.perfil.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { PESTANIAS.size }
    val textoExportada = stringResource(R.string.detalle_exportada)
    val textoNoExportada = stringResource(R.string.detalle_no_exportada)
    val textoEliminada = stringResource(R.string.detalle_eliminada)
    val textoNoEliminada = stringResource(R.string.detalle_no_eliminada)

    // Abre el selector de Android para elegir dónde guardar el archivo
    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.exportarUddf(uri.toString())
            Toast.makeText(context, if (ok) textoExportada else textoNoExportada, Toast.LENGTH_SHORT).show()
        }
    }

    // Resultado de eliminar (antes del return: después de borrar, la inmersión pasa a null)
    val eliminada by viewModel.eliminada.collectAsStateWithLifecycle()
    LaunchedEffect(eliminada) {
        when (eliminada) {
            true -> {
                Toast.makeText(context, textoEliminada, Toast.LENGTH_SHORT).show()
                onEliminada()
            }
            false -> {
                Toast.makeText(context, textoNoEliminada, Toast.LENGTH_SHORT).show()
                viewModel.errorEliminarMostrado()
            }
            null -> Unit
        }
    }

    val i = inmersion ?: return   // todavía cargando desde Room

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = i.lugar.ifBlank { stringResource(R.string.inicio_sin_lugar) },
            navegacion = NavegacionBarra.VOLVER,
            onNavegacion = onVolver,
            acciones = {
                IconButton(onClick = { onEditar(i.id) }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.detalle_editar))
                }
                Box {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.detalle_mas_opciones))
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detalle_editar)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                onEditar(i.id)
                            },
                            colors = MenuDefaults.itemColors(textColor = TextoSuperficie, leadingIconColor = TextoSuperficieVariante)
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detalle_exportar)) },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_ms_ios_share), contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                exportarLauncher.launch("inmersion_${i.id}_${i.fecha}.uddf")
                            },
                            colors = MenuDefaults.itemColors(textColor = TextoSuperficie, leadingIconColor = TextoSuperficieVariante)
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detalle_eliminar)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                mostrarDialogo = true
                            },
                            colors = MenuDefaults.itemColors(textColor = ColorError, leadingIconColor = ColorError)
                        )
                    }
                }
            }
        )

        PrimaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Acento,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = Divisor) }
        ) {
            PESTANIAS.forEachIndexed { index, titulo ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(titulo)) },
                    selectedContentColor = Acento,
                    unselectedContentColor = TextoSuperficieVariante
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (page) {
                    0 -> PestanaGeneral(i)
                    1 -> PestanaGrafico(i, perfil)
                    2 -> PestanaEquipo(i)
                    3 -> PestanaCondiciones(i)
                }
            }
        }
    }

    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = ColorError) },
            title = { Text(stringResource(R.string.detalle_eliminar)) },
            text = { Text(stringResource(R.string.detalle_eliminar_confirmacion, i.lugar)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogo = false
                    viewModel.eliminar()
                }) { Text(stringResource(R.string.accion_eliminar), color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        )
    }
}

// ============================ Pestañas ============================

// Minutos entre entrada y salida (si cruza la medianoche, suma 24 h)
internal fun minutosEnAgua(i: Inmersion): Long {
    val m = JDuration.between(i.horaEntrada, i.horaSalida).toMinutes()
    return if (m >= 0) m else m + 24 * 60
}

// 18.0 -> "18", 18.5 -> "18,5"
internal fun Float.limpio(): String =
    if (this % 1f == 0f) toInt().toString() else "%.1f".format(Locale.forLanguageTag("es-AR"), this)

// 1.0 -> "1", 0.3 -> "0,3" (para las velocidades, que son Double)
internal fun Double.limpio(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(Locale.forLanguageTag("es-AR"), this)
