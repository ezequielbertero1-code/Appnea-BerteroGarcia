package com.berterogarcia.appnea.ui.theme

import androidx.compose.ui.graphics.Color

// ============================ Paleta de la app ============================
// ÚNICO lugar con colores escritos a mano: el resto de la app usa estos nombres.
// (Los hex van en minúscula a propósito: así los reemplazos automáticos, que buscan
//  en mayúscula, nunca tocan este archivo.)

// ---- Fondos y superficies ----
val FondoArriba = Color(0xff103b53)            // arriba del degradado de fondo
val FondoAbajo = Color(0xff03141f)             // abajo del degradado de fondo
val FondoIntermedio = Color(0xff0a2a3d)        // diálogos / menús (esquema de Material)
val FondoTarjeta = Color(0xff03141f)
val FondoTarjetaResaltada = Color(0xff245e82)  // tarjeta destacada, botón principal

// ---- Acento ----
val Acento = Color(0xff16f7fb)                 // cian de la app

// ---- Textos (sobre fondo oscuro) ----
val TextoDeshabilitado = Color(0x61ffffff)     // blanco 38 %
val TextoSobreResaltada = Color(0xffbfe6f2)    // subtítulo sobre FondoTarjetaResaltada
val TextoChip = Color(0xffe3f2f7)

// ---- Velos y bordes (blanco translúcido sobre fondo oscuro) ----
val VeloSuave = Color(0x0fffffff)              // 6 %: fondo de campos sin foco, deshabilitados
val Divisor = Color(0x26ffffff)                // 15 %: líneas divisorias
val Contorno = Color(0x4dffffff)               // 30 %: contornos, grillas de gráficos

// ---- Estados / semáforo ----
val ColorBueno = Color(0xff4cd787)
val ColorRegular = Color(0xffffc857)
val ColorMalo = Color(0xffff6b6b)
val ColorBajamar = Color(0xffff8a65)
val ColorError = ColorMalo                     // mismo rojo, nombre para formularios
val ColorAdvertencia = Color(0xffffb74d)       // naranja: service pendiente, fuerza media…
val ColorEntrenaDescanso = Color(0xff735cdd)    // entrenamiento: fase de descanso y sesión completada (violeta)
val ColorEntrenaApnea = Color(0xff46acc2)       // entrenamiento: fase de apnea (celeste)
val ColorOk = Color(0xff81c784)                // verde suave: vigente, contraseña fuerte

// ---- Tarjetas de aviso de seguridad (fondo claro) ----
val AvisoFondo = Color(0xfffff3e0)
val AvisoAcento = Color(0xffe65100)
val AvisoTexto = Color(0xff5d4037)


// ---- Estilo "Appnea circular" (componentes de Material 3) ----
val TextoSobreAcentoFuerte = Color(0xff00373a)          // texto e ícono sobre botón cian relleno
val ContenedorPrimarioFin = Color(0xff163f5a)           // final del degradado de la tarjeta de Inicio
val ContenedorSecundario = Color(0xff1f4d6b)            // fecha de buceo, chips y segmentados elegidos
val TextoSobreContenedorSecundario = Color(0xffcfe9f8)
val ContenedorApnea = Color(0xff1b4a4b)                 // fecha de apnea (se distingue de buceo)
val TextoSobreContenedorApnea = Color(0xffbff7f2)
val SuperficieTonal = Color(0x12ffffff)                 // blanco 7 %: tarjetas y filas sobre el degradado
val TextoSuperficieVariante = Color(0xffa9c0cc)         // subtítulos de filas y datos secundarios
val SuperficieTonalAlta = Color(0x1cffffff)             // blanco 11 %: buscador, steppers, botón de ícono resaltado
val SuperficieTonalMaxima = Color(0x2bffffff)           // blanco 17 %: barras de progreso vacías, switch apagado
val TextoSuperficie = Color(0xffe6f1f6)                 // texto principal sobre el degradado
val TextoSobreContenedorPrimario = Color(0xffd6f3ff)    // texto sobre FondoTarjetaResaltada (FAB, avatar)
val ContornoFuerte = Color(0xff6a8493)                  // contorno M3: campos, botones outlined, chips
val FondoBarraNavegacion = Color(0xff051925)            // barra de navegación inferior
