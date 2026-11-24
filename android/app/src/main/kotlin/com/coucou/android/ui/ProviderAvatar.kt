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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.coucou.android.app.AvatarArtwork
import com.coucou.android.core.AiProvider
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.AvatarBodyMotion
import com.coucou.android.core.IslandPalette
import com.coucou.android.core.MotionChoice
import com.coucou.android.core.MotionPolicy
import com.coucou.android.core.ProviderAppearance
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    provider: AiProvider? = null,
    fallback: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current.applicationContext
    val appearance = remember(providerId, name, provider) {
        provider?.let { ProviderAppearance.forProvider(it) } ?: ProviderAppearance.resolve(providerId, name)
    }
    val sprite = remember(context, appearance) { AvatarArtwork.sprite(context, appearance) }
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
    val faces = remember(context) { AvatarArtwork.faces(context) }
    val host = LocalAvatarSurface.current
    val clock = host?.clock
    val subscribed = faces != null && clock != null && host.avatarsEnabled
    if (subscribed) DisposableEffect(clock) {
        val playback = checkNotNull(clock)
        playback.visibleAvatars++
        onDispose { playback.visibleAvatars-- }
    }
    val started = remember(clock, providerId, mood) { clock?.millis ?: 0L }
    val image = remember(sprite) { sprite.asImageBitmap() }
    val atlas = remember(faces) { faces?.atlas?.asImageBitmap() }
    val pose by remember(faces, clock, subscribed, started, mood) {
        derivedStateOf(structuralEqualityPolicy()) {
            val animate = subscribed && clock?.running == true
            val age = if (animate) checkNotNull(clock).millis - started else 0L
            faces?.motion?.frame(mood, age, animate) ?: 0
        }
    }
    val movement by remember(clock, subscribed, started, mood) {
        derivedStateOf(structuralEqualityPolicy()) {
            val animate = subscribed && clock?.running == true && mood != AvatarMood.DISCONNECTED
            val age = if (animate) checkNotNull(clock).millis - started else 0L
            AvatarBodyMotion.at(mood, age, animate)
        }
    }
    Canvas(modifier) {
        val scale = size.minDimension * 0.90f / maxOf(image.width, image.height)
        val w = (image.width * scale).roundToInt().coerceAtLeast(1)
        val h = (image.height * scale).roundToInt().coerceAtLeast(1)
        val origin = IntOffset((center.x - w / 2f).roundToInt(), (center.y - h / 2f).roundToInt())
        val destination = IntSize(w, h)
        val body = movement
        withTransform({
            translate(body.x * size.minDimension, body.y * size.minDimension)
            rotate(body.tilt)
            scale(1f, body.stretch)
        }) {
            drawImage(image, dstOffset = origin, dstSize = destination, filterQuality = FilterQuality.None)
            if (faces != null && atlas != null) {
                drawImage(
                    atlas,
                    srcOffset = IntOffset(faces.tileWidth * pose, 0),
                    srcSize = IntSize(faces.tileWidth, atlas.height),
                    dstOffset = origin,
                    dstSize = destination,
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}
