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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BarraAccionesInferior
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.ProgresoPasosNavegable
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Características del agua, en grupos. Se guardan juntas en caracteristicasAgua (son datos, no
// textos de la interfaz: si se tradujeran, los registros viejos dejarían de coincidir).
internal val AGUA_TIPO = listOf("Dulce", "Salada")             // una
internal val AGUA_ENTRADA = listOf("Orilla", "Bote")           // una
internal val AGUA_MAR = listOf("Olas", "Corrientes", "Resaca") // varias

internal val tiempoFondoVisualTransformation = VisualTransformation { texto ->
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
            ubicacion = viewModel.ubicacionTelefono,
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

/** Nombre de cada paso del formulario. */
@Composable
internal fun Etapa.nombre(): String = stringResource(
    when (this) {
        Etapa.GENERAL -> R.string.nueva_etapa_general
        Etapa.INMERSION -> R.string.nueva_etapa_inmersion
        Etapa.CONDICIONES -> R.string.nueva_etapa_condiciones
        Etapa.EQUIPO -> R.string.nueva_etapa_equipo
        Etapa.NOTAS -> R.string.nueva_etapa_notas
    }
)
