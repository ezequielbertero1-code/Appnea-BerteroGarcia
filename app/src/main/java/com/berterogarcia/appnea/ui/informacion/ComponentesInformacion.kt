package com.berterogarcia.appnea.ui.informacion

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.ui.components.EncabezadoGrupo
import com.berterogarcia.appnea.ui.components.FilaListaExpandible
import com.berterogarcia.appnea.ui.components.IconoCirculo
import com.berterogarcia.appnea.ui.components.TonoIcono
import com.berterogarcia.appnea.ui.theme.AvisoAcento
import com.berterogarcia.appnea.ui.theme.AvisoFondo
import com.berterogarcia.appnea.ui.theme.AvisoTexto
import com.berterogarcia.appnea.ui.theme.TextoSuperficie
import com.berterogarcia.appnea.ui.theme.TextoSuperficieVariante

/**
 * Tarjeta destacada de PREVENCIÓN / SEGURIDAD. Se usa a lo largo de toda la pantalla,
 * junto a cada tema, tal como fue pedido: dar prevención siempre que se explique un concepto.
 * Mantiene los colores claros originales (AvisoFondo / AvisoAcento / AvisoTexto).
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
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AvisoFondo)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null, // el título ya lo dice
                    tint = AvisoAcento,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = titulo,
                    fontWeight = FontWeight.Bold,
                    color = AvisoAcento,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = AvisoTexto,
                modifier = Modifier.padding(top = 6.dp)
            )
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
                    fontWeight = FontWeight.Bold,
                    color = TextoSuperficie
                )
                Text(
                    text = punto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSuperficieVariante
                )
            }
        }
    }
}

// ============================ TEMAS DESPLEGABLES ============================

/** Imagen de un tema, con su pie de foto. */
data class ImagenTema(
    @param:DrawableRes val resId: Int,
    @param:StringRes val descripcion: Int,
    @param:StringRes val pie: Int
)

/** Alerta de prevención/seguridad asociada a un tema. */
data class AvisoSeguridad(@param:StringRes val titulo: Int, @param:StringRes val cuerpo: Int)

/**
 * Un tema de "Aprender": se muestra como una fila que se despliega.
 * [id] tiene que ser ÚNICO en toda la pestaña (se usa como key del LazyColumn y para
 * recordar si la fila estaba abierta). Una key repetida hace crashear el LazyColumn.
 */
data class TemaInfo(
    val id: String,
    @param:StringRes val titulo: Int,
    @param:StringRes val cuerpo: Int? = null,
    val imagenes: List<ImagenTema> = emptyList(),
    val puntos: List<Int> = emptyList(),   // ids de strings.xml
    val avisos: List<AvisoSeguridad> = emptyList()
)

/** Grupo de temas con un título opcional (no desplegable). */
data class SeccionInfo(@param:StringRes val titulo: Int?, val temas: List<TemaInfo>)

/**
 * Lista completa de una pestaña: cada sección es un grupo de filas desplegables (estilo Android 16).
 * El estado abierto/cerrado de cada fila se guarda con rememberSaveable dentro de cada item:
 * sobrevive al scroll, al cambio de pestaña y a la rotación de pantalla.
 */
@Composable
fun ListaDeTemas(secciones: List<SeccionInfo>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        secciones.forEachIndexed { indice, seccion ->
            seccion.titulo?.let { titulo ->
                item(key = "seccion_$indice") { EncabezadoGrupo(titulo = stringResource(titulo)) }
            }
            itemsIndexed(seccion.temas, key = { _, tema -> tema.id }) { i, tema ->
                var expandida by rememberSaveable { mutableStateOf(false) }
                FilaTema(
                    tema = tema,
                    indice = i,
                    total = seccion.temas.size,
                    expandida = expandida,
                    onCambio = { expandida = it },
                    // Mismo espacio de 2 dp que GrupoLista entre filas
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Tema desplegable. Si tiene alerta de seguridad, el ícono va en amarillo y el título de la
 * alerta se adelanta como subtítulo ("Nunca hiperventilar"), así se ve sin abrir la fila.
 */
@Composable
private fun FilaTema(
    tema: TemaInfo,
    indice: Int,
    total: Int,
    expandida: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val conAviso = tema.avisos.isNotEmpty()
    FilaListaExpandible(
        titulo = stringResource(tema.titulo),
        subtitulo = tema.avisos.firstOrNull()?.let { stringResource(it.titulo) },
        indice = indice,
        total = total,
        expandida = expandida,
        onCambio = onCambio,
        modifier = modifier,
        inicio = {
            IconoCirculo(
                rememberVectorPainter(if (conAviso) Icons.Rounded.Warning else Icons.Rounded.Info),
                tono = if (conAviso) TonoIcono.ADVERTENCIA else TonoIcono.NORMAL
            )
        }
    ) {
        tema.cuerpo?.let { cuerpo ->
            Text(text = stringResource(cuerpo), style = MaterialTheme.typography.bodyMedium, color = TextoSuperficieVariante)
            Spacer(Modifier.height(10.dp))
        }
        tema.imagenes.forEach { imagen ->
            ImagenDeTema(imagen)
        }
        if (tema.puntos.isNotEmpty()) {
            ListaDePuntos(puntos = tema.puntos.map { stringResource(it) }, modifier = Modifier.padding(bottom = 10.dp))
        }
        tema.avisos.forEach { aviso ->
            TarjetaSeguridad(titulo = stringResource(aviso.titulo), cuerpo = stringResource(aviso.cuerpo))
        }
    }
}

/** Imagen sin Card propia: va adentro de la superficie de la fila. */
@Composable
private fun ImagenDeTema(imagen: ImagenTema) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Image(
            painter = painterResource(imagen.resId),
            contentDescription = stringResource(imagen.descripcion),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(imagen.pie),
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuperficieVariante
        )
    }
}
