package com.berterogarcia.appnea.ui.perfil.certificados

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil
import com.berterogarcia.appnea.ui.perfil.comun.insigniaVencimiento

@Composable
fun CertificadosScreen(
    viewModel: CertificadosViewModel,
    onVolver: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()

    var idAEliminar by rememberSaveable { mutableStateOf<Long?>(null) }
    val certificadoAEliminar = idAEliminar?.let { id -> estado.items.firstOrNull { it.id == id } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            EncabezadoPerfil(titulo = "Certificaciones", onVolver = onVolver, modifier = Modifier.padding(top = 8.dp))
            MensajeError(estado.error ?: mensaje)

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = "Todavía no cargaste certificaciones.\nPodés agregar tus niveles de buceo y apnea, y tu apto médico para que te avise cuando vence.",
                    textoBoton = "Agregar certificación",
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.items, key = { it.id }) { certificado ->
                        TarjetaCertificado(
                            certificado = certificado,
                            onEditar = { onEditar(certificado.id) },
                            onEliminar = {
                                viewModel.mensajeMostrado()
                                idAEliminar = certificado.id
                            }
                        )
                    }
                }
            }
        }

        if (!estado.cargando && estado.error == null) {
            BotonAgregar(
                descripcion = "Agregar certificación",
                onClick = onAgregar,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }

    certificadoAEliminar?.let { certificado ->
        DialogoConfirmarEliminacion(
            queSeElimina = certificado.titulo,
            onConfirmar = {
                viewModel.eliminar(certificado.id)
                idAEliminar = null
            },
            onCancelar = { idAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaCertificado(certificado: CertificadoEntity, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val entidadEscuela = listOf(certificado.entidad, certificado.escuelaInstructor)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    TarjetaItemPerfil(
        titulo = certificado.titulo,
        subtitulo = certificado.tipo.etiqueta,
        lineas = listOf(
            entidadEscuela,
            if (certificado.numero.isNotBlank()) "N° ${certificado.numero}" else "",
            certificado.fechaEmision?.let { "Emitido: ${it.formatoLargo()}" }.orEmpty(),
            certificado.fechaVencimiento?.let { "Vence: ${it.formatoLargo()}" }.orEmpty(),
            certificado.notas
        ),
        insignias = listOfNotNull(insigniaVencimiento(certificado.fechaVencimiento)),
        onEditar = onEditar,
        onEliminar = onEliminar
    )
}
