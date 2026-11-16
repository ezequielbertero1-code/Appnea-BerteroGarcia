package com.berterogarcia.appnea.ui.detalleinmersion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.condiciones.Aptitud
import com.berterogarcia.appnea.data.local.InmersionEntity
import com.berterogarcia.appnea.data.local.formatearDuracion
import com.berterogarcia.appnea.data.perfil.PerfilSimulado
import com.berterogarcia.appnea.data.perfil.PuntoPerfil
import com.berterogarcia.appnea.ui.components.Chips
import com.berterogarcia.appnea.ui.components.DatoChico
import com.berterogarcia.appnea.ui.components.FilaMosaicos
import com.berterogarcia.appnea.ui.components.Mosaico
import com.berterogarcia.appnea.ui.components.TarjetaAviso
import com.berterogarcia.appnea.ui.components.TarjetaHero
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.nombreFaseLunar
import com.berterogarcia.appnea.ui.condiciones.FilaDato
import com.berterogarcia.appnea.ui.condiciones.Semaforo
import com.berterogarcia.appnea.ui.condiciones.TarjetaCondicion
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.colorIconoClima
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.iconoClima
import com.berterogarcia.appnea.ui.condiciones.num
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

// Pestañas Gráfico, Equipo y Condiciones del detalle de una inmersión.

@Composable
internal fun PestanaGrafico(i: InmersionEntity, perfil: List<PuntoPerfil>) {
    // La curva se dibuja de izquierda a derecha cada vez que se entra a la pestaña
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(perfil) {
        progreso.snapTo(0f)
        progreso.animateTo(1f, tween(durationMillis = 1200, easing = FastOutSlowInEasing))
    }

    TarjetaCondicion(stringResource(R.string.detalle_perfil)) {
        if (perfil.size < 2) {
            Text(stringResource(R.string.detalle_perfil_sin_datos), color = TextoSuperficieVariante)
        } else {
            GraficoPerfil(perfil, progreso = progreso.value)
        }
    }

    if (perfil.size >= 2) {
        val apnea = PerfilSimulado.esApnea(i)
        val bajada = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.DESCENSO_BUCEO * 60).limpio()} m/min"
        val subida = if (apnea) "${PerfilSimulado.VELOCIDAD_APNEA.limpio()} m/s"
        else "${(PerfilSimulado.ASCENSO_BUCEO * 60).limpio()} m/min"
        val supuesto = stringResource(R.string.detalle_supuesto)
        FilaMosaicos {
            Mosaico(stringResource(R.string.detalle_descenso), bajada, supuesto)
            Mosaico(stringResource(R.string.detalle_ascenso), subida, supuesto)
            Mosaico(stringResource(R.string.entrenar_duracion), formatearDuracion(perfil.last().segundo), stringResource(R.string.detalle_del_perfil))
        }
    }

    TarjetaAviso(
        titulo = stringResource(R.string.detalle_perfil_estimado),
        texto = stringResource(R.string.detalle_perfil_estimado_texto),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
internal fun PestanaEquipo(i: InmersionEntity) {
    // Lastre y protección extra son opcionales: si no se usaron, no se muestra nada
    if (i.lastreKg != null || i.proteccionMm != null) {
        FilaMosaicos {
            i.lastreKg?.let {
                Mosaico(stringResource(R.string.detalle_lastre), "${it.limpio()} kg", stringResource(R.string.detalle_lastre_detalle))
            }
            i.proteccionMm?.let {
                Mosaico(stringResource(R.string.detalle_proteccion), "${it.limpio()} mm", stringResource(R.string.detalle_proteccion_detalle))
            }
            if (i.lastreKg == null || i.proteccionMm == null) Spacer(Modifier.weight(1f))
        }
    }
    TarjetaCondicion(stringResource(R.string.detalle_equipamiento)) {
        Chips(i.equipamiento.toList(), vacio = stringResource(R.string.detalle_sin_equipamiento))
    }
}

@Composable
internal fun PestanaCondiciones(i: InmersionEntity) {
    val c = i.condiciones

    // ---- Clima grande (solo si se guardó con internet) ----
    if (c != null && c.conDatosOnline) {
        val aptitud = Aptitud.entries.firstOrNull { it.texto == c.aptitudAgua }
        TarjetaHero(
            emoji = "",
            icono = {
                Icon(
                    painterResource(iconoClima(c.codigoClima)),
                    contentDescription = null,
                    tint = colorIconoClima(c.codigoClima),
                    modifier = Modifier.size(44.dp)
                )
            },
            valor = c.temperaturaC?.let { num(it, unidad = "°") } ?: "${i.temperaturaAireC.limpio()}°",
            subtitulo = c.codigoClima?.let { descripcionClima(it) } ?: stringResource(R.string.detalle_sin_datos_cielo),
            modifier = Modifier.padding(bottom = 12.dp),
            derecha = {
                c.sensacionTermicaC?.let {
                    Text(
                        stringResource(R.string.detalle_sensacion, num(it, unidad = "°")),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuperficieVariante
                    )
                }
            },
            abajo = {
                Spacer(Modifier.height(14.dp))
                // Tres columnas del mismo ancho: así Ráfagas queda justo arriba de Olas
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_viento), c.vientoKmh?.let { "${num(it)} ${direccion(c.direccionVientoGrados)}" } ?: "-") }
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_rafagas), num(c.rafagasKmh, unidad = " km/h")) }
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_humedad), num(c.humedadPorc?.toDouble(), unidad = "%")) }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_presion), num(c.presionHpa, unidad = " hPa")) }
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_olas), c.olaM?.let { num(it, 1, " m") } ?: "-") }
                    Box(Modifier.weight(1f)) { DatoChico(stringResource(R.string.detalle_agua), c.temperaturaAguaC?.let { num(it, 1, "°C") } ?: "-") }
                }
                if (aptitud != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.detalle_para_entrar),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSuperficieVariante
                        )
                        Spacer(Modifier.width(8.dp))
                        Semaforo(colorAptitud(aptitud), aptitud.nombre())
                    }
                }
            }
        )
    }

    // ---- Mosaicos: marea, luna, corriente, visibilidad ----
    if (c != null) {
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.detalle_marea),
                c.estadoMarea ?: stringResource(R.string.detalle_sin_datos),
                c.alturaMareaM?.let { num(it, 2, " m") } ?: ""
            )
            Mosaico(stringResource(R.string.detalle_luna), "${c.iluminacionLunaPorc}%", nombreFaseLunar(c.faseLunar))
        }
        FilaMosaicos {
            Mosaico(
                stringResource(R.string.detalle_corriente),
                c.corrienteKmh?.let { num(it, 1, " km/h") } ?: stringResource(R.string.detalle_sin_datos),
                stringResource(R.string.detalle_al_entrar)
            )
            val escala = i.visibilidad?.nombre()
            val metros = i.visibilidadMetros?.let { "${it.limpio()} m" }
            Mosaico(
                stringResource(R.string.detalle_visibilidad),
                escala ?: metros ?: "—",
                if (escala != null && metros != null) metros else stringResource(R.string.detalle_cargada_por_vos)
            )
        }
    } else {
        TarjetaCondicion {
            Text(stringResource(R.string.detalle_sin_condiciones), color = TextoSuperficieVariante)
        }
    }

    // ---- Lo que cargó el usuario ----
    TarjetaCondicion(stringResource(R.string.detalle_tu_registro)) {
        Chips(i.caracteristicasAgua.toList(), vacio = stringResource(R.string.detalle_sin_caracteristicas))
        Spacer(Modifier.height(10.dp))
        FilaDato(
            stringResource(R.string.detalle_temperaturas),
            "${i.temperaturaAireC.limpio()} °C · ${i.temperaturaAguaC.limpio()} °C"
        )
    }

    if (c != null && !c.conDatosOnline) {
        Text(
            stringResource(R.string.detalle_sin_conexion),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
    }
}

// ============================ Helpers ============================
