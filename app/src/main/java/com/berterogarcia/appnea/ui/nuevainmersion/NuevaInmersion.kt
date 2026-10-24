package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.data.local.Visibilidad
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.ui.components.BarraAccionesInferior
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.CheckboxGroup
import com.berterogarcia.appnea.ui.components.ChipFiltro
import com.berterogarcia.appnea.ui.components.DatePickerField
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.ProgresoPasosNavegable
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.TimePickerField
import com.berterogarcia.appnea.ui.components.formaEnGrupo
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Características del agua, en grupos. Se guardan juntas en caracteristicasAgua (son datos, no
// textos de la interfaz: si se tradujeran, los registros viejos dejarían de coincidir).
private val AGUA_TIPO = listOf("Dulce", "Salada")             // una
private val AGUA_ENTRADA = listOf("Orilla", "Bote")           // una
private val AGUA_MAR = listOf("Olas", "Corrientes", "Resaca") // varias

private val tiempoFondoVisualTransformation = VisualTransformation { texto ->
    // Se llena desde el final: "500" se ve "05:00" (se completa con ceros a la izquierda)
    val digitos = texto.text
    val relleno = if (digitos.isEmpty()) 0 else 4 - digitos.length.coerceAtMost(4)
    val formateado = if (digitos.isEmpty()) "" else digitos.padStart(4, '0').let { "${it.take(2)}:${it.drop(2)}" }
    val offsetMapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val p = offset + relleno
            return if (p <= 2) p else p + 1
        }
        override fun transformedToOriginal(offset: Int): Int {
            val p = if (offset <= 2) offset else offset - 1
            return (p - relleno).coerceIn(0, digitos.length)
        }
    }
    TransformedText(AnnotatedString(formateado), offsetMapping)
}

/**
 * Registrar / editar inmersión (estilo "Appnea circular"): pantalla completa con "X" que pregunta
 * antes de descartar, progreso por pasos (los que tienen errores se marcan en rojo y los ya
 * visitados se pueden tocar para volver), campos outlined y acciones fijas abajo.
 */
@Composable
fun NuevaInmersionScreen(
    modifier: Modifier = Modifier,
    viewModel: NuevaInmersionViewModel,
    onGuardado: () -> Unit,
    onCargarEquipo: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val esPrimera = uiState.etapa == Etapa.entries.first()
    val esUltima = uiState.etapa == Etapa.entries.last()
    var confirmarSalir by rememberSaveable { mutableStateOf(false) }

    // Cuando el ViewModel confirma el guardado, volvemos a la pantalla anterior
    LaunchedEffect(uiState.guardadoOk) {
        if (uiState.guardadoOk) onGuardado()
    }
    // Mapa a pantalla completa: mientras está abierto, reemplaza al formulario
    var mapaAbierto by rememberSaveable { mutableStateOf(false) }
    if (mapaAbierto) {
        val centro = remember { viewModel.centroInicialMapa() }
        SelectorLugarMapa(
            centroInicial = centro,
            nombreInicial = uiState.lugar,
            onCancelar = { mapaAbierto = false },
            onConfirmar = { lat, lon, nombre ->
                viewModel.onUbicacionElegida(lat, lon, nombre)
                mapaAbierto = false
            },
            modifier = modifier
        )
        return
    }
    // Editando: mientras se lee la inmersión (o si no se encontró) no se muestra el formulario
    if (uiState.cargandoEdicion || uiState.errorCarga != null) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.cargandoEdicion) {
                CircularProgressIndicator(color = Acento)
            } else {
                Text(uiState.errorCarga?.resolver().orEmpty(), color = TextoSuperficieVariante)
                BotonSecundario(
                    texto = stringResource(R.string.accion_volver),
                    onClick = onGuardado,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        return
    }

    // Botón atrás del sistema: vuelve a la etapa anterior en vez de salir
    BackHandler(enabled = !esPrimera) { viewModel.anterior() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()   // reserva el alto del teclado: los botones quedan justo arriba
    ) {
        BarraSuperior(
            titulo = stringResource(if (uiState.editando) R.string.nueva_titulo_editar else R.string.nueva_titulo),
            navegacion = NavegacionBarra.CERRAR,
            onNavegacion = { confirmarSalir = true }
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            val etapas = Etapa.entries
            ProgresoPasosNavegable(
                total = etapas.size,
                actual = uiState.etapa.ordinal,
                conError = etapas.filter { e -> uiState.errores.keys.any { it in e.campos } }.map { it.ordinal }.toSet(),
                onIrA = { viewModel.irA(etapas[it]) },
                descripcion = { stringResource(R.string.nueva_ir_a_paso, it + 1, etapas[it].nombre()) }
            )
            Text(
                stringResource(
                    R.string.nueva_paso_de,
                    uiState.etapa.ordinal + 1,
                    Etapa.entries.size,
                    uiState.etapa.nombre()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuperficieVariante,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        AnimatedContent(
            targetState = uiState.etapa,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally { it * dir } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir } + fadeOut())
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            label = "etapa"
        ) { etapa ->
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            ) {
                when (etapa) {
                    Etapa.GENERAL -> EtapaGeneral(uiState, viewModel, onAbrirMapa = { mapaAbierto = true })
                    Etapa.INMERSION -> EtapaInmersion(uiState, viewModel)
                    Etapa.CONDICIONES -> EtapaCondiciones(uiState, viewModel)
                    Etapa.EQUIPO -> EtapaEquipo(uiState, viewModel, onCargarEquipo)
                    Etapa.NOTAS -> EtapaNotas(uiState, viewModel)
                }
            }
        }

        uiState.errorGuardado?.let {
            Text(
                it.resolver(),
                color = ColorError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        BarraAccionesInferior {
            if (esPrimera) Spacer(Modifier.weight(1f))
            else BotonContorno(
                texto = stringResource(R.string.nueva_anterior),
                onClick = viewModel::anterior,
                enabled = !uiState.guardando, // que no se pueda volver mientras guarda
                modifier = Modifier.weight(1f)
            )
            if (esUltima) BotonPrincipal(
                texto = stringResource(
                    when {
                        uiState.guardando -> R.string.accion_guardando
                        uiState.editando -> R.string.nueva_guardar_cambios
                        else -> R.string.accion_guardar
                    }
                ),
                onClick = viewModel::guardar,
                cargando = uiState.guardando,
                modifier = Modifier.weight(1f)
            )
            else BotonPrincipal(
                texto = stringResource(R.string.nueva_siguiente),
                onClick = viewModel::siguiente,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (confirmarSalir) {
        AlertDialog(
            onDismissRequest = { confirmarSalir = false },
            title = { Text(stringResource(if (uiState.editando) R.string.nueva_salir_titulo_editar else R.string.nueva_salir_titulo)) },
            text = { Text(stringResource(R.string.nueva_salir_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalir = false
                    onGuardado()
                }) { Text(stringResource(R.string.nueva_salir_descartar), color = ColorError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalir = false }) { Text(stringResource(R.string.nueva_salir_seguir)) }
            }
        )
    }
}

@Composable
private fun EtapaGeneral(
    uiState: NuevaInmersionUiState,
    viewModel: NuevaInmersionViewModel,
    onAbrirMapa: () -> Unit
) {
    val tipos = TipoInmersion.entries.associateWith { it.nombre() }
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_que_tipo),
        opciones = TipoInmersion.entries,
        seleccionada = uiState.tipo,
        etiqueta = { tipos.getValue(it) },
        onSeleccion = { tipo -> tipo?.let(viewModel::onTipoChange) },
        modifier = Modifier.padding(bottom = 12.dp)
    )
    DatePickerField(
        label = stringResource(R.string.nueva_fecha), valor = uiState.fecha, onValorChange = viewModel::onFechaChange,
        isError = uiState.errores.containsKey(Campo.FECHA), mensajeError = uiState.errores[Campo.FECHA]?.resolver(),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.lugar, onValorChange = viewModel::onLugarChange, etiqueta = stringResource(R.string.nueva_lugar),
        esError = uiState.errores.containsKey(Campo.LUGAR), mensajeError = uiState.errores[Campo.LUGAR]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
    TarjetaLugar(
        latitud = uiState.latitud,
        longitud = uiState.longitud,
        buscando = uiState.buscandoUbicacion,
        aviso = uiState.avisoUbicacion?.resolver(),
        onAbrirMapa = onAbrirMapa,
        onMiUbicacion = viewModel::usarMiUbicacion,
        onPermisoDenegado = viewModel::permisoUbicacionDenegado,
        onQuitar = viewModel::quitarUbicacion
    )
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TimePickerField(
            label = stringResource(R.string.nueva_entrada), valor = uiState.horaEntrada,
            onValorChange = viewModel::onHoraEntradaChange,
            isError = uiState.errores.containsKey(Campo.HORA_ENTRADA), mensajeError = uiState.errores[Campo.HORA_ENTRADA]?.resolver(),
            modifier = Modifier.weight(1f)
        )
        TimePickerField(
            label = stringResource(R.string.nueva_salida), valor = uiState.horaSalida,
            onValorChange = viewModel::onHoraSalidaChange,
            isError = uiState.errores.containsKey(Campo.HORA_SALIDA), mensajeError = uiState.errores[Campo.HORA_SALIDA]?.resolver(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EtapaInmersion(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    if (uiState.tipo == TipoInmersion.APNEA) {
        BloqueApnea(uiState, viewModel)
        return
    }
    CampoTexto(
        valor = uiState.profundidadMetros, onValorChange = viewModel::onProfundidadChange,
        etiqueta = stringResource(R.string.nueva_profundidad),
        esError = uiState.errores.containsKey(Campo.PROFUNDIDAD), mensajeError = uiState.errores[Campo.PROFUNDIDAD]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.tiempoFondo, onValorChange = viewModel::onTiempoFondoChange,
        etiqueta = stringResource(R.string.nueva_tiempo_fondo),
        visualTransformation = tiempoFondoVisualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        esError = uiState.errores.containsKey(Campo.TIEMPO_FONDO), mensajeError = uiState.errores[Campo.TIEMPO_FONDO]?.resolver() ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    CampoTexto(
        valor = uiState.paradaSeguridadMin, onValorChange = viewModel::onParadaChange,
        etiqueta = stringResource(R.string.nueva_parada),
        esError = uiState.errores.containsKey(Campo.PARADA), mensajeError = uiState.errores[Campo.PARADA]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
}

/**
 * Apnea: siempre se pide la cantidad de bajadas y la profundidad máxima; cargar cada bajada
 * (profundidad + tiempo) es opcional. Al activar el detalle se pre-generan tantas filas como
 * bajadas indicadas, y la profundidad máxima sale de ellas.
 */
@Composable
private fun BloqueApnea(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    CampoTexto(
        valor = uiState.cantidadBajadas, onValorChange = viewModel::onCantidadBajadasChange,
        etiqueta = stringResource(R.string.nueva_cantidad_bajadas),
        esError = uiState.errores.containsKey(Campo.CANTIDAD_BAJADAS),
        mensajeError = uiState.errores[Campo.CANTIDAD_BAJADAS]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
    if (!uiState.cargarDetalleBajadas) {
        CampoTexto(
            valor = uiState.profundidadMetros, onValorChange = viewModel::onProfundidadChange,
            etiqueta = stringResource(R.string.nueva_profundidad),
            esError = uiState.errores.containsKey(Campo.PROFUNDIDAD), mensajeError = uiState.errores[Campo.PROFUNDIDAD]?.resolver() ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }

    // Interruptor del detalle (toda la fila se toca)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .toggleable(
                value = uiState.cargarDetalleBajadas, role = Role.Switch,
                onValueChange = viewModel::onCargarDetalleBajadasChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(stringResource(R.string.nueva_cargar_cada_bajada), color = TextoSuperficie, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.nueva_cargar_cada_bajada_ayuda),
                color = TextoSuperficieVariante, style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = uiState.cargarDetalleBajadas, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = Acento, checkedThumbColor = TextoSobreAcentoFuerte)
        )
    }

    if (uiState.cargarDetalleBajadas) {
        val hayError = uiState.errores.containsKey(Campo.BAJADAS)
        val mejor = uiState.indiceMejorBajada
        val activas = uiState.bajadasActivas
        activas.forEachIndexed { indice, bajada ->
            val invalida = hayError && !bajada.esValida()
            val esMejor = indice == mejor
            val descMejor = stringResource(R.string.nueva_bajada_mejor)
            // Una sola línea: nº de bajada · profundidad · tiempo · tacho
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.width(30.dp).semantics { if (esMejor) contentDescription = descMejor },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (esMejor) Icon(Icons.Rounded.Star, contentDescription = null, tint = Acento, modifier = Modifier.size(14.dp))
                    Text(
                        "${indice + 1}",
                        color = if (esMejor) Acento else TextoSuperficieVariante,
                        fontWeight = if (esMejor) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                CampoTexto(
                    valor = bajada.profundidad,
                    onValorChange = { viewModel.onBajadaProfundidadChange(indice, it) },
                    etiqueta = stringResource(R.string.nueva_bajada_profundidad),
                    esError = invalida,
                    tamanoTexto = 14.sp, conTextoApoyo = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                CampoTexto(
                    valor = bajada.tiempo,
                    onValorChange = { viewModel.onBajadaTiempoChange(indice, it) },
                    etiqueta = stringResource(R.string.nueva_bajada_tiempo),
                    visualTransformation = tiempoFondoVisualTransformation,
                    esError = invalida,
                    tamanoTexto = 14.sp, conTextoApoyo = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { viewModel.onQuitarBajada(indice) },
                    enabled = activas.size > 1
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.nueva_bajada_borrar, indice + 1),
                        tint = if (activas.size > 1) ColorError else TextoSuperficieVariante.copy(alpha = 0.4f)
                    )
                }
            }
        }
        uiState.errores[Campo.BAJADAS]?.let {
            Text(it.resolver(), color = ColorError, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
        TextButton(onClick = viewModel::onAgregarBajada, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.nueva_agregar_bajada), color = Acento, modifier = Modifier.padding(start = 8.dp))
        }
    }

    val si = stringResource(R.string.nueva_si)
    val no = stringResource(R.string.nueva_no)
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_sincope),
        opciones = listOf(false, true),
        seleccionada = uiState.sincope,
        etiqueta = { if (it) si else no },
        onSeleccion = { viewModel.onSincopeChange(it ?: false) },
        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
    )

    // Resumen en vivo (se calcula con lo que se va cargando)
    uiState.resumenApnea?.let { r ->
        TarjetaCondicion(stringResource(R.string.nueva_resumen_apnea), resaltada = true) {
            FilaMosaicos {
                Mosaico(stringResource(R.string.nueva_resumen_bajadas), r.cantidad.toString())
                Mosaico(stringResource(R.string.nueva_resumen_prof_max), "${numeroCorto(r.profundidadMaximaMetros)} m")
            }
            if (r.tiempoTotalSegundos != null) {
                FilaMosaicos {
                    // La mejor bajada es la más profunda; su número es el de la lista del formulario
                    val numeroMejor = (uiState.indiceMejorBajada ?: r.indiceMejorBajada ?: 0) + 1
                    Mosaico(
                        stringResource(R.string.nueva_resumen_mejor),
                        stringResource(R.string.nueva_resumen_mejor_numero, numeroMejor),
                        stringResource(
                            R.string.nueva_resumen_mejor_detalle,
                            numeroCorto(r.profundidadMaximaMetros),
                            formatearDuracion(r.tiempoMejorBajadaSegundos ?: 0)
                        )
                    )
                    Mosaico(stringResource(R.string.nueva_resumen_tiempo_total), formatearDuracion(r.tiempoTotalSegundos))
                }
                FilaMosaicos {
                    Mosaico(stringResource(R.string.nueva_resumen_promedio), formatearDuracion(r.tiempoPromedioSegundos ?: 0))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 18.0 → "18", 18.5 → "18.5" */
private fun numeroCorto(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)

@Composable
private fun EtapaCondiciones(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    val una = stringResource(R.string.nueva_elegi_una)
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_tipo_agua), ayuda = una,
        opciones = AGUA_TIPO,
        seleccionada = AGUA_TIPO.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_TIPO, it) },
        permitirNinguna = true
    )
    SelectorSegmentado(
        titulo = stringResource(R.string.nueva_entrada_agua), ayuda = una,
        opciones = AGUA_ENTRADA,
        seleccionada = AGUA_ENTRADA.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_ENTRADA, it) },
        permitirNinguna = true,
        modifier = Modifier.padding(top = 16.dp)
    )
    CheckboxGroup(
        titulo = stringResource(R.string.nueva_estado_mar), opciones = AGUA_MAR,
        seleccionadas = uiState.caracteristicasAgua, onCambio = viewModel::onAguaToggle
    )
    CampoTexto(
        valor = uiState.temperaturaAireC, onValorChange = viewModel::onTempAireChange,
        etiqueta = stringResource(R.string.nueva_temp_aire),
        esError = uiState.errores.containsKey(Campo.TEMP_AIRE), mensajeError = uiState.errores[Campo.TEMP_AIRE]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAire) },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    CampoTexto(
        valor = uiState.temperaturaAguaC, onValorChange = viewModel::onTempAguaChange,
        etiqueta = stringResource(R.string.nueva_temp_agua),
        esError = uiState.errores.containsKey(Campo.TEMP_AGUA), mensajeError = uiState.errores[Campo.TEMP_AGUA]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAgua) },
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    Text(
        stringResource(R.string.nueva_visibilidad_escala),
        color = TextoSuperficie, style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Visibilidad.entries.forEach { v ->
            val elegida = uiState.visibilidad == v
            // Tocar la elegida la desmarca: es opcional
            ChipFiltro(texto = v.nombre(), elegido = elegida, onClick = { viewModel.onVisibilidadElegida(if (elegida) null else v) })
        }
    }
    CampoTexto(
        valor = uiState.visibilidadMetros, onValorChange = viewModel::onVisibilidadChange,
        etiqueta = stringResource(R.string.nueva_visibilidad),
        esError = uiState.errores.containsKey(Campo.VISIBILIDAD), mensajeError = uiState.errores[Campo.VISIBILIDAD]?.resolver() ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun EtapaEquipo(
    uiState: NuevaInmersionUiState,
    viewModel: NuevaInmersionViewModel,
    onCargarEquipo: () -> Unit
) {
    Row(Modifier.padding(start = 4.dp)) {
        Text(stringResource(R.string.nueva_que_equipo), color = TextoSuperficie, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.nueva_de_tu_perfil), color = TextoSuperficieVariante, style = MaterialTheme.typography.bodyMedium)
    }
    when {
        !uiState.equipoCargado -> LinearProgressIndicator(
            color = Acento,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
        uiState.equipoVisible.isEmpty() -> SinEquipo(
            perfilVacio = uiState.equipoPerfil.isEmpty(),
            onCargarEquipo = onCargarEquipo
        )
        else -> {
            uiState.equipoVisible
                .groupBy { it.categoria }
                .toSortedMap()   // en el orden del enum: Máscara, Snorkel, Aletas, Traje...
                .forEach { (categoria, equipos) ->
                    Text(
                        categoria.nombre(),
                        color = Acento,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 14.dp, start = 4.dp, bottom = 6.dp)
                    )
                    GrupoLista {
                        equipos.forEachIndexed { i, equipo ->
                            val etiqueta = equipo.etiquetaInmersion
                            FilaEquipo(
                                equipo = equipo,
                                marcado = etiqueta in uiState.equipamiento,
                                onCambio = { viewModel.onEquipamientoToggle(etiqueta, it) },
                                indice = i,
                                total = equipos.size
                            )
                        }
                    }
                }
            TextButton(onClick = onCargarEquipo, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Acento, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.nueva_agregar_equipo), color = Acento, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    PreguntaExtra(
        pregunta = stringResource(R.string.nueva_usaste_lastre),
        usado = uiState.usoLastre, onUsadoChange = viewModel::onUsoLastreChange,
        valor = uiState.lastreKg, onValorChange = viewModel::onLastreChange,
        etiqueta = stringResource(R.string.nueva_lastre),
        error = uiState.errores[Campo.LASTRE]?.resolver()
    )
    PreguntaExtra(
        pregunta = stringResource(R.string.nueva_usaste_proteccion),
        usado = uiState.usoProteccion, onUsadoChange = viewModel::onUsoProteccionChange,
        valor = uiState.proteccionMm, onValorChange = viewModel::onProteccionMmChange,
        etiqueta = stringResource(R.string.nueva_proteccion),
        error = uiState.errores[Campo.PROTECCION]?.resolver()
    )
}

/** Pregunta Sí/No; si es Sí aparece el campo del valor (obligatorio). No = no se guarda nada. */
@Composable
private fun PreguntaExtra(
    pregunta: String,
    usado: Boolean,
    onUsadoChange: (Boolean) -> Unit,
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?
) {
    val si = stringResource(R.string.nueva_si)
    val no = stringResource(R.string.nueva_no)
    SelectorSegmentado(
        titulo = pregunta,
        ayuda = stringResource(R.string.nueva_ademas_perfil),
        opciones = listOf(false, true),
        seleccionada = usado,
        etiqueta = { if (it) si else no },
        onSeleccion = { onUsadoChange(it ?: false) },
        modifier = Modifier.padding(top = 16.dp)
    )
    if (usado) {
        CampoTexto(
            valor = valor, onValorChange = onValorChange, etiqueta = etiqueta,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            esError = error != null, mensajeError = error ?: "",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

/** Toda la fila se toca (no solo el checkbox) y TalkBack la anuncia como checkbox. */
@Composable
private fun FilaEquipo(equipo: EquipoEntity, marcado: Boolean, onCambio: (Boolean) -> Unit, indice: Int, total: Int) {
    val nombre = listOf(equipo.marca, equipo.modelo)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { equipo.categoria.nombre() }
    ListItem(
        headlineContent = { Text(nombre, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = equipo.talle.takeIf { it.isNotBlank() }?.let { talle -> { Text(talle) } },
        leadingContent = {
            Checkbox(
                checked = marcado,
                onCheckedChange = null, // lo maneja el toggleable de la fila
                colors = CheckboxDefaults.colors(
                    checkedColor = Acento,
                    uncheckedColor = TextoSuperficieVariante,
                    checkmarkColor = TextoSobreAcentoFuerte
                )
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = SuperficieTonal,
            headlineColor = TextoSuperficie,
            supportingColor = TextoSuperficieVariante
        ),
        modifier = Modifier
            .clip(formaEnGrupo(indice, total))
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onCambio)
    )
}

@Composable
private fun SinEquipo(perfilVacio: Boolean, onCargarEquipo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(if (perfilVacio) R.string.nueva_sin_equipo_perfil else R.string.nueva_sin_equipo_tipo),
            color = TextoSuperficieVariante,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        BotonSecundario(
            texto = stringResource(R.string.nueva_cargar_equipo),
            onClick = onCargarEquipo,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EtapaNotas(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    CampoTexto(
        valor = uiState.actividad,
        onValorChange = viewModel::onActividadChange,
        etiqueta = stringResource(R.string.nueva_actividad), minLines = 3,
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.comentarios,
        onValorChange = viewModel::onComentariosChange,
        etiqueta = stringResource(R.string.nueva_comentarios),
        minLines = 3,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
    Text(
        stringResource(R.string.nueva_condiciones_automaticas),
        style = MaterialTheme.typography.bodySmall,
        color = TextoSuperficieVariante,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

// Botón para cambiar el signo: muchos teclados numéricos no tienen la tecla "-"
@Composable
private fun BotonSigno(onClick: () -> Unit) {
    val descripcion = stringResource(R.string.nueva_cambiar_signo)
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = descripcion } // para TalkBack
    ) {
        Text("±", style = MaterialTheme.typography.titleLarge, color = Acento)
    }
}

/** Nombre de cada paso del formulario. */
@Composable
private fun Etapa.nombre(): String = stringResource(
    when (this) {
        Etapa.GENERAL -> R.string.nueva_etapa_general
        Etapa.INMERSION -> R.string.nueva_etapa_inmersion
        Etapa.CONDICIONES -> R.string.nueva_etapa_condiciones
        Etapa.EQUIPO -> R.string.nueva_etapa_equipo
        Etapa.NOTAS -> R.string.nueva_etapa_notas
    }
)
