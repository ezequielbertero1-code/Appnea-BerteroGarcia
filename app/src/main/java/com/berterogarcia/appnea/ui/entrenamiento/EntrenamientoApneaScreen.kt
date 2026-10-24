package com.berterogarcia.appnea.ui.entrenamiento

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.ui.components.BarraAccionesInferior
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPeligro
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.ChipsSeleccionMultiple
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.FilaConSwitch
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.MantenerPantallaEncendida
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.OcultarBarraNavegacion
import com.berterogarcia.appnea.ui.components.ProgresoPorPasos
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorEntrenaDescanso
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// ================================ 1. ELEGIR TABLA ================================

/**
 * Lista de tablas (predefinidas + propias) con radio, "Estoy acompañado" (Switch) e Iniciar fijos abajo.
 * El aviso de seguridad se muestra solo si NO está activado "Estoy acompañado".
 */
@Composable
fun SeleccionTablaScreen(
    estado: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel,
    onCrearTabla: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tablaABorrar by remember { mutableStateOf<TablaPersonalizadaEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // ---- Lista (scrollea) ----
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // El encabezado "Tablas" ya trae su propio espacio superior
            if (!estado.acompanado) AvisoSeguridadApnea(Modifier.padding(top = 8.dp), separacionInferior = 0.dp)

            EncabezadoGrupo(titulo = stringResource(R.string.entrenar_tablas))
            GrupoLista {
                estado.tablasPredefinidas.forEachIndexed { i, tabla ->
                    FilaTabla(
                        tabla = tabla,
                        seleccionada = estado.tablaSeleccionada == tabla,
                        onClick = { viewModel.onSeleccionarTabla(tabla) },
                        indice = i,
                        total = estado.tablasPredefinidas.size
                    )
                }
            }

            EncabezadoGrupo(
                titulo = stringResource(R.string.entrenar_mis_tablas),
                accion = stringResource(R.string.entrenar_crear_tabla),
                onAccion = onCrearTabla
            )
            if (estado.tablasPersonalizadas.isEmpty()) {
                Text(
                    stringResource(R.string.entrenar_sin_tablas),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            } else {
                GrupoLista(modifier = Modifier.padding(bottom = 12.dp)) {
                    estado.tablasPersonalizadas.forEachIndexed { i, entidad ->
                        val tabla = entidad.aTablaApnea()
                        FilaTabla(
                            tabla = tabla,
                            seleccionada = estado.tablaSeleccionada == tabla,
                            onClick = { viewModel.onSeleccionarTabla(tabla) },
                            indice = i,
                            total = estado.tablasPersonalizadas.size,
                            onEliminar = { tablaABorrar = entidad }
                        )
                    }
                }
            }
        }

        // ---- Fijo abajo: acompañado + Iniciar ----
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
            FilaConSwitch(
                titulo = stringResource(R.string.entrenar_acompanado),
                subtitulo = stringResource(R.string.entrenar_acompanado_ayuda),
                activado = estado.acompanado,
                onCambio = viewModel::onAcompanadoChange,
                indice = 0,
                total = 1,
                inicio = { Icon(painterResource(R.drawable.ic_ms_group), contentDescription = null) }
            )
            BotonPrincipal(
                texto = stringResource(R.string.entrenar_iniciar),
                onClick = viewModel::onIniciar,
                enabled = estado.tablaSeleccionada != null,
                icono = rememberVectorPainter(Icons.Rounded.PlayArrow),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            if (estado.tablaSeleccionada == null) {
                Text(
                    stringResource(R.string.entrenar_elegi_tabla),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
    }

    tablaABorrar?.let { entidad ->
        AlertDialog(
            onDismissRequest = { tablaABorrar = null },
            title = { Text(stringResource(R.string.tabla_eliminar)) },
            text = { Text(stringResource(R.string.tabla_eliminar_confirmacion, entidad.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEliminarTablaPersonalizada(entidad)
                    tablaABorrar = null
                }) { Text(stringResource(R.string.accion_eliminar), color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { tablaABorrar = null }) { Text(stringResource(R.string.accion_cancelar)) } }
        )
    }
}

// ================================ 2. EN CURSO ================================

/**
 * Pantalla completa mientras corre la tabla: sin barra de navegación y con la pantalla encendida.
 * "Abortar" corta al instante (por seguridad, sin preguntar); la "X" y el botón atrás sí preguntan.
 */
@Composable
fun EntrenamientoEnCurso(
    estado: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel,
    modifier: Modifier = Modifier
) {
    OcultarBarraNavegacion()
    MantenerPantallaEncendida()

    var confirmarSalida by remember { mutableStateOf(false) }
    BackHandler { confirmarSalida = true }

    val tabla = estado.tablaSeleccionada ?: return
    val fase = estado.faseActual ?: return
    val siguiente = tabla.fases.getOrNull(estado.indiceFase + 1)
    val color = colorFase(fase.tipo)

    // El anillo se calcula contra el reloj en CADA cuadro (no por segundos enteros):
    // avanza continuo y llega justo al 100 % cuando termina la fase. Al cambiar de fase
    // (cambia indiceFase / finFaseMs) se reinicia desde 0.
    val durFaseMs = fase.duracionSegundos * 1000L
    val anillo by produceState(initialValue = 0f, estado.indiceFase, estado.finFaseMs) {
        while (true) {
            withFrameMillis { }
            val restanteMs = (estado.finFaseMs - SystemClock.elapsedRealtime()).coerceIn(0L, durFaseMs)
            value = if (durFaseMs > 0L) 1f - restanteMs.toFloat() / durFaseMs else 1f
            if (restanteMs == 0L) break // fase terminada: el anillo queda completo
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = tabla.nombre,
            navegacion = NavegacionBarra.CERRAR,
            onNavegacion = { confirmarSalida = true }
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgresoPorPasos(total = estado.totalFases, completados = estado.indiceFase + 1, modifier = Modifier.padding(top = 4.dp))
            Text(
                stringResource(R.string.entrenar_fase_de, estado.indiceFase + 1, estado.totalFases).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 0.6.sp,
                color = TextoSuperficieVariante,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (!estado.acompanado) AvisoSeguridadApnea(Modifier.padding(top = 12.dp))

            // El anillo se achica si la pantalla es baja (o si se muestra el aviso)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val tamAnillo = minOf(230.dp, maxWidth * 0.75f, maxHeight - 120.dp).coerceAtLeast(140.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnilloProgreso(progreso = anillo, color = color, grosor = 10.dp, modifier = Modifier.size(tamAnillo)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                etiquetaFase(fase.tipo).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 1.5.sp,
                                color = if (fase.tipo == TipoFase.PREPARACION) TextoSuperficieVariante else color
                            )
                            Text(minSeg(estado.segundosRestantes), fontSize = 52.sp, color = TextoSuperficie)
                            Text(
                                stringResource(R.string.entrenar_quedan_de, minSeg(fase.duracionSegundos)),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSuperficieVariante
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (siguiente != null) {
                            Text(stringResource(R.string.entrenar_siguiente), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
                            Spacer(Modifier.width(8.dp))
                            PastillaFase(siguiente)
                        } else {
                            Text(stringResource(R.string.entrenar_ultima_fase), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
                        }
                    }
                    LineaTiempoTabla(
                        fases = tabla.fases,
                        transcurridos = estado.segundosTranscurridos,
                        total = estado.duracionTabla,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }
        }

        BarraAccionesInferior(conFondo = false) {
            BotonPeligro(
                texto = stringResource(R.string.entrenar_abortar),
                onClick = viewModel::onAbortar,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(painterResource(R.drawable.ic_ms_pan_tool), contentDescription = null, tint = Acento) },
            title = { Text(stringResource(R.string.entrenar_cortar_titulo), textAlign = TextAlign.Center) },
            text = { Text(stringResource(R.string.entrenar_cortar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.onAbortar()
                }) { Text(stringResource(R.string.entrenar_cortar), color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text(stringResource(R.string.entrenar_seguir)) } }
        )
    }
}

// ================================ 3. RESULTADO ================================

/**
 * Resumen de ESTA sesión (fases y duración) + factores y sensaciones.
 * Las comparaciones con otras sesiones están en Estadísticas: si se pasa [onVerEstadisticas]
 * aparece el enlace.
 */
@Composable
fun ResultadoEntrenamiento(
    estado: EntrenamientoUiState,
    viewModel: EntrenamientoApneaViewModel,
    modifier: Modifier = Modifier,
    onVerEstadisticas: (() -> Unit)? = null
) {
    OcultarBarraNavegacion()
    val context = LocalContext.current
    var confirmarDescartar by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmarDescartar = true }
    val textoGuardada = stringResource(R.string.entrenar_sesion_guardada)

    // Guardado OK → aviso y vuelta a la lista de tablas
    LaunchedEffect(estado.guardadoOk) {
        if (estado.guardadoOk) {
            Toast.makeText(context, textoGuardada, Toast.LENGTH_SHORT).show()
            viewModel.onVolverASeleccion()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = stringResource(R.string.entrenar_sesion_terminada),
            navegacion = NavegacionBarra.CERRAR,
            onNavegacion = { confirmarDescartar = true }
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            TarjetaResultado(estado)

            Text(
                stringResource(R.string.entrenar_factores),
                style = MaterialTheme.typography.titleSmall,
                color = TextoSuperficie,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
            )
            ChipsSeleccionMultiple(
                opciones = FACTORES_DISPONIBLES,
                seleccionadas = estado.factoresSeleccionados,
                onCambio = viewModel::onFactorToggle
            )

            CampoTexto(
                valor = estado.sensaciones,
                onValorChange = viewModel::onSensacionesChange,
                etiqueta = stringResource(R.string.entrenar_como_te_sentiste),
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            if (onVerEstadisticas != null) {
                TextButton(onClick = onVerEstadisticas, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(
                        painterResource(R.drawable.ic_ms_trending_up),
                        contentDescription = null,
                        tint = Acento,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.entrenar_ver_progreso), color = Acento)
                }
            }

            estado.errorGuardado?.let {
                Text(it.resolver(), style = MaterialTheme.typography.bodySmall, color = ColorError, modifier = Modifier.padding(top = 8.dp))
            }
        }

        BarraAccionesInferior {
            BotonContorno(
                texto = stringResource(R.string.entrenar_descartar),
                onClick = { confirmarDescartar = true },
                enabled = !estado.guardando,
                modifier = Modifier.weight(1f)
            )
            BotonPrincipal(
                texto = stringResource(if (estado.guardando) R.string.accion_guardando else R.string.accion_guardar),
                onClick = viewModel::onGuardarSesion,
                cargando = estado.guardando,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (confirmarDescartar) {
        AlertDialog(
            onDismissRequest = { confirmarDescartar = false },
            title = { Text(stringResource(R.string.entrenar_descartar_titulo)) },
            text = { Text(stringResource(R.string.entrenar_descartar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescartar = false
                    viewModel.onVolverASeleccion()
                }) { Text(stringResource(R.string.entrenar_descartar), color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { confirmarDescartar = false }) { Text(stringResource(R.string.accion_cancelar)) } }
        )
    }
}

/** Tarjeta de resultado: verde con trofeo si completó la tabla, tonal si la cortó. */
@Composable
private fun TarjetaResultado(estado: EntrenamientoUiState) {
    val forma = RoundedCornerShape(16.dp)
    val fondo = if (estado.completada) {
        Modifier.background(Brush.linearGradient(listOf(ColorEntrenaDescanso.copy(alpha = 0.22f), ColorEntrenaDescanso.copy(alpha = 0.06f))), forma)
    } else {
        Modifier.background(SuperficieTonal, forma)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .then(fondo)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(if (estado.completada) R.drawable.ic_ms_emoji_events else R.drawable.ic_ms_pan_tool),
            contentDescription = null,
            tint = if (estado.completada) ColorEntrenaDescanso else TextoSuperficieVariante,
            modifier = Modifier.size(40.dp)
        )
        Text(
            if (estado.completada) stringResource(R.string.entrenar_completaste)
            else stringResource(R.string.entrenar_cortaste, estado.faseAlcanzada, estado.totalFases),
            fontSize = 22.sp,
            color = TextoSuperficie,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            estado.tablaSeleccionada?.nombre.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
        Row(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            DatoResultado(stringResource(R.string.entrenar_fases), "${estado.faseAlcanzada} / ${estado.totalFases}")
            DatoResultado(stringResource(R.string.entrenar_duracion), minSeg(estado.duracionSesionSegundos))
        }
    }
}

@Composable
private fun DatoResultado(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
    }
}
