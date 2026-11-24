package com.coucou.android.core

import kotlin.math.abs
import kotlin.math.sin

internal enum class AvatarMood {
    IDLE, TRACKING, THINKING, WORKING, COMPLETED, WAITING, ERROR, DISCONNECTED;

    companion object {
        fun forBot(state: BotState): AvatarMood = when (state) {
            BotState.IDLE -> IDLE
            BotState.THINKING -> THINKING
            BotState.SEARCHING, BotState.WORKING -> WORKING
            BotState.FINISHED -> COMPLETED
            BotState.QUESTION, BotState.APPROVAL -> WAITING
            BotState.ERROR, BotState.RATELIMIT, BotState.DIZZY -> ERROR
            BotState.SLEEPING -> DISCONNECTED
        }

        fun forIsland(spec: IslandSpec): AvatarMood = when {
            spec.manualTracking -> TRACKING
            spec.kind == IslandSpec.Kind.APP_UPDATE -> IDLE
            else -> forBot(spec.state)
        }
    }
}

internal data class AvatarFrame(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val tilt: Float = 0f,
    val blink: Boolean = false,
    val effect: AvatarMood = AvatarMood.IDLE,
    val glow: Float = 0f,
)

/** Coordinates are fractions of the drawing area. Elapsed time pauses with the surface clock. */
internal object AvatarMotion {
    const val CELEBRATION_MS = 1_200L
    const val SHAKE_MS = 560L

    fun frame(mood: AvatarMood, ageMs: Long, motion: Boolean): AvatarFrame {
        if (!motion) return AvatarFrame()
        val age = ageMs.coerceAtLeast(0)
        fun wave(period: Long) = sin((age % period) * (2 * Math.PI / period)).toFloat()
        fun breathing(period: Long, amount: Float) = AvatarFrame(
            scaleY = 1f + wave(period) * amount,
            blink = age % 5_400 in 5_220L..5_340L,
        )
        return when (mood) {
            AvatarMood.IDLE -> breathing(3_400, 0.035f)
            AvatarMood.TRACKING -> AvatarFrame(wave(3_600) * 0.045f, wave(2_250) * 0.04f,
                tilt = wave(3_600) * 4f, effect = mood, glow = 0.10f + wave(2_250) * 0.045f)
            AvatarMood.THINKING -> AvatarFrame(y = wave(2_000) * 0.02f, tilt = wave(2_000) * 5f, effect = mood)
            AvatarMood.WORKING -> AvatarFrame(y = -abs(wave(1_100)) * 0.085f,
                scaleY = 1f + wave(1_100) * 0.035f, tilt = wave(1_700) * 6f, effect = mood)
            AvatarMood.COMPLETED -> if (age < CELEBRATION_MS) {
                val bounce = sin(age * Math.PI / CELEBRATION_MS).toFloat()
                AvatarFrame(y = -bounce * 0.15f, scaleX = 1f + bounce * 0.05f, scaleY = 1f + bounce * 0.10f, effect = mood)
            } else frame(AvatarMood.IDLE, age - CELEBRATION_MS, true)
            AvatarMood.WAITING -> AvatarFrame(scaleY = 1f + wave(1_850) * 0.06f, effect = mood)
            AvatarMood.ERROR -> AvatarFrame(x = if (age < SHAKE_MS) wave(145) * 0.09f * (1f - age / SHAKE_MS.toFloat()) else 0f, effect = mood)
            AvatarMood.DISCONNECTED -> breathing(4_800, 0.02f).copy(effect = mood)
        }
    }
}
