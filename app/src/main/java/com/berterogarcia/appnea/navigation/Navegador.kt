package com.berterogarcia.appnea.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/** Abre una pantalla. launchSingleTop: un doble toque no la abre dos veces. */
fun NavHostController.abrir(ruta: Ruta) = navigate(ruta) { launchSingleTop = true }

/**
 * Vuelve atrás solo si la pantalla que lo pide sigue ARRIBA de la pila. Evita que un
 * doble toque en "volver" (o volver + guardado al mismo tiempo) saque DOS pantallas.
 */
fun NavHostController.volverDesde(entrada: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entrada.id) popBackStack()
}

/** Después del login o del registro: va a Inicio y limpia la pila (no se vuelve atrás al login). */
fun NavHostController.entrarALaApp() = navigate(Ruta.Inicio) {
    popUpTo(graph.id) { inclusive = true }
}

/**
 * Pestaña a la que pertenece la pantalla actual: la raíz de pestaña (además de Inicio) que
 * esté en la pila; si no hay ninguna, Inicio. Ej.: detalle abierto desde Inmersión → INMERSION;
 * detalle o Condiciones abiertos desde Inicio → INICIO; pantallas del perfil → CUENTA.
 */
fun NavHostController.pestaniaAbierta(): AppDestinations =
    AppDestinations.entries
        .firstOrNull { it != AppDestinations.INICIO && estaEnLaPila(it.ruta) }
        ?: AppDestinations.INICIO

/**
 * Navegación entre pestañas (barra inferior y accesos de Inicio).
 *  - Tocar la pestaña en la que ya estás (o una pantalla abierta desde ella, ej. un detalle)
 *    te devuelve a su pantalla principal.
 *  - Cambiar de pestaña guarda la que dejás y restaura la que abrís (scroll, detalle abierto…).
 *  - Inicio siempre vuelve "limpio": nunca restaura lo que estaba abierto arriba suyo.
 */
fun NavHostController.irAPestania(destino: AppDestinations) {
    if (destino == pestaniaAbierta()) {
        // Re-seleccionar la pestaña actual: cierra lo que haya arriba de su raíz
        popBackStack(destino.ruta, inclusive = false)
        return
    }

    if (!estaEnLaPila(Ruta.Inicio)) {
        // No debería pasar (Inicio es siempre la raíz después del login), pero si pasa,
        // se arranca una pila nueva en vez de dejarla rota.
        navigate(destino.ruta) { popUpTo(graph.id) { inclusive = true } }
        return
    }

    navigate(destino.ruta) {
        // Se usa Inicio como raíz y no graph.findStartDestination(): si entraste por el login,
        // el destino inicial del grafo es el login, que ya no está en la pila.
        popUpTo(Ruta.Inicio) { saveState = true }
        launchSingleTop = true
        restoreState = destino != AppDestinations.INICIO
    }
}

/**
 * Cierre de sesión: vuelve al login descartando TODO lo de la cuenta anterior.
 *
 * irAPestania guarda las pestañas que dejás (saveState = true), y ese estado guardado incluye
 * sus ViewModels. popUpTo(graph.id) solo saca la pila actual, no lo guardado: al entrar con otra
 * cuenta se restauraban los ViewModels viejos (el cronómetro seguía corriendo y la bitácora
 * mostraba un instante las inmersiones del usuario anterior). clearBackStack borra ese estado
 * y limpia sus ViewModels (onCleared cancela sus corrutinas).
 */
fun NavHostController.irAlLoginSinEstadoGuardado() {
    AppDestinations.entries.forEach { clearBackStack(it.ruta) }
    navigate(Ruta.Login) {
        popUpTo(graph.id) { inclusive = true }
    }
}

/** getBackStackEntry tira excepción si la ruta no está en la pila; acá se convierte en false. */
private fun NavHostController.estaEnLaPila(ruta: Ruta): Boolean =
    runCatching { getBackStackEntry(ruta) }.isSuccess
