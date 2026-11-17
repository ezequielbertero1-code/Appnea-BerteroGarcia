package com.berterogarcia.appnea.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.berterogarcia.appnea.R
import kotlinx.serialization.Serializable

/**
 * Rutas tipadas de la app. Navigation arma el texto de la ruta y los argumentos solo
 * (antes eran strings como "detalle_inmersion/{id}" que había que mantener a mano).
 */
sealed interface Ruta {
    // ---------------- Acceso ----------------
    @Serializable data object Login : Ruta
    @Serializable data object Registro : Ruta
    /** Cuenta de Google que entra por primera vez: el registro empieza con los datos ya cargados. */
    @Serializable data class RegistroGoogle(val email: String, val nombre: String, val apellido: String) : Ruta

    // ---------------- Pestañas (barra de navegación) ----------------
    @Serializable data object Inicio : Ruta
    @Serializable data object Entrenamiento : Ruta
    @Serializable data object Inmersiones : Ruta
    @Serializable data object Aprender : Ruta
    @Serializable data object Cuenta : Ruta

    // ---------------- Pantallas abiertas desde una pestaña ----------------
    @Serializable data object Condiciones : Ruta
    @Serializable data object CrearTabla : Ruta
    @Serializable data object NuevaInmersion : Ruta
    @Serializable data class DetalleInmersion(val id: Long) : Ruta
    @Serializable data class EditarInmersion(val id: Long) : Ruta
    /** pestania: 0 = Inmersiones, 1 = Entrenamiento (PESTANIA_ENTRENAMIENTO). */
    @Serializable data class Estadisticas(val pestania: Int = 0) : Ruta

    // ---------------- Perfil. id = 0: alta; id > 0: edición ----------------
    @Serializable data object DatosPersonales : Ruta
    @Serializable data object Equipamiento : Ruta
    @Serializable data class FormEquipo(val id: Long = 0) : Ruta
    @Serializable data object Certificados : Ruta
    @Serializable data class FormCertificado(val id: Long = 0) : Ruta
    @Serializable data object Contactos : Ruta
    @Serializable data class FormContacto(val id: Long = 0) : Ruta
}

/** id de un formulario de perfil: null si es un alta. */
fun idEdicion(id: Long): Long? = id.takeIf { it > 0 }

/** Pestañas de la barra de navegación, en orden. */
enum class AppDestinations(
    val ruta: Ruta,
    @param:StringRes val etiqueta: Int,
    @param:DrawableRes val icono: Int,
    @param:DrawableRes val iconoElegido: Int,     // variante rellena (Material Symbols) de la pestaña abierta
) {
    /** Clima del día, últimas inmersiones y accesos rápidos. */
    INICIO(Ruta.Inicio, R.string.nav_inicio, R.drawable.ic_ms_home, R.drawable.ic_ms_home_relleno),
    /** Tablas de apnea y cronómetro libre. */
    ENTRENAMIENTO(Ruta.Entrenamiento, R.string.nav_entrenar, R.drawable.ic_ms_timer, R.drawable.ic_ms_timer_relleno),
    /** Bitácora unificada de buceo y apnea. */
    INMERSION(Ruta.Inmersiones, R.string.nav_inmersion, R.drawable.ic_ms_scuba_diving, R.drawable.ic_ms_scuba_diving_relleno),
    /** Contenido teórico de apnea y buceo. */
    APRENDER(Ruta.Aprender, R.string.nav_aprender, R.drawable.ic_ms_menu_book, R.drawable.ic_ms_menu_book_relleno),
    /** Perfil del usuario. */
    CUENTA(Ruta.Cuenta, R.string.nav_cuenta, R.drawable.ic_ms_person, R.drawable.ic_ms_person_relleno),
}

/** Formularios a pantalla completa: se oculta la barra de navegación (ver MainActivity). */
private val RUTAS_SIN_BARRA = listOf(
    Ruta.NuevaInmersion::class,
    Ruta.EditarInmersion::class,
    Ruta.CrearTabla::class,
    Ruta.DatosPersonales::class,
    Ruta.FormEquipo::class,
    Ruta.FormCertificado::class,
    Ruta.FormContacto::class
)

private val RUTAS_DE_ACCESO = listOf(Ruta.Login::class, Ruta.Registro::class, Ruta.RegistroGoogle::class)

fun NavDestination.esFormulario(): Boolean = RUTAS_SIN_BARRA.any { hasRoute(it) }

/** Login y registro: todavía no hay sesión, no se muestra la barra. */
fun NavDestination.esDeAcceso(): Boolean = RUTAS_DE_ACCESO.any { hasRoute(it) }
