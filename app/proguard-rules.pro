# LSPosed discovers the module via assets/xposed_init. Keep the entry point
# and the wipe/config types the hook uses if R8 is ever enabled.
-keep class me.dontxpose.hook.DuressHook { *; }
-keep class me.dontxpose.wipe.CryptoWipe { *; }
-keep class me.dontxpose.config.** { *; }

-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**
