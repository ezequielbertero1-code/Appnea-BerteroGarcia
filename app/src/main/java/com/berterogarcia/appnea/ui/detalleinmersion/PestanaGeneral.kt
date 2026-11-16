package com.berterogarcia.appnea.ui.detalleinmersion

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.RegistroApnea
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.FilaLista
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.Insignia
import com.berterogarcia.appnea.ui.components.MapaLibre
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.PinMapa
import com.berterogarcia.appnea.ui.components.TarjetaTonal
import com.berterogarcia.appnea.ui.components.TipoInsignia
import com.berterogarcia.appnea.ui.components.formatoCoordenadas
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.condiciones.duracion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.util.Locale

// Pestaña General del detalle de una inmersión.

@Composable
internal fun PestanaGeneral(i: InmersionEntity) {
    val apnea = PerfilSimulado.esApnea(i)
    val fondo = formatearDuracion(i.tiempoFondo.inWholeSeconds.toInt())

    // ---- Profundidad máxima + tipo y fecha ----
    TarjetaTonal {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(i.profundidadMetros.limpio(), fontSize = 44.sp, lineHeight = 46.sp, color = TextoSuperficie)
                    Text(" m", fontSize = 20.sp, color = TextoSuperficieVariante, modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    stringResource(R.string.detalle_profundidad_maxima),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Insignia(
                    texto = i.tipo.nombre(),
                    tipo = TipoInsignia.INFO,
                    icono = painterResource(if (apnea) R.drawable.ic_ms_air else R.drawable.ic_ms_scuba_diving)
                )
                Text(
                    i.fecha.format(FORMATO_FECHA),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // ---- Tiempos ----
    val filas = buildList {
        add(Triple(R.drawable.ic_ms_login, stringResource(R.string.detalle_entrada), i.horaEntrada.format(HH_MM)))
        add(Triple(R.drawable.ic_ms_logout, stringResource(R.string.detalle_salida), i.horaSalida.format(HH_MM)))
        add(Triple(R.drawable.ic_ms_schedule, stringResource(R.string.detalle_en_el_agua), duracion(minutosEnAgua(i))))
        // Apnea sin detalle de bajadas: no hay tiempo total (se guarda en 0), no se muestra "00:00"
        if (!apnea || i.tiempoFondo.inWholeSeconds > 0) {
            add(
                Triple(
                    R.drawable.ic_ms_timer,
                    stringResource(if (apnea) R.string.detalle_tiempo_apnea else R.string.detalle_tiempo_fondo),
                    fondo
                )
            )
        }
    }
    val conParada = !apnea
    val total = filas.size + if (conParada) 1 else 0
    GrupoLista(modifier = Modifier.padding(top = 12.dp)) {
        filas.forEachIndexed { indice, (icono, titulo, valor) ->
            FilaValor(icono = icono, titulo = titulo, valor = valor, indice = indice, total = total)
        }
        if (conParada) {
            val hayParada = i.paradaSeguridadMin > 0f
            FilaValor(
                icono = R.drawable.ic_ms_pause_circle,
                titulo = stringResource(R.string.detalle_parada),
                subtitulo = if (hayParada) stringResource(R.string.detalle_parada_profundidad) else null,
                valor = if (hayParada) stringResource(R.string.detalle_minutos, i.paradaSeguridadMin.limpio())
                else stringResource(R.string.detalle_sin_parada),
                indice = filas.size,
                total = total
            )
        }
    }

    i.apnea?.let { TarjetaApnea(it) }

    TarjetaUbicacion(i)   // no muestra nada si la inmersión no tiene coordenadas

    // ---- Notas ----
    EncabezadoGrupo(titulo = stringResource(R.string.detalle_notas))
    TarjetaTonal {
        Text(stringResource(R.string.detalle_actividad), style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
        Text(
            i.actividad.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuperficie,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        if (i.comentarios.isNotBlank()) {
            // Comentario como "cita": barrita cian a la izquierda
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(Acento))
                Spacer(Modifier.width(10.dp))
                Text(
                    "“${i.comentarios}”",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSuperficieVariante
                )
            }
        }
    }
}

/** Resumen de apnea (bajadas, máxima, tiempos) + síncope y, si se cargó, el detalle de cada bajada. */
@Composable
internal fun TarjetaApnea(a: RegistroApnea) {
    val r = a.resumen()
    EncabezadoGrupo(titulo = stringResource(R.string.detalle_apnea_titulo))
    FilaMosaicos {
        Mosaico(stringResource(R.string.nueva_resumen_bajadas), r.cantidad.toString())
        Mosaico(stringResource(R.string.nueva_resumen_prof_max), "${r.profundidadMaximaMetros.limpio()} m")
    }
    if (r.tiempoTotalSegundos != null) {
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.nueva_resumen_mejor),
                stringResource(R.string.nueva_resumen_mejor_numero, (r.indiceMejorBajada ?: 0) + 1),
                stringResource(
                    R.string.nueva_resumen_mejor_detalle,
                    r.profundidadMaximaMetros.limpio(),
                    formatearDuracion(r.tiempoMejorBajadaSegundos ?: 0)
                )
            )
            Mosaico(stringResource(R.string.nueva_resumen_tiempo_total), formatearDuracion(r.tiempoTotalSegundos))
        }
        FilaMosaicos {
            Mosaico(stringResource(R.string.nueva_resumen_promedio), formatearDuracion(r.tiempoPromedioSegundos ?: 0))
            Mosaico(
                stringResource(R.string.detalle_apnea_sincope),
                stringResource(if (a.sincope) R.string.detalle_apnea_sincope_si else R.string.detalle_apnea_sincope_no),
                colorValor = if (a.sincope) ColorError else TextoSuperficie
            )
        }
    } else {
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.detalle_apnea_sincope),
                stringResource(if (a.sincope) R.string.detalle_apnea_sincope_si else R.string.detalle_apnea_sincope_no),
                colorValor = if (a.sincope) ColorError else TextoSuperficie
            )
        }
    }
    if (a.bajadas.isNotEmpty()) {
        GrupoLista(modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
            a.bajadas.forEachIndexed { n, b ->
                FilaLista(
                    titulo = stringResource(R.string.detalle_apnea_bajada, n + 1),
                    subtitulo = if (n == r.indiceMejorBajada) stringResource(R.string.nueva_bajada_mejor) else null,
                    indice = n,
                    total = a.bajadas.size,
                    fin = {
                        Text(
                            "${b.profundidadMetros.limpio()} m · ${formatearDuracion(b.tiempoSegundos)}",
                            style = MaterialTheme.typography.titleMedium, color = TextoSuperficie
                        )
                    }
                )
            }
        }
    }
}

/** Fila con ícono a la izquierda y el valor destacado a la derecha ("Entrada ····· 10:15"). */
@Composable
internal fun FilaValor(icono: Int, titulo: String, valor: String, indice: Int, total: Int, subtitulo: String? = null) {
    FilaLista(
        titulo = titulo,
        subtitulo = subtitulo,
        indice = indice,
        total = total,
        inicio = { Icon(painterResource(icono), contentDescription = null) },
        fin = { Text(valor, style = MaterialTheme.typography.titleMedium, color = TextoSuperficie) }
    )
}

/** Mini mapa del lugar marcado. Las inmersiones sin coordenadas no muestran la sección. */
@Composable
internal fun TarjetaUbicacion(i: InmersionEntity) {
    val lat = i.latitud ?: return
    val lon = i.longitud ?: return
    val context = LocalContext.current
    val etiquetaPorDefecto = stringResource(R.string.detalle_etiqueta_mapa)
    val sinAppMapas = stringResource(R.string.detalle_sin_app_mapas)

    // Abre la app de mapas del teléfono con un pin con el nombre del lugar.
    // Coordenadas con Locale.US: el URI necesita punto decimal, no coma.
    val abrirEnMapas: () -> Unit = {
        val etiqueta = Uri.encode(i.lugar.ifBlank { etiquetaPorDefecto })
        val coords = "%.6f,%.6f".format(Locale.US, lat, lon)
        val intent = Intent(Intent.ACTION_VIEW, "geo:0,0?q=$coords($etiqueta)".toUri())
        // Si no hay app de mapas (o falla por cualquier motivo) se avisa en vez de crashear
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, sinAppMapas, Toast.LENGTH_SHORT).show() }
    }

    EncabezadoGrupo(
        titulo = stringResource(R.string.detalle_lugar),
        accion = stringResource(R.string.detalle_abrir_mapas),
        onAccion = abrirEnMapas
    )
    TarjetaTonal(relleno = PaddingValues(0.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            MapaLibre(
                centro = Ubicacion(lat, lon),
                zoom = 14.0,
                interactivo = false,
                modifier = Modifier.matchParentSize()
            )
            PinMapa(Modifier.align(Alignment.Center))
            // Capa transparente: un toque abre Mapas y el deslizar lateral sigue cambiando de pestaña
            Box(Modifier.matchParentSize().clickable(onClick = abrirEnMapas))
        }
        Text(
            formatoCoordenadas(lat, lon),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
