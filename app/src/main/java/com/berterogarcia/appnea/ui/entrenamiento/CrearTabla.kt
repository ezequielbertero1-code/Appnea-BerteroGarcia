package com.berterogarcia.appnea.ui.entrenamiento

import com.berterogarcia.appnea.util.intentar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Fase
import com.berterogarcia.appnea.dominio.modelo.TablaPersonalizada
import com.berterogarcia.appnea.dominio.modelo.TipoFase
import com.berterogarcia.appnea.dominio.repositorios.RepositorioTablas
import com.berterogarcia.appnea.ui.components.BarraAccionesInferior
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.components.Stepper
import com.berterogarcia.appnea.ui.components.TextoUi
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.components.textoUi
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PASO_SEG = 15            // lo que suman / restan los botones − / +
private const val MIN_SEG = 15
private const val MAX_SEG = 30 * 60
private const val MAX_NOMBRE = 40

// ================================ ViewModel ================================

data class CrearTablaUiState(
    val nombre: String = "",
    val fases: List<Fase> = emptyList(),
    val tipoNueva: TipoFase = TipoFase.PREPARACION, // la primera suele ser la preparación
    val duracionNueva: Int = 120,
    val guardando: Boolean = false,
    val guardadoOk: Boolean = false,
    val error: TextoUi? = null
) {
    val tieneApnea: Boolean get() = fases.any { it.tipo == TipoFase.APNEA }
    val puedeGuardar: Boolean get() = nombre.isNotBlank() && tieneApnea && !guardando
    val puedeRepetir: Boolean get() = tieneApnea && fases.any { it.tipo == TipoFase.DESCANSO }
}

class CrearTablaViewModel(private val repository: RepositorioTablas) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearTablaUiState())
    val uiState: StateFlow<CrearTablaUiState> = _uiState

    fun onNombreChange(v: String) = _uiState.update { it.copy(nombre = v.take(MAX_NOMBRE), error = null) }

    fun onTipoNuevaChange(tipo: TipoFase) = _uiState.update { it.copy(tipoNueva = tipo) }

    fun onDuracionNuevaPaso(delta: Int) = _uiState.update { it.copy(duracionNueva = ajustar(it.duracionNueva, delta)) }

    /** Agrega la fase y deja elegido el tipo que suele venir después (apnea ↔ descanso). */
    fun onAgregar() = _uiState.update {
        val siguiente = when (it.tipoNueva) {
            TipoFase.PREPARACION, TipoFase.DESCANSO -> TipoFase.APNEA
            TipoFase.APNEA -> TipoFase.DESCANSO
        }
        it.copy(fases = it.fases + Fase(it.tipoNueva, it.duracionNueva), tipoNueva = siguiente, error = null)
    }

    fun onAjustarFase(indice: Int, delta: Int) = _uiState.update {
        it.copy(fases = it.fases.mapIndexed { i, f ->
            if (i == indice) f.copy(duracionSegundos = ajustar(f.duracionSegundos, delta)) else f
        })
    }

    fun onQuitarFase(indice: Int) = _uiState.update {
        it.copy(fases = it.fases.filterIndexed { i, _ -> i != indice })
    }

    /**
     * Repite la última apnea y el último descanso, en el orden que corresponda:
     * termina en apnea → agrega descanso + apnea; termina en descanso → apnea + descanso.
     */
    fun onRepetirUltimas() = _uiState.update { s ->
        val apnea = s.fases.lastOrNull { it.tipo == TipoFase.APNEA } ?: return@update s
        val descanso = s.fases.lastOrNull { it.tipo == TipoFase.DESCANSO } ?: return@update s
        val nuevas = if (s.fases.last().tipo == TipoFase.DESCANSO) listOf(apnea, descanso) else listOf(descanso, apnea)
        s.copy(fases = s.fases + nuevas)
    }

    fun onGuardar() {
        val s = _uiState.value
        if (!s.puedeGuardar) return
        _uiState.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            // guardar() devuelve false si no hay sesión; si la base falla, runCatching da false
            val ok = intentar("CrearTabla") {
                repository.guardar(TablaPersonalizada(nombre = s.nombre.trim(), fases = s.fases))
            }.getOrDefault(false)
            _uiState.update {
                if (ok) it.copy(guardando = false, guardadoOk = true)
                else it.copy(guardando = false, error = textoUi(R.string.crear_tabla_error_guardar))
            }
        }
    }

    private fun ajustar(segundos: Int, delta: Int) = (segundos + delta).coerceIn(MIN_SEG, MAX_SEG)

}

// ================================ Pantalla ================================

@Composable
fun CrearTablaScreen(
    viewModel: CrearTablaViewModel,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()
    // Las etiquetas se resuelven acá: el selector recibe una función común (no @Composable)
    val etiquetas = TipoFase.entries.associateWith { etiquetaFase(it) }

    LaunchedEffect(s.guardadoOk) { if (s.guardadoOk) onCerrar() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        BarraSuperior(
            titulo = stringResource(R.string.crear_tabla_titulo),
            navegacion = NavegacionBarra.CERRAR,
            onNavegacion = onCerrar
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            CampoTexto(
                valor = s.nombre,
                onValorChange = viewModel::onNombreChange,
                etiqueta = stringResource(R.string.crear_tabla_nombre),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (s.fases.isEmpty()) {
                Text(
                    stringResource(R.string.crear_tabla_sin_fases),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp)) {
                    Text(
                        stringResource(
                            R.string.crear_tabla_resumen,
                            s.fases.size,
                            s.fases.count { it.tipo == TipoFase.APNEA }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.tabla_total, minSeg(s.fases.sumOf { it.duracionSegundos })),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
                TiraFases(s.fases, alto = 8.dp, modifier = Modifier.padding(bottom = 10.dp))
                GrupoLista {
                    s.fases.forEachIndexed { i, fase ->
                        FilaFaseEditable(
                            numero = i + 1,
                            fase = fase,
                            indice = i,
                            total = s.fases.size,
                            onMenos = { viewModel.onAjustarFase(i, -PASO_SEG) },
                            onMas = { viewModel.onAjustarFase(i, PASO_SEG) },
                            onQuitar = { viewModel.onQuitarFase(i) }
                        )
                    }
                }
                Text(
                    stringResource(R.string.crear_tabla_ayuda_pasos, PASO_SEG),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            // ---- Agregar fase ----
            Text(
                stringResource(R.string.crear_tabla_agregar_fase),
                style = MaterialTheme.typography.titleSmall,
                color = Acento,
                modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
            )
            SelectorSegmentado(
                opciones = TipoFase.entries,
                seleccionada = s.tipoNueva,
                etiqueta = { etiquetas.getValue(it) },
                onSeleccion = { tipo -> tipo?.let(viewModel::onTipoNuevaChange) }
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Stepper(
                    valor = minSeg(s.duracionNueva),
                    onMenos = { viewModel.onDuracionNuevaPaso(-PASO_SEG) },
                    onMas = { viewModel.onDuracionNuevaPaso(PASO_SEG) },
                    descripcionMenos = stringResource(R.string.crear_tabla_restar_nueva, PASO_SEG),
                    descripcionMas = stringResource(R.string.crear_tabla_sumar_nueva, PASO_SEG)
                )
                BotonContorno(
                    texto = stringResource(R.string.crear_tabla_agregar),
                    onClick = viewModel::onAgregar,
                    icono = rememberVectorPainter(Icons.Rounded.Add),
                    modifier = Modifier.weight(1f)
                )
            }
            if (s.puedeRepetir) {
                BotonSecundario(
                    texto = stringResource(R.string.crear_tabla_repetir),
                    onClick = viewModel::onRepetirUltimas,
                    icono = painterResource(R.drawable.ic_ms_repeat),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            // Por qué no se puede guardar todavía (o el error si falló)
            val pista = when {
                s.error != null -> s.error?.resolver()
                s.nombre.isBlank() -> stringResource(R.string.crear_tabla_pista_nombre)
                !s.tieneApnea -> stringResource(R.string.crear_tabla_pista_apnea)
                else -> null
            }
            pista?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.error != null) ColorError else TextoSuperficieVariante,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        }

        BarraAccionesInferior {
            BotonPrincipal(
                texto = stringResource(if (s.guardando) R.string.accion_guardando else R.string.crear_tabla_guardar),
                onClick = viewModel::onGuardar,
                enabled = s.puedeGuardar,
                cargando = s.guardando,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Fase de la tabla en armado: ícono del tipo, nombre, stepper −/+ y botón para quitarla. */
@Composable
private fun FilaFaseEditable(
    numero: Int,
    fase: Fase,
    indice: Int,
    total: Int,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    onQuitar: () -> Unit
) {
    FilaLista(
        titulo = etiquetaFase(fase.tipo),
        subtitulo = stringResource(R.string.crear_tabla_fase_n, numero),
        indice = indice,
        total = total,
        inicio = {
            IconoCirculo(
                painterResource(iconoFase(fase.tipo)),
                tono = if (fase.tipo == TipoFase.APNEA) TonoIcono.APNEA else TonoIcono.NORMAL,
                modifier = Modifier.size(32.dp)
            )
        },
        fin = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(
                    valor = minSeg(fase.duracionSegundos),
                    onMenos = onMenos,
                    onMas = onMas,
                    descripcionMenos = stringResource(R.string.crear_tabla_restar_fase, PASO_SEG, numero),
                    descripcionMas = stringResource(R.string.crear_tabla_sumar_fase, PASO_SEG, numero)
                )
                IconButton(onClick = onQuitar) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.crear_tabla_quitar_fase, numero))
                }
            }
        }
    )
}
