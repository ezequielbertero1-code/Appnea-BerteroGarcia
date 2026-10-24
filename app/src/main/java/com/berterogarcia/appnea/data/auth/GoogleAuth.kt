package com.berterogarcia.appnea.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.berterogarcia.appnea.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CancellationException

/** Datos de la cuenta de Google que eligió el usuario. */
data class CuentaGoogle(
    val email: String,
    val nombre: String,
    val apellido: String
)

sealed interface ResultadoGoogle {
    data class Ok(val cuenta: CuentaGoogle) : ResultadoGoogle
    /** El usuario cerró el selector de cuentas: no es un error, no se muestra nada. */
    data object Cancelado : ResultadoGoogle
    /** No hay ninguna cuenta de Google en el dispositivo. */
    data object SinCuentas : ResultadoGoogle
    /** Falta el Client ID en local.properties (client_web=...). */
    data object SinConfigurar : ResultadoGoogle
    data object Error : ResultadoGoogle
}

/**
 * "Iniciar sesión con Google" con Credential Manager.
 *
 * La app es 100% local (no tiene servidor), así que se confía en el token que devuelve Google en el
 * dispositivo y solo se usan email y nombre. Si algún día hay backend, hay que mandarle
 * `idToken` y verificarlo allá.
 */
object GoogleAuth {

    /** [context] tiene que ser una Activity: el selector de cuentas se dibuja encima de ella. */
    suspend fun pedirCuenta(context: Context): ResultadoGoogle {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) return ResultadoGoogle.SinConfigurar

        val pedido = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()

        return try {
            val credencial = CredentialManager.create(context).getCredential(context, pedido).credential
            if (credencial is CustomCredential &&
                credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credencial.data)
                ResultadoGoogle.Ok(
                    CuentaGoogle(
                        email = google.id.trim().lowercase(),
                        nombre = google.givenName?.takeIf { it.isNotBlank() }
                            ?: google.displayName?.takeIf { it.isNotBlank() }
                            ?: google.id.substringBefore('@'),
                        apellido = google.familyName.orEmpty()
                    )
                )
            } else {
                ResultadoGoogle.Error
            }
        } catch (e: GetCredentialCancellationException) {
            ResultadoGoogle.Cancelado
        } catch (e: NoCredentialException) {
            ResultadoGoogle.SinCuentas
        } catch (e: GetCredentialException) {
            ResultadoGoogle.Error
        } catch (e: GoogleIdTokenParsingException) {
            ResultadoGoogle.Error
        } catch (e: CancellationException) {
            throw e
        }
    }
}
