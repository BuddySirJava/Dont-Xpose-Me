// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import me.dontxpose.util.RootShell

data class TestEvent(
    val atMs: Long,
    val kind: String,
    val pinLength: Int,
)

object TestEventLog {
    const val MAX_EVENTS = 50

    fun parse(text: String): List<TestEvent> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence()
            .mapNotNull { line ->
                val parts = line.trim().split('\t')
                if (parts.size < 2) return@mapNotNull null
                val at = parts[0].toLongOrNull() ?: return@mapNotNull null
                val kind = parts[1]
                val pinLength = parts.getOrNull(2)?.toIntOrNull() ?: 0
                TestEvent(atMs = at, kind = kind, pinLength = pinLength)
            }
            .toList()
            .asReversed() // newest first
    }

    suspend fun read(): List<TestEvent> {
        val r = RootShell.su("cat ${Paths.TEST_EVENTS} 2>/dev/null")
        if (!r.success || r.stdout.isBlank()) return emptyList()
        return parse(r.stdout)
    }

    suspend fun clear(): Boolean {
        val r = RootShell.su(
            "rm -f ${Paths.TEST_EVENTS} && " +
                "mkdir -p ${Paths.DIR} && chown system:system ${Paths.DIR} && chmod 700 ${Paths.DIR}",
        )
        return r.success
    }
}
