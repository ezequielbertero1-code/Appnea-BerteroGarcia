package com.berterogarcia.appnea.ui.informacion

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.FondoTarjeta
import com.berterogarcia.appnea.ui.theme.TextoSecundario

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
        colors = CardDefaults.cardColors(containerColor = AvisoFondo)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = "Prevención y seguridad",
                tint = AvisoAcento
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = AvisoAcento,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    text = cuerpo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AvisoTexto
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

// ============================ TEMAS DESPLEGABLES ============================

/** Imagen de un tema, con su pie de foto. */
data class ImagenTema(
    @param:DrawableRes val resId: Int,
    val descripcion: String,
    val pie: String
)

/** Alerta de prevención/seguridad asociada a un tema. */
data class AvisoSeguridad(val titulo: String, val cuerpo: String)

/**
 * Un tema de "Aprender": se muestra como una tarjeta que se despliega.
 * [id] tiene que ser ÚNICO en toda la pestaña (se usa como key del LazyColumn y para
 * recordar si la tarjeta estaba abierta). Una key repetida hace crashear el LazyColumn.
 */
data class TemaInfo(
    val id: String,
    val titulo: String,
    val cuerpo: String? = null,
    val imagenes: List<ImagenTema> = emptyList(),
    val puntos: List<String> = emptyList(),
    val avisos: List<AvisoSeguridad> = emptyList()
)

/** Grupo de temas con un título opcional (no desplegable). */
data class SeccionInfo(val titulo: String?, val temas: List<TemaInfo>)

/**
 * Lista completa de una pestaña. El estado abierto/cerrado de cada tarjeta se guarda con
 * rememberSaveable dentro de cada item: sobrevive al scroll, al cambio de pestaña y a la
 * rotación de pantalla.
 */
@Composable
fun ListaDeTemas(secciones: List<SeccionInfo>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        secciones.forEachIndexed { indice, seccion ->
            seccion.titulo?.let { titulo ->
                item(key = "seccion_$indice") { TituloSeccion(titulo) }
            }
            items(seccion.temas, key = { it.id }) { tema ->
                var expandida by rememberSaveable { mutableStateOf(false) }
                TarjetaDesplegable(
                    tema = tema,
                    expandida = expandida,
                    onToggle = { expandida = !expandida }
                )
            }
        }
    }
}

@Composable
fun TarjetaDesplegable(
    tema: TemaInfo,
    expandida: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotacionFlecha by animateFloatAsState(
        targetValue = if (expandida) 180f else 0f,
        label = "rotacionFlecha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = FondoTarjeta,
            contentColor = Color.White
        )
    ) {
        // ---- Encabezado (siempre visible, es lo que se toca) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expandida) "Contraer" else "Expandir",
                    onClick = onToggle
                )
                .semantics { stateDescription = if (expandida) "Expandido" else "Contraído" }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tema.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Indica, sin abrir la tarjeta, que el tema tiene una alerta de seguridad
            if (tema.avisos.isNotEmpty()) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = "Incluye alerta de seguridad",
                    tint = ColorAdvertencia,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null, // el estado ya lo anuncia stateDescription
                modifier = Modifier.rotate(rotacionFlecha)
            )
        }

        // ---- Contenido: texto, imágenes, puntos y alertas ----
        AnimatedVisibility(
            visible = expandida,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                tema.cuerpo?.let { cuerpo ->
                    Text(text = cuerpo, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                }
                tema.imagenes.forEach { imagen ->
                    ImagenDeTema(imagen)
                }
                if (tema.puntos.isNotEmpty()) {
                    ListaDePuntos(puntos = tema.puntos, modifier = Modifier.padding(bottom = 10.dp))
                }
                tema.avisos.forEach { aviso ->
                    TarjetaSeguridad(titulo = aviso.titulo, cuerpo = aviso.cuerpo)
                }
            }
        }
    }
}

/** Imagen sin Card propia: va adentro de la tarjeta oscura (una Card dentro de otra del mismo color no se distinguía). */
@Composable
private fun ImagenDeTema(imagen: ImagenTema) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Image(
            painter = painterResource(imagen.resId),
            contentDescription = imagen.descripcion,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = imagen.pie,
            style = MaterialTheme.typography.bodySmall,
            color = TextoSecundario
        )
    }
}