// SPDX-License-Identifier: Apache-2.0

package de.robv.android.xposed;

/** Compile-only stub of {@code de.robv.android.xposed.XposedHelpers}. */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static Object getObjectField(Object obj, String fieldName) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        throw new UnsupportedOperationException("Xposed API is provided by the framework at runtime");
    }
}
