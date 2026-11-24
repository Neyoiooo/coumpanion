package com.coucou.android.core

/** The three places the bottom bar leads to. */
enum class Tab { HOME, ACTIVITY, SETTINGS }

/** Every screen of the app. Tabs are the roots; the rest are one level down. */
enum class Screen { HOME, CHAT, SETTINGS, HISTORY, DIAGNOSTICS, LICENCES, SESSION, AI_TRACKING, AI_HISTORY }

/**
 * Where the user is and where Back goes. Pure, so every rule is a unit test:
 * - the bar always has Home, Activity and Settings;
 * - the bar shows on the three tab roots, and hides on every page below them and while the keyboard is open;
 * - Back goes up one level; on Home it is the system's (leave the app).
 */
object Nav {
    fun tabs(): List<Tab> = Tab.entries

    fun screenOf(tab: Tab): Screen = when (tab) {
        Tab.HOME -> Screen.HOME
        Tab.ACTIVITY -> Screen.AI_HISTORY
        Tab.SETTINGS -> Screen.SETTINGS
    }

    /** The tab a screen belongs to, so the bar can highlight it; null for full-screen tasks. */
    fun tabOf(screen: Screen): Tab? = when (screen) {
        Screen.HOME -> Tab.HOME
        Screen.AI_HISTORY -> Tab.ACTIVITY
        Screen.SETTINGS, Screen.HISTORY, Screen.DIAGNOSTICS, Screen.LICENCES -> Tab.SETTINGS
        Screen.CHAT, Screen.SESSION, Screen.AI_TRACKING -> null
    }

    fun barVisible(screen: Screen, keyboardOpen: Boolean): Boolean =
        !keyboardOpen && (screen == Screen.HOME || screen == Screen.AI_HISTORY || screen == Screen.SETTINGS)

    /** One level up, or null when Back should leave the app. */
    fun back(screen: Screen): Screen? = when (screen) {
        Screen.HOME -> null
        Screen.CHAT, Screen.SETTINGS, Screen.SESSION, Screen.AI_TRACKING, Screen.AI_HISTORY -> Screen.HOME
        Screen.HISTORY, Screen.DIAGNOSTICS, Screen.LICENCES -> Screen.SETTINGS
    }
}
