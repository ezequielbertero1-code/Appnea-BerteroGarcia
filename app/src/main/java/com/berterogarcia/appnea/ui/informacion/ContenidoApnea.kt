package com.berterogarcia.appnea.ui.informacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berterogarcia.appnea.R

/**
 * Contenido informativo básico sobre Apnea / Freediving. Cada tema es una tarjeta
 * desplegable con su texto, imagen y alerta de seguridad.
 *
 * Fuente de referencia: material de la cursada (Escuela de Buceo Zeus - Apnea Nivel 1).
 */
@Composable
fun ContenidoApnea(modifier: Modifier = Modifier) {
    ListaDeTemas(secciones = SECCIONES_APNEA, modifier = modifier)
}

private val SECCIONES_APNEA = listOf(
    SeccionInfo(
        titulo = R.string.aprender_apnea_seccion_1,
        temas = listOf(
            TemaInfo(
                id = "apnea_que_es",
                titulo = R.string.aprender_apnea_que_es_titulo,
                cuerpo = R.string.aprender_apnea_que_es_cuerpo,
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_apnea_que_es_aviso_titulo_1,
                        cuerpo = R.string.aprender_apnea_que_es_aviso_cuerpo_1
                    )
                )
            ),
            TemaInfo(
                id = "apnea_reflejo",
                titulo = R.string.aprender_apnea_reflejo_titulo,
                cuerpo = R.string.aprender_apnea_reflejo_cuerpo
            ),
            TemaInfo(
                id = "apnea_respiracion",
                titulo = R.string.aprender_apnea_respiracion_titulo,
                cuerpo = R.string.aprender_apnea_respiracion_cuerpo,
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_apnea_respiracion_aviso_titulo_1,
                        cuerpo = R.string.aprender_apnea_respiracion_aviso_cuerpo_1
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = R.string.aprender_apnea_seccion_2,
        temas = listOf(
            TemaInfo(
                id = "apnea_mascara_traje",
                titulo = R.string.aprender_apnea_mascara_traje_titulo,
                cuerpo = R.string.aprender_apnea_mascara_traje_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_vision,
                        descripcion = R.string.aprender_apnea_mascara_traje_imagen_descripcion_1,
                        pie = R.string.aprender_apnea_mascara_traje_imagen_pie_1
                    )
                )
            ),
            TemaInfo(
                id = "apnea_aletas",
                titulo = R.string.aprender_apnea_aletas_titulo,
                cuerpo = R.string.aprender_apnea_aletas_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_aletas,
                        descripcion = R.string.aprender_apnea_aletas_imagen_descripcion_1,
                        pie = R.string.aprender_apnea_aletas_imagen_pie_1
                    )
                )
            ),
            TemaInfo(
                id = "apnea_lastre",
                titulo = R.string.aprender_apnea_lastre_titulo,
                cuerpo = R.string.aprender_apnea_lastre_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_flotabilidad,
                        descripcion = R.string.aprender_apnea_lastre_imagen_descripcion_1,
                        pie = R.string.aprender_apnea_lastre_imagen_pie_1
                    )
                )
            ),
            TemaInfo(
                id = "apnea_compensacion",
                titulo = R.string.aprender_apnea_compensacion_titulo,
                cuerpo = R.string.aprender_apnea_compensacion_cuerpo,
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_apnea_compensacion_aviso_titulo_1,
                        cuerpo = R.string.aprender_apnea_compensacion_aviso_cuerpo_1
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = R.string.aprender_apnea_seccion_3,
        temas = listOf(
            TemaInfo(
                id = "apnea_checklist",
                titulo = R.string.aprender_apnea_checklist_titulo,
                puntos = listOf(
                    R.string.aprender_apnea_checklist_punto_1,
                    R.string.aprender_apnea_checklist_punto_2,
                    R.string.aprender_apnea_checklist_punto_3,
                    R.string.aprender_apnea_checklist_punto_4,
                    R.string.aprender_apnea_checklist_punto_5
                ),
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_apnea_checklist_aviso_titulo_1,
                        cuerpo = R.string.aprender_apnea_checklist_aviso_cuerpo_1
                    )
                )
            )
        )
    )
)