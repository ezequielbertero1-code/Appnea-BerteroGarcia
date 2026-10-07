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
val FondoPeligro = Color(0xff7a1f1f)           // botón "Abortar"

// ---- Acento ----
val Acento = Color(0xff16f7fb)                 // cian de la app
val TextoSobreAcento = Color(0xff0a3049)       // tilde del checkbox sobre cian

// ---- Textos (sobre fondo oscuro) ----
val TextoSecundario = Color(0xbfffffff)        // blanco 75 %
val TextoTerciario = Color(0xb3ffffff)         // blanco 70 %: etiquetas, ejes, íconos sin foco
val TextoDeshabilitado = Color(0x61ffffff)     // blanco 38 %
val TextoSobreResaltada = Color(0xffbfe6f2)    // subtítulo sobre FondoTarjetaResaltada
val TextoChip = Color(0xffe3f2f7)

// ---- Velos y bordes (blanco translúcido sobre fondo oscuro) ----
val VeloSuave = Color(0x0fffffff)              // 6 %: fondo de campos sin foco, deshabilitados
val VeloMedio = Color(0x14ffffff)              // 8 %: botón secundario, chips, buscador
val VeloFuerte = Color(0x1affffff)             // 10 %: fondo de campos con foco
val Divisor = Color(0x26ffffff)                // 15 %: líneas divisorias
val Contorno = Color(0x4dffffff)               // 30 %: contornos, grillas de gráficos
val BordeSuave = Color(0x66ffffff)             // 40 %: borde de campos sin foco, placeholder

// ---- Estados / semáforo ----
val ColorBueno = Color(0xff4cd787)
val ColorRegular = Color(0xffffc857)
val ColorMalo = Color(0xffff6b6b)
val ColorBajamar = Color(0xffff8a65)
val ColorError = ColorMalo                     // mismo rojo, nombre para formularios
val ColorAdvertencia = Color(0xffffb74d)       // naranja: service pendiente, fuerza media…
val ColorOk = Color(0xff81c784)                // verde suave: vigente, contraseña fuerte

// ---- Tarjetas de aviso de seguridad (fondo claro) ----
val AvisoFondo = Color(0xfffff3e0)
val AvisoAcento = Color(0xffe65100)
val AvisoTexto = Color(0xff5d4037)