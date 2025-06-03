package com.nathanielmoehring.vknconfig

import android.view.Surface

object NativeBridge { // Using an object for a singleton-like utility
    init {
        // Must match the project_name in app/src/main/cpp/CMakeLists.txt
        System.loadLibrary("vknconfig_tester")
    }

    // External functions are Kotlin's way of declaring JNI methods
    external fun nativeInit()
    external fun nativeSetSurface(surface: Surface?) // Surface can be null
    external fun nativeRender()
    external fun nativeSurfaceDestroyed()
    external fun nativeCleanup()
    external fun nativeRunTests(): Int // Returns 0 on success
}