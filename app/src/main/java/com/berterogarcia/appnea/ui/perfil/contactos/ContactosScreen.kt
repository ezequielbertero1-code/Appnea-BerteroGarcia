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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.data.local.ContactoEmergenciaEntity
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.condiciones.Acento
import com.berterogarcia.appnea.ui.perfil.comun.BotonAgregar
import com.berterogarcia.appnea.ui.perfil.comun.CargandoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.DialogoConfirmarEliminacion
import com.berterogarcia.appnea.ui.perfil.comun.EncabezadoPerfil
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVacio
import com.berterogarcia.appnea.ui.perfil.comun.Insignia
import com.berterogarcia.appnea.ui.perfil.comun.MensajeError
import com.berterogarcia.appnea.ui.perfil.comun.TarjetaItemPerfil

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
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            EncabezadoPerfil(titulo = "Contactos de emergencia", onVolver = onVolver, modifier = Modifier.padding(top = 8.dp))
            MensajeError(estado.error ?: mensaje)

            when {
                estado.cargando -> CargandoPerfil()
                estado.items.isEmpty() && estado.error == null -> EstadoVacio(
                    texto = "No tenés contactos de emergencia.\nCargá al menos uno antes de tu próxima salida al agua.",
                    textoBoton = "Agregar contacto",
                    onAccion = onAgregar
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.items, key = { it.id }) { contacto ->
                        TarjetaContacto(
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
                descripcion = "Agregar contacto",
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
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onLlamar: (String) -> Unit,
    onHacerPrincipal: () -> Unit
) {
    TarjetaItemPerfil(
        titulo = contacto.nombre,
        subtitulo = contacto.relacion,
        lineas = listOf(
            "Tel: ${contacto.telefono}",
            if (contacto.telefonoAlternativo.isNotBlank()) "Alternativo: ${contacto.telefonoAlternativo}" else "",
            contacto.notas
        ),
        insignias = if (contacto.esPrincipal) listOf(Insignia("Principal", Acento)) else emptyList(),
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
            BotonSecundario(
                texto = "Llamar",
                onClick = { onLlamar(contacto.telefono) },
                icono = rememberVectorPainter(Icons.Default.Call)
            )
            if (!contacto.esPrincipal) {
                TextButton(
                    onClick = onHacerPrincipal,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) { Text("Hacer principal") }
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
