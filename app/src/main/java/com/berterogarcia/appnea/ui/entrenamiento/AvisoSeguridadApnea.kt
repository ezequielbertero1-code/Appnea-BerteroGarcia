package com.berterogarcia.appnea.ui.entrenamiento

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto

@Composable
fun AvisoSeguridadApnea(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = AvisoFondo)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = "Advertencia",
                tint = AvisoAcento
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Nunca practiques apnea en soledad",
                    fontWeight = FontWeight.Bold,
                    color = AvisoAcento
                )
                Text(
                    "Entrená siempre acompañado por alguien que pueda asistirte, en un entorno seguro, y sin forzar más allá de tus límites. El desmayo por hipoxia puede ocurrir sin previo aviso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AvisoTexto
                )
            }
        }
    }
}
