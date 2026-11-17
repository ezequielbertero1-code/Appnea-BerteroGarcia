package com.berterogarcia.appnea.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.berterogarcia.appnea.R
import com.berterogarcia.appnea.dominio.modelo.Aptitud
import com.berterogarcia.appnea.dominio.modelo.MotivoAptitud
import com.berterogarcia.appnea.dominio.modelo.TipoMotivo
import com.berterogarcia.appnea.dominio.modelo.CategoriaEquipo
import com.berterogarcia.appnea.dominio.modelo.TipoCertificado
import com.berterogarcia.appnea.dominio.modelo.TipoInmersion
import com.berterogarcia.appnea.dominio.modelo.Visibilidad
import com.berterogarcia.appnea.ui.registro.FuerzaPassword

// ============================ Etiquetas de datos para mostrar ============================
// Los enums y textos de la capa de datos son CLAVES (algunos se guardan en la base, como la
// categoría del equipo dentro de cada inmersión o la fase lunar): no se cambian. Lo que se
// muestra en pantalla sale de acá, con strings.xml.

@Composable
fun TipoInmersion.nombre(): String = stringResource(
    when (this) {
        TipoInmersion.BUCEO -> R.string.tipo_buceo
        TipoInmersion.APNEA -> R.string.tipo_apnea
    }
)

@Composable
fun Visibilidad.nombre(): String = stringResource(
    when (this) {
        Visibilidad.NULA -> R.string.visibilidad_nula
        Visibilidad.MALA -> R.string.visibilidad_mala
        Visibilidad.BUENA -> R.string.visibilidad_buena
        Visibilidad.MUY_BUENA -> R.string.visibilidad_muy_buena
        Visibilidad.EXCELENTE -> R.string.visibilidad_excelente
    }
)

@Composable
fun CategoriaEquipo.nombre(): String = stringResource(
    when (this) {
        CategoriaEquipo.MASCARA -> R.string.categoria_mascara
        CategoriaEquipo.SNORKEL -> R.string.categoria_snorkel
        CategoriaEquipo.ALETAS -> R.string.categoria_aletas
        CategoriaEquipo.TRAJE -> R.string.categoria_traje
        CategoriaEquipo.GUANTES_ESCARPINES -> R.string.categoria_guantes
        CategoriaEquipo.LASTRE -> R.string.categoria_lastre
        CategoriaEquipo.COMPUTADORA -> R.string.categoria_computadora
        CategoriaEquipo.REGULADOR -> R.string.categoria_regulador
        CategoriaEquipo.CHALECO -> R.string.categoria_chaleco
        CategoriaEquipo.TUBO -> R.string.categoria_tubo
        CategoriaEquipo.BOYA -> R.string.categoria_boya
        CategoriaEquipo.CUCHILLO -> R.string.categoria_cuchillo
        CategoriaEquipo.LINTERNA -> R.string.categoria_linterna
        CategoriaEquipo.OTRO -> R.string.categoria_otro
    }
)

@Composable
fun TipoCertificado.nombre(): String = stringResource(
    when (this) {
        TipoCertificado.BUCEO -> R.string.certificado_buceo
        TipoCertificado.APNEA -> R.string.certificado_apnea
        TipoCertificado.PRIMEROS_AUXILIOS -> R.string.certificado_primeros_auxilios
        TipoCertificado.APTO_MEDICO -> R.string.certificado_apto_medico
        TipoCertificado.OTRO -> R.string.certificado_otro
    }
)

/** "Buenas" / "Regulares" / "Malas" (para "… para entrar al agua"). */
@Composable
fun Aptitud.nombre(): String = stringResource(
    when (this) {
        Aptitud.BUENA -> R.string.aptitud_buena
        Aptitud.REGULAR -> R.string.aptitud_regular
        Aptitud.MALA -> R.string.aptitud_mala
    }
)

/** Motivo del semáforo para entrar al agua ("Olas de 1,2 m", "Lluvia"…). */
@Composable
fun MotivoAptitud.texto(): String {
    val v = valor.orEmpty()
    return when (tipo) {
        TipoMotivo.OLAS -> stringResource(R.string.motivo_olas, v)
        TipoMotivo.VIENTO -> stringResource(R.string.motivo_viento, v)
        TipoMotivo.RAFAGAS -> stringResource(R.string.motivo_rafagas, v)
        TipoMotivo.TORMENTA -> stringResource(R.string.motivo_tormenta)
        TipoMotivo.LLUVIA -> stringResource(R.string.motivo_lluvia)
        TipoMotivo.NIEBLA -> stringResource(R.string.motivo_niebla)
        TipoMotivo.CORRIENTE -> stringResource(R.string.motivo_corriente, v)
        TipoMotivo.TRANQUILO -> stringResource(R.string.motivo_tranquilo)
    }
}

@Composable
fun FuerzaPassword.nombre(): String = when (this) {
    FuerzaPassword.VACIA -> ""
    FuerzaPassword.DEBIL -> stringResource(R.string.fuerza_debil)
    FuerzaPassword.MEDIA -> stringResource(R.string.fuerza_media)
    FuerzaPassword.FUERTE -> stringResource(R.string.fuerza_fuerte)
}

/**
 * Nombre de la fase lunar para mostrar. La fase se guarda como texto en cada inmersión
 * (CondicionesRegistro.faseLunar), así que se traduce desde esa clave; si no se reconoce
 * (registros viejos), se muestra tal cual.
 */
@Composable
fun nombreFaseLunar(fase: String): String = when (fase) {
    "Luna nueva" -> stringResource(R.string.fase_luna_nueva)
    "Creciente" -> stringResource(R.string.fase_luna_creciente)
    "Cuarto creciente" -> stringResource(R.string.fase_luna_cuarto_creciente)
    "Gibosa creciente" -> stringResource(R.string.fase_luna_gibosa_creciente)
    "Luna llena" -> stringResource(R.string.fase_luna_llena)
    "Gibosa menguante" -> stringResource(R.string.fase_luna_gibosa_menguante)
    "Cuarto menguante" -> stringResource(R.string.fase_luna_cuarto_menguante)
    "Menguante" -> stringResource(R.string.fase_luna_menguante)
    else -> fase
}

/** Descripción del cielo para un código WMO ("Parcialmente nublado"). */
@Composable
fun descripcionClima(codigo: Int): String = stringResource(
    when (codigo) {
        0 -> R.string.clima_despejado
        1 -> R.string.clima_mayormente_despejado
        2 -> R.string.clima_parcialmente_nublado
        3 -> R.string.clima_nublado
        45, 48 -> R.string.clima_niebla
        51, 53, 55 -> R.string.clima_llovizna
        56, 57 -> R.string.clima_llovizna_helada
        61, 63, 65 -> R.string.clima_lluvia
        66, 67 -> R.string.clima_lluvia_helada
        71, 73, 75 -> R.string.clima_nieve
        77 -> R.string.clima_granizo_fino
        80, 81, 82 -> R.string.clima_chubascos
        85, 86 -> R.string.clima_chubascos_nieve
        95 -> R.string.clima_tormenta
        96, 99 -> R.string.clima_tormenta_granizo
        else -> R.string.detalle_sin_datos
    }
)
