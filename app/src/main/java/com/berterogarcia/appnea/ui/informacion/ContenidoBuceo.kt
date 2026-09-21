package com.berterogarcia.appnea.ui.informacion

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.berterogarcia.appnea.R

/**
 * Contenido informativo básico sobre buceo autónomo / Scuba Open Water, con imágenes
 * ilustrativas y prevención intercalada en cada tema.
 *
 * Fuente de referencia: material de la cursada (Escuela de Buceo Zeus - Open Water Diver,
 * Manual CMAS y especialidad de Flotabilidad).
 */
@Composable
fun ContenidoBuceo(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            TarjetaInfo(
                titulo = "¿Qué es el buceo autónomo (SCUBA)?",
                cuerpo = "SCUBA significa \"Self-Contained Underwater Breathing Apparatus\": un " +
                    "equipo autónomo de respiración submarina. Se popularizó en 1943, cuando " +
                    "Jacques Cousteau y Émile Gagnan desarrollaron el regulador a demanda, que " +
                    "entrega aire a la presión adecuada en cada respiración. A diferencia de la " +
                    "apnea, el buceador respira aire comprimido y puede permanecer bajo el agua " +
                    "por mucho más tiempo, pero eso trae riesgos propios relacionados con la " +
                    "presión que hay que conocer y respetar."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_fisica,
                descripcion = "Física básica del buceo: presión con la profundidad",
                pie = "Por cada 10 metros de profundidad, la presión aumenta 1 bar (atmósfera) " +
                    "más. El mayor cambio relativo de volumen del aire ocurre entre 0 y 10 " +
                    "metros: por eso hay que compensar desde el inicio del descenso."
            )
        }

        item {
            TarjetaInfo(
                titulo = "Flotabilidad",
                cuerpo = "El Principio de Arquímedes dice que todo cuerpo sumergido recibe un " +
                    "empuje hacia arriba igual al peso del líquido que desplaza. Un buceador " +
                    "puede tener flotabilidad positiva (tiende a flotar), negativa (tiende a " +
                    "hundirse) o neutra (no flota ni se hunde, queda suspendido). El objetivo " +
                    "es lograr flotabilidad positiva en superficie y neutra en profundidad, " +
                    "usando el chaleco compensador (BCD) y el cinturón de lastre."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_chaleco,
                descripcion = "Chaleco compensador de flotabilidad (BCD)",
                pie = "El chaleco compensador de flotabilidad (BCD / \"chaleco\") permite " +
                    "ajustar la flotabilidad inflando o desinflando aire durante la inmersión."
            )
        }

        item {
            TarjetaInfo(
                titulo = "Equipo básico",
                cuerpo = "Además de máscara, aletas y traje de neopreno (compartidos con la " +
                    "apnea), el buceo autónomo suma: botella de aire comprimido, regulador " +
                    "(primera y segunda etapa, entrega el aire a la presión correcta), " +
                    "manómetro (indica cuánto aire queda), chaleco compensador y, " +
                    "generalmente, una computadora u ordenador de buceo con profundímetro."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_equipo_botella,
                descripcion = "Botella de aire comprimido",
                pie = "La botella se transporta siempre en posición vertical con la salida de " +
                    "aire enfrentada al lado contrario del buceador, y horizontal cuando no la " +
                    "lleva puesta."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_accesorios,
                descripcion = "Accesorios de buceo",
                pie = "Accesorios recomendados: profundímetro/computadora, cuchillo, brújula, " +
                    "linterna, silbato y boya/bandera de señalización para marcar la posición " +
                    "del buceador en superficie."
            )
        }

        item {
            TarjetaSeguridad(
                titulo = "Revisá el equipo antes de cada inmersión",
                cuerpo = "Comprobá el estado de los o-rings, que el regulador entregue aire " +
                    "correctamente en ambas etapas, la carga de la botella en el manómetro y " +
                    "el funcionamiento del inflador/purga del chaleco. Nunca entres al agua con " +
                    "una duda sobre el equipo sin resolverla antes."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_entrada_agua,
                descripcion = "Técnicas de entrada al agua",
                pie = "Existen distintas técnicas de entrada al agua (desde la orilla, la " +
                    "playa, un barco o una embarcación ligera). La elección depende del punto " +
                    "de partida y de las condiciones del mar, siempre priorizando la seguridad."
            )
        }

        item {
            TituloSeccion("Presión y accidentes: lo esencial")
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_respiratorio,
                descripcion = "Aparato respiratorio",
                pie = "El aire respirado en profundidad está a mayor presión. Al ascender, ese " +
                    "aire dentro de los pulmones se expande: por eso nunca hay que contener la " +
                    "respiración durante el ascenso."
            )
        }

        item {
            TarjetaSeguridad(
                titulo = "Nunca contener la respiración al ascender",
                cuerpo = "Si se retiene el aire durante el ascenso, los pulmones se " +
                    "sobredistienden y pueden desgarrarse, provocando barotrauma pulmonar y, en " +
                    "el peor caso, una embolia gaseosa (burbujas de aire que pasan a la sangre y " +
                    "pueden llegar al cerebro). Los síntomas incluyen mareo, visión borrosa, " +
                    "dificultad para respirar y hasta parálisis. Ante cualquiera de estos " +
                    "síntomas al salir del agua: oxígeno inmediato y traslado urgente a un " +
                    "centro con cámara hiperbárica."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_embolia,
                descripcion = "Accidentes de sobrepresión pulmonar",
                pie = "Los riesgos de sobrepresión pulmonar son mayores en los últimos 10 " +
                    "metros de ascenso, donde el aire se expande proporcionalmente más rápido."
            )
        }

        item {
            TarjetaInfo(
                titulo = "Enfermedad por descompresión (DCS)",
                cuerpo = "Durante la inmersión, el cuerpo absorbe nitrógeno del aire respirado. " +
                    "Si el ascenso es demasiado rápido, ese nitrógeno puede formar burbujas en " +
                    "la sangre y los tejidos (\"enfermedad de los buzos\"), afectando la piel, " +
                    "las articulaciones, la médula espinal o el cerebro, con síntomas que van " +
                    "desde erupciones y dolor articular hasta hormigueo, dificultad para " +
                    "moverse o parálisis."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_dcs_cuerpo,
                descripcion = "Zonas del cuerpo afectadas por la enfermedad por descompresión",
                pie = "La enfermedad por descompresión puede afectar la piel, las " +
                    "articulaciones, la médula espinal o el cerebro, según dónde se formen las " +
                    "burbujas de nitrógeno."
            )
        }

        item {
            ImagenIlustrativa(
                resId = R.drawable.buceo_ascenso,
                descripcion = "Ascenso controlado",
                pie = "El chaleco debe desinflarse gradualmente durante el ascenso, ya que el " +
                    "aire dentro se expande y aumenta la flotabilidad: hay que controlar la " +
                    "velocidad en todo momento y nunca dejar de respirar con normalidad."
            )
        }

        item {
            TituloSeccion("Reglas de oro del buceo seguro")
        }

        item {
            TarjetaSeguridad(
                titulo = "Antes de bucear",
                cuerpo = "Buen estado físico y descanso, buena hidratación, sin alcohol ni " +
                    "esfuerzo físico excesivo previo. Planificá la inmersión con tu compañero/a " +
                    "(profundidad, tiempo y procedimientos de emergencia) y respetá siempre el " +
                    "límite de profundidad de tu certificación (18 m para Open Water)."
            )
        }

        item {
            ListaDePuntos(
                puntos = listOf(
                    "Bucear siempre en pareja (buddy system), nunca en solitario.",
                    "Respetar las tablas de buceo y no exceder los tiempos de fondo.",
                    "Ascender a no más de 10 metros por minuto.",
                    "Hacer la parada de seguridad de 3 minutos a 3-5 metros.",
                    "Ecualizar los oídos de forma frecuente durante todo el descenso.",
                    "No bucear con apnea el mismo día después de bucear con botellas.",
                    "No tomar un avión ni subir a mucha altura tras bucear.",
                    "Ante cualquier síntoma extraño al salir del agua, pedir ayuda de inmediato."
                )
            )
        }
    }
}
