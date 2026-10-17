package com.berterogarcia.appnea.ui.perfil.comun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.BotonFlotante
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.NavegacionBarra
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.components.formaEnGrupo
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Componentes compartidos por todas las pantallas del perfil (estilo "Appnea circular"):
// barra superior de Material 3, filas agrupadas sobre superficie tonal y FAB extendido.


/** Barra superior con flecha de volver (listas del perfil). */
@Composable
fun EncabezadoPerfil(titulo: String, onVolver: () -> Unit, modifier: Modifier = Modifier) {
    BarraSuperior(
        titulo = titulo,
        navegacion = NavegacionBarra.VOLVER,
        onNavegacion = onVolver,
        modifier = modifier
    )
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
        color = ColorError,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/**
 * "Guardar" de la barra superior de los formularios: botón de texto cian, que se deshabilita
 * y muestra una ruedita mientras guarda.
 */
@Composable
fun BotonGuardar(guardando: Boolean, habilitado: Boolean = true, onClick: () -> Unit) {
    if (guardando) {
        CircularProgressIndicator(
            color = Acento,
            strokeWidth = 2.dp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .size(20.dp)
        )
    } else {
        TextButton(onClick = onClick, enabled = habilitado) {
            Text(stringResource(R.string.accion_guardar), color = if (habilitado) Acento else TextoSuperficieVariante)
        }
    }
}

/**
 * Esqueleto de los formularios del perfil (estilo "Appnea circular"): pantalla completa con "X" y
 * "Guardar" en la barra superior, error general y campos con scroll.
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
    ) {
        BarraSuperior(
            titulo = titulo,
            navegacion = NavegacionBarra.CERRAR,
            onNavegacion = onVolver,
            acciones = { BotonGuardar(guardando = guardando, habilitado = habilitado, onClick = onGuardar) }
        )
        MensajeError(errorGeneral, Modifier.padding(horizontal = 16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            content = campos
        )
    }
}

/** Botón flotante extendido para agregar en las listas ("Agregar equipo", "Agregar contacto"...). */
@Composable
fun BotonAgregar(descripcion: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BotonFlotante(
        texto = descripcion,
        icono = rememberVectorPainter(Icons.Rounded.Add),
        onClick = onClick,
        modifier = modifier
    )
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
        Text(texto, color = TextoSuperficieVariante, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        BotonPrincipal(texto = textoBoton, onClick = onAccion, icono = rememberVectorPainter(Icons.Rounded.Add))
    }
}

/** Insignia de color (Vencido, Principal, Service pronto...), mismo estilo que components.Insignia. */
@Composable
fun InsigniaChica(insignia: Insignia, modifier: Modifier = Modifier) {
    Text(
        text = insignia.texto,
        color = insignia.color,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .background(insignia.color.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * Fila de un ítem de lista del perfil (equipo, certificado, contacto) dentro de un grupo:
 * ícono en círculo, título, subtítulo, detalle e insignias. Toca la fila para editar; el menú ⋮
 * tiene Editar y Eliminar (en rojo).
 * @param lineas detalle debajo del título; las vacías se omiten.
 * @param acciones botones extra (ej: "Llamar") debajo del detalle.
 * @param indice / [total]: posición en el grupo (esquinas grandes solo en los extremos).
 */
@OptIn(ExperimentalLayoutApi::class)
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
    icono: Painter? = null,
    tonoIcono: TonoIcono = TonoIcono.NORMAL,
    indice: Int = 0,
    total: Int = 1,
    acciones: @Composable () -> Unit = {}
) {
    val detalle = lineas.filter { it.isNotBlank() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(formaEnGrupo(indice, total))
            .background(SuperficieTonal)
            .clickable(onClickLabel = stringResource(R.string.perfil_editar), onClick = onEditar)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                IconoCirculo(icono, tono = tonoIcono, modifier = Modifier.alpha(if (atenuada) 0.5f else 1f))
                Spacer(Modifier.width(16.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (atenuada) 0.6f else 1f)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextoSuperficie,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitulo.isNotBlank()) {
                    Text(
                        subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSuperficieVariante,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                detalle.forEach { linea ->
                    Text(linea, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
                }
                if (insignias.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        insignias.forEach { InsigniaChica(it) }
                    }
                }
            }
            MenuItemPerfil(onEditar = onEditar, onEliminar = onEliminar, nombre = titulo)
        }
        Box(Modifier.padding(end = 12.dp)) { acciones() }
    }
}

@Composable
private fun MenuItemPerfil(onEditar: () -> Unit, onEliminar: () -> Unit, nombre: String) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.tabla_opciones, nombre),
                tint = TextoSuperficieVariante
            )
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.perfil_editar)) },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = {
                    abierto = false
                    onEditar()
                },
                colors = MenuDefaults.itemColors(textColor = TextoSuperficie, leadingIconColor = TextoSuperficieVariante)
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.accion_eliminar)) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                onClick = {
                    abierto = false
                    onEliminar()
                },
                colors = MenuDefaults.itemColors(textColor = ColorError, leadingIconColor = ColorError)
            )
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
        icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = ColorError) },
        title = { Text(stringResource(R.string.perfil_eliminar_titulo)) },
        text = { Text(stringResource(R.string.perfil_eliminar_texto, queSeElimina)) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = ColorError)
            ) { Text(stringResource(R.string.accion_eliminar)) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) } }
    )
}
