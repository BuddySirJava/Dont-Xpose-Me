// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.wipe

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.util.Log
import me.dontxpose.config.Paths
import java.io.File
import java.io.FileOutputStream

/**
 * Armed wipe path: KeyMint deleteAllKeys (retry once), then recovery userdata wipe,
 * then shutdown as last resort. Prefer running inside system_server.
 */
@SuppressLint("PrivateApi")
object CryptoWipe {
    private const val TAG = Paths.TAG
    private const val RECOVERY_COMMAND = "/cache/recovery/command"

    /**
     * @return true if deleteAllKeys was invoked without throwing
     */
    fun deleteAllKeys(): Boolean {
        return try {
            val clazz = Class.forName("android.security.AndroidKeyStoreMaintenance")
            val method = clazz.getDeclaredMethod("deleteAllKeys")
            method.isAccessible = true
            method.invoke(null)
            Log.w(TAG, "deleteAllKeys completed")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "deleteAllKeys failed", t)
            false
        }
    }

    fun canCallDeleteAllKeys(): Boolean {
        return try {
            val clazz = Class.forName("android.security.AndroidKeyStoreMaintenance")
            clazz.getDeclaredMethod("deleteAllKeys")
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun shutdown(context: Context?) {
        try {
            if (context != null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                // hidden API: shutdown(boolean confirm, String reason, boolean wait)
                val method = PowerManager::class.java.getMethod(
                    "shutdown",
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
                method.invoke(pm, false, "DontXpose-duress", false)
                return
            }
        } catch (t: Throwable) {
            Log.e(TAG, "PowerManager.shutdown failed", t)
        }
        try {
            Runtime.getRuntime().exec(arrayOf("reboot", "-p"))
        } catch (t: Throwable) {
            Log.e(TAG, "reboot -p failed", t)
        }
    }

    /**
     * Full wipe path. Returns only if recovery wipe and shutdown both failed to leave
     * the process — caller should record a degraded status.
     *
     * @return true if KeyMint deleteAllKeys succeeded (on first try or retry)
     */
    fun wipeAndShutdown(context: Context?): Boolean {
        var keysOk = deleteAllKeys()
        if (!keysOk) {
            Log.w(TAG, "deleteAllKeys failed — retrying once")
            keysOk = deleteAllKeys()
        }
        Log.w(TAG, "keysOk=$keysOk — attempting recovery userdata wipe")

        if (rebootWipeUserData(context)) {
            // Should not return; recovery reboot is in progress.
            return keysOk
        }
        if (rebootRecoveryWipeCommand()) {
            return keysOk
        }

        Log.e(
            TAG,
            "wipe degraded: recovery userdata wipe failed; shutting down (keysOk=$keysOk)",
        )
        shutdown(context)
        return keysOk
    }

    /** @return true if the call was invoked (may never return on success) */
    private fun rebootWipeUserData(context: Context?): Boolean {
        if (context == null) {
            Log.w(TAG, "RecoverySystem.rebootWipeUserData skipped — no Context")
            return false
        }
        return try {
            val clazz = Class.forName("android.os.RecoverySystem")
            val method = clazz.getMethod("rebootWipeUserData", Context::class.java)
            method.invoke(null, context)
            Log.w(TAG, "RecoverySystem.rebootWipeUserData invoked")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "RecoverySystem.rebootWipeUserData failed", t)
            false
        }
    }

    /**
     * Best-effort when RecoverySystem is unavailable: write --wipe_data and reboot recovery.
     * @return true if reboot recovery was exec'd
     */
    private fun rebootRecoveryWipeCommand(): Boolean {
        return try {
            val commandFile = File(RECOVERY_COMMAND)
            val parent = commandFile.parentFile
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    Log.e(TAG, "failed to create ${parent.path}")
                    return false
                }
            }
            FileOutputStream(commandFile).use { out ->
                out.write("--wipe_data\n".toByteArray(Charsets.UTF_8))
            }
            Runtime.getRuntime().exec(arrayOf("reboot", "recovery"))
            Log.w(TAG, "wrote $RECOVERY_COMMAND and rebooted to recovery")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "recovery command wipe failed", t)
            false
        }
    }
}
