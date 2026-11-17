package com.berterogarcia.appnea.ui.perfil.equipamiento

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.Equipo
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.Insignia
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil
import com.berterogarcia.appnea.ui.perfil.comun.insigniaMantenimiento
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

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
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoPerfil(titulo = stringResource(R.string.perfil_equipamiento_titulo), onVolver = onVolver)
            MensajeError((estado.error ?: mensaje)?.resolver(), Modifier.padding(horizontal = 16.dp))

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = stringResource(R.string.perfil_equipamiento_vacio),
                    textoBoton = stringResource(R.string.perfil_agregar_equipo),
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                    itemsIndexed(estado.items, key = { _, it -> it.id }) { i, equipo ->
                        TarjetaEquipo(
                            indice = i,
                            total = estado.items.size,
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
                descripcion = stringResource(R.string.perfil_agregar_equipo),
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
private fun TarjetaEquipo(equipo: Equipo, indice: Int, total: Int, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val insignias = buildList {
        if (!equipo.enUso) add(Insignia(stringResource(R.string.perfil_retirado), TextoSuperficieVariante))
        if (equipo.enUso) insigniaMantenimiento(equipo.proximoMantenimiento)?.let { add(it) }
    }
    val marcaModelo = listOf(equipo.marca, equipo.modelo).filter { it.isNotBlank() }.joinToString(" ")

    TarjetaItemPerfil(
        titulo = marcaModelo.ifBlank { equipo.categoria.nombre() },
        subtitulo = equipo.categoria.nombre(),
        lineas = listOf(
            if (equipo.talle.isNotBlank()) stringResource(R.string.perfil_talle, equipo.talle) else "",
            equipo.fechaCompra?.let { stringResource(R.string.perfil_comprado, it.formatoLargo()) }.orEmpty(),
            equipo.proximoMantenimiento?.let { stringResource(R.string.perfil_proximo_service, it.formatoLargo()) }.orEmpty(),
            equipo.notas
        ),
        insignias = insignias,
        atenuada = !equipo.enUso,
        icono = painterResource(iconoCategoria(equipo.categoria)),
        indice = indice,
        total = total,
        modifier = Modifier.padding(bottom = 2.dp),
        onEditar = onEditar,
        onEliminar = onEliminar
    )
}

/** Ícono de Material Symbols para cada categoría (las que no tienen uno claro usan el de buceo). */
@DrawableRes
private fun iconoCategoria(categoria: CategoriaEquipo): Int = when (categoria) {
    CategoriaEquipo.MASCARA -> R.drawable.ic_ms_visibility
    CategoriaEquipo.SNORKEL -> R.drawable.ic_ms_air
    CategoriaEquipo.ALETAS -> R.drawable.ic_ms_footprint
    CategoriaEquipo.TRAJE -> R.drawable.ic_ms_checkroom
    CategoriaEquipo.GUANTES_ESCARPINES -> R.drawable.ic_ms_back_hand
    CategoriaEquipo.LASTRE -> R.drawable.ic_ms_fitness_center
    CategoriaEquipo.COMPUTADORA -> R.drawable.ic_ms_watch
    CategoriaEquipo.LINTERNA -> R.drawable.ic_ms_flashlight_on
    CategoriaEquipo.CUCHILLO -> R.drawable.ic_ms_construction
    CategoriaEquipo.BOYA -> R.drawable.ic_ms_waves
    else -> R.drawable.ic_ms_scuba_diving   // regulador, chaleco, tubo, otro
}
