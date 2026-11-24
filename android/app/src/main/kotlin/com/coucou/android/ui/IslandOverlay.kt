package com.coucou.android.ui

import android.annotation.SuppressLint
import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.coucou.android.R
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.BotState
import com.coucou.android.core.IconKind
import com.coucou.android.core.IslandBox
import com.coucou.android.core.IslandGeometry
import com.coucou.android.core.IslandPalette
import com.coucou.android.core.IslandPosition
import com.coucou.android.core.IslandSpec
import com.coucou.android.core.IslandSurface
import com.coucou.android.core.IslandTimeline
import com.coucou.android.core.MotionPolicy
import com.coucou.android.core.MotionState
import com.coucou.android.core.OverlayPolicy
import com.coucou.android.core.PxRect
import com.coucou.android.core.Tokens
import com.coucou.android.core.Tracked
import com.coucou.android.core.WatchedAiApp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A pill over other apps, aligned to the camera but drawn below Android's cutout and status bar.
 * Hold a status to reveal its details. Results still have the timeline's 10-second hold, and
 * approval buttons stay available as soon as a request appears.
 *
 * When it opens and closes is decided by [IslandTimeline] (pure, tested); this class only draws it
 * and keeps one timer, for the next deadline. Nothing exists while it is hidden: the window is added
 * when it opens and removed when it has gone up, so there is no timer and no frame loop.
 * Needs the user's one-time "display over other apps" permission.
 */
class IslandOverlay(
    private val context: Context,
    private val clock: () -> Double,
    private val onAllow: (fingerprint: String) -> Unit,
    private val onDeny: (fingerprint: String) -> Unit,
    private val onOpen: () -> Unit,
    /** The user swiped the island up: what it showed now belongs in the notification shade. */
    private val onSwiped: (IslandSpec) -> Unit = {},
    private val onOpenWatchedApp: (packageName: String) -> Unit,
    private val onStopTracking: (WatchedAiApp) -> Unit,
    private val position: () -> IslandPosition,
    private val plain: () -> Boolean,
    private val animatedAvatars: () -> Boolean,
) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val timeline = IslandTimeline(clock)

    private var view: ComposeView? = null
    private var owner: Owner? = null
    private var box: IslandBox? = null
    private var shownAt = 0.0

    private var spec by mutableStateOf<IslandSpec?>(null)
    private var expanded by mutableStateOf(false)
    private var approvalContentHeight by mutableIntStateOf(0)
    private var sessionsContentHeight by mutableIntStateOf(0)
    private var glowMotion by mutableStateOf(false)
    private var avatarMotionVisible by mutableStateOf(false)
    private val width = Tracked(0.0)
    private val height = Tracked(0.0)
    private val radius = Tracked(0.0)
    /** Bumped whenever a new target is set, to (re)start the frame loop that only runs while moving. */
    private var motionKey by mutableIntStateOf(0)

    private val deadline = Runnable { safely { timeline.tick(); sync() } }

    /**
     * A rotation, a change of screen resolution or of font size while the island is open: measure again, so it never keeps
     * the box of the old screen. Registered only while the window exists, so nothing listens while it is hidden.
     */
    private val screenChanged = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) { main.post { remeasure() } }
        override fun onLowMemory() = Unit
    }
    private var watchingScreen = false

    fun permitted(): Boolean = Settings.canDrawOverlays(context)

    /** Something is going on right now: an agent works, a question waits, a request waits. Null: nothing. */
    fun setActive(next: IslandSpec?) = safely {
        timeline.setActive(next)
        sync()
    }

    /** An agent finished, failed or hit a limit: shown for 10 s, then the island goes up. */
    fun flash(next: IslandSpec) = safely {
        timeline.flash(next)
        sync()
    }

    fun showWatchedUpdate(next: IslandSpec) {
        if (seenByUser()) flash(next)
    }

    /** Clear notification text as soon as access is revoked or the screen locks. */
    fun clearWatchedUpdate() = safely {
        timeline.clearFlash(IslandSpec.Kind.APP_UPDATE)
        // Remove the outgoing title immediately rather than animating it during a screen lock.
        if (timeline.current?.kind == IslandSpec.Kind.APP_UPDATE) timeline.retractFinished()
        sync()
    }

    /** The user swiped up or chose "Dismiss" in TalkBack. */
    fun swipeAway() = safely {
        val shown = timeline.current?.takeIf { timeline.phase == IslandTimeline.Phase.OPEN }
        timeline.swipeAway()
        sync()
        shown?.let(onSwiped)
    }

    /** Close at once, without animation (Coucou came to the front, the switch went off, the link dropped). */
    fun hide() = safely {
        timeline.dismiss()
        removeView()
    }

    /** True when the island is on screen showing a request: the notification can then stay quiet. */
    fun isShowingRequest(): Boolean =
        view != null && timeline.phase == IslandTimeline.Phase.OPEN && timeline.current?.kind == IslandSpec.Kind.APPROVAL &&
            seenByUser()

    /** The island is a window of its own: nobody sees it with the screen off, in a screen saver or behind the lock screen. */
    fun seenByUser(): Boolean = runCatching {
        context.getSystemService(android.os.PowerManager::class.java).isInteractive &&
            !context.getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
    }.getOrDefault(false)

    /** The island is a nicety on top of the app: nothing that goes wrong with the window may reach the caller. */
    private inline fun safely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "island failed: ${e.javaClass.simpleName}")
            runCatching { timeline.dismiss(); removeView() }
        }
    }

    // ── From the timeline to the window ──────────────────────────────────────

    private fun sync() {
        main.removeCallbacks(deadline)
        val phase = timeline.phase
        val current = timeline.current
        Log.d(TAG, "phase=$phase content=${current?.kind}")
        if (phase == IslandTimeline.Phase.HIDDEN || current == null) {
            removeView()
            return
        }
        val opening = view == null
        if (!ensureView()) return
        val b = box ?: return
        if (spec?.kind != current.kind || spec?.fingerprint != current.fingerprint ||
            (current.kind == IslandSpec.Kind.APP_UPDATE && spec != current)) shownAt = clock()
        val sameGroup = spec?.manualTracking == true && current.manualTracking
        if (spec?.kind != current.kind || (!sameGroup && spec?.pillId != current.pillId) || spec?.fingerprint != current.fingerprint) {
            expanded = false
            sessionsContentHeight = 0
        }
        spec = current
        val now = clock()
        if (opening) {
            width.jump(b.seedWidth.toDouble()); height.jump(0.0); radius.jump(b.panelCorner.toDouble())
        }
        val reduced = reducedMotion()
        avatarMotionVisible = phase == IslandTimeline.Phase.OPEN
        glowMotion = !reduced && !plain() && !current.manualTracking && current.kind != IslandSpec.Kind.APP_UPDATE && phase == IslandTimeline.Phase.OPEN
        if (phase == IslandTimeline.Phase.OPEN) {
            val w = IslandGeometry.sizeFor(b, current.kind, expanded).first
            val h = targetHeight(b, current.kind)
            val corner = (if (expanded || current.kind == IslandSpec.Kind.APPROVAL) b.panelCorner else h / 2).toDouble()
            // "Remove animations": no spring, the island simply appears at its size.
            if (reduced) { width.jump(w.toDouble()); height.jump(h.toDouble()); radius.jump(corner) }
            else { width.goTo(w.toDouble(), now); height.goTo(h.toDouble(), now); radius.goTo(corner, now) }
        } else if (reduced) {
            width.jump(b.seedWidth.toDouble()); height.jump(0.0); radius.jump(b.panelCorner.toDouble())
        } else {
            width.curveTowards(b.seedWidth.toDouble(), now); height.curveTowards(0.0, now); radius.curveTowards(b.panelCorner.toDouble(), now)
        }
        positionWindow()
        motionKey++
        timeline.nextDeadline()?.let { main.postDelayed(deadline, (it - clock()).toLong().coerceAtLeast(0)) }
    }

    private fun reducedMotion() = MotionPolicy.reduced(
        MotionState.choice,
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f),
    )

    private fun targetHeight(b: IslandBox, kind: IslandSpec.Kind): Int = when {
        kind == IslandSpec.Kind.APPROVAL && approvalContentHeight > 0 -> approvalContentHeight
        expanded && spec?.manualSessions?.isNotEmpty() == true && sessionsContentHeight > 0 -> sessionsContentHeight
        spec?.manualSessions?.isNotEmpty() == true && !expanded -> trackingHeaderHeight(b)
        else -> IslandGeometry.sizeFor(b, kind, expanded).second
    }

    private fun trackingHeaderHeight(b: IslandBox) = maxOf(b.workingH, (48 * context.resources.displayMetrics.density).roundToInt())

    private fun measureSessions(pixels: Int) = safely {
        val b = box ?: return@safely
        if (pixels <= 0 || !expanded || sessionsContentHeight == pixels) return@safely
        sessionsContentHeight = pixels
        if (timeline.phase == IslandTimeline.Phase.OPEN) {
            val h = targetHeight(b, IslandSpec.Kind.WORKING).toDouble()
            if (reducedMotion()) height.jump(h) else height.goTo(h, clock())
            motionKey++
        }
    }

    private fun toggleDetails() = safely {
        if (timeline.phase != IslandTimeline.Phase.OPEN || spec?.kind == IslandSpec.Kind.APPROVAL) return@safely
        expanded = !expanded
        sync()
    }

    /** Fit the translated request, buttons and lock hint to the measured content. */
    private fun measureApproval(pixels: Int) = safely {
        val b = box ?: return@safely
        if (pixels == 0 || approvalContentHeight == pixels || spec?.kind != IslandSpec.Kind.APPROVAL) return@safely
        approvalContentHeight = pixels
        if (timeline.phase == IslandTimeline.Phase.OPEN) {
            val h = targetHeight(b, IslandSpec.Kind.APPROVAL).toDouble()
            if (reducedMotion()) height.jump(h) else height.goTo(h, clock())
            motionKey++
        }
    }

    /** The screen changed under the open island: new box, same window moved to the new centre, then the usual sync. */
    private fun remeasure() = safely {
        if (view == null) return@safely
        val b = measure()
        if (b == box) return@safely
        box = b
        sync()
    }

    private fun visibleWidth(b: IslandBox): Int = width.value.roundToInt().coerceIn(1, b.maxWidth)

    private fun positionWindow() {
        val v = view ?: return
        val b = box ?: return
        val params = v.layoutParams as? WindowManager.LayoutParams ?: return
        val left = b.leftFor(visibleWidth(b), position())
        val secure = WindowManager.LayoutParams.FLAG_SECURE
        val flags = if (spec?.kind == IslandSpec.Kind.APP_UPDATE) params.flags or secure else params.flags and secure.inv()
        if (params.x == left && params.y == b.windowTop && params.flags == flags) return
        params.x = left
        params.y = b.windowTop
        params.flags = flags
        wm.updateViewLayout(v, params)
    }

    private fun removeView() {
        main.removeCallbacks(deadline)
        if (watchingScreen) {
            runCatching { context.unregisterComponentCallbacks(screenChanged) }
            watchingScreen = false
        }
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        owner?.destroy()
        owner = null
        spec = null
        expanded = false
        approvalContentHeight = 0
        sessionsContentHeight = 0
    }

    private fun measure(): IslandBox {
        val density = context.resources.displayMetrics.density
        val metrics = wm.currentWindowMetrics
        val insets = metrics.windowInsets
        val cutout = insets.displayCutout
        val topCutout = cutout?.boundingRectTop?.let { PxRect(it.left, it.top, it.right, it.bottom) }
        val statusBar = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
        val screenWidth = metrics.bounds.width()
        val panelWidth = runCatching {
            val mode = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY).mode
            if (screenWidth <= metrics.bounds.height()) minOf(mode.physicalWidth, mode.physicalHeight)
            else maxOf(mode.physicalWidth, mode.physicalHeight)
        }.getOrDefault(screenWidth)
        return IslandGeometry.place(
            screenWidth, density, topCutout, statusBar,
            safeTop = cutout?.safeInsetTop ?: 0,
            safeLeft = cutout?.safeInsetLeft ?: 0,
            safeRight = cutout?.safeInsetRight ?: 0,
            panelWidth = panelWidth,
            fontScale = context.resources.configuration.fontScale,
        )
    }

    @SuppressLint("RtlHardcoded") // x is a physical display coordinate.
    private fun ensureView(): Boolean {
        if (view != null) return true
        val b = measure()
        box = b
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Position in display coordinates. The measured x and y keep the pill clear of cutouts.
            // Touches outside this non-modal window go to the app underneath.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = b.leftFor(b.seedWidth, position())
            y = b.windowTop
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        val lifecycleOwner = Owner()
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent { Island() }
        }
        return try {
            wm.addView(v, params)
            owner = lifecycleOwner
            view = v
            context.registerComponentCallbacks(screenChanged)
            watchingScreen = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "cannot add the window: ${e.javaClass.simpleName}") // permission taken away meanwhile
            lifecycleOwner.destroy()
            false
        }
    }

    /** Every tap goes through here: one made in the first moments is ignored (see OverlayPolicy). */
    private fun tap(what: String, action: () -> Unit) {
        if (!OverlayPolicy.tapAccepted(shownAt, clock())) {
            Log.d(TAG, "ignored an early tap on $what")
            return
        }
        action()
    }

    // ── What is drawn ────────────────────────────────────────────────────────

    private val dim = Color(IslandSurface.TEXT_DIM)
    private val foreground = Color(IslandSurface.TEXT)

    private fun Modifier.islandBackground(palette: IslandPalette, sweep: () -> Float) = drawWithCache {
        val fill = Brush.verticalGradient(listOf(Color(0xFF191A22), Color(0xFF0D0E14)))
        val glow = listOf(Color(palette.glow).copy(alpha = 0.16f), Color(palette.glow).copy(alpha = 0f))
        val trail = listOf(Color(palette.trail).copy(alpha = 0.10f), Color(palette.trail).copy(alpha = 0f))
        onDrawBehind {
            drawRect(fill)
            if (size.height > 0f && size.width > 0f) {
                val travel = sweep()
                drawRect(Brush.radialGradient(trail, Offset(size.width * (1f - travel), 0f), size.width * 0.65f))
                drawRect(Brush.radialGradient(glow, Offset(size.width * travel, size.height * 0.8f), size.width * 0.62f))
            }
        }
    }

    @Composable
    private fun Island() {
        var frame by remember { mutableLongStateOf(0L) }
        // The window's shape only needs frame callbacks while it is growing or retracting.
        LaunchedEffect(motionKey) {
            var last = 0L
            while (width.animating || height.animating || radius.animating) {
                androidx.compose.runtime.withFrameNanos { ns ->
                    val dt = if (last == 0L) 1.0 / 60 else ((ns - last) / 1e9).coerceIn(0.0, 0.1)
                    last = ns
                    val now = clock()
                    width.step(dt, now); height.step(dt, now); radius.step(dt, now)
                    safely { positionWindow() }
                    frame = ns
                }
            }
            if (timeline.phase == IslandTimeline.Phase.RETRACTING) {
                timeline.retractFinished()
                removeView()
            }
        }
        frame // read, so the island redraws as it moves
        val b = box ?: return
        val s = spec ?: return
        val density = LocalDensity.current
        fun px(v: Double): Dp = with(density) { v.toFloat().toDp() }
        val approval = s.kind == IslandSpec.Kind.APPROVAL
        val (compactW, defaultCompactH) = IslandGeometry.sizeFor(b, s.kind)
        val sessionList = s.manualSessions.isNotEmpty()
        val compactH = if (sessionList) trackingHeaderHeight(b) else defaultCompactH
        val shownHeight = height.value.coerceAtLeast(0.0)
        val reveal = if (approval) 0f else ((shownHeight - compactH) / (b.expandedH - compactH).coerceAtLeast(1))
            .coerceIn(0.0, 1.0).toFloat()
        val open = ((shownHeight / compactH.coerceAtLeast(1)) - 0.28) / 0.52
        val corner = minOf(radius.value, shownHeight / 2).coerceAtLeast(0.0)
        val palette = remember(s.pillId, s.agent) { IslandPalette.forAgent(s.pillId, s.agent) }
        val accent = Color(palette.label)
        val shape = RoundedCornerShape(px(corner))
        val borderBrush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), accent.copy(alpha = 0.14f)))
        AvatarMotionHost(animatedAvatars(), MotionState.choice, avatarMotionVisible, animateBackground = glowMotion) {
            CompositionLocalProvider(LocalTokens provides Tokens.DARK) {
                val avatarClock = LocalAvatarSurface.current?.clock
                val dismissLabel = stringResource(R.string.island_dismiss)
                val swipeThreshold = with(density) { SWIPE_UP_DP.dp.toPx() }
                Box(
                    Modifier.width(px(visibleWidth(b).toDouble())).height(px(shownHeight))
                        .clip(shape).then(
                            if (plain()) Modifier.background(Color(0xFF0D0E14))
                            else Modifier.islandBackground(palette) {
                                if (glowMotion && avatarClock?.running == true) {
                                    0.5f + 0.34f * sin(avatarClock.millis / 770.0).toFloat()
                                } else 0.25f
                            }
                        )
                        .border(1.dp, borderBrush, shape)
                        .pointerInput(s.kind, s.fingerprint, sessionList && expanded) {
                            if (sessionList && expanded) return@pointerInput
                            var travelled = 0f
                            detectVerticalDragGestures(
                                onDragStart = { travelled = 0f },
                                onDragEnd = { if (travelled <= -swipeThreshold) swipeAway() },
                                onDragCancel = { travelled = 0f },
                            ) { _, dy -> travelled += dy }
                        }
                        .semantics { customActions = listOf(CustomAccessibilityAction(dismissLabel) { swipeAway(); true }) },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    val content = Modifier.wrapContentHeight(Alignment.Top, unbounded = true)
                        .alpha(open.coerceIn(0.0, 1.0).toFloat())
                        .semantics { liveRegion = if (approval) LiveRegionMode.Assertive else LiveRegionMode.Polite }
                    if (approval) {
                        RequestCard(s, accent, content.requiredWidth(px(compactW.toDouble())).onSizeChanged { measureApproval(it.height) })
                    } else if (sessionList) {
                        val maxListHeight = remember(b, compactH) {
                            val metrics = wm.currentWindowMetrics
                            val bottom = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom
                            (metrics.bounds.height() - b.windowTop - bottom - compactH).coerceAtLeast(0)
                        }
                        val listWidth = if (expanded) b.expandedW else compactW
                        SessionStrip(s, palette, px(compactH.toDouble()), px(maxListHeight.toDouble()),
                            content.requiredWidth(px(listWidth.toDouble())))
                    } else {
                        StatusStrip(s, palette, px(compactH.toDouble()), reveal, content)
                    }
                }
            }
        }
    }

    @Composable
    private fun statusText(s: IslandSpec): String = stringResource(when (s.kind) {
        IslandSpec.Kind.WORKING -> when (s.state) {
            BotState.THINKING -> R.string.state_thinking
            BotState.SEARCHING -> R.string.state_searching
            else -> R.string.state_working
        }
        IslandSpec.Kind.ERROR -> R.string.notif_error
        IslandSpec.Kind.RATELIMIT -> R.string.notif_ratelimit
        IslandSpec.Kind.QUESTION -> R.string.notif_question
        IslandSpec.Kind.APP_UPDATE -> R.string.watch_apps_update
        else -> R.string.notif_finished
    })

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun SessionStrip(s: IslandSpec, palette: IslandPalette, headerHeight: Dp, maxListHeight: Dp, modifier: Modifier) {
        val sessions = s.manualSessions
        val label = if (s.manualTracking && sessions.size > 1) stringResource(R.string.ai_sessions) else s.agent
        val status = when {
            !s.manualTracking -> statusText(s)
            sessions.size > 1 -> stringResource(R.string.ai_tracking_count, sessions.size)
            else -> stringResource(R.string.manual_ai_tracking)
        }
        val toggle = stringResource(if (expanded) R.string.island_collapse_details else R.string.island_expand_details)
        Column(modifier.fillMaxWidth().onSizeChanged { measureSessions(it.height) }) {
            Row(
                Modifier.fillMaxWidth().height(headerHeight).combinedClickable(
                    onClickLabel = if (s.manualTracking) toggle else null,
                    onClick = { tap("the session strip") { if (s.manualTracking) toggleDetails() else onOpen() } },
                    onLongClickLabel = toggle,
                    onLongClick = { tap("the island long press") { toggleDetails() } },
                ).padding(horizontal = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProviderAvatar(s.pillId, s.agent, AvatarMood.forIsland(s), Modifier.size(27.dp))
                Text(label, Modifier.weight(1f), color = foreground, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, color = Color(palette.label), fontSize = 11.sp, maxLines = 1)
                CoucouIcon(IconKind.CHEVRON, Modifier.rotate(if (expanded) -90f else 90f), tint = dim, size = 12.dp, mirror = false)
            }
            if (expanded) {
                val now by trackingClock(sessions)
                Column(Modifier.heightIn(max = maxListHeight).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                    if (!s.manualTracking && s.text.isNotBlank()) Text(s.text, color = foreground, fontSize = 13.sp)
                    Text(stringResource(R.string.manual_ai_unconfirmed), Modifier.padding(vertical = 6.dp), color = dim, fontSize = 12.sp)
                    sessions.forEach { session ->
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            ProviderAvatar(session.app.packageName, session.app.label, AvatarMood.TRACKING, Modifier.size(32.dp))
                            Spacer(Modifier.width(6.dp))
                            Column(
                                Modifier.weight(1f).heightIn(min = 56.dp).clickable(role = Role.Button) {
                                    tap("a tracked app") { onOpenWatchedApp(session.app.packageName) }
                                }.padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(session.app.label, color = foreground, fontSize = 13.sp)
                                Text(elapsedTracking(session, now), color = dim, fontSize = 12.sp)
                            }
                            StopTrackingButton(session) { tap("stop tracking") { onStopTracking(session.app) } }
                        }
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    private fun StatusStrip(s: IslandSpec, palette: IslandPalette, headerHeight: Dp, reveal: Float, modifier: Modifier) {
        val status = statusText(s)
        val accent = Color(palette.label)
        Column(
            modifier.fillMaxWidth().combinedClickable(
                onClick = {
                    tap("the status strip") {
                        if (s.kind == IslandSpec.Kind.APP_UPDATE) onOpenWatchedApp(s.pillId) else onOpen()
                    }
                },
                onLongClick = { tap("the island long press") { toggleDetails() } },
                onLongClickLabel = stringResource(if (expanded) R.string.island_collapse_details else R.string.island_expand_details),
            ),
        ) {
            Row(
                Modifier.fillMaxWidth().height(headerHeight).padding(horizontal = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProviderAvatar(s.pillId, s.agent, AvatarMood.forIsland(s), Modifier.size(27.dp))
                Text(s.agent, Modifier.weight(1f), color = foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status, Modifier.widthIn(max = 74.dp), color = accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (expanded || reveal > 0f) {
                val detail = when {
                    s.kind == IslandSpec.Kind.APP_UPDATE -> listOf(stringResource(s.notificationEvent.labelRes()), s.text)
                        .filter { it.isNotBlank() }.joinToString(" · ")
                    s.kind == IslandSpec.Kind.QUESTION -> stringResource(R.string.island_answer_on_pc)
                    s.text.isBlank() -> stringResource(R.string.island_open_app)
                    else -> s.text
                }
                Column(Modifier.fillMaxWidth().graphicsLayer {
                    alpha = reveal
                    translationY = -(1f - reveal) * 8.dp.toPx()
                }.padding(horizontal = 13.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Text(detail, Modifier.padding(vertical = 9.dp), color = foreground, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun RequestCard(s: IslandSpec, accent: Color, modifier: Modifier) {
        val fingerprint = s.fingerprint ?: return
        Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.clickable { tap("the request header") { onOpen() } }, verticalAlignment = Alignment.CenterVertically) {
                ProviderAvatar(s.pillId, s.agent, AvatarMood.WAITING, Modifier.size(27.dp)) {
                    Box(Modifier.size(24.dp).background(accent.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                        CoucouIcon(IconKind.LOCK, tint = accent, size = 15.dp)
                    }
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.agent, color = dim, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // What kind of action, never the command: the exact text is in the app and in the lock prompt.
                    Text(stringResource(R.string.approval_wants, s.text), color = foreground, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.action_deny), { tap("Deny") { onDeny(fingerprint) } }, Modifier.weight(1f), onDark = true)
                if (s.canAllow) {
                    PillButton(
                        stringResource(R.string.link_approval), { tap("Review") { onAllow(fingerprint) } }, Modifier.weight(1f), PillKind.PRIMARY, onDark = true,
                    )
                }
            }
            Text(stringResource(if (s.canAllow) R.string.approval_hint else R.string.approval_on_computer), color = dim, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }

    /** A window outside any activity has no lifecycle of its own: Compose needs one to run. */
    private class Owner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
        }
    }

    private companion object {
        const val TAG = "CoumpanionIsland"
        /** How far up the finger must travel for a swipe to count (a little over a tap's wobble). */
        const val SWIPE_UP_DP = 40
    }
}
