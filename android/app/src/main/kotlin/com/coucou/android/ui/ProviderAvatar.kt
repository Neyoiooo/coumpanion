package com.coucou.android.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.coucou.android.app.AvatarArtwork
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.AvatarMotion
import com.coucou.android.core.IslandPalette
import com.coucou.android.core.MotionChoice
import com.coucou.android.core.MotionPolicy
import com.coucou.android.core.ProviderAppearance
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal class AvatarClock {
    var millis by mutableLongStateOf(0L)
    var running by mutableStateOf(false)
    var visibleAvatars by mutableIntStateOf(0)
}

internal data class AvatarSurface(val clock: AvatarClock, val avatarsEnabled: Boolean)
internal val LocalAvatarSurface = staticCompositionLocalOf<AvatarSurface?> { null }

/** A single frame clock per visible surface. Bitmaps and all layout state stay out of the frame loop. */
@Composable
internal fun AvatarMotionHost(
    enabled: Boolean,
    motion: MotionChoice,
    visible: Boolean = true,
    animateBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visibleNow = visible && lifecycle.isAtLeast(Lifecycle.State.STARTED)
    val clock = remember { AvatarClock() }
    val observing = visibleNow && (enabled || animateBackground)
    var systemAllowsMotion by remember(observing, motion) { mutableStateOf(observing && motionAllowed(context, motion)) }
    DisposableEffect(context, observing, motion) {
        if (!observing) return@DisposableEffect onDispose { }
        fun refresh() { systemAllowsMotion = motionAllowed(context, motion) }
        val events = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refresh()
        }
        ContextCompat.registerReceiver(context, events, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
        )
        refresh()
        onDispose {
            context.unregisterReceiver(events)
            context.contentResolver.unregisterContentObserver(observer)
        }
    }
    val active = observing && systemAllowsMotion && (animateBackground || enabled && clock.visibleAvatars > 0)
    LaunchedEffect(clock, active) {
        clock.running = active
        if (!active) return@LaunchedEffect
        var previous = 0L
        while (true) {
            withFrameNanos { frame ->
                val elapsed = frame / 1_000_000
                if (previous > 0) clock.millis += (elapsed - previous).coerceIn(0, 60)
                previous = elapsed
            }
            delay(25)
        }
    }
    CompositionLocalProvider(LocalAvatarSurface provides AvatarSurface(clock, enabled), content = content)
}

private fun motionAllowed(context: Context, choice: MotionChoice): Boolean {
    val power = context.getSystemService(PowerManager::class.java)
    val lock = context.getSystemService(KeyguardManager::class.java)
    val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    return power.isInteractive && !power.isPowerSaveMode && !lock.isKeyguardLocked &&
        !MotionPolicy.reduced(choice, scale) && scale > 0f
}

@Composable
internal fun ProviderAvatar(
    providerId: String,
    name: String,
    mood: AvatarMood,
    modifier: Modifier,
    fallback: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current.applicationContext
    val appearance = remember(providerId, name) { ProviderAppearance.resolve(providerId, name) }
    val sprite = remember(context, appearance) { AvatarArtwork.sprite(context, appearance) }
    val accent = Color(appearance.accent or (0xFF shl 24))
    if (sprite == null) {
        if (fallback != null) {
            fallback()
            return
        }
        val mark = remember(providerId, name) { IslandPalette.forAgent(providerId, name) }
        val markColor = Color(mark.label)
        Canvas(modifier) {
            val cell = size.minDimension / 8
            val origin = center - Offset(cell * 2.5f, cell * 2.5f)
            mark.pixels.forEachIndexed { y, row ->
                repeat(5) { x ->
                    if (row and (16 shr x) != 0) drawRect(markColor, origin + Offset(x * cell, y * cell), Size(cell * 0.83f, cell * 0.83f))
                }
            }
        }
        return
    }
    val host = LocalAvatarSurface.current
    val clock = host?.clock
    val subscribed = clock != null && host.avatarsEnabled
    if (subscribed) DisposableEffect(clock) {
        val playback = checkNotNull(clock)
        playback.visibleAvatars++
        onDispose { playback.visibleAvatars-- }
    }
    val started = remember(clock, providerId, mood) { clock?.millis ?: 0L }
    val awake = remember(sprite) { sprite.awake.asImageBitmap() }
    val blink = remember(sprite) { sprite.blinking.asImageBitmap() }
    Canvas(modifier) {
        val animated = subscribed && clock?.running == true
        val age = if (animated) checkNotNull(clock).millis - started else 0L
        val frame = AvatarMotion.frame(mood, age, animated)
        val unit = size.minDimension
        if (frame.glow > 0f) {
            drawCircle(accent.copy(alpha = frame.glow * 0.5f), unit * 0.48f)
            drawCircle(accent.copy(alpha = frame.glow), unit * 0.39f)
        }
        val image = if (frame.blink) blink else awake
        val scale = unit * 0.79f / maxOf(image.width, image.height)
        val w = (image.width * scale).roundToInt().coerceAtLeast(1)
        val h = (image.height * scale).roundToInt().coerceAtLeast(1)
        val origin = IntOffset((center.x - w / 2f).roundToInt(), (center.y - h / 2f + unit * 0.04f).roundToInt())
        withTransform({
            translate(frame.x * unit, frame.y * unit)
            rotate(frame.tilt)
            scale(frame.scaleX, frame.scaleY)
        }) {
            drawImage(image, dstOffset = origin, dstSize = IntSize(w, h), filterQuality = FilterQuality.None)
        }
        avatarEffects(frame.effect, age, accent, unit)
    }
}

private fun DrawScope.avatarEffects(effect: AvatarMood, age: Long, accent: Color, unit: Float) {
    fun pixel(x: Float, y: Float, width: Float = 0.043f, color: Color = accent) {
        val edge = (unit * width).roundToInt().coerceAtLeast(1).toFloat()
        drawRect(color, Offset((size.width * x).roundToInt().toFloat(), (size.height * y).roundToInt().toFloat()), Size(edge, edge))
    }
    fun glyph(rows: IntArray, x: Float, y: Float, step: Float) {
        rows.forEachIndexed { row, bits ->
            repeat(3) { col -> if (bits and (4 shr col) != 0) pixel(x + step * col, y + step * row, step) }
        }
    }
    val radians = (age % 2_200) / 2_200.0 * 2.0 * Math.PI
    when (effect) {
        AvatarMood.TRACKING, AvatarMood.THINKING -> repeat(3) { i ->
            val phase = radians + i * 2.0 * Math.PI / 3
            if (effect == AvatarMood.TRACKING || (age / 260 + i) % 3 != 0L) {
                pixel(0.48f + cos(phase).toFloat() * 0.27f, 0.085f + sin(phase).toFloat() * 0.067f)
            }
            if (effect == AvatarMood.THINKING && i <= (age / 230) % 3) pixel(0.39f + i * 0.09f, 0.93f)
        }
        AvatarMood.WORKING -> repeat(3) { i ->
            val phase = ((age + i * 110) % 470) / 470f
            pixel(if (i == 1) 0.91f else 0.045f, 0.35f + phase * 0.48f, color = accent.copy(alpha = 1f - phase * 0.64f))
        }
        AvatarMood.COMPLETED -> repeat(4) { i ->
            val travel = age / AvatarMotion.CELEBRATION_MS.toFloat()
            val x = 0.49f + cos(i * Math.PI / 2 + Math.PI / 4).toFloat() * (0.21f + travel * 0.41f)
            val y = 0.47f + sin(i * Math.PI / 2 + Math.PI / 4).toFloat() * (0.21f + travel * 0.41f)
            pixel(x, y, 0.052f)
            pixel(x + 0.051f, y - 0.041f, 0.033f, accent.copy(alpha = 0.7f))
        }
        AvatarMood.WAITING -> glyph(QUESTION_PIXELS, 0.79f, 0.02f + sin(radians).toFloat() * 0.025f, 0.048f)
        AvatarMood.ERROR -> glyph(WARNING_PIXELS, 0.81f, 0.04f, 0.045f)
        AvatarMood.DISCONNECTED -> {
            val progress = (age % 1_900) / 1_900f
            glyph(SLEEP_PIXELS, 0.74f + progress * 0.11f, 0.11f - progress * 0.10f, 0.039f)
        }
        AvatarMood.IDLE -> Unit
    }
}

private val QUESTION_PIXELS = intArrayOf(7, 1, 3, 0, 2)
private val WARNING_PIXELS = intArrayOf(2, 2, 2, 0, 2)
private val SLEEP_PIXELS = intArrayOf(7, 1, 2, 4, 7)
