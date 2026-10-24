package com.berterogarcia.appnea.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.nombreCompleto
import com.berterogarcia.appnea.ui.components.Avatar
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.FilaDeTarjetas
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.TarjetaKpi
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.perfil.comun.EstadoVencimiento
import com.berterogarcia.appnea.ui.perfil.comun.estadoVencimiento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.util.Locale

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")

/** Estados que piden atención (aviso naranja en la fila en vez del chevron). */
private val ESTADOS_AVISO = setOf(EstadoVencimiento.POR_VENCER, EstadoVencimiento.VENCIDO)

/**
 * Cuenta (estilo "Appnea circular", como Ajustes de Android): avatar y nombre, dos números
 * clave y una lista agrupada con cada sección del perfil. Cada fila resume lo cargado y abre
 * su pantalla; si algo vence pronto, la fila muestra un aviso en vez del chevron.
 */
@Composable
fun CuentaScreen(
    viewModel: CuentaViewModel,
    onCerrarSesion: () -> Unit,
    onEditarDatos: () -> Unit,
    onVerEquipamiento: () -> Unit,
    onVerCertificados: () -> Unit,
    onVerContactos: () -> Unit,
    onVerEstadisticas: () -> Unit = {}
) {
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    val stats by viewModel.estadisticas.collectAsStateWithLifecycle()
    val usuario = viewModel.usuario.orEmpty()
    val perfil = resumen.perfil
    val cargando = stringResource(R.string.cuenta_cargando)

    Column(modifier = Modifier.fillMaxSize()) {
        BarraSuperior(titulo = stringResource(R.string.cuenta_titulo))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // ------------------------------ Encabezado ------------------------------
            val nombre = perfil?.nombreCompleto?.takeIf { it.isNotBlank() }
                ?: usuario.replaceFirstChar { it.titlecase(LOCALE_ES) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Avatar(inicial = nombre.take(1).takeIf { it.isNotBlank() }?.uppercase(LOCALE_ES), tamanio = 72.dp)
                Text(nombre, style = MaterialTheme.typography.titleLarge, color = TextoSuperficie, textAlign = TextAlign.Center)
                val detalle = listOf("@$usuario".takeIf { usuario.isNotBlank() }, perfil?.ciudad?.takeIf { it.isNotBlank() })
                    .filterNotNull()
                    .joinToString(" · ")
                if (detalle.isNotEmpty()) {
                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
                }
            }

            if (resumen.error) {
                Text(
                    text = stringResource(R.string.cuenta_error),
                    color = ColorError,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // ------------------------------ Números clave ------------------------------
            FilaDeTarjetas(modifier = Modifier.padding(top = 14.dp)) {
                TarjetaKpi(
                    etiqueta = stringResource(R.string.cuenta_kpi_inmersiones),
                    valor = stats.inmersiones.toString(),
                    modifier = Modifier.weight(1f)
                )
                TarjetaKpi(
                    etiqueta = stringResource(R.string.cuenta_kpi_profundidad),
                    valor = stats.profundidadMaxima,
                    unidad = if (stats.profundidadMaxima != "-") "m" else null,
                    modifier = Modifier.weight(1f)
                )
            }

            // ------------------------------ Secciones ------------------------------
            val certificadosConAviso = resumen.certificados.count { estadoVencimiento(it.fechaVencimiento) in ESTADOS_AVISO }
            val principal = resumen.contactoPrincipal
            val filas = 5
            GrupoLista(modifier = Modifier.padding(top = 16.dp)) {
                FilaLista(
                    titulo = stringResource(R.string.cuenta_datos_personales),
                    subtitulo = when {
                        resumen.cargando -> cargando
                        perfil == null -> stringResource(R.string.cuenta_datos_vacio)
                        else -> listOf(perfil.nombreCompleto, perfil.email).filter { it.isNotBlank() }.joinToString(" · ")
                    },
                    indice = 0, total = filas,
                    onClick = onEditarDatos,
                    inicio = { IconoCirculo(painterResource(R.drawable.ic_ms_badge)) },
                    fin = { if (perfil == null && !resumen.cargando) IconoAviso() else Chevron() }
                )
                FilaLista(
                    titulo = stringResource(R.string.cuenta_certificaciones),
                    subtitulo = when {
                        resumen.cargando -> cargando
                        resumen.certificados.isEmpty() -> stringResource(R.string.cuenta_certificaciones_vacio)
                        certificadosConAviso > 0 -> pluralStringResource(
                            R.plurals.cuenta_certificaciones_aviso, certificadosConAviso,
                            resumen.certificados.size, certificadosConAviso
                        )
                        else -> "${resumen.certificados.size} · ${resumen.certificados.first().titulo}"
                    },
                    indice = 1, total = filas,
                    onClick = onVerCertificados,
                    inicio = { IconoCirculo(painterResource(R.drawable.ic_ms_workspace_premium)) },
                    fin = { if (certificadosConAviso > 0) IconoAviso() else Chevron() }
                )
                FilaLista(
                    titulo = stringResource(R.string.cuenta_equipamiento),
                    subtitulo = when {
                        resumen.cargando -> cargando
                        resumen.equiposEnUso.isEmpty() -> stringResource(R.string.cuenta_equipamiento_vacio)
                        resumen.equiposConServicePendiente > 0 -> pluralStringResource(
                            R.plurals.cuenta_equipamiento_service, resumen.equiposConServicePendiente,
                            resumen.equiposEnUso.size, resumen.equiposConServicePendiente
                        )
                        else -> stringResource(R.string.cuenta_equipamiento_en_uso, resumen.equiposEnUso.size)
                    },
                    indice = 2, total = filas,
                    onClick = onVerEquipamiento,
                    inicio = { IconoCirculo(painterResource(R.drawable.ic_ms_scuba_diving)) },
                    fin = { if (resumen.equiposConServicePendiente > 0) IconoAviso() else Chevron() }
                )
                FilaLista(
                    titulo = stringResource(R.string.accion_estadisticas),
                    subtitulo = stringResource(
                        R.string.cuenta_estadisticas_resumen,
                        stats.tiempoTotalFondo, stats.sesionesEntrenamiento, stats.mejorApnea
                    ),
                    indice = 3, total = filas,
                    onClick = onVerEstadisticas,
                    inicio = { IconoCirculo(painterResource(R.drawable.ic_ms_bar_chart), tono = TonoIcono.APNEA) },
                    fin = { Chevron() }
                )
                FilaLista(
                    titulo = stringResource(R.string.cuenta_contacto_emergencia),
                    subtitulo = when {
                        resumen.cargando -> cargando
                        principal == null -> stringResource(R.string.cuenta_contacto_vacio)
                        else -> listOf(principal.nombre, principal.relacion).filter { it.isNotBlank() }.joinToString(" · ")
                    },
                    indice = 4, total = filas,
                    onClick = onVerContactos,
                    inicio = { IconoCirculo(painterResource(R.drawable.ic_ms_emergency), tono = TonoIcono.ERROR) },
                    // Sin contacto de emergencia es lo único que conviene resolver ya: aviso
                    fin = { if (principal == null && !resumen.cargando) IconoAviso() else Chevron() }
                )
            }

            GrupoLista(modifier = Modifier.padding(top = 12.dp)) {
                FilaLista(
                    titulo = stringResource(R.string.cuenta_cerrar_session),
                    indice = 0, total = 1,
                    onClick = {
                        viewModel.cerrarSesion()
                        onCerrarSesion()
                    },
                    inicio = {
                        Icon(painterResource(R.drawable.ic_ms_logout), contentDescription = null, tint = ColorError)
                    },
                    colorTitulo = ColorError
                )
            }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
}

/** Reemplaza al chevron cuando la sección necesita atención (vencido, service, sin contacto). */
@Composable
private fun IconoAviso() {
    Icon(
        painterResource(R.drawable.ic_ms_error),
        contentDescription = stringResource(R.string.cuenta_requiere_atencion),
        tint = ColorAdvertencia
    )
}
