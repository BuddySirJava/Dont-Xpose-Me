// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuressConfigTest {
    @Test
    fun roundTripProperties() {
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin("87654321", salt)
        val original = DuressConfig(
            salt = salt,
            pinHash = hash,
            armed = false,
            testMode = true,
            pinLength = 8,
        )
        val parsed = DuressConfig.fromProperties(original.toProperties())
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun parseRejectsMissingHash() {
        val text = "salt=00\narmed=false\ntestMode=true\npinLength=8\n"
        assertNull(DuressConfig.parse(text))
    }

    @Test
    fun parseArmedAndTestMode() {
        val salt = PinHasher.toHex(ByteArray(16) { 1 })
        val hash = PinHasher.toHex(ByteArray(32) { 2 })
        val parsed = DuressConfig.parse(
            "salt=$salt\npinHash=$hash\narmed=true\ntestMode=false\npinLength=10\n",
        )
        assertNotNull(parsed)
        assertTrue(parsed!!.armed)
        assertFalse(parsed.testMode)
        assertEquals(10, parsed.pinLength)
    }
}
