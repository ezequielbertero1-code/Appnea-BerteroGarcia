package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto
import com.berterogarcia.appnea.ui.theme.BordeSuave
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario
import com.berterogarcia.appnea.ui.theme.TextoTerciario
import com.berterogarcia.appnea.ui.theme.VeloFuerte
import com.berterogarcia.appnea.ui.theme.VeloSuave

@Composable
fun CampoTexto(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    esPassword: Boolean = false,
    esError: Boolean = false,
    mensajeError: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    minLines: Int = 1,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        trailingIcon = trailingIcon,
        visualTransformation = if (esPassword) PasswordVisualTransformation() else visualTransformation,
        isError = esError,
        supportingText = { if (esError) Text(mensajeError) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = modifier,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = VeloFuerte,
            unfocusedContainerColor = VeloSuave,
            focusedBorderColor = Acento,
            unfocusedBorderColor = BordeSuave,
            focusedLabelColor = Acento,
            unfocusedLabelColor = TextoTerciario,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Acento,
            errorContainerColor = VeloFuerte,
            errorBorderColor = ColorError,
            errorLabelColor = ColorError,
            errorTextColor = Color.White,
            errorSupportingTextColor = ColorError,
            errorCursorColor = ColorError
        )
    )
}

// ============================ Tarjetas y datos ============================
// Estilo tomado de la sección de clima de Inicio (Clima.kt), compartido por toda la app.

/** Etiqueta gris arriba, valor en negrita abajo. */
@Composable
fun DatoChico(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = TextoSecundario)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
    }
}

/**
 * Mosaico de dato para usar dentro de un Row (ocupa el mismo ancho que sus hermanos).
 * Si [onClick] es null no es clickeable.
 */
@Composable
fun RowScope.Mosaico(
    titulo: String,
    valor: String,
    detalle: String = "",
    onClick: (() -> Unit)? = null,
    colorValor: Color = Color.White,
    extra: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()   // solo tiene efecto si el Row tiene altura (ver FilaMosaicos)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = Acento)
            Spacer(Modifier.height(4.dp))
            Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal, color = colorValor)
            extra?.invoke()
            if (detalle.isNotBlank()) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSecundario)
            }
        }
    }
}

/** Fila de mosaicos con la misma altura aunque uno tenga más texto. */
@Composable
fun FilaMosaicos(contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido
    )
}

/** Tarjeta protagonista: emoji grande + número grande + subtítulo, con contenido extra. */
@Composable
fun TarjetaHero(
    emoji: String,
    valor: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    derecha: @Composable ColumnScope.() -> Unit = {},
    abajo: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta, contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 44.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(valor, fontSize = 34.sp, fontWeight = FontWeight.Normal)
                    Text(subtitulo, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End, content = derecha)
            }
            abajo()
        }
    }
}

/** Aviso naranja con ícono de info (condiciones orientativas, perfil simulado, etc.). */
@Composable
fun TarjetaAviso(titulo: String, texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AvisoFondo)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = "Advertencia", tint = AvisoAcento)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(titulo, fontWeight = FontWeight.Normal, color = AvisoAcento)
                Text(texto, style = MaterialTheme.typography.bodySmall, color = AvisoTexto)
            }
        }
    }
}

/** Chips celestes. Si la lista está vacía muestra [vacio]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(items: List<String>, vacio: String) {
    if (items.isEmpty()) {
        Text(vacio, color = TextoSecundario)
        return
    }
    val forma = RoundedCornerShape(50)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { t ->
            Text(
                t,
                style = MaterialTheme.typography.labelLarge,
                color = Acento,
                modifier = Modifier
                    .background(Acento.copy(alpha = 0.12f), forma)
                    .border(1.dp, Acento.copy(alpha = 0.4f), forma)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}