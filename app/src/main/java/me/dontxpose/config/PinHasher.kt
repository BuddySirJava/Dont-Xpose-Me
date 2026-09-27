// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import java.security.MessageDigest
import java.security.SecureRandom

object PinHasher {
    private const val SALT_BYTES = 16
    const val MIN_PIN_LENGTH = 4

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }

    fun matches(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actual = hashPin(pin, salt)
        if (actual.size != expectedHash.size) return false
        var diff = 0
        for (i in actual.indices) {
            diff = diff or (actual[i].toInt() xor expectedHash[i].toInt())
        }
        return diff == 0
    }

    fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    fun fromHex(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "invalid hex" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    /** True when every digit is the same (0000, 1111, …). */
    fun isTrivialPin(pin: String): Boolean {
        if (pin.isEmpty()) return true
        val first = pin[0]
        return pin.all { it == first }
    }
}
