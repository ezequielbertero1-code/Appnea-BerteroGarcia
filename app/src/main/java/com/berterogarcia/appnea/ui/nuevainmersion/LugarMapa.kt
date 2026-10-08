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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.data.local.Geocodificador
import com.berterogarcia.appnea.data.local.Ubicacion
import com.berterogarcia.appnea.data.local.UbicacionHelper
import com.berterogarcia.appnea.ui.components.BotonPrincipal
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.CampoTexto
import com.berterogarcia.appnea.ui.components.MapaLibre
import com.berterogarcia.appnea.ui.components.PinMapa
import com.berterogarcia.appnea.ui.components.formatoCoordenadas
import com.berterogarcia.appnea.ui.components.rememberPedirUbicacion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
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
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FondoTarjeta)
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
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    formatoCoordenadas(latitud, longitud),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoTerciario,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onQuitar) { Text("Quitar", color = Acento) }
            }
        } else {
            Text(
                "Sin ubicación marcada. Sirve para guardar el clima y la marea del lugar.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSecundario,
                modifier = Modifier.padding(12.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BotonSecundario(
                texto = if (hayUbicacion) "Cambiar" else "Marcar en mapa",
                onClick = onAbrirMapa,
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = if (buscando) "Buscando…" else "Mi ubicación",
                onClick = pedirUbicacion,
                enabled = !buscando,
                cargando = buscando,
                modifier = Modifier.weight(1f)
            )
        }
        aviso?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = ColorAdvertencia,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
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

    val pedirUbicacion = rememberPedirUbicacion { concedido ->
        if (!concedido) {
            aviso = "Sin permiso de ubicación: mové el mapa a mano."
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
                aviso = if (!UbicacionHelper.ubicacionActivada(appContext))
                    "Activá la ubicación del teléfono."
                else "No se pudo obtener tu ubicación."
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

        Text(
            "Mové el mapa o tocá un punto",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(FondoTarjeta)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )

        // Hoja inferior
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(FondoTarjeta)
                .imePadding()
                .padding(16.dp)
        ) {
            Text(
                nombre.ifBlank { if (buscandoNombre) "Buscando nombre…" else "Lugar sin nombre" },
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                formatoCoordenadas(marcado.latitud, marcado.longitud),
                style = MaterialTheme.typography.bodySmall,
                color = TextoTerciario
            )
            CampoTexto(
                valor = nombre,
                onValorChange = {
                    nombre = it
                    nombreEditado = it.isNotBlank() // si lo borra todo, vuelve a autocompletarse
                },
                etiqueta = "Nombre del lugar (podés editarlo)",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            // El usuario escribió su propio nombre: se le ofrece el encontrado sin pisarlo
            val s = sugerencia
            if (nombreEditado && s != null && s != nombre) {
                TextButton(onClick = { nombre = s; nombreEditado = false }) {
                    Text("Usar \"$s\"", color = Acento, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            aviso?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ColorAdvertencia)
            }
            BotonSecundario(
                texto = if (buscando) "Buscando…" else "Mi ubicación",
                onClick = pedirUbicacion,
                enabled = !buscando,
                cargando = buscando,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotonSecundario(texto = "Cancelar", onClick = onCancelar, modifier = Modifier.weight(1f))
                BotonPrincipal(
                    texto = "Usar este lugar",
                    onClick = { onConfirmar(marcado.latitud, marcado.longitud, nombre) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}