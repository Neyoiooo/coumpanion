package com.coucou.android.core

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

/** Selects a face tile from the shared timeline used for the app and showcase. */
internal class AvatarMotion private constructor(
    private val timelines: Map<AvatarMood, List<Pose>>,
    val frameCount: Int,
) {
    private data class Pose(val tile: Int, val endMs: Long)

    fun frame(mood: AvatarMood, ageMs: Long, animate: Boolean): Int {
        val timeline = timelines.getValue(mood)
        if (!animate) return timeline.first().tile
        val duration = timeline.last().endMs
        val age = ageMs.coerceAtLeast(0)
        if (mood == AvatarMood.COMPLETED && age >= duration) return timeline.last().tile
        val elapsed = age % duration
        return timeline.first { elapsed < it.endMs }.tile
    }

    companion object {
        fun parse(source: String): AvatarMotion {
            val tiles = mutableMapOf<String, Int>()
            val timelines = mutableMapOf<AvatarMood, List<Pose>>()
            source.lineSequence().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.forEach { line ->
                val fields = line.split(Regex("\\s+"))
                val mood = AvatarMood.valueOf(fields.first())
                require(fields.size > 1 && mood !in timelines)
                var elapsed = 0L
                timelines[mood] = fields.drop(1).map { entry ->
                    val (name, duration) = entry.split(':')
                    elapsed += duration.toLong().also { require(it > 0) }
                    Pose(tiles.getOrPut(name) { tiles.size }, elapsed)
                }
            }
            require(timelines.size == AvatarMood.entries.size)
            return AvatarMotion(timelines, tiles.size)
        }
    }
}

/** A transform in fractions of the avatar box. It applies to the sprite, faceplate and badge together. */
internal data class AvatarBody(val x: Float = 0f, val y: Float = 0f, val stretch: Float = 1f, val tilt: Float = 0f)

internal object AvatarBodyMotion {
    val still = AvatarBody()

    fun at(mood: AvatarMood, elapsedMs: Long, animate: Boolean): AvatarBody {
        if (!animate) return still
        val age = elapsedMs.coerceAtLeast(0)
        fun wave(period: Long) = kotlin.math.sin((age % period) * (2.0 * Math.PI / period)).toFloat()
        return when (mood) {
            AvatarMood.IDLE -> AvatarBody(stretch = 1f + wave(3_600) * 0.027f)
            AvatarMood.TRACKING -> AvatarBody(y = wave(3_200) * 0.029f)
            AvatarMood.THINKING -> AvatarBody(tilt = wave(3_000) * 3.1f)
            AvatarMood.WORKING -> AvatarBody(y = -kotlin.math.abs(wave(1_260)) * 0.043f, tilt = wave(2_100) * 2.4f)
            AvatarMood.COMPLETED -> if (age >= 950) still else {
                AvatarBody(y = -kotlin.math.sin(Math.PI * age / 950.0).toFloat() * 0.085f)
            }
            AvatarMood.WAITING -> AvatarBody(stretch = 1f + wave(2_500) * 0.035f)
            AvatarMood.ERROR -> if (age >= 420) still else AvatarBody(x = wave(135) * 0.04f * (1f - age / 420f))
            AvatarMood.DISCONNECTED -> still
        }
    }
}
