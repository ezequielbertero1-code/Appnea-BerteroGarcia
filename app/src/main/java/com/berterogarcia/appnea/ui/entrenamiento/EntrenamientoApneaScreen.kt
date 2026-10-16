package com.berterogarcia.appnea.ui.entrenamiento

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.ui.components.BotonPeligro
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.ChipsSeleccionMultiple
import com.berterogarcia.appnea.ui.components.MantenerPantallaEncendida
import com.berterogarcia.appnea.ui.components.OcultarBarraNavegacion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.BordeSuave
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoChip
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoSobreAcento
import com.berterogarcia.appnea.ui.theme.TextoSobreResaltada
import com.berterogarcia.appnea.ui.theme.TextoTerciario

// ================================ 1. ELEGIR TABLA ================================

/**
 * Lista de tablas (predefinidas + propias), tilde "Estoy acompañado" e Iniciar fijos abajo.
 * El aviso de seguridad se muestra solo si NO está tildado "Estoy acompañado".
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
                .padding(top = 12.dp)
        ) {
            if (!estado.acompanado) AvisoSeguridadApnea()

            TituloSeccion("Tablas")
            estado.tablasPredefinidas.forEach { tabla ->
                TarjetaTabla(
                    tabla = tabla,
                    seleccionada = estado.tablaSeleccionada == tabla,
                    onClick = { viewModel.onSeleccionarTabla(tabla) }
                )
            }
            LeyendaFases(Modifier.padding(start = 2.dp, bottom = 4.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TituloSeccion("Mis tablas", Modifier.weight(1f))
                TextButton(onClick = onCrearTabla) { Text("+ Crear tabla", color = Acento) }
            }
            if (estado.tablasPersonalizadas.isEmpty()) {
                Text(
                    "Todavía no creaste tablas propias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(start = 2.dp, bottom = 12.dp)
                )
            } else {
                estado.tablasPersonalizadas.forEach { entidad ->
                    val tabla = entidad.aTablaApnea()
                    TarjetaTabla(
                        tabla = tabla,
                        seleccionada = estado.tablaSeleccionada == tabla,
                        onClick = { viewModel.onSeleccionarTabla(tabla) },
                        onEliminar = { tablaABorrar = entidad }
                    )
                }
            }
        }

        // ---- Fijo abajo: tilde + Iniciar ----
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .toggleable(
                        value = estado.acompanado,
                        role = Role.Checkbox,
                        onValueChange = viewModel::onAcompanadoChange
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = estado.acompanado,
                    onCheckedChange = null, // lo maneja el toggleable de toda la fila
                    colors = CheckboxDefaults.colors(
                        checkedColor = Acento,
                        checkmarkColor = TextoSobreAcento,
                        uncheckedColor = BordeSuave
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text("Estoy acompañado", style = MaterialTheme.typography.bodyMedium)
            }
            BotonPrincipal(
                texto = "Iniciar entrenamiento",
                onClick = viewModel::onIniciar,
                enabled = estado.tablaSeleccionada != null,
                icono = painterResource(R.drawable.ic_cronometro),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            if (estado.tablaSeleccionada == null) {
                Text(
                    "Elegí una tabla para empezar",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTerciario,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }
    }

    tablaABorrar?.let { entidad ->
        AlertDialog(
            onDismissRequest = { tablaABorrar = null },
            title = { Text("Eliminar tabla") },
            text = { Text("¿Seguro que querés eliminar \"${entidad.nombre}\"? No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEliminarTablaPersonalizada(entidad)
                    tablaABorrar = null
                }) { Text("Eliminar", color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { tablaABorrar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Normal,
        modifier = modifier.padding(start = 2.dp, top = 8.dp, bottom = 6.dp)
    )
}

// ================================ 2. EN CURSO ================================

/**
 * Pantalla completa mientras corre la tabla: sin barra de navegación y con la pantalla encendida.
 * "Abortar" corta al instante (por seguridad, sin preguntar); el botón atrás del sistema sí pregunta.
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

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            tabla.nombre,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "Fase ${estado.indiceFase + 1} de ${estado.totalFases}",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
        if (!estado.acompanado) AvisoSeguridadApnea(Modifier.padding(top = 12.dp))

        // El anillo se achica si la pantalla es baja (o si se muestra el aviso)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val tamAnillo = minOf(230.dp, maxWidth * 0.75f, maxHeight - 120.dp).coerceAtLeast(140.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnilloProgreso(progreso = anillo, color = color, modifier = Modifier.size(tamAnillo)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            etiquetaFase(fase.tipo).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp,
                            color = if (fase.tipo == TipoFase.PREPARACION) TextoChip else color
                        )
                        Text(minSeg(estado.segundosRestantes), fontSize = 56.sp, fontWeight = FontWeight.Normal)
                        Text(
                            "quedan · de ${minSeg(fase.duracionSegundos)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSecundario
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (siguiente != null) {
                        Text("Siguiente", style = MaterialTheme.typography.bodyMedium, color = TextoChip)
                        Spacer(Modifier.width(8.dp))
                        PastillaFase(siguiente)
                    } else {
                        Text("Última fase", style = MaterialTheme.typography.bodyMedium, color = TextoChip)
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

        BotonPeligro(texto = "Abortar", onClick = viewModel::onAbortar, modifier = Modifier.fillMaxWidth())
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cortar el entrenamiento?") },
            text = { Text("La tabla se detiene y vas a poder guardar hasta dónde llegaste.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.onAbortar()
                }) { Text("Cortar", color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir") } }
        )
    }
}

// ================================ 3. RESULTADO ================================

/**
 * Resumen de ESTA sesión (fases y duración) + factores y sensaciones.
 * Las comparaciones con otras sesiones van en la futura pantalla de Estadísticas:
 * cuando exista, se pasa [onVerEstadisticas] y aparece el enlace.
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

    // Guardado OK → aviso y vuelta a la lista de tablas
    LaunchedEffect(estado.guardadoOk) {
        if (estado.guardadoOk) {
            Toast.makeText(context, "Sesión guardada", Toast.LENGTH_SHORT).show()
            viewModel.onVolverASeleccion()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Sesión terminada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Normal)

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FondoTarjetaResaltada, contentColor = Color.White),
                border = BorderStroke(1.dp, Acento.copy(alpha = 0.45f))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        if (estado.completada) "🎉 ¡Completaste la tabla!"
                        else "Cortaste en la fase ${estado.faseAlcanzada} de ${estado.totalFases}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        estado.tablaSeleccionada?.nombre.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSobreResaltada,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        DatoResultado("Fases", "${estado.faseAlcanzada} de ${estado.totalFases}", Modifier.weight(1f))
                        DatoResultado("Duración", minSeg(estado.duracionSesionSegundos), Modifier.weight(1f))
                    }
                }
            }

            Row(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Text("¿Qué pudo haber influido hoy?", style = MaterialTheme.typography.bodyMedium, color = TextoChip)
                Text(" · varias", style = MaterialTheme.typography.bodyMedium, color = TextoTerciario)
            }
            ChipsSeleccionMultiple(
                opciones = FACTORES_DISPONIBLES,
                seleccionadas = estado.factoresSeleccionados,
                onCambio = viewModel::onFactorToggle
            )

            CampoTexto(
                valor = estado.sensaciones,
                onValorChange = viewModel::onSensacionesChange,
                etiqueta = "¿Cómo te sentiste?",
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            if (onVerEstadisticas != null) {
                TextButton(onClick = onVerEstadisticas, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("📈 Ver tu progreso en Estadísticas →", color = Acento)
                }
            }

            estado.errorGuardado?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ColorError, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonSecundario(
                texto = "Descartar",
                onClick = { confirmarDescartar = true },
                enabled = !estado.guardando,
                modifier = Modifier.weight(1f)
            )
            BotonPrincipal(
                texto = if (estado.guardando) "Guardando…" else "Guardar",
                onClick = viewModel::onGuardarSesion,
                cargando = estado.guardando,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (confirmarDescartar) {
        AlertDialog(
            onDismissRequest = { confirmarDescartar = false },
            title = { Text("Descartar sesión") },
            text = { Text("No se va a guardar este entrenamiento.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescartar = false
                    viewModel.onVolverASeleccion()
                }) { Text("Descartar", color = ColorError) }
            },
            dismissButton = { TextButton(onClick = { confirmarDescartar = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DatoResultado(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSobreResaltada)
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal)
    }
}
