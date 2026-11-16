package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Condiciones
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.nombreFaseLunar
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Página 7 días de Condiciones.

@Composable
internal fun PaginaSemana(c: Condiciones) {
    val hoy = c.ahora.toLocalDate()
    c.dias.forEach { d ->
        val astro = c.astroDias.firstOrNull { it.fecha == d.fecha }
        val mareasDia = c.mareas.filter { it.hora.toLocalDate() == d.fecha }
        TarjetaCondicion(resaltada = d.fecha == hoy) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(diaCorto(d.fecha, hoy), style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
                    Text(
                        d.codigoClima?.let { descripcionClima(it) } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
                Icon(
                    painterResource(iconoClima(d.codigoClima)),
                    contentDescription = null,
                    tint = colorIconoClima(d.codigoClima),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(num(d.maximaC, unidad = "°"), style = MaterialTheme.typography.titleMedium, color = TextoSuperficie)
                    Text(num(d.minimaC, unidad = "°"), color = TextoSuperficieVariante)
                }
            }
            Spacer(Modifier.height(8.dp))
            FilaDato(stringResource(R.string.condiciones_viento_rafagas), "${num(d.vientoMaxKmh)} / ${num(d.rafagasMaxKmh)} km/h ${direccion(d.direccionVientoGrados)}")
            FilaDato(stringResource(R.string.condiciones_lluvia), "${num(d.lluviaMm, 1, " mm")} (${num(d.probabilidadLluviaPorc?.toDouble(), unidad = "%")})")
            FilaDato(stringResource(R.string.condiciones_ola_max), num(d.olaMaxM, 1, " m"))
            FilaDato(stringResource(R.string.condiciones_uv_max), num(d.uvMax, 1))
            if (astro != null) {
                FilaDato(stringResource(R.string.condiciones_sol), "${hora(astro.amanecer)} - ${hora(astro.atardecer)}")
                FilaDato(stringResource(R.string.detalle_luna), "${nombreFaseLunar(astro.fase)} (${astro.iluminacionPorc}%)")
            }
            if (mareasDia.isNotEmpty()) {
                FilaDato(stringResource(R.string.condiciones_tab_mareas), mareasDia.joinToString("  ") { "${if (it.esPleamar) "▲" else "▼"}${hora(it.hora)}" })
            }
        }
    }
}
