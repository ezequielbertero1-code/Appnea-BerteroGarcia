package com.berterogarcia.appnea.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.navigation.AppDestinations
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Aptitud
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.components.Avatar
import com.berterogarcia.appnea.ui.components.BarraSuperior
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.inmersion.FilaInmersion
import com.berterogarcia.appnea.ui.components.GrupoLista
import com.berterogarcia.appnea.ui.components.descripcionClima
import com.berterogarcia.appnea.ui.components.nombre
import com.berterogarcia.appnea.ui.components.resolver
import com.berterogarcia.appnea.ui.condiciones.colorAptitud
import com.berterogarcia.appnea.ui.condiciones.colorIconoClima
import com.berterogarcia.appnea.ui.condiciones.direccion
import com.berterogarcia.appnea.ui.condiciones.iconoClima
import com.berterogarcia.appnea.ui.condiciones.num
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ContenedorPrimarioFin
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSobreAcentoFuerte
import com.berterogarcia.appnea.ui.theme.TextoSobreResaltada
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import java.util.Locale

private val LOCALE_ES: Locale = Locale.forLanguageTag("es-AR")

/**
 * Inicio (estilo "Appnea circular", Material 3):
 * TopAppBar con saludo y avatar → tarjeta para registrar → últimas inmersiones en lista
 * agrupada → resumen del clima, que abre Condiciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    nombreUsuario: String?,
    inicioViewModel: InicioViewModel,
    climaViewModel: ClimaViewModel,
    onNuevaInmersion: () -> Unit,
    onVerDetalleInmersion: (Long) -> Unit,
    onVerCondiciones: () -> Unit,
    onIrA: (AppDestinations) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by inicioViewModel.uiState.collectAsStateWithLifecycle()
    // "gianfranco" → "Gianfranco"; null si no hay usuario guardado
    val nombre = nombreUsuario?.trim()?.takeIf { it.isNotEmpty() }
        ?.replaceFirstChar { it.titlecase(LOCALE_ES) }
    val descripcionCuenta = stringResource(R.string.inicio_ir_a_cuenta)

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = if (nombre == null) stringResource(R.string.inicio_saludo_sin_nombre)
            else stringResource(R.string.inicio_saludo, nombre),
            acciones = {
                IconButton(
                    onClick = { onIrA(AppDestinations.CUENTA) },
                    modifier = Modifier.semantics { contentDescription = descripcionCuenta }
                ) {
                    Avatar(inicial = nombre?.take(1))
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // ======================= ACCIÓN PRINCIPAL =======================
            TarjetaRegistrar(
                onRegistrar = onNuevaInmersion,
                onEntrenar = { onIrA(AppDestinations.ENTRENAMIENTO) }
            )

            // ======================= ÚLTIMAS INMERSIONES =======================
            val e = estado
            EncabezadoGrupo(
                titulo = stringResource(R.string.inicio_ultimas),
                accion = if (e != null && e.total > 0) stringResource(R.string.inicio_ver_todas) else null,
                onAccion = { onIrA(AppDestinations.INMERSION) }
            )
            when {
                e == null -> CircularProgressIndicator(
                    color = Acento,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
                e.error -> MensajeVacio(stringResource(R.string.inicio_error_inmersiones))
                e.ultimas.isEmpty() -> MensajeVacio(stringResource(R.string.inicio_sin_inmersiones))
                else -> GrupoLista {
                    e.ultimas.forEachIndexed { indice, inmersion ->
                        FilaInmersion(
                            inmersion = inmersion,
                            indice = indice,
                            total = e.ultimas.size,
                            onClick = { onVerDetalleInmersion(inmersion.id) }
                        )
                    }
                }
            }

            // ======================= CLIMA (resumen) =======================
            EncabezadoGrupo(
                titulo = stringResource(R.string.inicio_condiciones_hoy),
                accion = stringResource(R.string.inicio_ver_detalle),
                onAccion = onVerCondiciones
            )
            TarjetaClimaHoy(viewModel = climaViewModel, onVerCondiciones = onVerCondiciones)
        }
    }
}

// ================================ Componentes de Inicio ================================

/** Tarjeta destacada con degradado: botón cian "Registrar" + botón translúcido "Entrenar". */
@Composable
private fun TarjetaRegistrar(onRegistrar: () -> Unit, onEntrenar: () -> Unit) {
    val forma = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Brush.linearGradient(listOf(FondoTarjetaResaltada, ContenedorPrimarioFin)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.inicio_registrar_titulo),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                stringResource(R.string.inicio_registrar_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSobreResaltada,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Botón en pastilla relleno de cian: la acción principal de toda la pantalla
            Button(
                onClick = onRegistrar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Acento,
                    contentColor = TextoSobreAcentoFuerte
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.inicio_boton_registrar))
            }
            Button(
                onClick = onEntrenar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                ),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    painterResource(R.drawable.ic_ms_timer),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.inicio_boton_entrenar))
            }
        }
    }
}

/** Texto centrado sobre superficie tonal, para "cargando mal" o "todavía no hay nada". */
@Composable
private fun MensajeVacio(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSuperficieVariante,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SuperficieTonal)
            .padding(16.dp)
    )
}

/** Resumen del clima: temperatura grande, lugar + viento y el semáforo para entrar al agua. */
@Composable
private fun TarjetaClimaHoy(viewModel: ClimaViewModel, onVerCondiciones: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val c = uiState.condiciones

    Card(
        onClick = onVerCondiciones,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperficieTonal, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                c == null && uiState.cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Acento,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.inicio_cargando_clima), color = TextoSuperficieVariante)
                }
                c == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        uiState.error?.resolver() ?: stringResource(R.string.inicio_sin_clima),
                        color = ColorError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.cargarClima() }) {
                        Text(stringResource(R.string.inicio_reintentar), color = Acento)
                    }
                }
                else -> {
                    val clima = c.climaActual
                    val lugar = if (uiState.buscandoUbicacion) stringResource(R.string.inicio_buscando_ubicacion)
                    else uiState.ubicacion.nombre ?: stringResource(R.string.inicio_ubicacion_seleccionada)
                    // "12 km/h SO"; sin dirección queda "12 km/h"; sin dato de viento, "-"
                    val viento = clima?.vientoKmh?.let { v ->
                        "${num(v)} km/h ${direccion(clima.direccionVientoGrados)}".removeSuffix(" -")
                    } ?: "-"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val esDeDia = clima?.esDeDia ?: true
                        Icon(
                            painterResource(iconoClima(clima?.codigoClima, esDeDia)),
                            contentDescription = clima?.codigoClima?.let { descripcionClima(it) },
                            tint = colorIconoClima(clima?.codigoClima, esDeDia),
                            modifier = Modifier.size(44.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(num(clima?.temperaturaC, unidad = "°"), fontSize = 40.sp, lineHeight = 44.sp)
                            Text(
                                stringResource(R.string.inicio_clima_detalle, lugar, viento),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextoSuperficieVariante,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        InsigniaAptitud(c.aptitud.nivel)
                    }
                    if (uiState.cargando) {
                        LinearProgressIndicator(
                            color = Acento,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Insignia del semáforo: ✓ verde si están buenas, ⚠ amarillo/rojo si no. */
@Composable
private fun InsigniaAptitud(nivel: Aptitud) {
    val color = colorAptitud(nivel)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            if (nivel == Aptitud.BUENA) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(nivel.nombre(), style = MaterialTheme.typography.labelMedium, color = color)
    }
}
