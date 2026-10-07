package com.berterogarcia.appnea.ui.perfil.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.FormaBoton
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario

// Componentes compartidos por todas las pantallas del perfil.
// Mismo estilo que el resto de la app: tarjetas FondoTarjeta, acento celeste, texto blanco.

private val RojoError = Color(0xFFFF6B6B)

/** Título de pantalla con botón de volver. */
@Composable
fun EncabezadoPerfil(titulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onVolver) {
            Icon(painterResource(R.drawable.ic_volver), contentDescription = "Volver", tint = Color.White)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Indicador centrado mientras se lee la base. */
@Composable
fun CargandoPerfil(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Acento)
    }
}

/** Texto rojo para errores que no son de un campo puntual (falló la base, no hay sesión...). */
@Composable
fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
    if (texto.isNullOrBlank()) return
    Text(
        text = texto,
        color = RojoError,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/** Botón principal de los formularios: se deshabilita y muestra progreso mientras guarda. */
@Composable
fun BotonGuardar(guardando: Boolean, habilitado: Boolean = true, onClick: () -> Unit) {
    BotonPrincipal(
        texto = if (guardando) "Guardando…" else "Guardar",
        onClick = onClick,
        enabled = habilitado,
        cargando = guardando,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

/**
 * Esqueleto de los formularios del perfil, igual que en NuevaInmersion:
 *
 *  ┌──────────────────────────┐
 *  │ ← Título (fijo)          │
 *  │ mensaje de error         │
 *  │ ┌──────────────────────┐ │
 *  │ │ campos (scroll)      │ │  ← weight(1f): se achica cuando aparece el teclado
 *  │ └──────────────────────┘ │
 *  │ [ Guardar ] (fijo)       │  ← siempre visible, justo arriba del teclado
 *  └──────────────────────────┘
 *
 * La clave es imePadding(): reserva el alto del teclado, así el área de scroll se achica y
 * Compose puede llevar el campo con foco a la parte visible (lo hace solo al enfocarlo).
 * Requiere android:windowSoftInputMode="adjustResize" en el Manifest (ya lo tienen).
 */
@Composable
fun FormularioPerfil(
    titulo: String,
    onVolver: () -> Unit,
    guardando: Boolean,
    habilitado: Boolean,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier,
    errorGeneral: String? = null,
    campos: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        EncabezadoPerfil(titulo = titulo, onVolver = onVolver)
        MensajeError(errorGeneral)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            content = campos
        )

        BotonGuardar(guardando = guardando, habilitado = habilitado, onClick = onGuardar)
    }
}

/** Botón flotante "+" para las listas. */
@Composable
fun BotonAgregar(descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = FormaBoton,
        containerColor = FondoTarjetaResaltada,
        contentColor = Color.White,
        modifier = modifier.border(1.dp, Acento.copy(alpha = 0.45f), FormaBoton)
    ) {
        Icon(
            painterResource(R.drawable.ic_agregar),
            contentDescription = descripcion,
            modifier = Modifier.size(28.dp)
        )
    }
}

/** Mensaje cuando una lista está vacía, con botón para cargar el primero. */
@Composable
fun EstadoVacio(texto: String, textoBoton: String, onAccion: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(texto, color = TextoSecundario, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        BotonPrincipal(texto = textoBoton, onClick = onAccion)
    }
}

/** Píldora de color (Vencido, Principal, Service pronto...). */
@Composable
fun InsigniaChica(insignia: Insignia, modifier: Modifier = Modifier) {
    Text(
        text = insignia.texto,
        color = insignia.color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(insignia.color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

/**
 * Tarjeta de un ítem de lista (equipo, certificado, contacto).
 * @param lineas detalle debajo del título; las vacías se omiten.
 * @param acciones botones extra (ej: "Llamar") a la izquierda de editar/borrar.
 */
@Composable
fun TarjetaItemPerfil(
    titulo: String,
    subtitulo: String,
    lineas: List<String>,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    insignias: List<Insignia> = emptyList(),
    atenuada: Boolean = false,
    acciones: @Composable () -> Unit = {}
) {
    val alfa = if (atenuada) 0.55f else 1f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (subtitulo.isNotBlank()) {
                        Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = Acento.copy(alpha = alfa))
                    }
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = alfa)
                    )
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                }
                IconButton(onClick = onEliminar) {
                    Icon(painterResource(R.drawable.ic_borrar), contentDescription = "Eliminar", tint = RojoError)
                }
            }
            lineas.filter { it.isNotBlank() }.forEach { linea ->
                Text(linea, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            }
            if (insignias.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    insignias.forEach { InsigniaChica(it) }
                }
            }
            acciones()
        }
    }
}

/** Confirmación antes de borrar (no hay "deshacer"). */
@Composable
fun DialogoConfirmarEliminacion(
    queSeElimina: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("¿Eliminar?") },
        text = { Text("Se va a eliminar \"$queSeElimina\". Esta acción no se puede deshacer.") },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = RojoError)
            ) { Text("Eliminar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
