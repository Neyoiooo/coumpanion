package com.coucou.android.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.coucou.android.R
import com.coucou.android.core.BotState

/**
 * Plays Coumpanion's short alert tones through a
 * SoundPool. Short clips, no streaming; a sound that is not found is silently ignored.
 */
class SoundPlayer(private val context: Context) {
    var enabled = true
    var volume = SoundVolume.DEFAULT
        set(v) { field = SoundVolume.clamp(v) }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        RAW.keys.forEach(::load)
    }

    private fun load(name: String): Int? = ids.getOrPut(name) {
        val res = RAW[name] ?: return null
        pool.load(context, res, 1)
    }

    fun play(name: String) {
        if (!enabled || volume <= 0f) return
        val id = load(name) ?: return
        if (id in ready) pool.play(id, volume, volume, 1, 0, 1f)
    }

    companion object {
        val stateSounds: Map<BotState, String> = mapOf(
            BotState.WORKING to "work", BotState.THINKING to "think", BotState.SEARCHING to "search",
            BotState.APPROVAL to "approval", BotState.QUESTION to "question", BotState.ERROR to "error",
            BotState.FINISHED to "finish", BotState.RATELIMIT to "rate", BotState.SLEEPING to "sleep",
            BotState.DIZZY to "dizzy",
        )

        /** Every bundled sound, by file name (an explicit table: no resource reflection). */
        private val RAW: Map<String, Int> = mapOf(
            "approval" to R.raw.approval,
            "dizzy" to R.raw.dizzy,
            "error" to R.raw.error,
            "finish" to R.raw.finish,
            "question" to R.raw.question,
            "rate" to R.raw.rate,
            "search" to R.raw.search,
            "sleep" to R.raw.sleep,
            "think" to R.raw.think,
            "work" to R.raw.work,
        )
    }
}
