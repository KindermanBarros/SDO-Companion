package com.kinderman.sdo.presentation

/** Retained by the application ViewModel so configuration changes are not background trips. */
internal class AppVisibility {
    private var foreground = false

    fun started(): Boolean {
        if (foreground) return false
        foreground = true
        return true
    }

    fun stopped(changingConfiguration: Boolean): Boolean {
        if (changingConfiguration || !foreground) return false
        foreground = false
        return true
    }
}
