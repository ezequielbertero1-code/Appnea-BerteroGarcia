package com.berterogarcia.appnea.data

import com.berterogarcia.appnea.data.seguridad.SeguridadPassword
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SeguridadPasswordTest {

    @Test
    fun `verifica la contrasenia correcta y rechaza otra`() {
        val hash = SeguridadPassword.hashear("Secreta123")
        assertTrue(SeguridadPassword.verificar("Secreta123", hash))
        assertFalse(SeguridadPassword.verificar("secreta123", hash))
    }

    @Test
    fun `dos hashes de la misma contrasenia son distintos por el salt`() {
        assertNotEquals(SeguridadPassword.hashear("abc12345"), SeguridadPassword.hashear("abc12345"))
    }

    @Test
    fun `un hash nuevo no necesita actualizarse`() {
        assertFalse(SeguridadPassword.necesitaActualizar(SeguridadPassword.hashear("abc12345")))
    }

    @Test
    fun `acepta el hash viejo sha256 y pide actualizarlo`() {
        val viejo = MessageDigest.getInstance("SHA-256").digest("Vieja123".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertTrue(SeguridadPassword.verificar("Vieja123", viejo))
        assertTrue(SeguridadPassword.necesitaActualizar(viejo))
    }

    @Test
    fun `menos iteraciones que las actuales pide actualizar`() {
        val actual = SeguridadPassword.hashear("abc12345")
        val conMenos = actual.replace("$${SeguridadPassword.ITERACIONES}$", "$1000$")
        assertTrue(SeguridadPassword.necesitaActualizar(conMenos))
    }

    @Test
    fun `un valor corrupto nunca coincide ni lanza`() {
        assertFalse(SeguridadPassword.verificar("x", "pbkdf2\$sha256\$abc\$zz\$zz"))
        assertFalse(SeguridadPassword.verificar("x", "!google"))
        assertFalse(SeguridadPassword.verificar("x", ""))
    }
}
