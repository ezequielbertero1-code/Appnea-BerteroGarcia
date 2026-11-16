package com.berterogarcia.appnea.data.seguridad

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2 + salt aleatorio (reemplaza a hashearPassword, SHA-256 sin salt).
 *
 * Formato guardado:  pbkdf2$sha256$210000$<salt hex>$<hash hex>
 * Guarda algoritmo e iteraciones, así se pueden subir sin romper contraseñas existentes.
 *
 * - PBKDF2WithHmacSHA256 existe desde API 26; con minSdk 24, en Android 7 se usa PBKDF2WithHmacSHA1.
 * - Los hashes viejos (SHA-256, 64 hex) se aceptan y se re-hashean al próximo login (UsuarioRepository).
 * - hashear/verificar tardan a propósito: llamarlas fuera del hilo principal.
 */
object SeguridadPassword {

    /** Iteraciones de PBKDF2. Si se sube, los usuarios se re-hashean solos al loguear. */
    const val ITERACIONES = 210_000
    private const val BYTES_SALT = 16
    private const val BITS_HASH = 256
    private const val PREFIJO = "pbkdf2"
    private const val SEPARADOR = '$'
    private val REGEX_SHA256_VIEJO = Regex("^[0-9a-f]{64}$")

    private enum class Algoritmo(val etiqueta: String, val nombreJca: String) {
        SHA256("sha256", "PBKDF2WithHmacSHA256"),
        SHA1("sha1", "PBKDF2WithHmacSHA1");

        companion object {
            fun desdeEtiqueta(etiqueta: String): Algoritmo? = entries.firstOrNull { it.etiqueta == etiqueta }
        }
    }

    private val random = SecureRandom()

    /** El mejor algoritmo disponible en este dispositivo (se calcula una sola vez). */
    private val algoritmoPreferido: Algoritmo by lazy {
        if (soportaSha256()) Algoritmo.SHA256 else Algoritmo.SHA1
    }

    /** Genera el texto a guardar en la base para [password]. */
    fun hashear(password: String): String {
        val algoritmo = algoritmoPreferido
        val salt = ByteArray(BYTES_SALT).also { random.nextBytes(it) }
        val hash = pbkdf2(password, salt, ITERACIONES, algoritmo)
        return listOf(PREFIJO, algoritmo.etiqueta, ITERACIONES.toString(), salt.aHex(), hash.aHex())
            .joinToString(SEPARADOR.toString())
    }

    /** true si [password] corresponde a [guardado]. Nunca lanza: un valor corrupto no coincide. */
    fun verificar(password: String, guardado: String): Boolean = runCatching {
        if (esFormatoViejo(guardado)) {
            return@runCatching MessageDigest.isEqual(sha256(password).toByteArray(), guardado.toByteArray())
        }
        val partes = guardado.split(SEPARADOR)
        if (partes.size != 5 || partes[0] != PREFIJO) return@runCatching false
        val algoritmo = Algoritmo.desdeEtiqueta(partes[1]) ?: return@runCatching false
        val iteraciones = partes[2].toInt()
        val salt = partes[3].desdeHex()
        val esperado = partes[4].desdeHex()
        val calculado = pbkdf2(password, salt, iteraciones, algoritmo)
        // Comparación en tiempo constante: no revela cuántos bytes coincidieron.
        MessageDigest.isEqual(calculado, esperado)
    }.getOrDefault(false)

    /** true si conviene re-hashear (formato viejo, menos iteraciones o algoritmo más débil). */
    fun necesitaActualizar(guardado: String): Boolean {
        if (esFormatoViejo(guardado)) return true
        val partes = guardado.split(SEPARADOR)
        if (partes.size != 5 || partes[0] != PREFIJO) return true
        val iteraciones = partes[2].toIntOrNull() ?: return true
        return iteraciones < ITERACIONES || partes[1] != algoritmoPreferido.etiqueta
    }

    private fun esFormatoViejo(guardado: String) = REGEX_SHA256_VIEJO.matches(guardado)

    private fun pbkdf2(password: String, salt: ByteArray, iteraciones: Int, algoritmo: Algoritmo): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iteraciones, BITS_HASH)
        return try {
            SecretKeyFactory.getInstance(algoritmo.nombreJca).generateSecret(spec).encoded
        } finally {
            spec.clearPassword() // borra la copia de la contraseña en memoria
        }
    }

    /** false en Android 7 (API 24-25), que no trae PBKDF2WithHmacSHA256. */
    private fun soportaSha256(): Boolean =
        runCatching { SecretKeyFactory.getInstance(Algoritmo.SHA256.nombreJca) }.isSuccess

    /** Hash viejo, solo para verificar contraseñas guardadas antes de este cambio. */
    private fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray()).aHex()

    private fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.desdeHex(): ByteArray {
        require(length % 2 == 0) { "Hex inválido" }
        return ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}