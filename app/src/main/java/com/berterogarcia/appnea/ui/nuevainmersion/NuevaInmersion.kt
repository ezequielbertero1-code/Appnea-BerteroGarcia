package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.TipoInmersion
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.CheckboxGroup
import com.berterogarcia.appnea.ui.components.DatePickerField
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.TimePickerField
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.BordeSuave
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoSobreAcento
import com.berterogarcia.appnea.ui.theme.TextoTerciario

// Características del agua, en grupos. Se siguen guardando juntas en caracteristicasAgua.
private val AGUA_TIPO = listOf("Dulce", "Salada")             // una
private val AGUA_ENTRADA = listOf("Orilla", "Bote")           // una
private val AGUA_MAR = listOf("Olas", "Corrientes", "Resaca") // varias

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
    onGuardado: () -> Unit,
    onCargarEquipo: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val esPrimera = uiState.etapa == Etapa.entries.first()
    val esUltima = uiState.etapa == Etapa.entries.last()

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
    // Botón atrás del sistema: vuelve a la etapa anterior en vez de salir
    BackHandler(enabled = !esPrimera) { viewModel.anterior() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()   // reserva el alto del teclado: los botones quedan justo arriba
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp)
    ) {
        IndicadorEtapas(
            actual = uiState.etapa,
            conErrores = Etapa.entries.filter { e -> uiState.errores.keys.any { it in e.campos } }.toSet(),
            onEtapaClick = viewModel::irA
        )

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
                    .padding(top = 16.dp)
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

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (esPrimera) Spacer(Modifier.weight(1f))
            else BotonSecundario(
                texto = "Anterior",
                onClick = viewModel::anterior,
                enabled = !uiState.guardando, // que no se pueda volver mientras guarda
                modifier = Modifier.weight(1f)
            )
            if (esUltima) BotonPrincipal(
                texto = if (uiState.guardando) "Guardando…" else "Guardar",
                onClick = viewModel::guardar,
                cargando = uiState.guardando,
                modifier = Modifier.weight(1f)
            )
            else BotonPrincipal(
                texto = "Siguiente",
                onClick = viewModel::siguiente,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun IndicadorEtapas(actual: Etapa, conErrores: Set<Etapa>, onEtapaClick: (Etapa) -> Unit) {
    val colores = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Etapa.entries.forEach { etapa ->
            val visitada = etapa.ordinal <= actual.ordinal
            val fondo = when {
                etapa in conErrores -> colores.error
                visitada -> colores.primary
                else -> colores.surfaceVariant
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(fondo)
                    .clickable(enabled = etapa.ordinal < actual.ordinal) { onEtapaClick(etapa) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${etapa.ordinal + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (etapa in conErrores || visitada) colores.onPrimary else colores.onSurfaceVariant
                )
            }
        }
    }
    LinearProgressIndicator(
        progress = { (actual.ordinal + 1f) / Etapa.entries.size },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
    // Título de la etapa y debajo "Paso n de 5"
    Text(
        text = actual.titulo,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 24.dp)
    )
    Text(
        text = "Paso ${actual.ordinal + 1} de ${Etapa.entries.size}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextoTerciario,
        modifier = Modifier.padding(top = 2.dp)
    )
}


// Crea un FocusRequester y pide el foco apenas la etapa entra en pantalla
@Composable
private fun rememberFocoInicial(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return focusRequester
}

@Composable
private fun EtapaGeneral(
    uiState: NuevaInmersionUiState,
    viewModel: NuevaInmersionViewModel,
    onAbrirMapa: () -> Unit
) {
    val foco = rememberFocoInicial()
    SelectorSegmentado(
        titulo = "¿Qué tipo de inmersión fue?",
        opciones = TipoInmersion.entries,
        seleccionada = uiState.tipo,
        etiqueta = { "${it.emoji} ${it.etiqueta}" },
        onSeleccion = { tipo -> tipo?.let(viewModel::onTipoChange) },
        modifier = Modifier.padding(bottom = 12.dp)
    )
    DatePickerField(
        label = "Fecha", valor = uiState.fecha, onValorChange = viewModel::onFechaChange,
        isError = uiState.errores.containsKey(Campo.FECHA), mensajeError = uiState.errores[Campo.FECHA],
        modifier = Modifier.fillMaxWidth()
    )
    CampoTexto(
        valor = uiState.lugar, onValorChange = viewModel::onLugarChange, etiqueta = "Lugar",
        esError = uiState.errores.containsKey(Campo.LUGAR), mensajeError = uiState.errores[Campo.LUGAR] ?: "",
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).focusRequester(foco),
    )
    TarjetaLugar(
        latitud = uiState.latitud,
        longitud = uiState.longitud,
        buscando = uiState.buscandoUbicacion,
        aviso = uiState.avisoUbicacion,
        onAbrirMapa = onAbrirMapa,
        onMiUbicacion = viewModel::usarMiUbicacion,
        onPermisoDenegado = viewModel::permisoUbicacionDenegado,
        onQuitar = viewModel::quitarUbicacion,
        modifier = Modifier.padding(top = 4.dp)
    )
    TimePickerField(
        label = "Hora de entrada", valor = uiState.horaEntrada, onValorChange = viewModel::onHoraEntradaChange,
        isError = uiState.errores.containsKey(Campo.HORA_ENTRADA), mensajeError = uiState.errores[Campo.HORA_ENTRADA],
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    TimePickerField(
        label = "Hora de salida", valor = uiState.horaSalida, onValorChange = viewModel::onHoraSalidaChange,
        isError = uiState.errores.containsKey(Campo.HORA_SALIDA), mensajeError = uiState.errores[Campo.HORA_SALIDA],
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
private fun EtapaInmersion(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    val foco = rememberFocoInicial()
    CampoTexto(
        valor = uiState.profundidadMetros, onValorChange = viewModel::onProfundidadChange,
        etiqueta = "Profundidad máxima (m)",
        esError = uiState.errores.containsKey(Campo.PROFUNDIDAD), mensajeError = uiState.errores[Campo.PROFUNDIDAD] ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().focusRequester(foco)
    )
    val esApnea = uiState.tipo == TipoInmersion.APNEA
    CampoTexto(
        valor = uiState.tiempoFondo, onValorChange = viewModel::onTiempoFondoChange,
        etiqueta = if (esApnea) "Tiempo de apnea (mm:ss)" else "Tiempo de fondo (mm:ss)",
        visualTransformation = tiempoFondoVisualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = if (esApnea) ImeAction.Done else ImeAction.Next
        ),
        esError = uiState.errores.containsKey(Campo.TIEMPO_FONDO), mensajeError = uiState.errores[Campo.TIEMPO_FONDO] ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    // En apnea no hay parada de seguridad
    if (!esApnea) {
        CampoTexto(
            valor = uiState.paradaSeguridadMin, onValorChange = viewModel::onParadaChange,
            etiqueta = "Parada de seguridad (min)",
            esError = uiState.errores.containsKey(Campo.PARADA), mensajeError = uiState.errores[Campo.PARADA] ?: "",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
private fun EtapaCondiciones(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    SelectorSegmentado(
        titulo = "Tipo de agua", ayuda = "una",
        opciones = AGUA_TIPO,
        seleccionada = AGUA_TIPO.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_TIPO, it) },
        permitirNinguna = true
    )
    SelectorSegmentado(
        titulo = "Entrada", ayuda = "una",
        opciones = AGUA_ENTRADA,
        seleccionada = AGUA_ENTRADA.firstOrNull { it in uiState.caracteristicasAgua },
        etiqueta = { it },
        onSeleccion = { viewModel.onAguaUnica(AGUA_ENTRADA, it) },
        permitirNinguna = true,
        modifier = Modifier.padding(top = 12.dp)
    )
    CheckboxGroup(
        titulo = "Estado del mar", opciones = AGUA_MAR,
        seleccionadas = uiState.caracteristicasAgua, onCambio = viewModel::onAguaToggle
    )
    CampoTexto(
        valor = uiState.temperaturaAireC, onValorChange = viewModel::onTempAireChange,
        etiqueta = "Temperatura del aire (°C)",
        esError = uiState.errores.containsKey(Campo.TEMP_AIRE), mensajeError = uiState.errores[Campo.TEMP_AIRE] ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAire) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    CampoTexto(
        valor = uiState.temperaturaAguaC, onValorChange = viewModel::onTempAguaChange,
        etiqueta = "Temperatura del agua (°C)",
        esError = uiState.errores.containsKey(Campo.TEMP_AGUA), mensajeError = uiState.errores[Campo.TEMP_AGUA] ?: "",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        trailingIcon = { BotonSigno(onClick = viewModel::onAlternarSignoAgua) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    CampoTexto(
        valor = uiState.visibilidadMetros, onValorChange = viewModel::onVisibilidadChange,
        etiqueta = "Visibilidad (m)",
        esError = uiState.errores.containsKey(Campo.VISIBILIDAD), mensajeError = uiState.errores[Campo.VISIBILIDAD] ?: "",
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
    Row {
        Text("¿Qué equipo usaste?", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Text(" · de tu perfil", color = TextoTerciario, style = MaterialTheme.typography.bodyMedium)
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
                        categoria.etiqueta,
                        color = TextoTerciario,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 10.dp, start = 2.dp)
                    )
                    equipos.forEach { equipo ->
                        val etiqueta = equipo.etiquetaInmersion
                        FilaEquipo(
                            equipo = equipo,
                            marcado = etiqueta in uiState.equipamiento,
                            onCambio = { viewModel.onEquipamientoToggle(etiqueta, it) }
                        )
                    }
                }
            TextButton(onClick = onCargarEquipo, modifier = Modifier.padding(top = 4.dp)) {
                Text("+ Agregar otro equipo", color = Acento)
            }
        }
    }
    CampoTexto(
        valor = uiState.lastreKg, onValorChange = viewModel::onLastreChange, etiqueta = "Lastre (kg)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        esError = uiState.errores.containsKey(Campo.LASTRE), mensajeError = uiState.errores[Campo.LASTRE] ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    )
    CampoTexto(
        valor = uiState.proteccionMm, onValorChange = viewModel::onProteccionMmChange, etiqueta = "Protección (mm)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        esError = uiState.errores.containsKey(Campo.PROTECCION), mensajeError = uiState.errores[Campo.PROTECCION] ?: "",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

/** Toda la fila se toca (no solo el checkbox) y TalkBack la anuncia como checkbox. */
@Composable
private fun FilaEquipo(equipo: EquipoEntity, marcado: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onCambio)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = marcado,
            onCheckedChange = null, // lo maneja el toggleable de la fila
            colors = CheckboxDefaults.colors(
                checkedColor = Acento,
                uncheckedColor = BordeSuave,
                checkmarkColor = TextoSobreAcento
            )
        )
        Column(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
            val nombre = listOf(equipo.marca, equipo.modelo)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { equipo.categoria.etiqueta }
            Text(nombre, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (equipo.talle.isNotBlank()) {
                Text(equipo.talle, color = TextoTerciario, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SinEquipo(perfilVacio: Boolean, onCargarEquipo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (perfilVacio) "Todavía no cargaste equipo en tu perfil."
            else "No tenés equipo en uso para este tipo de inmersión.",
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
        BotonSecundario(
            texto = "Cargar equipo",
            onClick = onCargarEquipo,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EtapaNotas(uiState: NuevaInmersionUiState, viewModel: NuevaInmersionViewModel) {
    val foco = rememberFocoInicial()
    CampoTexto(
        valor = uiState.actividad,
        onValorChange = viewModel::onActividadChange,
        etiqueta = "Actividad", minLines = 3,
        modifier = Modifier.fillMaxWidth().focusRequester(foco)
    )
    CampoTexto(
        valor = uiState.comentarios,
        onValorChange = viewModel::onComentariosChange,
        etiqueta = "Comentarios",
        minLines = 3,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    Text(
        "Al guardar se registran solas las condiciones del momento de entrada: clima, olas, marea y luna.",
        style = MaterialTheme.typography.bodySmall,
        color = TextoSecundario,
        modifier = Modifier.padding(top = 8.dp)
    )
}

// Botón para cambiar el signo: muchos teclados numéricos no tienen la tecla "-"
@Composable
private fun BotonSigno(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = "Cambiar signo" } // para TalkBack
    ) {
        Text("±", style = MaterialTheme.typography.titleLarge, color = Acento)
    }
}