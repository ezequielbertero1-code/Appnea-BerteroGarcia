package com.berterogarcia.appnea.ui.perfil.contactos

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.Insignia
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil
import com.berterogarcia.appnea.ui.theme.Acento

@Composable
fun ContactosScreen(
    viewModel: ContactosViewModel,
    onVolver: () -> Unit,
    onAgregar: () -> Unit,
    onEditar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val mensaje by viewModel.mensaje.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var idAEliminar by rememberSaveable { mutableStateOf<Long?>(null) }
    val contactoAEliminar = idAEliminar?.let { id -> estado.items.firstOrNull { it.id == id } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EncabezadoPerfil(titulo = stringResource(R.string.perfil_contactos_titulo), onVolver = onVolver)
            MensajeError((estado.error ?: mensaje)?.resolver(), Modifier.padding(horizontal = 16.dp))

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = stringResource(R.string.perfil_contactos_vacio),
                    textoBoton = stringResource(R.string.perfil_agregar_contacto),
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)) {
                    itemsIndexed(estado.items, key = { _, it -> it.id }) { i, contacto ->
                        TarjetaContacto(
                            indice = i,
                            total = estado.items.size,
                            contacto = contacto,
                            onEditar = { onEditar(contacto.id) },
                            onEliminar = {
                                viewModel.mensajeMostrado()
                                idAEliminar = contacto.id
                            },
                            onLlamar = { telefono ->
                                if (!abrirMarcador(context, telefono)) viewModel.noSePudoLlamar()
                            },
                            onHacerPrincipal = { viewModel.hacerPrincipal(contacto.id) }
                        )
                    }
                }
            }
        }

        if (!estado.cargando && estado.error == null) {
            BotonAgregar(
                descripcion = stringResource(R.string.perfil_agregar_contacto),
                onClick = onAgregar,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
    }

    contactoAEliminar?.let { contacto ->
        DialogoConfirmarEliminacion(
            queSeElimina = contacto.nombre,
            onConfirmar = {
                viewModel.eliminar(contacto.id)
                idAEliminar = null
            },
            onCancelar = { idAEliminar = null }
        )
    }
}

@Composable
private fun TarjetaContacto(
    contacto: ContactoEmergenciaEntity,
    indice: Int,
    total: Int,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onLlamar: (String) -> Unit,
    onHacerPrincipal: () -> Unit
) {
    TarjetaItemPerfil(
        titulo = contacto.nombre,
        subtitulo = contacto.relacion,
        lineas = listOf(
            stringResource(R.string.perfil_telefono, contacto.telefono),
            if (contacto.telefonoAlternativo.isNotBlank()) stringResource(R.string.perfil_alternativo, contacto.telefonoAlternativo) else "",
            contacto.notas
        ),
        insignias = if (contacto.esPrincipal) listOf(Insignia(stringResource(R.string.perfil_principal), Acento)) else emptyList(),
        icono = painterResource(if (contacto.esPrincipal) R.drawable.ic_ms_emergency else R.drawable.ic_ms_person),
        tonoIcono = if (contacto.esPrincipal) TonoIcono.ERROR else TonoIcono.NORMAL,
        indice = indice,
        total = total,
        modifier = Modifier.padding(bottom = 2.dp),
        onEditar = onEditar,
        onEliminar = onEliminar
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // El principal lleva "Llamar" relleno; el resto, tonal (como en el mockup)
            val llamar = stringResource(R.string.perfil_llamar)
            val iconoLlamar = rememberVectorPainter(Icons.Rounded.Call)
            if (contacto.esPrincipal) {
                BotonPrincipal(texto = llamar, onClick = { onLlamar(contacto.telefono) }, icono = iconoLlamar, modifier = Modifier.weight(1f))
            } else {
                BotonSecundario(texto = llamar, onClick = { onLlamar(contacto.telefono) }, icono = iconoLlamar, modifier = Modifier.weight(1f))
                BotonContorno(texto = stringResource(R.string.perfil_hacer_principal), onClick = onHacerPrincipal, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Abre el marcador con el número cargado. ACTION_DIAL NO necesita permiso
 * (a diferencia de ACTION_CALL): el usuario confirma la llamada en la app de teléfono.
 * @return false si el dispositivo no tiene app de teléfono.
 */
private fun abrirMarcador(context: Context, telefono: String): Boolean {
    val numero = telefono.filter { it.isDigit() || it == '+' }
    if (numero.isEmpty()) return false
    val intent = Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(numero)}".toUri())
    // Si no hay app de teléfono (o falla por cualquier motivo) devuelve false y se muestra el aviso.
    return runCatching { context.startActivity(intent) }.isSuccess
}
