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
        titulo = "General",
        temas = listOf(
            TemaInfo(
                id = "apnea_que_es",
                titulo = "¿Qué es la apnea?",
                cuerpo = "La apnea (o freediving) es bucear conteniendo la respiración, sin " +
                        "equipo autónomo. Se entrena principalmente en tres disciplinas: STA " +
                        "(apnea estática, aguantar inmóvil el mayor tiempo posible en superficie), " +
                        "DYN (apnea dinámica, nadar la mayor distancia posible bajo el agua) y " +
                        "CWT (peso constante, alcanzar la mayor profundidad posible con aletas). " +
                        "Es una disciplina que combina relajación, técnica respiratoria y control mental.",
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = "Regla de oro: nunca solo/a",
                        cuerpo = "La apnea NUNCA se practica en soledad. Siempre debe haber un " +
                                "compañero/a que observe activamente (\"uno abajo, uno mirando desde la " +
                                "superficie\"), listo para asistir ante una pérdida de conocimiento, que " +
                                "puede ocurrir sin previo aviso y sin dolor."
                    )
                )
            ),
            TemaInfo(
                id = "apnea_reflejo",
                titulo = "El reflejo de inmersión de los mamíferos",
                cuerpo = "Al sumergirnos, el cuerpo activa una serie de respuestas fisiológicas " +
                        "que ayudan a ahorrar oxígeno: bradicardia (baja la frecuencia cardíaca), " +
                        "esplenocontracción (el bazo libera glóbulos rojos extra), vasoconstricción " +
                        "periférica (la sangre se retira de brazos y piernas) y el \"blood shift\" " +
                        "(la sangre se concentra en el pecho para proteger los pulmones de la " +
                        "presión). Este reflejo es más fuerte cuanto más frío el agua en contacto " +
                        "con la cara, y se entrena y mejora con la práctica."
            ),
            TemaInfo(
                id = "apnea_respiracion",
                titulo = "Respiración: antes y después de la apnea",
                cuerpo = "La respiración previa debe ser lenta, relajada y consciente (por " +
                        "ejemplo: inhalar 4 segundos, exhalar 7-8 segundos), con 2 a 4 " +
                        "respiraciones completas antes de la última inhalación total. Al salir a " +
                        "superficie, la respiración de recuperación es clave: inhalar completo, " +
                        "sostener 2-3 segundos y exhalar en forma pasiva. Una buena recuperación " +
                        "ayuda a prevenir la samba (pérdida de control motor) y el síncope " +
                        "(desmayo por hipoxia).",
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = "Nunca hiperventilar",
                        cuerpo = "Respirar rápido y en exceso antes de sumergirse (hiperventilar) NO " +
                                "aumenta el oxígeno disponible: sólo baja el CO₂, que es la señal que " +
                                "nuestro cuerpo usa para avisarnos que hay que respirar. El resultado es " +
                                "que la necesidad de respirar aparece tarde, cuando el nivel de oxígeno ya " +
                                "puede ser peligrosamente bajo, aumentando el riesgo de desmayo bajo el " +
                                "agua. Señales de alerta: mareos, hormigueo en los labios, entumecimiento " +
                                "de las extremidades, problemas auditivos o pérdida del equilibrio."
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = "Equipamiento básico",
        temas = listOf(
            TemaInfo(
                id = "apnea_mascara_traje",
                titulo = "Máscara, snorkel y traje",
                cuerpo = "La máscara de apnea suele tener bajo volumen interno (facilita la " +
                        "compensación). El snorkel se usa en superficie y se retira antes de " +
                        "descender. El traje de neopreno aísla del frío; debe quedar ajustado pero " +
                        "sin oprimir, especialmente en el diafragma, para no dificultar la " +
                        "respiración ni gastar energía de más.",
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_vision,
                        descripcion = "Visión y color bajo el agua a través de la máscara",
                        pie = "Al pasar del agua al aire de la máscara la luz se dobla (refracción): " +
                                "los objetos se ven un 33 % más grandes y un 25 % más cerca. Además, " +
                                "con la profundidad los colores cálidos (rojo, naranja, amarillo) " +
                                "desaparecen primero y quedan solo azules y violetas (absorción)."
                    )
                )
            ),
            TemaInfo(
                id = "apnea_aletas",
                titulo = "Aletas",
                cuerpo = "Las aletas de apnea tienen pala larga y flexible, distinta a las de " +
                        "buceo con botellas: transmiten mejor la energía de la pierna y permiten " +
                        "recorrer grandes distancias durante mucho tiempo sin cansarse. Hay dos " +
                        "tipos: de pie completo, con forma de zapato y talles estándar, y de talón " +
                        "abierto, en talles chico a extra grande, que se sujetan con una correa ajustable.",
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_aletas,
                        descripcion = "Partes de una aleta de apnea",
                        pie = "1. Pala  2. Rail para estabilizar la pala  3. Espacio para los pies."
                    )
                )
            ),
            TemaInfo(
                id = "apnea_lastre",
                titulo = "Cinturón de lastre, computadora y cuchillo",
                cuerpo = "El cinturón de lastre compensa la flotabilidad extra del traje y " +
                        "debe tener una hebilla de zafado rápido, liberable con un solo " +
                        "movimiento. La computadora de apnea permite controlar el tiempo bajo el " +
                        "agua y el tiempo de recuperación en superficie. El cuchillo se usa del " +
                        "lado interno de la pierna (nunca en el brazo) para poder liberarse rápido " +
                        "ante un enganche.",
                imagenes = listOf(
                    ImagenTema(
                        resId = R.drawable.apnea_flotabilidad,
                        descripcion = "Flotabilidad positiva, negativa y neutra",
                        pie = "Flotabilidad positiva, negativa y neutra. En apnea se suele lastrar " +
                                "apenas lo necesario para tener flotabilidad neutra alrededor de los -10 m, " +
                                "y positiva en superficie."
                    )
                )
            ),
            TemaInfo(
                id = "apnea_compensacion",
                titulo = "Compensación de oídos",
                cuerpo = "Al descender aumenta la presión sobre los oídos y los senos " +
                        "paranasales. Se compensa con la maniobra de Valsalva (pinzar la nariz y " +
                        "soplar suave) o la de Frenzel (bloquear la nariz, emitir el sonido de la " +
                        "letra \"K\" y tragar). La compensación debe hacerse de forma temprana y " +
                        "frecuente durante todo el descenso, nunca de forma brusca o forzada.",
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = "Si no compensás, no sigas bajando",
                        cuerpo = "Nunca practiques apnea estando resfriado/a, con gripe o congestión: " +
                                "aumenta mucho el riesgo de barotrauma de oído. Si en algún momento no " +
                                "podés compensar, hay que detener el descenso en el momento; forzarlo " +
                                "puede lesionar el oído o los senos."
                    )
                )
            )
        )
    ),
    SeccionInfo(
        titulo = "Reglas de oro de la apnea segura",
        temas = listOf(
            TemaInfo(
                id = "apnea_checklist",
                titulo = "Checklist de seguridad",
                puntos = listOf(
                    "No hiperventilar antes de sumergirse.",
                    "Descender y ascender sin apurarse, respetando las señales del cuerpo.",
                    "Reconocer los signos de samba y síncope, y saber asistir a un compañero en superficie.",
                    "Nunca forzar más allá de las primeras contracciones diafragmáticas fuertes.",
                    "Hacer siempre una respiración de recuperación completa al salir a superficie."
                ),
                avisos = listOf(
                    AvisoSeguridad(
                        titulo = "Antes de cada sesión",
                        cuerpo = "Practicá siempre acompañado/a, en un lugar conocido y seguro. " +
                                "Esperá al menos 12 horas después de haber buceado con botellas antes de " +
                                "hacer apnea (24 horas si fueron varias inmersiones o inmersiones " +
                                "profundas), para eliminar el exceso de nitrógeno del cuerpo. Nunca " +
                                "respires de un equipo autónomo (botella) bajo el agua durante una sesión " +
                                "de apnea."
                    )
                )
            )
        )
    )
)