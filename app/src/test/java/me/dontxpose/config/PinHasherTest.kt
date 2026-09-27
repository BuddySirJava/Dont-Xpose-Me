// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun matchesSamePin() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin("12345678", salt)
        assertTrue(PinHasher.matches("12345678", salt, hash))
        assertFalse(PinHasher.matches("12345679", salt, hash))
    }

    @Test
    fun differentSaltDoesNotMatch() {
        val hash = PinHasher.hashPin("12345678", PinHasher.generateSalt())
        assertFalse(PinHasher.matches("12345678", PinHasher.generateSalt(), hash))
    }

    @Test
    fun hexRoundTrip() {
        val salt = PinHasher.generateSalt()
        val hex = PinHasher.toHex(salt)
        assertEquals(32, hex.length)
        assertTrue(PinHasher.fromHex(hex).contentEquals(salt))
    }

    @Test
    fun minPinLengthIsFour() {
        assertEquals(4, PinHasher.MIN_PIN_LENGTH)
    }

    @Test
    fun trivialPinIsAllSameDigit() {
        assertTrue(PinHasher.isTrivialPin("0000"))
        assertTrue(PinHasher.isTrivialPin("1111"))
        assertTrue(PinHasher.isTrivialPin("999999"))
        assertFalse(PinHasher.isTrivialPin("1234"))
        assertFalse(PinHasher.isTrivialPin("1121"))
    }
}
