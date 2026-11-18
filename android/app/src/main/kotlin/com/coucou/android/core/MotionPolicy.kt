package com.coucou.android.core

/** What the user chose for motion: follow Android's "Remove animations" (the default), always reduce it, or always keep it. */
enum class MotionChoice(val key: String) {
    SYSTEM("system"), REDUCE("reduce"), FULL("full");

    companion object {
        /** An unknown or missing key follows the system. */
        fun parse(key: String?): MotionChoice = values().firstOrNull { it.key == key } ?: SYSTEM
    }
}

/**
 * Whether the island and sheets use a short fade or a jump. Pure, so every case is a unit test.
 */
object MotionPolicy {
    fun reduced(choice: MotionChoice, animatorDurationScale: Float): Boolean = when (choice) {
        MotionChoice.REDUCE -> true
        MotionChoice.FULL -> false
        MotionChoice.SYSTEM -> MotionSpec.isReduced(animatorDurationScale)
    }
}

/** The choice in force, set by the app model when Settings change; read by the island and the sheets. */
object MotionState {
    @Volatile var choice: MotionChoice = MotionChoice.SYSTEM
}
