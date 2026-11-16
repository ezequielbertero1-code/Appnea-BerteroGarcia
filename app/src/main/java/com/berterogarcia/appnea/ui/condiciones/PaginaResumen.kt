package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.texto
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// Página Resumen de Condiciones (clima actual y aptitud del agua).

@Composable
internal fun PaginaResumen(c: Condiciones) {
    val clima = c.climaActual
    val mar = c.marActual
    val esDeDia = clima?.esDeDia ?: true

    TarjetaCondicion {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(iconoClima(clima?.codigoClima, esDeDia)),
                contentDescription = null,
                tint = colorIconoClima(clima?.codigoClima, esDeDia),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(num(clima?.temperaturaC, unidad = "°"), fontSize = 40.sp, lineHeight = 44.sp, color = TextoSuperficie)
                Text(
                    listOfNotNull(
                        clima?.codigoClima?.let { descripcionClima(it) },
                        clima?.sensacionTermicaC?.let { stringResource(R.string.detalle_sensacion, num(it, unidad = "°")) }
                    ).joinToString(" · ").ifBlank { stringResource(R.string.detalle_sin_datos) },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante
                )
            }
        }
        // Semáforo como contenedor tonal del color de la aptitud
        AvisoAptitud(c.aptitud.nivel, c.aptitud.motivos.map { it.texto() }.joinToString(" · "), Modifier.padding(top = 14.dp))
    }

    TarjetaCondicion(stringResource(R.string.condiciones_atmosfera)) {
        val tendencia = c.tendenciaPresionHpa3h
        FilaDato(stringResource(R.string.condiciones_sensacion), num(clima?.sensacionTermicaC, unidad = "°C"))
        FilaDato(stringResource(R.string.condiciones_rocio), num(clima?.puntoRocioC, unidad = "°C"))
        FilaDato(stringResource(R.string.detalle_humedad), num(clima?.humedadPorc?.toDouble(), unidad = "%"))
        FilaDato(
            stringResource(R.string.detalle_presion),
            num(clima?.presionHpa, unidad = " hPa") +
                (tendencia?.let { " " + stringResource(R.string.condiciones_tendencia, "${if (it >= 0) "+" else ""}${"%.1f".format(it)}") } ?: "")
        )
        FilaDato(stringResource(R.string.condiciones_nubosidad), num(clima?.nubosidadPorc?.toDouble(), unidad = "%"))
        FilaDato(stringResource(R.string.detalle_visibilidad), clima?.visibilidadM?.let { if (it >= 1000) "${(it / 1000).roundToInt()} km" else "${it.roundToInt()} m" } ?: "-")
        FilaDato(stringResource(R.string.condiciones_precipitacion), num(clima?.precipitacionMm, 1, " mm"))
        FilaDato(stringResource(R.string.condiciones_prob_lluvia), num(clima?.probabilidadLluviaPorc?.toDouble(), unidad = "%"))
        FilaDato(stringResource(R.string.condiciones_uv), num(clima?.indiceUv, 1))
    }

    TarjetaCondicion(stringResource(R.string.detalle_viento)) {
        FilaDato(stringResource(R.string.condiciones_velocidad), num(clima?.vientoKmh, unidad = " km/h"))
        FilaDato(stringResource(R.string.condiciones_direccion), "${direccion(clima?.direccionVientoGrados)} (${num(clima?.direccionVientoGrados, unidad = "°")})")
        FilaDato(stringResource(R.string.detalle_rafagas), num(clima?.rafagasKmh, unidad = " km/h"))
        FilaDato(stringResource(R.string.condiciones_nudos), clima?.vientoKmh?.let { "${(it / 1.852).roundToInt()} kn" } ?: "-")
    }

    TarjetaCondicion(stringResource(R.string.condiciones_mar)) {
        if (!c.marDisponible || mar == null) {
            Text(stringResource(R.string.condiciones_sin_mar), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
        } else {
            FilaDato(stringResource(R.string.condiciones_altura_ola), num(mar.olaM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_direccion_ola), direccion(mar.direccionOlaGrados))
            FilaDato(stringResource(R.string.condiciones_periodo), num(mar.periodoOlaS, unidad = " s"))
            FilaDato(stringResource(R.string.condiciones_ola_viento), num(mar.olaVientoM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_mar_fondo), "${num(mar.marDeFondoM, 1, " m")} ${direccion(mar.direccionMarDeFondoGrados)} · ${num(mar.periodoMarDeFondoS, unidad = " s")}")
            FilaDato(stringResource(R.string.nueva_temp_agua), num(mar.temperaturaAguaC, 1, "°C"))
            FilaDato(stringResource(R.string.detalle_corriente), "${num(mar.corrienteKmh, 1, " km/h")} ${direccion(mar.direccionCorrienteGrados)}")
            c.estadoMarea?.let {
                FilaDato(
                    stringResource(R.string.detalle_marea),
                    stringResource(
                        if (it.subiendo) R.string.condiciones_marea_subiendo else R.string.condiciones_marea_bajando,
                        "%.2f".format(it.alturaM)
                    )
                )
            }
        }
    }

    Text(
        stringResource(R.string.condiciones_proximas_24),
        style = MaterialTheme.typography.titleSmall,
        color = Acento,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
    val desde = c.ahora.truncatedTo(ChronoUnit.HOURS)
    val horas = c.horasClima.filter { !it.hora.isBefore(desde) }.take(24)
    val marPorHora = c.horasMar.associateBy { it.hora }
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        horas.forEachIndexed { i, h ->
            val ahora = i == 0
            // Pastilla por hora: la de "ahora" con relleno tonal, como en el mockup
            Column(
                modifier = Modifier
                    .widthIn(min = 60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (ahora) ContenedorSecundario else SuperficieTonal)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val colorTexto = if (ahora) TextoSobreContenedorSecundario else TextoSuperficieVariante
                Text(
                    if (ahora) stringResource(R.string.condiciones_ahora) else hora(h.hora),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorTexto
                )
                Icon(
                    painterResource(iconoClima(h.codigoClima, h.esDeDia ?: true)),
                    contentDescription = h.codigoClima?.let { descripcionClima(it) },
                    tint = colorIconoClima(h.codigoClima, h.esDeDia ?: true),
                    modifier = Modifier.size(24.dp)
                )
                Text(num(h.temperaturaC, unidad = "°"), style = MaterialTheme.typography.titleSmall, color = TextoSuperficie)
                DatoHora(R.drawable.ic_ms_air, "${num(h.vientoKmh)} ${direccion(h.direccionVientoGrados)}", colorTexto)
                marPorHora[h.hora]?.olaM?.let { DatoHora(R.drawable.ic_ms_waves, "%.1f".format(it), colorTexto) }
                h.probabilidadLluviaPorc?.takeIf { it > 0 }?.let { DatoHora(R.drawable.ic_ms_water_drop, "$it%", colorTexto) }
            }
        }
    }
}

/** Ícono chico + valor (viento, ola, lluvia) dentro de la pastilla de cada hora. */
@Composable
internal fun DatoHora(icono: Int, valor: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icono), contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(2.dp))
        Text(valor, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** "Buenas para entrar al agua" en un contenedor del color del semáforo, con los motivos debajo. */
@Composable
fun AvisoAptitud(nivel: Aptitud, motivos: String, modifier: Modifier = Modifier) {
    val color = colorAptitud(nivel)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (nivel == Aptitud.BUENA) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = color
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(R.string.condiciones_aptitud, nivel.nombre()),
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
            if (motivos.isNotBlank()) {
                Text(motivos, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
            }
        }
    }
}

// ============================== MAREAS ==============================
