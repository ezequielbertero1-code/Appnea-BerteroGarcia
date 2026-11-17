package com.berterogarcia.appnea.ui.inmersion

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Ubicacion
import com.berterogarcia.appnea.data.ubicacion.UbicacionHelper
import com.berterogarcia.appnea.ui.theme.ColorMalo
import com.berterogarcia.appnea.ui.theme.FondoIntermedio
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import java.util.Locale

/** Estilo gratuito de OpenFreeMap: sin API key ni registro. La atribución (ⓘ) la agrega MapLibre. */
private const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"

/**
 * MapView de MapLibre adentro de Compose. Le reenvía el ciclo de vida de la pantalla
 * (onStart/onResume/onPause/onStop/onDestroy), como pide la guía de MapLibre.
 */
@Composable
fun MapaLibre(
    centro: Ubicacion,
    modifier: Modifier = Modifier,
    zoom: Double = 13.0,
    interactivo: Boolean = true,
    onMapaListo: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onListo by rememberUpdatedState(onMapaListo)
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var fallo by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context.applicationContext) // hay que llamarlo antes de crear el MapView
        // textureMode: se dibuja como una View común, así anda bien dentro de scrolls y animaciones
        val opciones = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        MapView(context, opciones).apply {
            onCreate(null)
            addOnDidFailLoadingMapListener { fallo = true } // sin internet o servidor caído
            getMapAsync { m ->
                m.uiSettings.setAllGesturesEnabled(interactivo)
                m.uiSettings.isRotateGesturesEnabled = false
                m.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(centro.latitud, centro.longitud))
                    .zoom(zoom)
                    .build()
                m.setStyle(ESTILO_MAPA)
                mapa = m
                onListo(m)
            }
        }
    }

    // Si cambia el centro (por ejemplo, "Mi ubicación" en la tarjeta), se mueve la cámara
    LaunchedEffect(centro, mapa) {
        mapa?.moveCamera(CameraUpdateFactory.newLatLng(LatLng(centro.latitud, centro.longitud)))
    }

    DisposableEffect(lifecycle, mapView) {
        val observador = LifecycleEventObserver { _, evento ->
            runCatching {
                when (evento) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    else -> Unit
                }
            }
        }
        lifecycle.addObserver(observador)
        onDispose {
            lifecycle.removeObserver(observador)
            // Lo bajamos en orden según en qué estado quedó
            runCatching {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    Box(modifier.background(FondoIntermedio)) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        if (fallo) {
            Text(
                stringResource(R.string.mapa_error),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuperficieVariante,
                modifier = Modifier.align(Alignment.Center).padding(12.dp)
            )
        }
    }
}

/** Pin con la punta justo en el centro de donde se lo alinea. */
@Composable
fun PinMapa(modifier: Modifier = Modifier, tamanio: Dp = 36.dp) {
    Icon(
        Icons.Filled.Place,
        contentDescription = stringResource(R.string.mapa_lugar_marcado),
        tint = ColorMalo,
        modifier = modifier.offset(y = -tamanio / 2).size(tamanio)
    )
}

/**
 * Devuelve una función que pide permiso de ubicación (si hace falta) y avisa el resultado.
 * Si ya hay permiso, no muestra nada y responde true de una.
 */
@Composable
fun rememberPedirUbicacion(onResultado: (concedido: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val resultado by rememberUpdatedState(onResultado)
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> resultado(r.values.any { it }) }
    return remember(lanzador) {
        {
            if (UbicacionHelper.tienePermiso(context)) resultado(true)
            else runCatching {
                lanzador.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }.onFailure { resultado(false) }
        }
    }
}

fun formatoCoordenadas(latitud: Double, longitud: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitud, longitud)