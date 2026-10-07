package com.berterogarcia.appnea.ui.perfil.comun

import androidx.compose.ui.graphics.Color
import com.berterogarcia.appnea.ui.theme.ColorAdvertencia
import com.berterogarcia.appnea.ui.theme.ColorError
import com.berterogarcia.appnea.ui.theme.ColorOk
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Estado de una fecha límite (vencimiento de certificado / apto médico, próximo service).
 * Se calcula al mostrar, NO se guarda en la base (cambia solo con el paso de los días).
 */
enum class EstadoVencimiento { SIN_FECHA, VIGENTE, POR_VENCER, VENCIDO }

const val DIAS_AVISO_VENCIMIENTO = 30L

fun estadoVencimiento(
    fecha: LocalDate?,
    hoy: LocalDate = LocalDate.now(),
    diasAviso: Long = DIAS_AVISO_VENCIMIENTO
): EstadoVencimiento = when {
    fecha == null -> EstadoVencimiento.SIN_FECHA
    fecha.isBefore(hoy) -> EstadoVencimiento.VENCIDO
    ChronoUnit.DAYS.between(hoy, fecha) <= diasAviso -> EstadoVencimiento.POR_VENCER
    else -> EstadoVencimiento.VIGENTE
}

/** Etiqueta + color para mostrar como insignia. null = no mostrar nada. */
data class Insignia(val texto: String, val color: Color)


fun insigniaVencimiento(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): Insignia? =
    when (estadoVencimiento(fecha, hoy)) {
        EstadoVencimiento.SIN_FECHA -> null
        EstadoVencimiento.VENCIDO -> Insignia("Vencido", ColorError)
        EstadoVencimiento.POR_VENCER -> {
            val dias = fecha?.let { ChronoUnit.DAYS.between(hoy, it) } ?: 0L
            val texto = when (dias) {
                0L -> "Vence hoy"
                1L -> "Vence mañana"
                else -> "Vence en $dias días"
            }
            Insignia(texto, ColorAdvertencia)
        }
        EstadoVencimiento.VIGENTE -> Insignia("Vigente", ColorOk)
    }

fun insigniaMantenimiento(fecha: LocalDate?, hoy: LocalDate = LocalDate.now()): Insignia? =
    when (estadoVencimiento(fecha, hoy)) {
        EstadoVencimiento.SIN_FECHA, EstadoVencimiento.VIGENTE -> null
        EstadoVencimiento.VENCIDO -> Insignia("Service atrasado", ColorError)
        EstadoVencimiento.POR_VENCER -> Insignia("Service pronto", ColorAdvertencia)
    }
