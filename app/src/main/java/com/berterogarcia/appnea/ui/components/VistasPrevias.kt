package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.theme.AppneaTheme
import com.berterogarcia.appnea.ui.theme.fondoApp

/** Tema, fondo y color de texto de la app, para que las vistas previas se vean como en el teléfono. */
@Composable
fun VistaPrevia(contenido: @Composable () -> Unit) {
    AppneaTheme {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.background(fondoApp).padding(16.dp)) { contenido() }
        }
    }
}

@Preview(name = "Botones", widthDp = 360)
@Composable
private fun VistaBotones() = VistaPrevia {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonPrincipal(texto = "Guardar", onClick = {})
        BotonPrincipal(texto = "Guardando…", onClick = {}, cargando = true)
        BotonSecundario(texto = "Anterior", onClick = {})
        BotonContorno(texto = "Marcar en mapa", onClick = {})
        BotonPeligro(texto = "Abortar", onClick = {})
    }
}

@Preview(name = "Tarjetas e insignias", widthDp = 360)
@Composable
private fun VistaTarjetas() = VistaPrevia {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilaDeTarjetas {
            TarjetaKpi("Prof. máxima", "22,3", Modifier.weight(1f), unidad = "m")
            TarjetaKpi("Inmersiones", "24", Modifier.weight(1f))
        }
        TarjetaTonal { Text("Tarjeta tonal con contenido libre") }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TipoInsignia.entries.forEach { Insignia(it.name.lowercase(), it) }
        }
    }
}

@Preview(name = "Barra superior", widthDp = 360)
@Composable
private fun VistaBarraSuperior() = VistaPrevia {
    Column {
        BarraSuperior(titulo = "Estadísticas", navegacion = NavegacionBarra.VOLVER)
        BarraSuperior(titulo = "Nueva inmersión", navegacion = NavegacionBarra.CERRAR)
    }
}
