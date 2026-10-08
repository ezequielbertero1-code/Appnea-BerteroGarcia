package com.berterogarcia.appnea.ui.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.clima.ClimaCompacto
import com.berterogarcia.appnea.ui.clima.ClimaViewModel
import com.berterogarcia.appnea.ui.components.BotonSecundario
import com.berterogarcia.appnea.ui.components.TarjetaInmersion
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.FondoTarjetaResaltada
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoSobreResaltada

/**
 * Inicio: la acción principal (registrar una inmersión) va primero, después las últimas
 * inmersiones y al final un resumen chico del clima que abre Condiciones.
 */
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Hola ${nombreUsuario.orEmpty()}! 🤿🫧",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
        )

        // ======================= ACCIÓN PRINCIPAL =======================
        TarjetaNuevaInmersion(onClick = onNuevaInmersion)

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonSecundario(
                texto = "Entrenar",
                icono = painterResource(R.drawable.ic_cronometro),
                onClick = { onIrA(AppDestinations.ENTRENAMIENTO) },
                modifier = Modifier.weight(1f)
            )
            BotonSecundario(
                texto = "Aprender",
                icono = painterResource(R.drawable.ic_informate),
                onClick = { onIrA(AppDestinations.APRENDER) },
                modifier = Modifier.weight(1f)
            )
        }

        // ======================= ÚLTIMAS INMERSIONES =======================
        Spacer(Modifier.height(24.dp))
        val e = estado
        EncabezadoSeccion(
            titulo = "Tus últimas inmersiones",
            accion = if (e != null && e.total > 0) "Ver todas (${e.total})" else null,
            onAccion = { onIrA(AppDestinations.INMERSION) }
        )
        when {
            e == null -> CircularProgressIndicator(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            e.error -> TextoSecundarioCentrado(
                "No se pudieron cargar tus inmersiones. Probá cerrar y volver a abrir la app."
            )
            e.ultimas.isEmpty() -> TextoSecundarioCentrado(
                "Todavía no registraste inmersiones.\nTocá \"Registrar inmersión\" para cargar la primera."
            )
            else -> e.ultimas.forEach { inmersion ->
                TarjetaInmersion(
                    inmersion = inmersion,
                    onClick = { onVerDetalleInmersion(inmersion.id) },
                    compacta = true
                )
            }
        }

        // ======================= CLIMA (resumen) =======================
        Spacer(Modifier.height(24.dp))
        EncabezadoSeccion(titulo = "Condiciones de hoy")
        ClimaCompacto(viewModel = climaViewModel, onVerCondiciones = onVerCondiciones)
    }
}


@Composable
private fun TarjetaNuevaInmersion(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FondoTarjetaResaltada,
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Acento.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Círculo cian con el "+": es el único lugar con cian lleno
            Icon(
                painter = painterResource(R.drawable.ic_agregar),
                contentDescription = null, // el texto de al lado ya lo describe
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Registrar inmersión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Buceo o apnea",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSobreResaltada
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Acento
            )
        }
    }
}

@Composable
private fun EncabezadoSeccion(titulo: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(accion, color = Acento) }
        }
    }
}

@Composable
private fun TextoSecundarioCentrado(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSecundario,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}