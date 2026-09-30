package com.berterogarcia.appnea.ui.inicio

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.AppDestinations
import com.berterogarcia.appnea.ui.clima.ClimaSeccion
import com.berterogarcia.appnea.ui.clima.ClimaViewModel

/**
 * Descripción de cada ambiente de la app, tal como se muestra en Inicio.
 */
private data class Ambiente(
    val destino: AppDestinations,
    val titulo: String,
    val descripcion: String
)

private val AMBIENTES = listOf(
    Ambiente(
        AppDestinations.ENTRENAMIENTO,
        "Entrenamiento",
        "Tablas de apnea estática (predefinidas o creadas por vos) con avisos de sonido, " +
            "cronómetro libre y el historial de tus sesiones."
    ),
    Ambiente(
        AppDestinations.INMERSION,
        "Inmersión",
        "Tu bitácora en un solo lugar: registrá inmersiones de buceo y apnea, y tus salidas " +
            "de pesca submarina con capturas, foto y ubicación."
    ),
    Ambiente(
        AppDestinations.APRENDER,
        "Aprender",
        "Conceptos básicos de apnea y buceo con imágenes ilustrativas y consejos de " +
            "prevención y seguridad."
    ),
    Ambiente(
        AppDestinations.CUENTA,
        "Cuenta",
        "Tus datos personales, certificaciones, contacto de emergencia y estadísticas."
    )
)

@Composable
fun InicioScreen(
    nombreUsuario: String?,
    climaViewModel: ClimaViewModel,
    onIrA: (AppDestinations) -> Unit,
    onVerCondiciones: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Hola ${nombreUsuario ?: ""}! 🤿🫧",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
        )

        // ============================ CLIMA ============================
        ClimaSeccion(viewModel = climaViewModel, onVerCondiciones = onVerCondiciones)

        // =========================== AMBIENTES ===========================
        Spacer(Modifier.height(20.dp))
        Text(
            text = "¿Qué podés hacer en la app?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AMBIENTES.forEach { ambiente ->
            TarjetaAmbiente(ambiente = ambiente, onClick = { onIrA(ambiente.destino) })
        }
    }
}

@Composable
private fun TarjetaAmbiente(ambiente: Ambiente, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF03141F),
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(ambiente.destino.icon),
                contentDescription = ambiente.titulo,
                tint = Color(0xFF16F7FB),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ambiente.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = ambiente.descripcion,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
