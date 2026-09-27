// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.status

import android.content.Context
import android.content.pm.PackageManager
import me.dontxpose.config.Paths
import me.dontxpose.config.PinHasher
import me.dontxpose.util.RootShell
import me.dontxpose.wipe.CryptoWipe
import java.io.StringReader
import java.util.Properties

data class SystemStatus(
    val magiskRoot: Boolean,
    val lsposedModulePresent: Boolean,
    val hookLoaded: Boolean,
    val hookScopeAndroid: Boolean,
    val hookScopeSystemUi: Boolean,
    val deleteAllKeysPresent: Boolean,
    val hookDeleteAllKeysOk: Boolean,
    val armed: Boolean,
    val testMode: Boolean,
    val pinSet: Boolean,
    val pinLength: Int,
    /** From hook_status.properties: loaded, test_match, wipe_triggered, wipe_degraded, … */
    val lastEvent: String?,
) {
    val lastEventLabel: String
        get() = when (lastEvent) {
            null, "" -> "none"
            "loaded" -> "Hook loaded"
            "test_match" -> "Test match (wipe suppressed)"
            "wipe_triggered" -> "Wipe triggered"
            "wipe_degraded" -> "Wipe degraded — check device"
            else -> lastEvent
        }

    val lastEventIsDegraded: Boolean
        get() = lastEvent == "wipe_degraded"
    /** Fail-closed: only allow Arm when the wipe can actually run. */
    val canArm: Boolean
        get() = magiskRoot &&
            lsposedModulePresent &&
            hookLoaded &&
            (hookScopeAndroid || hookScopeSystemUi) &&
            (deleteAllKeysPresent || hookDeleteAllKeysOk) &&
            pinSet &&
            pinLength >= PinHasher.MIN_PIN_LENGTH

    val readyMessage: String
        get() = when {
            !magiskRoot -> "Magisk root (su) is required"
            !lsposedModulePresent -> "Enable this module in LSPosed (scope: android + SystemUI)"
            !hookLoaded -> "Hook not loaded — enable module, then soft-reboot / reboot"
            !(hookScopeAndroid || hookScopeSystemUi) -> "Hook status missing scope — re-enable in LSPosed"
            !(deleteAllKeysPresent || hookDeleteAllKeysOk) ->
                "deleteAllKeys unavailable on this ROM — cannot arm (fail-closed)"
            !pinSet || pinLength < PinHasher.MIN_PIN_LENGTH ->
                "Set a duress PIN of at least ${PinHasher.MIN_PIN_LENGTH} digits"
            else -> "Ready to arm"
        }
}

object StatusChecker {
    private const val HOOK_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000 // 7 days since last heartbeat

    suspend fun refresh(context: Context): SystemStatus {
        val magiskRoot = RootShell.isRootAvailable()
        val lsposedInstalled = isLikelyLsposedInstalled(context)

        val hookProps = readHookStatus()
        val now = System.currentTimeMillis()
        val loadedAt = hookProps?.getProperty("loadedAt")?.toLongOrNull()
        val heartbeatAt = hookProps?.getProperty("heartbeatAt")?.toLongOrNull() ?: loadedAt
        val age = heartbeatAt?.let { now - it }
        val hookLoaded = heartbeatAt != null && (age == null || age in 0..HOOK_MAX_AGE_MS)

        val deleteFromApp = CryptoWipe.canCallDeleteAllKeys()
        val hookDeleteOk = hookProps?.getProperty("deleteAllKeys") == "true"
        val scopeAndroid = hookProps?.getProperty("scopeAndroid") == "true"
        val scopeSystemUi = hookProps?.getProperty("scopeSystemUi") == "true"

        val configText = if (magiskRoot) {
            RootShell.su("cat ${Paths.CONFIG} 2>/dev/null")
        } else {
            null
        }
        val config = if (configText?.success == true && configText.stdout.isNotBlank()) {
            me.dontxpose.config.DuressConfig.parse(configText.stdout)
        } else {
            null
        }

        return SystemStatus(
            magiskRoot = magiskRoot,
            lsposedModulePresent = lsposedInstalled || hookLoaded,
            hookLoaded = hookLoaded,
            hookScopeAndroid = scopeAndroid,
            hookScopeSystemUi = scopeSystemUi,
            deleteAllKeysPresent = deleteFromApp,
            hookDeleteAllKeysOk = hookDeleteOk,
            armed = config?.armed == true,
            testMode = config?.testMode != false,
            pinSet = config != null,
            pinLength = config?.pinLength ?: 0,
            lastEvent = hookProps?.getProperty("lastEvent"),
        )
    }

    private fun isLikelyLsposedInstalled(context: Context): Boolean {
        if (isPackageInstalled(context, "org.lsposed.manager")) return true
        if (isPackageInstalled(context, "org.lsposed.manager.v2")) return true
        // Best-effort Magisk module path check (non-blocking-ish)
        return RootShell.suBlocking(
            "test -d /data/adb/lspd -o -d /data/adb/modules/zygisk_lsposed -o -d /data/adb/modules/LSPosed",
            timeoutSec = 5,
        ).success
    }

    private suspend fun readHookStatus(): Properties? {
        val r = RootShell.su("cat ${Paths.STATUS} 2>/dev/null")
        if (!r.success || r.stdout.isBlank()) return null
        return try {
            Properties().apply { load(StringReader(r.stdout)) }
        } catch (_: Exception) {
            null
        }
    }

    private fun isPackageInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}
