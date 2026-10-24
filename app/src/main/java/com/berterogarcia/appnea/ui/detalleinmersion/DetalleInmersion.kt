package com.berterogarcia.appnea.ui.detalleinmersion

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.Chips
import com.berterogarcia.appnea.ui.components.DatoChico
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.Insignia
import com.berterogarcia.appnea.ui.components.MapaLibre
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.PinMapa
import com.berterogarcia.appnea.ui.components.TarjetaAviso
import com.berterogarcia.appnea.ui.components.TarjetaHero
import com.berterogarcia.appnea.ui.components.TarjetaTonal
import com.berterogarcia.appnea.ui.components.TipoInsignia
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.formatoCoordenadas
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.nombreFaseLunar
import com.berterogarcia.appnea.ui.condiciones.FilaDato
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.colorIconoClima
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.duracion
import com.berterogarcia.appnea.ui.condiciones.iconoClima
import com.berterogarcia.appnea.ui.condiciones.num
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.Duration as JDuration

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm")
private val PESTANIAS = listOf(
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

    // Abre el selector de Android para elegir dónde guardar el archivo
    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.exportarUddf(uri)
            Toast.makeText(context, if (ok) textoExportada else textoNoExportada, Toast.LENGTH_SHORT).show()
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
                    viewModel.eliminar {
                        Toast.makeText(context, textoEliminada, Toast.LENGTH_SHORT).show()
                        onEliminada()
                    }
                }) { Text(stringResource(R.string.accion_eliminar), color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.accion_cancelar)) }
            }
        )
    }
}

// ============================ Pestañas ============================

@Composable
private fun PestanaGeneral(i: InmersionEntity) {
    val apnea = PerfilSimulado.esApnea(i)
    val fondo = formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt())

    // ---- Profundidad máxima + tipo y fecha ----
    TarjetaTonal {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(i.profundidadMetros.limpio(), fontSize = 44.sp, lineHeight = 46.sp, color = TextoSuperficie)
                    Text(" m", fontSize = 20.sp, color = TextoSuperficieVariante, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    stringResource(R.string.detalle_profundidad_maxima),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Insignia(
                    texto = i.tipo.nombre(),
                    tipo = TipoInsignia.INFO,
                    icono = painterResource(if (apnea) R.drawable.ic_ms_air else R.drawable.ic_ms_scuba_diving)
                )
                Text(
                    i.fecha.format(FORMATO_FECHA),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // ---- Tiempos ----
    val filas = buildList {
        add(Triple(R.drawable.ic_ms_login, stringResource(R.string.detalle_entrada), i.horaEntrada.format(HH_MM)))
        add(Triple(R.drawable.ic_ms_logout, stringResource(R.string.detalle_salida), i.horaSalida.format(HH_MM)))
        add(Triple(R.drawable.ic_ms_schedule, stringResource(R.string.detalle_en_el_agua), duracion(minutosEnAgua(i))))
        add(
            Triple(
                R.drawable.ic_ms_timer,
                stringResource(if (apnea) R.string.detalle_tiempo_apnea else R.string.detalle_tiempo_fondo),
                fondo
            )
        )
    }
    val conParada = !apnea
    val total = filas.size + if (conParada) 1 else 0
    GrupoLista(modifier = Modifier.padding(top = 12.dp)) {
        filas.forEachIndexed { indice, (icono, titulo, valor) ->
            FilaValor(icono = icono, titulo = titulo, valor = valor, indice = indice, total = total)
        }
        if (conParada) {
            val hayParada = i.paradaSeguridadMin > 0f
            FilaValor(
                icono = R.drawable.ic_ms_pause_circle,
                titulo = stringResource(R.string.detalle_parada),
                subtitulo = if (hayParada) stringResource(R.string.detalle_parada_profundidad) else null,
                valor = if (hayParada) stringResource(R.string.detalle_minutos, i.paradaSeguridadMin.limpio())
                else stringResource(R.string.detalle_sin_parada),
                indice = filas.size,
                total = total
            )
        }
    }

    TarjetaUbicacion(i)   // no muestra nada si la inmersión no tiene coordenadas

    // ---- Notas ----
    EncabezadoGrupo(titulo = stringResource(R.string.detalle_notas))
    TarjetaTonal {
        Text(stringResource(R.string.detalle_actividad), style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
        Text(
            i.actividad.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuperficie,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        if (i.comentarios.isNotBlank()) {
            // Comentario como "cita": barrita cian a la izquierda
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(Acento))
                Spacer(Modifier.width(10.dp))
                Text(
                    "“${i.comentarios}”",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSuperficieVariante
                )
            }
        }
    }
}

/** Fila con ícono a la izquierda y el valor destacado a la derecha ("Entrada ····· 10:15"). */
@Composable
private fun FilaValor(icono: Int, titulo: String, valor: String, indice: Int, total: Int, subtitulo: String? = null) {
    FilaLista(
        titulo = titulo,
        subtitulo = subtitulo,
        indice = indice,
        total = total,
        inicio = { Icon(painterResource(icono), contentDescription = null) },
        fin = { Text(valor, style = MaterialTheme.typography.titleMedium, color = TextoSuperficie) }
    )
}

/** Mini mapa del lugar marcado. Las inmersiones sin coordenadas no muestran la sección. */
@Composable
private fun TarjetaUbicacion(i: InmersionEntity) {
    val lat = i.latitud ?: return
    val lon = i.longitud ?: return
    val context = LocalContext.current
    val etiquetaPorDefecto = stringResource(R.string.detalle_etiqueta_mapa)
    val sinAppMapas = stringResource(R.string.detalle_sin_app_mapas)

    // Abre la app de mapas del teléfono con un pin con el nombre del lugar.
    // Coordenadas con Locale.US: el URI necesita punto decimal, no coma.
    val abrirEnMapas: () -> Unit = {
        val etiqueta = Uri.encode(i.lugar.ifBlank { etiquetaPorDefecto })
        val coords = "%.6f,%.6f".format(Locale.US, lat, lon)
        val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$coords($etiqueta)".toUri())
        // Si no hay app de mapas (o falla por cualquier motivo) se avisa en vez de crashear
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, sinAppMapas, Toast.LENGTH_SHORT).show() }
    }

    EncabezadoGrupo(
        titulo = stringResource(R.string.detalle_lugar),
        accion = stringResource(R.string.detalle_abrir_mapas),
        onAccion = abrirEnMapas
    )
    TarjetaTonal(relleno = PaddingValues(0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            MapaLibre(
                centro = Ubicacion(lat, lon),
                zoom = 14.0,
                interactivo = false,
                modifier = Modifier.matchParentSize()
            )
            PinMapa(Modifier.align(Alignment.Center))
            // Capa transparente: un toque abre Mapas y el deslizar lateral sigue cambiando de pestaña
            Box(Modifier.matchParentSize().clickable(onClick = abrirEnMapas))
        }
        Text(
            formatoCoordenadas(lat, lon),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun PestanaGrafico(i: InmersionEntity, perfil: List<PuntoPerfil>) {
    // La curva se dibuja de izquierda a derecha cada vez que se entra a la pestaña
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(perfil) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }

    TarjetaCondicion(stringResource(R.string.detalle_perfil)) {
        if (perfil.size < 2) {
            Text(stringResource(R.string.detalle_perfil_sin_datos), color = TextoSuperficieVariante)
        } else {
            GraficoPerfil(perfil, progreso = progreso.value)
        }
    }

    if (perfil.size >= 2) {
        val apnea = PerfilSimulado.esApnea(i)
        val bajada = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.DESCENSO_BUCEO * 60).limpio()} m/min"
        val subida = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.ASCENSO_BUCEO * 60).limpio()} m/min"
        val supuesto = stringResource(R.string.detalle_supuesto)
        FilaMosaicos {
            Mosaico(stringResource(R.string.detalle_descenso), bajada, supuesto)
            Mosaico(stringResource(R.string.detalle_ascenso), subida, supuesto)
            Mosaico(stringResource(R.string.entrenar_duracion), formatearDuracion(perfil.last().segundo), stringResource(R.string.detalle_del_perfil))
        }
    }

    TarjetaAviso(
        titulo = stringResource(R.string.detalle_perfil_estimado),
        texto = stringResource(R.string.detalle_perfil_estimado_texto),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun PestanaEquipo(i: InmersionEntity) {
    FilaMosaicos {
        Mosaico(stringResource(R.string.detalle_lastre), "${i.lastreKg.limpio()} kg", stringResource(R.string.detalle_lastre_detalle))
        Mosaico(stringResource(R.string.detalle_proteccion), "${i.proteccionMm.limpio()} mm", stringResource(R.string.detalle_proteccion_detalle))
    }
    TarjetaCondicion(stringResource(R.string.detalle_equipamiento)) {
        Chips(i.equipamiento.toList(), vacio = stringResource(R.string.detalle_sin_equipamiento))
    }
}

@Composable
private fun PestanaCondiciones(i: InmersionEntity) {
    val c = i.condiciones

    // ---- Clima grande (solo si se guardó con internet) ----
    if (c != null && c.conDatosOnline) {
        val aptitud = Aptitud.entries.firstOrNull { it.texto == c.aptitudAgua }
        TarjetaHero(
            emoji = "",
            icono = {
                Icon(
                    painterResource(iconoClima(c.codigoClima)),
                    contentDescription = null,
                    tint = colorIconoClima(c.codigoClima),
                    modifier = Modifier.size(44.dp)
                )
            },
            valor = c.temperaturaC?.let { num(it, unidad = "°") } ?: "${i.temperaturaAireC.limpio()}°",
            subtitulo = c.codigoClima?.let { descripcionClima(it) } ?: stringResource(R.string.detalle_sin_datos_cielo),
            modifier = Modifier.padding(bottom = 12.dp),
            derecha = {
                c.sensacionTermicaC?.let {
                    Text(
                        stringResource(R.string.detalle_sensacion, num(it, unidad = "°")),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
            },
            abajo = {
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoChico(stringResource(R.string.detalle_viento), c.vientoKmh?.let { "${num(it)} ${direccion(c.direccionVientoGrados)}" } ?: "-")
                    DatoChico(stringResource(R.string.detalle_rafagas), num(c.rafagasKmh, unidad = " km/h"))
                    DatoChico(stringResource(R.string.detalle_humedad), num(c.humedadPorc?.toDouble(), unidad = "%"))
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DatoChico(stringResource(R.string.detalle_presion), num(c.presionHpa, unidad = " hPa"))
                    DatoChico(stringResource(R.string.detalle_olas), c.olaM?.let { num(it, 1, " m") } ?: "-")
                    DatoChico(stringResource(R.string.detalle_agua), c.temperaturaAguaC?.let { num(it, 1, "°C") } ?: "-")
                }
                if (aptitud != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.detalle_para_entrar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSuperficieVariante
                        )
                        Spacer(Modifier.width(8.dp))
                        Semaforo(colorAptitud(aptitud), aptitud.nombre())
                    }
                }
            }
        )
    }

    // ---- Mosaicos: marea, luna, corriente, visibilidad ----
    if (c != null) {
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.detalle_marea),
                c.estadoMarea ?: stringResource(R.string.detalle_sin_datos),
                c.alturaMareaM?.let { num(it, 2, " m") } ?: ""
            )
            Mosaico(stringResource(R.string.detalle_luna), "${c.iluminacionLunaPorc}%", nombreFaseLunar(c.faseLunar))
        }
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.detalle_corriente),
                c.corrienteKmh?.let { num(it, 1, " km/h") } ?: stringResource(R.string.detalle_sin_datos),
                stringResource(R.string.detalle_al_entrar)
            )
            Mosaico(stringResource(R.string.detalle_visibilidad), "${i.visibilidadMetros.limpio()} m", stringResource(R.string.detalle_cargada_por_vos))
        }
    } else {
        TarjetaCondicion {
            Text(stringResource(R.string.detalle_sin_condiciones), color = TextoSuperficieVariante)
        }
    }

    // ---- Lo que cargó el usuario ----
    TarjetaCondicion(stringResource(R.string.detalle_tu_registro)) {
        Chips(i.caracteristicasAgua.toList(), vacio = stringResource(R.string.detalle_sin_caracteristicas))
        Spacer(Modifier.height(10.dp))
        FilaDato(stringResource(R.string.nueva_temp_aire), "${i.temperaturaAireC.limpio()} °C")
        FilaDato(stringResource(R.string.nueva_temp_agua), "${i.temperaturaAguaC.limpio()} °C")
    }

    if (c != null && !c.conDatosOnline) {
        Text(
            stringResource(R.string.detalle_sin_conexion),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
    }
}

// ============================ Helpers ============================

// Minutos entre entrada y salida (si cruza la medianoche, suma 24 h)
private fun minutosEnAgua(i: InmersionEntity): Long {
    val m = JDuration.between(i.horaEntrada, i.horaSalida).toMinutes()
    return if (m >= 0) m else m + 24 * 60
}

// 18.0 -> "18", 18.5 -> "18,5"
private fun Float.limpio(): String =
    if (this % 1f == 0f) toInt().toString() else "%.1f".format(Locale.forLanguageTag("es-AR"), this)

// 1.0 -> "1", 0.3 -> "0,3" (para las velocidades, que son Double)
private fun Double.limpio(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(Locale.forLanguageTag("es-AR"), this)
