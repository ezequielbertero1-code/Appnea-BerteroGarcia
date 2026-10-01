package com.berterogarcia.appnea.ui.perfil.equipamiento

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.EquipoEntity
import com.berterogarcia.appnea.data.local.descripcion
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.Insignia
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil
import com.berterogarcia.appnea.ui.perfil.comun.insigniaMantenimiento

@Composable
fun EquipamientoScreen(
    viewModel: EquipamientoViewModel,
    onVolver: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()

    // Se guarda solo el id (sobrevive a la rotación) y se busca el ítem en la lista actual.
    var idAEliminar by rememberSaveable { mutableStateOf<Long?>(null) }
    val equipoAEliminar = idAEliminar?.let { id -> estado.items.firstOrNull { it.id == id } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            EncabezadoPerfil(titulo = "Mi equipamiento", onVolver = onVolver, modifier = Modifier.padding(top = 8.dp))
            MensajeError(estado.error ?: mensaje)

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = "Todavía no cargaste equipamiento.\nAgregá tus aletas, máscara, traje, computadora...",
                    textoBoton = "Agregar equipo",
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.items, key = { it.id }) { equipo ->
                        TarjetaEquipo(
                            equipo = equipo,
                            onEditar = { onEditar(equipo.id) },
                            onEliminar = {
                                viewModel.mensajeMostrado()
                                idAEliminar = equipo.id
                            }
                        )
                    }
                }
            }
        }

        if (!estado.cargando && estado.error == null) {
            BotonAgregar(
                descripcion = "Agregar equipo",
                onClick = onAgregar,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }

    equipoAEliminar?.let { equipo ->
        DialogoConfirmarEliminacion(
            queSeElimina = equipo.descripcion,
            onConfirmar = {
                viewModel.eliminar(equipo.id)
                idAEliminar = null
            },
            onCancelar = { idAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaEquipo(equipo: EquipoEntity, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val insignias = buildList {
        if (!equipo.enUso) add(Insignia("Retirado", Color.White.copy(alpha = 0.6f)))
        if (equipo.enUso) insigniaMantenimiento(equipo.proximoMantenimiento)?.let { add(it) }
    }
    val marcaModelo = listOf(equipo.marca, equipo.modelo).filter { it.isNotBlank() }.joinToString(" ")

    TarjetaItemPerfil(
        titulo = marcaModelo.ifBlank { equipo.categoria.etiqueta },
        subtitulo = equipo.categoria.etiqueta,
        lineas = listOf(
            if (equipo.talle.isNotBlank()) "Talle / medida: ${equipo.talle}" else "",
            equipo.fechaCompra?.let { "Comprado: ${it.formatoLargo()}" }.orEmpty(),
            equipo.proximoMantenimiento?.let { "Próximo service: ${it.formatoLargo()}" }.orEmpty(),
            equipo.notas
        ),
        insignias = insignias,
        atenuada = !equipo.enUso,
        onEditar = onEditar,
        onEliminar = onEliminar
    )
}
