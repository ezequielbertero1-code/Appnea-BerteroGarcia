package com.berterogarcia.appnea.ui.condiciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Condiciones
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorBajamar
import com.berterogarcia.appnea.ui.theme.ColorRegular
import com.berterogarcia.appnea.ui.theme.Divisor
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Página Mareas de Condiciones.

@Composable
internal fun PaginaMareas(c: Condiciones) {
    val estado = c.estadoMarea
    if (estado == null) {
        TarjetaCondicion(stringResource(R.string.condiciones_tab_mareas)) {
            Text(stringResource(R.string.condiciones_sin_mareas), color = TextoSuperficieVariante)
        }
        return
    }
    val pleamar = stringResource(R.string.condiciones_pleamar)
    val bajamar = stringResource(R.string.condiciones_bajamar)

    TarjetaCondicion(stringResource(R.string.condiciones_estado_actual)) {
        val anterior = estado.anterior
        val siguiente = estado.siguiente
        val progreso = if (anterior != null && siguiente != null) {
            val total = ChronoUnit.MINUTES.between(anterior.hora, siguiente.hora).toFloat()
            ChronoUnit.MINUTES.between(anterior.hora, c.ahora) / total
        } else 0.5f
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (estado.subiendo) R.string.condiciones_subiendo else R.string.condiciones_bajando),
                color = TextoSuperficieVariante
            )
            Text("%.2f m".format(estado.alturaM), fontSize = 22.sp, color = TextoSuperficie)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DatoGrande(
                hora(anterior?.hora),
                if (anterior?.esPleamar == true) pleamar else bajamar,
                color = if (anterior?.esPleamar == true) Acento else ColorBajamar
            )
            MedioCirculo(progreso, if (estado.subiendo) Acento else ColorBajamar, ancho = 110.dp)
            DatoGrande(
                hora(siguiente?.hora),
                if (siguiente?.esPleamar == true) pleamar else bajamar,
                color = if (siguiente?.esPleamar == true) Acento else ColorBajamar
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Divisor)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            DatoGrande(
                anterior?.let { duracion(ChronoUnit.MINUTES.between(it.hora, c.ahora)) } ?: "-",
                stringResource(
                    if (anterior?.esPleamar == true) R.string.condiciones_desde_pleamar else R.string.condiciones_desde_bajamar
                )
            )
            DatoGrande(
                siguiente?.let { duracion(ChronoUnit.MINUTES.between(c.ahora, it.hora)) } ?: "-",
                stringResource(
                    if (siguiente?.esPleamar == true) R.string.condiciones_para_pleamar else R.string.condiciones_para_bajamar
                )
            )
        }
        if (anterior != null && siguiente != null) {
            Spacer(Modifier.height(8.dp))
            FilaDato(stringResource(R.string.condiciones_amplitud), "%.2f m".format(abs(siguiente.alturaM - anterior.alturaM)))
        }
    }

    TarjetaCondicion(stringResource(R.string.condiciones_extremos_hoy)) {
        GraficoMarea(
            fecha = c.ahora.toLocalDate(),
            horas = c.horasMar,
            extremos = c.mareas,
            amanecer = c.astroHoy.amanecer,
            atardecer = c.astroHoy.atardecer,
            ahora = c.ahora
        )
        Text(
            stringResource(R.string.condiciones_leyenda_marea),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
    }

    TarjetaCondicion(stringResource(R.string.condiciones_proximos_dias)) {
        c.mareas.filter { !it.hora.toLocalDate().isBefore(c.ahora.toLocalDate()) }
            .groupBy { it.hora.toLocalDate() }
            .forEach { (fecha, extremos) ->
                Text(
                    diaCorto(fecha, c.ahora.toLocalDate()),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextoSuperficie,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    extremos.forEach { e ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(if (e.esPleamar) R.drawable.ic_ms_north else R.drawable.ic_ms_south),
                                    contentDescription = if (e.esPleamar) pleamar else bajamar,
                                    tint = if (e.esPleamar) Acento else ColorBajamar,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    hora(e.hora),
                                    color = if (e.esPleamar) Acento else ColorBajamar,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text("%.2f m".format(e.alturaM), style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
                        }
                    }
                }
            }
    }

    Text(
        stringResource(R.string.condiciones_aviso_mareas),
        style = MaterialTheme.typography.bodySmall,
        color = ColorRegular,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

// ============================== SOL Y LUNA ==============================
