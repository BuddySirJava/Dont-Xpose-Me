// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.hook

import android.content.Context
import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.dontxpose.config.DuressConfig
import me.dontxpose.config.Paths
import me.dontxpose.config.PinHasher
import me.dontxpose.wipe.CryptoWipe
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean

class DuressHook : IXposedHookLoadPackage {
    companion object {
        private const val TAG = Paths.TAG
        private const val CREDENTIAL_TYPE_PIN = 3
        private const val MAX_TEST_EVENTS = 50
        private const val DEDUPE_WINDOW_MS = 2500L
        private val wiping = AtomicBoolean(false)
    }

    private var scopeAndroid = false
    private var scopeSystemUi = false

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            "android" -> {
                scopeAndroid = true
                Log.i(TAG, "loading hooks in system_server")
                hookLockSettingsService(lpparam)
                writeStatus(deleteAllKeysOk = CryptoWipe.canCallDeleteAllKeys(), lastEvent = "loaded")
            }
            "com.android.systemui" -> {
                scopeSystemUi = true
                Log.i(TAG, "loading fallback hooks in SystemUI")
                hookLockPatternUtils(lpparam)
                writeStatus(deleteAllKeysOk = CryptoWipe.canCallDeleteAllKeys(), lastEvent = "loaded")
            }
            else -> return
        }
    }

    private fun hookLockSettingsService(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = XposedHelpers.findClass(
            "com.android.server.locksettings.LockSettingsService",
            lpparam.classLoader,
        )
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                touchHeartbeat()
                if (wiping.get() || handleCredential(param.args.getOrNull(0), contextFromLss(param.thisObject))) {
                    failCredential(param, lpparam.classLoader)
                }
            }
        }
        XposedBridge.hookAllMethods(clazz, "checkCredential", hook)
        XposedBridge.hookAllMethods(clazz, "verifyCredential", hook)
    }

    private fun hookLockPatternUtils(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = XposedHelpers.findClass(
            "com.android.internal.widget.LockPatternUtils",
            lpparam.classLoader,
        )
        XposedBridge.hookAllMethods(
            clazz,
            "checkCredential",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    touchHeartbeat()
                    if (wiping.get() || handleCredential(param.args.getOrNull(0), null)) {
                        failCredential(param, lpparam.classLoader)
                    }
                }
            },
        )
    }

    private fun contextFromLss(lss: Any?): Context? {
        if (lss == null) return null
        return try {
            XposedHelpers.callMethod(lss, "getContext") as? Context
        } catch (_: Throwable) {
            try {
                XposedHelpers.getObjectField(lss, "mContext") as? Context
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * @return true if the duress PIN matched (caller must fail the credential)
     */
    private fun handleCredential(credentialObj: Any?, context: Context?): Boolean {
        if (credentialObj == null) return false
        if (wiping.get()) return true

        val config = loadConfig() ?: return false
        if (!config.armed && !config.testMode) return false

        val type = try {
            XposedHelpers.callMethod(credentialObj, "getType") as Int
        } catch (_: Throwable) {
            return false
        }
        if (type != CREDENTIAL_TYPE_PIN) return false

        val pin = try {
            val bytes = XposedHelpers.callMethod(credentialObj, "getCredential") as ByteArray
            String(bytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            return false
        }
        if (pin.isEmpty()) return false

        if (!PinHasher.matches(pin, config.salt, config.pinHash)) return false

        if (config.testMode && !config.armed) {
            recordTestMatch(pin.length)
            writeStatus(deleteAllKeysOk = CryptoWipe.canCallDeleteAllKeys(), lastEvent = "test_match")
            return true
        }

        if (!config.armed) return false

        if (!wiping.compareAndSet(false, true)) return true

        writeStatus(deleteAllKeysOk = true, lastEvent = "wipe_triggered")
        val keysOk = CryptoWipe.wipeAndShutdown(context)
        writeStatus(deleteAllKeysOk = keysOk, lastEvent = "wipe_degraded")
        return true
    }

    private fun failCredential(param: XC_MethodHook.MethodHookParam, classLoader: ClassLoader) {
        val returnType = (param.method as? java.lang.reflect.Method)?.returnType
        when {
            returnType == Boolean::class.javaPrimitiveType || returnType == java.lang.Boolean::class.java -> {
                param.setResult(false)
            }
            returnType != null && returnType.name.contains("VerifyCredentialResponse") -> {
                val error = errorVerifyCredentialResponse(classLoader)
                if (error != null) {
                    param.setResult(error)
                } else {
                    param.setThrowable(SecurityException("DontXpose duress"))
                }
            }
            else -> {
                val error = errorVerifyCredentialResponse(classLoader)
                if (error != null) {
                    param.setResult(error)
                } else {
                    param.setThrowable(SecurityException("DontXpose duress"))
                }
            }
        }
    }

    private fun errorVerifyCredentialResponse(classLoader: ClassLoader): Any? {
        return try {
            val clazz = XposedHelpers.findClass(
                "com.android.internal.widget.VerifyCredentialResponse",
                classLoader,
            )
            try {
                XposedHelpers.getStaticObjectField(clazz, "ERROR")
            } catch (_: Throwable) {
                try {
                    XposedHelpers.callStaticMethod(clazz, "fromError")
                } catch (_: Throwable) {
                    try {
                        // RESPONSE_ERROR == 1 on AOSP
                        XposedHelpers.newInstance(clazz, 1)
                    } catch (_: Throwable) {
                        null
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "VerifyCredentialResponse.ERROR unavailable", t)
            null
        }
    }

    private fun loadConfig(): DuressConfig? {
        return try {
            val file = File(Paths.CONFIG)
            if (!file.isFile) return null
            FileReader(file).use { reader ->
                val props = Properties()
                props.load(reader)
                DuressConfig.fromProperties(props)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "failed to load config", t)
            null
        }
    }

    /**
     * Append a test-mode match for the setup app to show. Never stores the PIN.
     * Dedupes near-duplicate hooks from system_server + SystemUI on the same unlock.
     */
    private fun recordTestMatch(pinLength: Int) {
        try {
            val dir = File(Paths.DIR)
            if (!dir.exists()) dir.mkdirs()
            val file = File(Paths.TEST_EVENTS)
            val now = System.currentTimeMillis()
            if (file.isFile) {
                val lastLine = try {
                    file.readLines().lastOrNull()
                } catch (_: Exception) {
                    null
                }
                val lastTs = lastLine?.substringBefore('\t')?.toLongOrNull()
                if (lastTs != null && now - lastTs < DEDUPE_WINDOW_MS) {
                    return
                }
            }
            val line = "$now\ttest_match\t$pinLength\n"
            FileOutputStream(file, true).use { out ->
                out.write(line.toByteArray(Charsets.UTF_8))
            }
            try {
                val lines = file.readLines()
                if (lines.size > MAX_TEST_EVENTS) {
                    file.writeText(lines.takeLast(MAX_TEST_EVENTS).joinToString("\n", postfix = "\n"))
                }
            } catch (_: Exception) {
            }
            try {
                file.setReadable(true, true)
                file.setWritable(true, true)
            } catch (_: Exception) {
            }
        } catch (t: Throwable) {
            Log.e(TAG, "failed to record test match", t)
        }
    }

    private fun touchHeartbeat() {
        writeStatus(deleteAllKeysOk = null, lastEvent = null)
    }

    private fun writeStatus(deleteAllKeysOk: Boolean?, lastEvent: String?) {
        try {
            val dir = File(Paths.DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val existing = Properties()
            val statusFile = File(Paths.STATUS)
            if (statusFile.isFile) {
                try {
                    FileReader(statusFile).use { existing.load(it) }
                } catch (_: Exception) {
                }
            }
            val now = System.currentTimeMillis().toString()
            if (existing.getProperty("loadedAt") == null) {
                existing.setProperty("loadedAt", now)
            }
            existing.setProperty("heartbeatAt", now)
            if (deleteAllKeysOk != null) {
                existing.setProperty("deleteAllKeys", deleteAllKeysOk.toString())
            }
            if (scopeAndroid) existing.setProperty("scopeAndroid", "true")
            if (scopeSystemUi) existing.setProperty("scopeSystemUi", "true")
            if (lastEvent != null) {
                existing.setProperty("lastEvent", lastEvent)
            }
            existing.setProperty("module", "me.dontxpose")

            FileOutputStream(statusFile).use { out ->
                existing.store(out, "DontXpose hook status")
            }
            // SystemUI is u0_a*; loosen DAC so it can update this file after load.
            relaxStatusPermissions(dir, statusFile)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to write hook status", t)
        }
    }

    private fun relaxStatusPermissions(dir: File, statusFile: File) {
        try {
            dir.setReadable(true, false)
            dir.setExecutable(true, false)
            dir.setWritable(true, false)
            statusFile.setReadable(true, false)
            statusFile.setWritable(true, false)
        } catch (_: Exception) {
        }
        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "777", dir.absolutePath)).waitFor()
            Runtime.getRuntime().exec(arrayOf("chmod", "666", statusFile.absolutePath)).waitFor()
        } catch (_: Exception) {
        }
    }
}
