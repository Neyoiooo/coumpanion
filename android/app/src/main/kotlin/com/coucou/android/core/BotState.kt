package com.coucou.android.core

/** Components 0..1, like the TS `RGB` tuple. */
data class Rgb(val r: Double, val g: Double, val b: Double)

enum class BotState(val key: String) {
    IDLE("idle"), WORKING("working"), THINKING("thinking"), SEARCHING("searching"),
    APPROVAL("approval"), QUESTION("question"), ERROR("error"), FINISHED("finished"),
    RATELIMIT("ratelimit"), SLEEPING("sleeping"), DIZZY("dizzy"),
}

object StatePalette {
    val C_IDLE = Rgb(0.902, 0.914, 0.933)
    val C_WORKING = Rgb(0.231, 0.62, 1.0)
    val C_THINKING = Rgb(0.545, 0.361, 0.965)
    val C_SEARCHING = Rgb(0.388, 0.396, 0.949)
    val C_APPROVAL = Rgb(0.961, 0.647, 0.141)
    val C_QUESTION = Rgb(0.133, 0.827, 0.933)
    val C_ERROR = Rgb(0.957, 0.314, 0.369)
    val C_FINISHED = Rgb(0.204, 0.831, 0.6)
    val C_RATELIMIT = Rgb(0.984, 0.573, 0.235)
    val C_SLEEPING = Rgb(0.58, 0.635, 0.722)
    val C_DIZZY = Rgb(0.957, 0.447, 0.714)

    fun color(state: BotState): Rgb = when (state) {
        BotState.IDLE -> C_IDLE
        BotState.WORKING -> C_WORKING
        BotState.THINKING -> C_THINKING
        BotState.SEARCHING -> C_SEARCHING
        BotState.APPROVAL -> C_APPROVAL
        BotState.QUESTION -> C_QUESTION
        BotState.ERROR -> C_ERROR
        BotState.FINISHED -> C_FINISHED
        BotState.RATELIMIT -> C_RATELIMIT
        BotState.SLEEPING -> C_SLEEPING
        BotState.DIZZY -> C_DIZZY
    }
}
