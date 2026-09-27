// SPDX-License-Identifier: Apache-2.0

package de.robv.android.xposed.callbacks;

/** Compile-only stub of {@code de.robv.android.xposed.callbacks.XC_LoadPackage}. */
public abstract class XC_LoadPackage {
    public static final class LoadPackageParam {
        public String packageName = "";
        public String processName = "";
        public ClassLoader classLoader;
        public boolean isFirstApplication;
    }

    public abstract void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;
}
