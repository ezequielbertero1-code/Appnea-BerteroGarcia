package com.berterogarcia.appnea.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.berterogarcia.appnea.ui.theme.Acento
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ContenedorSecundario
import com.berterogarcia.appnea.ui.theme.ContornoFuerte
import com.berterogarcia.appnea.ui.theme.SuperficieTonal
import com.berterogarcia.appnea.ui.theme.TextoDeshabilitado
import com.berterogarcia.appnea.ui.theme.TextoSobreContenedorSecundario
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

/**
 * Campo de texto outlined de Material 3 (estilo "Appnea circular"): sin relleno, para que se vea
 * el degradado de fondo, con contorno que pasa a cian con foco y a rojo con error.
 * [ayuda]: texto de apoyo debajo del campo cuando no hay error ("Marcá el punto en el mapa…").
 * [placeholder]: texto gris dentro del campo vacío ("Opcional", "--:--").
 * [soloLectura]: no abre el teclado (lo usa el desplegable de [SelectorOpciones]).
 */
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
    enabled: Boolean = true,
    ayuda: String? = null,
    placeholder: String? = null,
    soloLectura: Boolean = false,
    /** Tamaño del texto escrito y de la etiqueta; Unspecified = el de siempre. Para filas compactas. */
    tamanoTexto: TextUnit = TextUnit.Unspecified,
    /** false = sin el espacio reservado para el error/ayuda (filas compactas; el error se muestra aparte). */
    conTextoApoyo: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        textStyle = LocalTextStyle.current.copy(fontSize = tamanoTexto),
        label = { Text(etiqueta, fontSize = tamanoTexto, maxLines = 1) },
        placeholder = placeholder?.let { texto -> { Text(texto) } },
        trailingIcon = trailingIcon,
        visualTransformation = if (esPassword) PasswordVisualTransformation() else visualTransformation,
        isError = esError,
        // Siempre presente (aunque vacío) para que el campo no salte de alto cuando aparece el error
        supportingText = if (!conTextoApoyo) null else {
            {
                when {
                    esError -> Text(mensajeError)
                    ayuda != null -> Text(ayuda)
                }
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = modifier,
        enabled = enabled,
        readOnly = soloLectura,
        colors = coloresCampoTexto()
    )
}

/**
 * Colores de los campos outlined de la app (CampoTexto, DatePickerField, TimePickerField, CampoFecha):
 * sin relleno, contorno que pasa a cian con foco y a rojo con error.
 */
@Composable
fun coloresCampoTexto(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedBorderColor = Acento,
    unfocusedBorderColor = ContornoFuerte,
    disabledBorderColor = TextoDeshabilitado,
    errorBorderColor = ColorError,
    focusedLabelColor = Acento,
    unfocusedLabelColor = TextoSuperficieVariante,
    disabledLabelColor = TextoDeshabilitado,
    errorLabelColor = ColorError,
    focusedTextColor = TextoSuperficie,
    unfocusedTextColor = TextoSuperficie,
    disabledTextColor = TextoDeshabilitado,
    errorTextColor = TextoSuperficie,
    focusedPlaceholderColor = TextoSuperficieVariante,
    unfocusedPlaceholderColor = TextoSuperficieVariante,
    focusedTrailingIconColor = TextoSuperficieVariante,
    unfocusedTrailingIconColor = TextoSuperficieVariante,
    errorTrailingIconColor = ColorError,
    focusedSupportingTextColor = TextoSuperficieVariante,
    unfocusedSupportingTextColor = TextoSuperficieVariante,
    errorSupportingTextColor = ColorError,
    cursorColor = Acento,
    errorCursorColor = ColorError
)

// ============================ Tarjetas y datos ============================
// Estilo "Appnea circular": superficies tonales (velo blanco 7 %) con esquinas de 16 dp.
// Los usan Detalle, Condiciones, Clima y Estadísticas.

private val FormaTarjetaDato = RoundedCornerShape(16.dp)

/** Etiqueta gris arriba, valor abajo. */
@Composable
fun DatoChico(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextoSuperficie)
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
    colorValor: Color = TextoSuperficie,
    extra: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()   // solo tiene efecto si el Row tiene altura (ver FilaMosaicos)
            .then(if (onClick != null) Modifier.clip(FormaTarjetaDato).clickable { onClick() } else Modifier),
        shape = FormaTarjetaDato,
        colors = CardDefaults.cardColors(containerColor = SuperficieTonal, contentColor = TextoSuperficie)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(titulo, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
            Text(
                valor,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                color = colorValor,
                modifier = Modifier.padding(top = 2.dp)
            )
            extra?.invoke()
            if (detalle.isNotBlank()) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
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

/**
 * Tarjeta protagonista: ícono (o emoji, en pantallas viejas) + número grande + subtítulo,
 * con contenido extra a la derecha y abajo. [icono] tiene prioridad sobre [emoji].
 */
@Composable
fun TarjetaHero(
    emoji: String,
    valor: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    icono: (@Composable () -> Unit)? = null,
    derecha: @Composable ColumnScope.() -> Unit = {},
    abajo: @Composable ColumnScope.() -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjetaDato,
        colors = CardDefaults.cardColors(containerColor = SuperficieTonal, contentColor = TextoSuperficie)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    icono != null -> {
                        icono()
                        Spacer(Modifier.width(14.dp))
                    }
                    emoji.isNotBlank() -> {
                        Text(emoji, fontSize = 40.sp)
                        Spacer(Modifier.width(12.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(valor, fontSize = 40.sp, lineHeight = 44.sp, color = TextoSuperficie)
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TextoSuperficieVariante)
                }
                Column(horizontalAlignment = Alignment.End, content = derecha)
            }
            abajo()
        }
    }
}

/** Aviso con los colores claros de seguridad (condiciones orientativas, perfil simulado, etc.). */
@Composable
fun TarjetaAviso(titulo: String, texto: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaTarjetaDato,
        colors = CardDefaults.cardColors(containerColor = AvisoFondo)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = AvisoAcento, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(titulo, fontWeight = FontWeight.Bold, color = AvisoAcento, style = MaterialTheme.typography.titleSmall)
            }
            Text(texto, style = MaterialTheme.typography.bodySmall, color = AvisoTexto, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Etiquetas tonales de solo lectura (equipamiento, características del agua). Si la lista está vacía muestra [vacio]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(items: List<String>, vacio: String) {
    if (items.isEmpty()) {
        Text(vacio, style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
        return
    }
    val forma = RoundedCornerShape(8.dp)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { t ->
            Text(
                t,
                style = MaterialTheme.typography.labelLarge,
                color = TextoSobreContenedorSecundario,
                modifier = Modifier
                    .background(ContenedorSecundario, forma)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}
