// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import java.io.StringReader
import java.util.Properties

data class DuressConfig(
    val salt: ByteArray,
    val pinHash: ByteArray,
    val armed: Boolean,
    val testMode: Boolean,
    val pinLength: Int,
) {
    fun toProperties(): Properties = Properties().apply {
        setProperty("salt", PinHasher.toHex(salt))
        setProperty("pinHash", PinHasher.toHex(pinHash))
        setProperty("armed", armed.toString())
        setProperty("testMode", testMode.toString())
        setProperty("pinLength", pinLength.toString())
        setProperty("version", "1")
    }

    companion object {
        fun fromProperties(props: Properties): DuressConfig? {
            val saltHex = props.getProperty("salt") ?: return null
            val hashHex = props.getProperty("pinHash") ?: return null
            return try {
                DuressConfig(
                    salt = PinHasher.fromHex(saltHex),
                    pinHash = PinHasher.fromHex(hashHex),
                    armed = props.getProperty("armed", "false").toBooleanStrict(),
                    testMode = props.getProperty("testMode", "true").toBooleanStrict(),
                    pinLength = props.getProperty("pinLength", "0").toIntOrNull() ?: 0,
                )
            } catch (_: Exception) {
                null
            }
        }

        fun parse(text: String): DuressConfig? {
            val props = Properties()
            props.load(StringReader(text))
            return fromProperties(props)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DuressConfig) return false
        return salt.contentEquals(other.salt) &&
            pinHash.contentEquals(other.pinHash) &&
            armed == other.armed &&
            testMode == other.testMode &&
            pinLength == other.pinLength
    }

    override fun hashCode(): Int {
        var result = salt.contentHashCode()
        result = 31 * result + pinHash.contentHashCode()
        result = 31 * result + armed.hashCode()
        result = 31 * result + testMode.hashCode()
        result = 31 * result + pinLength
        return result
    }
}
