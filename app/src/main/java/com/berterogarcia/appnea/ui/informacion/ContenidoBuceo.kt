package com.berterogarcia.appnea.ui.informacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.berterogarcia.appnea.R

/**
 * Contenido informativo básico sobre buceo autónomo / Scuba Open Water. Cada tema es una
 * tarjeta desplegable con su texto, imágenes y alerta de seguridad.
 *
 * Fuente de referencia: material de la cursada (Escuela de Buceo Zeus - Open Water Diver,
 * Manual CMAS y especialidad de Flotabilidad).
 */
@Composable
fun ContenidoBuceo(modifier: Modifier = Modifier) {
    ListaDeTemas(secciones = SECCIONES_BUCEO, modifier = modifier)
}

private val SECCIONES_BUCEO = listOf(
    SeccionInfo(
        titulo = R.string.aprender_buceo_seccion_1,
        temas = listOf(
            TemaInfo(
                id = "buceo_que_es",
                titulo = R.string.aprender_buceo_que_es_titulo,
                cuerpo = R.string.aprender_buceo_que_es_cuerpo,
            ),
            TemaInfo(
                id = "buceo_flotabilidad",
                titulo = R.string.aprender_buceo_flotabilidad_titulo,
                cuerpo = R.string.aprender_buceo_flotabilidad_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_chaleco,
                        descripcion = R.string.aprender_buceo_flotabilidad_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_flotabilidad_imagen_pie_1
                    )
                )
            ),
            TemaInfo(
                id = "buceo_equipo",
                titulo = R.string.aprender_buceo_equipo_titulo,
                cuerpo = R.string.aprender_buceo_equipo_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_equipo_botella,
                        descripcion = R.string.aprender_buceo_equipo_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_equipo_imagen_pie_1
                    ),
                    ImagenTema(
                        resId = R.drawable.buceo_accesorios,
                        descripcion = R.string.aprender_buceo_equipo_imagen_descripcion_2,
                        pie = R.string.aprender_buceo_equipo_imagen_pie_2
                    ),
                ),
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_buceo_equipo_aviso_titulo_1,
                        cuerpo = R.string.aprender_buceo_equipo_aviso_cuerpo_1
                    )
                )
            ),
            TemaInfo(
                id = "buceo_entrada",
                titulo = R.string.aprender_buceo_entrada_titulo,
                cuerpo = R.string.aprender_buceo_entrada_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_entrada_agua,
                        descripcion = R.string.aprender_buceo_entrada_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_entrada_imagen_pie_1
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = R.string.aprender_buceo_seccion_2,
        temas = listOf(
            TemaInfo(
                id = "buceo_presion",
                titulo = R.string.aprender_buceo_presion_titulo,
                cuerpo = R.string.aprender_buceo_presion_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_fisica,
                        descripcion = R.string.aprender_buceo_presion_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_presion_imagen_pie_1
                    )
                )
            ),
            TemaInfo(
                id = "buceo_sobrepresion",
                titulo = R.string.aprender_buceo_sobrepresion_titulo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_respiratorio,
                        descripcion = R.string.aprender_buceo_sobrepresion_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_sobrepresion_imagen_pie_1
                    ),
                    ImagenTema(
                        resId = R.drawable.buceo_embolia,
                        descripcion = R.string.aprender_buceo_sobrepresion_imagen_descripcion_2,
                        pie = R.string.aprender_buceo_sobrepresion_imagen_pie_2
                    )
                ),
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_buceo_sobrepresion_aviso_titulo_1,
                        cuerpo = R.string.aprender_buceo_sobrepresion_aviso_cuerpo_1
                    )
                )
            ),
            TemaInfo(
                id = "buceo_dcs",
                titulo = R.string.aprender_buceo_dcs_titulo,
                cuerpo = R.string.aprender_buceo_dcs_cuerpo,
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.buceo_dcs_cuerpo,
                        descripcion = R.string.aprender_buceo_dcs_imagen_descripcion_1,
                        pie = R.string.aprender_buceo_dcs_imagen_pie_1
                    ),
                    ImagenTema(
                        resId = R.drawable.buceo_ascenso,
                        descripcion = R.string.aprender_buceo_dcs_imagen_descripcion_2,
                        pie = R.string.aprender_buceo_dcs_imagen_pie_2
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = R.string.aprender_buceo_seccion_3,
        temas = listOf(
            TemaInfo(
                id = "buceo_checklist",
                titulo = R.string.aprender_buceo_checklist_titulo,
                puntos = listOf(
                    R.string.aprender_buceo_checklist_punto_1,
                    R.string.aprender_buceo_checklist_punto_2,
                    R.string.aprender_buceo_checklist_punto_3,
                    R.string.aprender_buceo_checklist_punto_4,
                    R.string.aprender_buceo_checklist_punto_5,
                    R.string.aprender_buceo_checklist_punto_6,
                    R.string.aprender_buceo_checklist_punto_7,
                    R.string.aprender_buceo_checklist_punto_8
                ),
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = R.string.aprender_buceo_checklist_aviso_titulo_1,
                        cuerpo = R.string.aprender_buceo_checklist_aviso_cuerpo_1
                    )
                )
            )
        )
    )
)
