package com.berterogarcia.appnea.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.nombreCompleto
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.DatoChico
import com.berterogarcia.appnea.ui.components.formatoLargo
import com.berterogarcia.appnea.ui.condiciones.Acento
import com.berterogarcia.appnea.ui.condiciones.FondoTarjeta
import com.berterogarcia.appnea.ui.condiciones.TextoSecundario
import com.berterogarcia.appnea.ui.perfil.comun.Insignia
import com.berterogarcia.appnea.ui.perfil.comun.InsigniaChica
import com.berterogarcia.appnea.ui.perfil.comun.insigniaMantenimiento
import com.berterogarcia.appnea.ui.perfil.comun.insigniaVencimiento

// Estilo tomado de Clima / Condiciones: tarjetas FondoTarjeta y datos "etiqueta arriba, valor
// abajo" (DatoChico), así la etiqueta ya no le roba ancho al valor. Los renglones se separan
// solo con espacio vertical (sin fondos ni degradés).

private const val MAX_ITEMS_RESUMEN = 3

/** Separación entre renglones (vertical) y entre las dos columnas (horizontal). */
private val EspacioRenglones = 14.dp
private val EspacioColumnas = 12.dp

/** Un dato de la grilla. [anchoCompleto] = ocupa toda la fila (textos largos como el email). */
private data class Dato(val etiqueta: String, val valor: String, val anchoCompleto: Boolean = false)

@Composable
fun CuentaScreen(
    viewModel: CuentaViewModel,
    onCerrarSesion: () -> Unit,
    onEditarDatos: () -> Unit,
    onVerEquipamiento: () -> Unit,
    onVerCertificados: () -> Unit,
    onVerContactos: () -> Unit
) {
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val stats by viewModel.estadisticas.collectAsStateWithLifecycle()
    val usuario = viewModel.usuario.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // [CAMBIO] Saludo con el NOMBRE DE USUARIO usando un string con formato ("Hola, %1$s").
        Text(
            text = stringResource(R.string.cuenta_greeting, usuario),
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        if (resumen.error) {
            Text(
                text = "No se pudieron cargar los datos del perfil.",
                color = Color(0xFFFF6B6B),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // ------------------------------ Datos personales ------------------------------
        val perfil = resumen.perfil
        TarjetaSeccion(
            titulo = "👤 Datos personales",
            textoAccion = if (perfil == null) "Completar" else "Editar",
            onAccion = onEditarDatos
        ) {
            if (perfil == null) {
                TextoVacio(if (resumen.cargando) "Cargando..." else "Todavía no cargaste tus datos personales.")
            } else {
                GrillaDatos(
                    listOf(
                        Dato("Nombre", perfil.nombreCompleto.ifBlank { "-" }, anchoCompleto = true),
                        Dato("Usuario", usuario.ifBlank { "-" }),
                        Dato("Nacimiento", perfil.fechaNacimiento.formatoLargo()),
                        Dato("Email", perfil.email.ifBlank { "-" }, anchoCompleto = true),
                        Dato("Teléfono", perfil.telefono.ifBlank { "-" }),
                        Dato("Ciudad", perfil.ciudad.ifBlank { "-" }),
                        Dato("Compañero/a habitual", perfil.companeroHabitual.ifBlank { "-" }, anchoCompleto = true)
                    )
                )
            }
        }

        // ------------------------------- Certificaciones -------------------------------
        TarjetaSeccion(
            titulo = "🎓 Certificaciones (${resumen.certificados.size})",
            textoAccion = if (resumen.certificados.isEmpty()) "Agregar" else "Ver todas",
            onAccion = onVerCertificados
        ) {
            if (resumen.certificados.isEmpty()) {
                TextoVacio(if (resumen.cargando) "Cargando..." else "Sin certificaciones cargadas.")
            } else {
                ListaPaneles {
                    resumen.certificados.take(MAX_ITEMS_RESUMEN).forEach { c ->
                        PanelItem(
                            titulo = c.titulo,
                            subtitulo = listOf(c.tipo.etiqueta, c.entidad).filter { it.isNotBlank() }.joinToString(" · "),
                            insignia = insigniaVencimiento(c.fechaVencimiento)
                        )
                    }
                }
                Restantes(resumen.certificados.size)
            }
        }

        // -------------------------------- Equipamiento --------------------------------
        TarjetaSeccion(
            titulo = "🤿 Equipamiento (${resumen.equiposEnUso.size} en uso)",
            textoAccion = if (resumen.equipos.isEmpty()) "Agregar" else "Ver todo",
            onAccion = onVerEquipamiento
        ) {
            if (resumen.equiposEnUso.isEmpty()) {
                TextoVacio(if (resumen.cargando) "Cargando..." else "Sin equipamiento en uso.")
            } else {
                ListaPaneles {
                    resumen.equiposEnUso.take(MAX_ITEMS_RESUMEN).forEach { e ->
                        val marcaModelo = listOf(e.marca, e.modelo).filter { it.isNotBlank() }.joinToString(" ")
                        PanelItem(
                            titulo = marcaModelo.ifBlank { e.categoria.etiqueta },
                            subtitulo = listOf(e.categoria.etiqueta, e.talle).filter { it.isNotBlank() }.joinToString(" · "),
                            insignia = insigniaMantenimiento(e.proximoMantenimiento)
                        )
                    }
                }
                Restantes(resumen.equiposEnUso.size)
                if (resumen.equiposConServicePendiente > 0) {
                    Text(
                        text = "⚠ ${resumen.equiposConServicePendiente} equipo(s) con service pendiente",
                        color = Color(0xFFFFB74D),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // --------------------------------- Estadísticas ---------------------------------
        TarjetaSeccion(titulo = "📊 Estadísticas") {
            GrillaDatos(
                listOf(
                    Dato("🤿 Inmersiones", stats.inmersiones.toString()),
                    Dato("📏 Prof. máxima", stats.profundidadMaxima),
                    Dato("⏱️ Fondo total", stats.tiempoTotalFondo),
                    Dato("🫁 Mejor apnea", stats.mejorApnea),
                    Dato(
                        "🏋️ Entrenamientos",
                        "${stats.sesionesEntrenamiento} (${stats.sesionesCompletas} completos)",
                        anchoCompleto = true
                    ),
                    Dato("🎣 Salidas de pesca", stats.salidasPesca.toString()),
                    Dato("🐟 Capturas", stats.capturasTotales.toString()),
                    Dato("🏆 Mayor pieza", stats.mayorPieza, anchoCompleto = true)
                )
            )
        }

        // ---------------------------- Contacto de emergencia ----------------------------
        val principal = resumen.contactoPrincipal
        TarjetaSeccion(
            titulo = "🆘 Contacto de emergencia",
            textoAccion = if (resumen.contactos.isEmpty()) "Agregar" else "Gestionar",
            onAccion = onVerContactos
        ) {
            if (principal == null) {
                TextoVacio(
                    if (resumen.cargando) "Cargando..."
                    else "No tenés contacto de emergencia. Te recomendamos cargar uno."
                )
            } else {
                GrillaDatos(
                    listOf(
                        Dato("Nombre", principal.nombre, anchoCompleto = true),
                        Dato("Relación", principal.relacion.ifBlank { "-" }),
                        Dato("Teléfono", principal.telefono)
                    )
                )
                if (resumen.contactos.size > 1) {
                    Text(
                        "+${resumen.contactos.size - 1} contacto(s) más",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSecundario,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        BotonSecundario(
            texto = stringResource(R.string.cuenta_cerrar_session),
            onClick = {
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            icono = painterResource(R.drawable.ic_cerrar_sesion),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ====================================== Componentes ======================================

/** Tarjeta de sección, mismo estilo que TarjetaCondicion, con botón de acción opcional. */
@Composable
private fun TarjetaSeccion(
    titulo: String,
    textoAccion: String? = null,
    onAccion: () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (textoAccion != null) {
                    TextButton(
                        onClick = onAccion,
                        colors = ButtonDefaults.textButtonColors(contentColor = Acento)
                    ) { Text(textoAccion) }
                }
            }
            Spacer(Modifier.height(if (textoAccion != null) 4.dp else 10.dp))
            contenido()
        }
    }
}

/**
 * Grilla de 2 columnas: cada dato es "etiqueta arriba / valor abajo".
 * Los datos con anchoCompleto ocupan la fila entera.
 */
@Composable
private fun GrillaDatos(datos: List<Dato>) {
    val filas = remember(datos) { armarFilas(datos) }
    Column(verticalArrangement = Arrangement.spacedBy(EspacioRenglones)) {
        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EspacioColumnas)
            ) {
                fila.forEach { dato ->
                    Column(modifier = Modifier.weight(1f)) {
                        DatoChico(etiqueta = dato.etiqueta, valor = dato.valor)
                    }
                }
            }
        }
    }
}

/** Agrupa los datos de a 2 por fila; uno de ancho completo cierra la fila anterior. */
private fun armarFilas(datos: List<Dato>): List<List<Dato>> {
    val filas = mutableListOf<List<Dato>>()
    var pendiente: Dato? = null
    for (dato in datos) {
        val anterior = pendiente
        when {
            dato.anchoCompleto -> {
                if (anterior != null) filas.add(listOf(anterior))
                filas.add(listOf(dato))
                pendiente = null
            }
            anterior == null -> pendiente = dato
            else -> {
                filas.add(listOf(anterior, dato))
                pendiente = null
            }
        }
    }
    pendiente?.let { filas.add(listOf(it)) }
    return filas
}

/** Contenedor de ítems apilados con el mismo espacio que la grilla. */
@Composable
private fun ListaPaneles(contenido: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(EspacioRenglones), content = contenido)
}

/** Ítem de lista (certificado / equipo): título, subtítulo y una insignia a la derecha. */
@Composable
private fun PanelItem(titulo: String, subtitulo: String, insignia: Insignia?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            if (subtitulo.isNotBlank()) {
                Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
            }
        }
        if (insignia != null) {
            Spacer(Modifier.width(8.dp))
            InsigniaChica(insignia)
        }
    }
}

@Composable
private fun TextoVacio(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSecundario,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}

@Composable
private fun Restantes(total: Int) {
    if (total > MAX_ITEMS_RESUMEN) {
        Text(
            "+${total - MAX_ITEMS_RESUMEN} más",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
