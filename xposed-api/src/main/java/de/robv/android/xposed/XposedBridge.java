// SPDX-License-Identifier: Apache-2.0

package de.robv.android.xposed;

import java.util.Collections;
import java.util.Set;

/** Compile-only stub of {@code de.robv.android.xposed.XposedBridge}. */
public final class XposedBridge {
    private XposedBridge() {}

    public static Set<Object> hookAllMethods(
            Class<?> hookClass,
            String methodName,
            XC_MethodHook callback
    ) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static void log(String text) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }
}
