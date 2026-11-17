package com.berterogarcia.appnea.ui.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Texto para mostrar que se arma FUERA de Compose (ViewModels, validaciones): el id de
 * strings.xml + sus argumentos, sin necesitar un Context. La pantalla lo convierte con [resolver].
 *
 * Los argumentos pueden ser otros TextoUi (ej. "Ingresá %1$s." con "tu nombre"): se resuelven
 * también. Es Kotlin puro (solo ids de R), así que las validaciones se siguen testeando con JUnit.
 */
sealed interface TextoUi {
    data class Recurso(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : TextoUi
    data class Plural(@param:PluralsRes val id: Int, val cantidad: Int, val args: List<Any> = emptyList()) : TextoUi
}

/** Atajo: `textoUi(R.string.x, arg1, arg2)`. */
fun textoUi(@StringRes id: Int, vararg args: Any): TextoUi = TextoUi.Recurso(id, args.toList())

/** Atajo para plurales: `pluralUi(R.plurals.x, cantidad, arg1…)`. */
fun pluralUi(@PluralsRes id: Int, cantidad: Int, vararg args: Any): TextoUi = TextoUi.Plural(id, cantidad, args.toList())

/** El texto final, con los argumentos que también sean TextoUi ya resueltos. */
@Composable
fun TextoUi.resolver(): String = when (this) {
    is TextoUi.Recurso -> stringResource(id, *resolverArgs(args))
    is TextoUi.Plural -> pluralStringResource(id, cantidad, *resolverArgs(args))
}

@Composable
private fun resolverArgs(args: List<Any>): Array<Any> =
    args.map { if (it is TextoUi) it.resolver() else it }.toTypedArray()
