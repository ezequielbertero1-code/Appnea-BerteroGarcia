package com.berterogarcia.appnea.ui.nuevainmersion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.data.local.Geocodificador
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.ui.components.BotonContorno
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.MapaLibre
import com.berterogarcia.appnea.ui.components.PinMapa
import com.berterogarcia.appnea.ui.components.formatoCoordenadas
import com.berterogarcia.appnea.ui.components.rememberPedirUbicacion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.FondoIntermedio
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Tarjeta de "Datos generales": mini mapa + coordenadas + botones. */
@Composable
fun TarjetaLugar(
    latitud: Double?,
    longitud: Double?,
    buscando: Boolean,
    aviso: String?,
    onAbrirMapa: () -> Unit,
    onMiUbicacion: () -> Unit,      // se llama con el permiso ya concedido
    onPermisoDenegado: () -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pedirUbicacion = rememberPedirUbicacion { concedido ->
        if (concedido) onMiUbicacion() else onPermisoDenegado()
    }
    val hayUbicacion = latitud != null && longitud != null

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SuperficieTonal)
    ) {
        if (latitud != null && longitud != null) {
            Box(Modifier.fillMaxWidth().height(120.dp)) {
                MapaLibre(
                    centro = Ubicacion(latitud, longitud),
                    interactivo = false,
                    modifier = Modifier.matchParentSize()
                )
                PinMapa(Modifier.align(Alignment.Center))
                // Capa transparente: tocar el mini mapa abre el mapa grande
                Box(Modifier.matchParentSize().clickable(onClick = onAbrirMapa))
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatoCoordenadas(latitud, longitud),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuperficieVariante,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onQuitar) { Text(stringResource(R.string.lugar_quitar), color = Acento) }
            }
        } else {
            Text(
                stringResource(R.string.lugar_sin_ubicacion),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuperficieVariante,
                modifier = Modifier.padding(16.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonContorno(
                texto = stringResource(if (hayUbicacion) R.string.lugar_cambiar else R.string.lugar_marcar),
                onClick = onAbrirMapa,
                icono = painterResource(R.drawable.ic_ms_map),
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = stringResource(if (buscando) R.string.lugar_buscando else R.string.lugar_mi_ubicacion),
                onClick = pedirUbicacion,
                enabled = !buscando,
                cargando = buscando,
                icono = painterResource(R.drawable.ic_ms_my_location),
                modifier = Modifier.weight(1f)
            )
        }
        aviso?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = ColorAdvertencia,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Mapa a pantalla completa. El pin queda fijo en el centro: se mueve el mapa
 * (o se toca un punto y el mapa se centra ahí).
 */
@Composable
fun SelectorLugarMapa(
    centroInicial: Ubicacion,
    nombreInicial: String,
    onCancelar: () -> Unit,
    onConfirmar: (latitud: Double, longitud: Double, nombre: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onCancelar) // el botón atrás cierra el mapa, no el formulario

    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var marcado by remember { mutableStateOf(centroInicial) }
    var nombre by rememberSaveable { mutableStateOf(nombreInicial) }
    // Si ya venía un nombre escrito, la sugerencia no lo pisa
    var nombreEditado by rememberSaveable { mutableStateOf(nombreInicial.isNotBlank()) }
    var sugerencia by remember { mutableStateOf<String?>(null) }
    var buscandoNombre by remember { mutableStateOf(false) }

    // Cada vez que el pin queda quieto se busca el nombre. Si el mapa se vuelve a mover antes,
    // este efecto se cancela y arranca otro: no se hacen pedidos de más.
    LaunchedEffect(marcado) {
        delay(600.milliseconds)
        buscandoNombre = true
        val encontrado = Geocodificador.nombrePara(appContext, marcado.latitud, marcado.longitud)
        buscandoNombre = false
        sugerencia = encontrado
        if (encontrado != null && !nombreEditado) nombre = encontrado
    }
    var buscando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }
    val avisoSinPermiso = stringResource(R.string.lugar_aviso_sin_permiso)
    val avisoActivar = stringResource(R.string.lugar_aviso_activar)
    val avisoError = stringResource(R.string.lugar_aviso_error)

    val pedirUbicacion = rememberPedirUbicacion { concedido ->
        if (!concedido) {
            aviso = avisoSinPermiso
        } else scope.launch {
            buscando = true
            aviso = null
            val u = runCatching {
                UbicacionHelper.obtenerUbicacionActual(appContext, 15.seconds)
            }.getOrNull()
            buscando = false
            if (u != null) {
                marcado = u
                mapa?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(u.latitud, u.longitud), 15.0)
                )
            } else {
                aviso = if (!UbicacionHelper.ubicacionActivada(appContext)) avisoActivar else avisoError
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        MapaLibre(
            centro = centroInicial,
            modifier = Modifier.fillMaxSize(),
            onMapaListo = { m ->
                mapa = m
                // Cuando el mapa se deja de mover, lo que está bajo el pin es el lugar marcado
                m.addOnCameraIdleListener {
                    m.cameraPosition.target?.let { marcado = Ubicacion(it.latitude, it.longitude) }
                }
                m.addOnMapClickListener { punto ->
                    m.animateCamera(CameraUpdateFactory.newLatLng(punto))
                    true
                }
            }
        )
        PinMapa(Modifier.align(Alignment.Center), tamanio = 44.dp)

        // Barra flotante: volver + ayuda (no hay búsqueda de lugares: se marca moviendo el mapa)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(FondoIntermedio)
                .padding(start = 4.dp, end = 16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelar) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.accion_volver),
                    tint = TextoSuperficie
                )
            }
            Text(
                stringResource(R.string.lugar_ayuda_mapa),
                style = MaterialTheme.typography.bodyLarge,
                color = TextoSuperficieVariante,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            // FAB chico "Mi ubicación", apoyado sobre la hoja
            SmallFloatingActionButton(
                onClick = { if (!buscando) pedirUbicacion() },
                containerColor = FondoIntermedio,
                contentColor = Acento,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 12.dp)
            ) {
                if (buscando) {
                    CircularProgressIndicator(color = Acento, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        painterResource(R.drawable.ic_ms_my_location),
                        contentDescription = stringResource(R.string.lugar_mi_ubicacion)
                    )
                }
            }

            // Hoja inferior con el lugar elegido
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(FondoIntermedio)
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Manija, como la de una BottomSheet
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ContornoFuerte)
                        .align(Alignment.CenterHorizontally)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoCirculo(painterResource(R.drawable.ic_ms_map))
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(
                            nombre.ifBlank {
                                stringResource(if (buscandoNombre) R.string.lugar_buscando_nombre else R.string.lugar_sin_nombre)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextoSuperficie,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatoCoordenadas(marcado.latitud, marcado.longitud),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuperficieVariante
                        )
                    }
                }
                CampoTexto(
                    valor = nombre,
                    onValorChange = {
                        nombre = it
                        nombreEditado = it.isNotBlank() // si lo borra toodo, vuelve a autocompletarse
                    },
                    etiqueta = stringResource(R.string.lugar_nombre_editable),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                // El usuario escribió su propio nombre: se le ofrece el encontrado sin pisarlo
                val s = sugerencia
                if (nombreEditado && s != null && s != nombre) {
                    TextButton(onClick = { nombre = s; nombreEditado = false }) {
                        Text(stringResource(R.string.lugar_usar_sugerencia, s), color = Acento, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                aviso?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ColorAdvertencia)
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BotonContorno(texto = stringResource(R.string.accion_cancelar), onClick = onCancelar, modifier = Modifier.weight(1f))
                    BotonPrincipal(
                        texto = stringResource(R.string.lugar_usar),
                        onClick = { onConfirmar(marcado.latitud, marcado.longitud, nombre) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
