// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    val success: Boolean get() = exitCode == 0
}

object RootShell {
    suspend fun su(command: String, timeoutSec: Long = 15): ShellResult =
        withContext(Dispatchers.IO) {
            exec(arrayOf("su", "-c", command), timeoutSec)
        }

    suspend fun sh(command: String, timeoutSec: Long = 10): ShellResult =
        withContext(Dispatchers.IO) {
            exec(arrayOf("sh", "-c", command), timeoutSec)
        }

    fun suBlocking(command: String, timeoutSec: Long = 15): ShellResult =
        exec(arrayOf("su", "-c", command), timeoutSec)

    private fun exec(cmd: Array<String>, timeoutSec: Long): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val stdoutBuf = ByteArrayOutputStream()
            val stderrBuf = ByteArrayOutputStream()
            val outThread = Thread {
                process.inputStream.use { it.copyTo(stdoutBuf) }
            }
            val errThread = Thread {
                process.errorStream.use { it.copyTo(stderrBuf) }
            }
            outThread.start()
            errThread.start()
            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                outThread.join(1000)
                errThread.join(1000)
                return ShellResult(-1, utf8(stdoutBuf), "timeout")
            }
            outThread.join()
            errThread.join()
            ShellResult(
                exitCode = process.exitValue(),
                stdout = utf8(stdoutBuf).trimEnd(),
                stderr = utf8(stderrBuf).trimEnd(),
            )
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "exec failed")
        }
    }

    private fun utf8(buf: ByteArrayOutputStream): String =
        String(buf.toByteArray(), Charsets.UTF_8)

    suspend fun isRootAvailable(): Boolean {
        val r = su("id -u")
        return r.success && r.stdout.trim() == "0"
    }
}
