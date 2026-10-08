package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.berterogarcia.appnea.data.local.Fase
import com.berterogarcia.appnea.data.local.TablaPersonalizadaEntity
import com.berterogarcia.appnea.data.local.TipoFase
import com.berterogarcia.appnea.data.repository.TablaPersonalizadaRepository
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.SelectorSegmentado
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
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
    val error: String? = null
) {
    val tieneApnea: Boolean get() = fases.any { it.tipo == TipoFase.APNEA }
    val puedeGuardar: Boolean get() = nombre.isNotBlank() && tieneApnea && !guardando
    val puedeRepetir: Boolean get() = tieneApnea && fases.any { it.tipo == TipoFase.DESCANSO }
}

class CrearTablaViewModel(private val repository: TablaPersonalizadaRepository) : ViewModel() {

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
            val ok = runCatching {
                repository.guardar(TablaPersonalizadaEntity(nombre = s.nombre.trim(), fases = s.fases))
            }.getOrDefault(false)
            _uiState.update {
                if (ok) it.copy(guardando = false, guardadoOk = true)
                else it.copy(guardando = false, error = "No se pudo guardar la tabla. Probá de nuevo.")
            }
        }
    }

    private fun ajustar(segundos: Int, delta: Int) = (segundos + delta).coerceIn(MIN_SEG, MAX_SEG)

    companion object {
        fun Factory(repository: TablaPersonalizadaRepository) = viewModelFactory {
            initializer { CrearTablaViewModel(repository) }
        }
    }
}

// ================================ Pantalla ================================

@Composable
fun CrearTablaScreen(
    viewModel: CrearTablaViewModel,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(s.guardadoOk) { if (s.guardadoOk) onCerrar() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, contentDescription = "Cerrar") }
            Text("Nueva tabla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CampoTexto(
                valor = s.nombre,
                onValorChange = viewModel::onNombreChange,
                etiqueta = "Nombre",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            if (s.fases.isEmpty()) {
                Text(
                    "Todavía no agregaste fases. Empezá con una preparación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSecundario,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                TiraFases(s.fases, alto = 9.dp, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
                    Text(
                        "${s.fases.size} fases · ${s.fases.count { it.tipo == TipoFase.APNEA }} apneas",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "Total ${minSeg(s.fases.sumOf { it.duracionSegundos })}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextoSecundario
                    )
                }
                s.fases.forEachIndexed { i, fase ->
                    FilaFaseEditable(
                        numero = i + 1,
                        fase = fase,
                        onMenos = { viewModel.onAjustarFase(i, -PASO_SEG) },
                        onMas = { viewModel.onAjustarFase(i, PASO_SEG) },
                        onQuitar = { viewModel.onQuitarFase(i) }
                    )
                }
            }

            if (s.puedeRepetir) {
                TextButton(
                    onClick = viewModel::onRepetirUltimas,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .border(1.dp, Acento.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                ) { Text("⟳ Repetir última apnea + descanso", color = Acento) }
            }

            Text(
                "Agregar fase",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            SelectorSegmentado(
                opciones = TipoFase.entries,
                seleccionada = s.tipoNueva,
                etiqueta = ::etiquetaFase,
                onSeleccion = { tipo -> tipo?.let(viewModel::onTipoNuevaChange) }
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectorPaso(
                    valor = minSeg(s.duracionNueva),
                    onMenos = { viewModel.onDuracionNuevaPaso(-PASO_SEG) },
                    onMas = { viewModel.onDuracionNuevaPaso(PASO_SEG) },
                    descripcion = "duración de la fase nueva",
                    modifier = Modifier.weight(1f)
                )
                BotonSecundario(texto = "+ Agregar", onClick = viewModel::onAgregar, modifier = Modifier.weight(1f))
            }
            Text(
                "Los botones − / + suman o restan 15 s.",
                style = MaterialTheme.typography.labelSmall,
                color = TextoTerciario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }

        // Por qué no se puede guardar todavía (o el error si falló)
        val pista = when {
            s.error != null -> s.error
            s.nombre.isBlank() -> "Poné un nombre para la tabla."
            !s.tieneApnea -> "Agregá al menos una apnea."
            else -> null
        }
        pista?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (s.error != null) ColorError else TextoTerciario,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        BotonPrincipal(
            texto = if (s.guardando) "Guardando…" else "Guardar tabla",
            onClick = viewModel::onGuardar,
            enabled = s.puedeGuardar,
            cargando = s.guardando,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

@Composable
private fun FilaFaseEditable(
    numero: Int,
    fase: Fase,
    onMenos: () -> Unit,
    onMas: () -> Unit,
    onQuitar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FondoTarjeta)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colorFase(fase.tipo))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(etiquetaFase(fase.tipo), style = MaterialTheme.typography.bodyMedium)
            Text("Fase $numero", style = MaterialTheme.typography.labelSmall, color = TextoTerciario)
        }
        SelectorPaso(
            valor = minSeg(fase.duracionSegundos),
            onMenos = onMenos,
            onMas = onMas,
            descripcion = "duración de la fase $numero"
        )
        IconButton(onClick = onQuitar) {
            Icon(Icons.Filled.Close, contentDescription = "Quitar fase $numero", tint = TextoTerciario)
        }
    }
}
