// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

import android.util.Base64
import me.dontxpose.util.RootShell
import java.io.StringWriter

object ConfigStore {
    suspend fun ensureDir(): Boolean {
        // SystemUI runs as an app UID, not system. It must traverse the dir, read config
        // (salted hash only), and write hook_status — so dir is 777 and status is 666.
        val r = RootShell.su(
            "mkdir -p ${Paths.DIR} && chown system:system ${Paths.DIR} && chmod 777 ${Paths.DIR} && " +
                "touch ${Paths.STATUS} && chown system:system ${Paths.STATUS} && chmod 666 ${Paths.STATUS}",
        )
        return r.success
    }

    suspend fun read(): DuressConfig? {
        val r = RootShell.su("cat ${Paths.CONFIG} 2>/dev/null")
        if (!r.success || r.stdout.isBlank()) return null
        return DuressConfig.parse(r.stdout)
    }

    suspend fun write(config: DuressConfig): Boolean {
        if (!ensureDir()) return false
        val writer = StringWriter()
        config.toProperties().store(writer, "DontXpose duress config — do not edit")
        val content = writer.toString()
        val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        // 644: system_server and SystemUI both need to read the salted hash for matching.
        val r = RootShell.su(
            "echo '$b64' | base64 -d > ${Paths.CONFIG} && " +
                "chown system:system ${Paths.CONFIG} && chmod 644 ${Paths.CONFIG}",
        )
        return r.success
    }

    suspend fun setArmed(armed: Boolean): Boolean {
        val current = read() ?: return false
        return write(
            current.copy(
                armed = armed,
                testMode = !armed,
            ),
        )
    }
}
