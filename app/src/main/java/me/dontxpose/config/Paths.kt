// SPDX-License-Identifier: Apache-2.0

package me.dontxpose.config

/**
 * Paths shared between the app (via Magisk su) and the LSPosed hook in system_server.
 * /data/system is readable by system_server; the app writes with root.
 */
object Paths {
    const val DIR = "/data/system/dontxpose"
    const val CONFIG = "$DIR/config.properties"
    const val STATUS = "$DIR/hook_status.properties"
    /** One line per test-mode match: epochMs, tab, test_match, tab, pinLength */
    const val TEST_EVENTS = "$DIR/test_events.log"
    const val TAG = "DontXpose"
}
