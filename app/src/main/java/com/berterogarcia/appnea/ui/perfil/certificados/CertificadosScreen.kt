package com.berterogarcia.appnea.ui.perfil.certificados

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
import com.berterogarcia.appnea.data.local.CertificadoEntity
import com.berterogarcia.appnea.data.local.TipoCertificado
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVencimiento
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil
import com.berterogarcia.appnea.ui.perfil.comun.estadoVencimiento
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
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoPerfil(titulo = stringResource(R.string.cuenta_certificaciones), onVolver = onVolver)
            MensajeError((estado.error ?: mensaje)?.resolver(), Modifier.padding(horizontal = 16.dp))

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = stringResource(R.string.perfil_certificados_vacio),
                    textoBoton = stringResource(R.string.perfil_agregar_certificado),
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                    itemsIndexed(estado.items, key = { _, it -> it.id }) { i, certificado ->
                        TarjetaCertificado(
                            indice = i,
                            total = estado.items.size,
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
                descripcion = stringResource(R.string.perfil_agregar_certificado),
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
private fun TarjetaCertificado(certificado: CertificadoEntity, indice: Int, total: Int, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val entidadEscuela = listOf(certificado.entidad, certificado.escuelaInstructor)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    TarjetaItemPerfil(
        titulo = certificado.titulo,
        subtitulo = certificado.tipo.nombre(),
        lineas = listOf(
            entidadEscuela,
            if (certificado.numero.isNotBlank()) stringResource(R.string.perfil_numero, certificado.numero) else "",
            certificado.fechaEmision?.let { stringResource(R.string.perfil_emitido, it.formatoLargo()) }.orEmpty(),
            certificado.fechaVencimiento?.let { stringResource(R.string.perfil_vence, it.formatoLargo()) }.orEmpty(),
            certificado.notas
        ),
        insignias = listOfNotNull(insigniaVencimiento(certificado.fechaVencimiento)),
        icono = painterResource(
            if (certificado.tipo == TipoCertificado.APTO_MEDICO) R.drawable.ic_ms_medical_services
            else R.drawable.ic_ms_workspace_premium
        ),
        // El círculo avisa el vencimiento sin tener que leer la insignia
        tonoIcono = when (estadoVencimiento(certificado.fechaVencimiento)) {
            EstadoVencimiento.VENCIDO -> TonoIcono.ERROR
            EstadoVencimiento.POR_VENCER -> TonoIcono.ADVERTENCIA
            else -> TonoIcono.NORMAL
        },
        indice = indice,
        total = total,
        modifier = Modifier.padding(bottom = 2.dp),
        onEditar = onEditar,
        onEliminar = onEliminar
    )
}
