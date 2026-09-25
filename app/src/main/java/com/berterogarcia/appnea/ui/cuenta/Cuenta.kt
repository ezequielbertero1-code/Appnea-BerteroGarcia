package com.berterogarcia.appnea.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berterogarcia.appnea.R

@Composable
fun CuentaScreen(viewModel: CuentaViewModel, onCerrarSesion: () -> Unit) {
    val perfil = viewModel.perfil
    val stats by viewModel.estadisticas.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.cuenta_greeting) + (viewModel.usuario ?: ""),
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
        Text(
            text = "Los datos personales, certificaciones y contacto de emergencia son de ejemplo " +
                "(simulados). Las estadísticas se calculan con lo que tenés cargado.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        TarjetaDatos(
            titulo = "Datos personales",
            filas = listOf(
                "Nombre" to perfil.nombreCompleto,
                "Usuario" to (viewModel.usuario ?: "-"),
                "Email" to perfil.email,
                "Fecha de nacimiento" to perfil.fechaNacimiento,
                "Ciudad" to perfil.ciudad,
                "Teléfono" to perfil.telefono
            )
        )

        TarjetaDatos(
            titulo = "Certificaciones",
            filas = listOf(
                "Buceo" to perfil.certificacionBuceo,
                "Apnea" to perfil.certificacionApnea,
                "Escuela" to perfil.escuela,
                "N° de certificado" to perfil.numeroCertificado,
                "Apto médico vence" to perfil.aptoMedicoVence
            )
        )

        TarjetaDatos(
            titulo = "Estadísticas",
            filas = listOf(
                "Inmersiones registradas" to stats.inmersiones.toString(),
                "Profundidad máxima" to stats.profundidadMaxima,
                "Tiempo total de fondo" to stats.tiempoTotalFondo,
                "Sesiones de entrenamiento" to
                    "${stats.sesionesEntrenamiento} (${stats.sesionesCompletas} completas)",
                "Mejor apnea estática" to stats.mejorApnea,
                "Salidas de pesca" to stats.salidasPesca.toString(),
                "Capturas totales" to stats.capturasTotales.toString(),
                "Mayor pieza" to stats.mayorPieza
            )
        )

        TarjetaDatos(
            titulo = "Contacto de emergencia",
            filas = listOf(
                "Nombre" to perfil.contactoEmergenciaNombre,
                "Relación" to perfil.contactoEmergenciaRelacion,
                "Teléfono" to perfil.contactoEmergenciaTelefono,
                "Compañero/a habitual" to perfil.companeroHabitual
            )
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cuenta_cerrar_session))
        }
    }
}

/**
 * Tarjeta con un título y filas "etiqueta: valor". Mismo estilo que las tarjetas
 * de Aprender y Clima (fondo 0xFF03141F, texto blanco).
 */
@Composable
private fun TarjetaDatos(titulo: String, filas: List<Pair<String, String>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF03141F),
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            filas.forEach { (etiqueta, valor) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
