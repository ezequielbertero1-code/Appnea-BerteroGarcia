package com.berterogarcia.appnea.ui.informacion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R

/**
 * Tarjeta genérica para un bloque de información básica (no relacionado a seguridad).
 */
@Composable
fun TarjetaInfo(
    titulo: String,
    cuerpo: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Tarjeta destacada de PREVENCIÓN / SEGURIDAD. Se usa a lo largo de toda la pantalla,
 * junto a cada tema, tal como fue pedido: dar prevención siempre que se explique un concepto.
 */
@Composable
fun TarjetaSeguridad(
    titulo: String,
    cuerpo: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = "Prevención y seguridad",
                tint = Color(0xFFE65100)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    text = cuerpo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D4037)
                )
            }
        }
    }
}

/**
 * Una lista simple de puntos (checklist informativo), sin depender de componentes
 * interactivos: cada línea se antepone con una viñeta.
 */
@Composable
fun ListaDePuntos(
    puntos: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        puntos.forEach { punto ->
            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(
                    text = "•  ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = punto,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Imagen ilustrativa con su pie de foto, dentro de una tarjeta.
 * Las imágenes provienen de los manuales de referencia de la cursada (Escuela de Buceo Zeus / CMAS)
 * y se usan aquí con fines educativos, dentro de la propia app.
 */
@Composable
fun ImagenIlustrativa(
    resId: Int,
    descripcion: String,
    pie: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF03141F),
            contentColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(resId),
                contentDescription = descripcion,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.padding(top = 6.dp))
            Text(
                text = pie,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Título de subsección dentro de una pestaña (Apnea / Buceo).
 */
@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    )
}