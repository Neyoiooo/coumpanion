package com.coucou.android.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.LruCache
import com.coucou.android.core.ProviderAppearance
import kotlin.math.roundToInt

/** One master image. Provider colors, badges and blink frames are derived locally and cached. */
internal object AvatarArtwork {
    private const val MASTER = "avatars/robot.png"
    private const val BADGES = "avatars/badges"
    private const val MAX_EDGE = 256
    private const val CACHE_BYTES = 8 * 1024 * 1024

    internal data class Sprite(val awake: Bitmap, val blinking: Bitmap)

    private var master: Bitmap? = null
    private var loaded = false
    private val sprites = object : LruCache<ProviderAppearance, Sprite>(CACHE_BYTES) {
        override fun sizeOf(key: ProviderAppearance, value: Sprite): Int =
            value.awake.allocationByteCount + value.blinking.allocationByteCount
    }
    private data class WidgetKey(val appearance: ProviderAppearance, val edge: Int)
    private val widgetSprites = object : LruCache<WidgetKey, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: WidgetKey, value: Bitmap): Int = value.allocationByteCount
    }

    @Synchronized
    fun sprite(context: Context, appearance: ProviderAppearance): Sprite? {
        sprites.get(appearance)?.let { return it }
        val source = master(context) ?: return null
        val awake = colorShell(source, appearance)
        drawBadge(context, awake, appearance)
        return Sprite(awake, blink(awake)).also { sprites.put(appearance, it) }
    }

    @Synchronized
    fun available(context: Context): Boolean = master(context) != null

    private fun master(context: Context): Bitmap? {
        if (!loaded) {
            master = runCatching { context.assets.open(MASTER).use(BitmapFactory::decodeStream)?.let(::trim) }.getOrNull()
            loaded = true
        }
        return master
    }

    @Synchronized
    fun widget(context: Context, appearance: ProviderAppearance, edge: Int): Bitmap? {
        val key = WidgetKey(appearance, edge.coerceIn(1, MAX_EDGE))
        widgetSprites.get(key)?.let { return it }
        val image = sprite(context, appearance)?.awake ?: return null
        val scale = key.edge.toFloat() / maxOf(image.width, image.height)
        val width = (image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (image.height * scale).roundToInt().coerceAtLeast(1)
        val out = Bitmap.createBitmap(key.edge, key.edge, Bitmap.Config.ARGB_8888)
        val x = (key.edge - width) / 2
        val y = (key.edge - height) / 2
        Canvas(out).drawBitmap(image, null, Rect(x, y, x + width, y + height), Paint())
        return out.also { widgetSprites.put(key, it) }
    }

    /** Trim transparent margins once without filtering the pixel artwork. */
    private fun trim(bitmap: Bitmap): Bitmap {
        val pixels = bitmap.pixels()
        val cleared = clearBlackBackdrop(pixels, bitmap.width, bitmap.height)
        var left = bitmap.width
        var top = bitmap.height
        var right = -1
        var bottom = -1
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(pixels[y * bitmap.width + x]) < 16) continue
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
        if (right < left) return bitmap
        val width = right - left + 1
        val height = bottom - top + 1
        val cropped = if (cleared) Bitmap.createBitmap(pixels, top * bitmap.width + left, bitmap.width, width, height, Bitmap.Config.ARGB_8888)
        else Bitmap.createBitmap(bitmap, left, top, width, height)
        val scale = MAX_EDGE.toFloat() / maxOf(cropped.width, cropped.height)
        if (scale >= 1f) return cropped
        return Bitmap.createScaledBitmap(cropped, (scale * cropped.width).roundToInt(), (scale * cropped.height).roundToInt(), false)
    }

    /** A flattened export can have a black background. Clear only near-black pixels connected to its edge. */
    private fun clearBlackBackdrop(pixels: IntArray, width: Int, height: Int): Boolean {
        fun black(p: Int) = Color.alpha(p) > 240 && maxOf(Color.red(p), Color.green(p), Color.blue(p)) <= 12
        if (!listOf(0, width - 1, pixels.size - width, pixels.lastIndex).all { black(pixels[it]) }) return false
        val pending = IntArray(pixels.size)
        var start = 0
        var end = 0
        fun visit(index: Int) {
            if (!black(pixels[index])) return
            pixels[index] = Color.TRANSPARENT
            pending[end++] = index
        }
        for (x in 0 until width) {
            visit(x)
            visit(pixels.size - width + x)
        }
        for (y in 0 until height) {
            visit(y * width)
            visit((y + 1) * width - 1)
        }
        while (start < end) {
            val i = pending[start++]
            if (i % width > 0) visit(i - 1)
            if (i % width < width - 1) visit(i + 1)
            if (i >= width) visit(i - width)
            if (i < pixels.size - width) visit(i + width)
        }
        return true
    }

    private fun colorShell(source: Bitmap, appearance: ProviderAppearance): Bitmap {
        val pixels = source.pixels()
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val i = y * source.width + x
                val p = pixels[i]
                val r = Color.red(p)
                val b = Color.blue(p)
                val nx = x.toFloat() / source.width
                val ny = y.toFloat() / source.height
                val cheek = ny in 0.54f..0.62f && (nx in 0.21f..0.32f || nx in 0.67f..0.80f)
                if (Color.alpha(p) == 0 || cheek || b - r < 4 || b < 77 || r > 226) continue
                val tint = colorAt(appearance.colors, nx)
                // Keep the luminance differences that make the original dither readable.
                val light = (r + Color.green(p) + b) / 3f
                fun shade(channel: Int): Int = when {
                    light < 158 -> (channel * (0.28f + 0.72f * light / 158f)).roundToInt()
                    else -> (channel + (255 - channel) * (light - 158f) / 97f).roundToInt()
                }
                pixels[i] = Color.argb(Color.alpha(p), shade(Color.red(tint)), shade(Color.green(tint)), shade(Color.blue(tint)))
            }
        }
        return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun colorAt(colors: List<Int>, x: Float): Int {
        if (colors.size == 1) return colors.first()
        val point = x * colors.lastIndex
        val index = point.toInt().coerceAtMost(colors.lastIndex - 1)
        val t = point - index
        val start = colors[index]
        val end = colors[index + 1]
        fun mix(a: Int, b: Int) = (a + (b - a) * t).roundToInt()
        return Color.rgb(mix(Color.red(start), Color.red(end)), mix(Color.green(start), Color.green(end)), mix(Color.blue(start), Color.blue(end)))
    }

    private fun drawBadge(context: Context, image: Bitmap, appearance: ProviderAppearance) {
        val canvas = Canvas(image)
        val edge = (image.width * 0.117f).roundToInt().coerceAtLeast(5)
        val x = (image.width - edge) / 2
        val y = (image.height * 0.827f - edge / 2f).roundToInt()
        val logo = appearance.logo?.let { name ->
            runCatching { context.assets.open("$BADGES/$name.png").use(BitmapFactory::decodeStream) }.getOrNull()
        }
        if (logo != null) {
            canvas.drawBitmap(logo, null, Rect(x, y, x + edge, y + edge), Paint(Paint.FILTER_BITMAP_FLAG))
            return
        }
        val icon = appearance.iconPackage?.let { runCatching { context.packageManager.getApplicationIcon(it) }.getOrNull() }
        if (icon != null) {
            val clip = android.graphics.Path().apply {
                addCircle(x + edge / 2f, y + edge / 2f, edge / 2f, android.graphics.Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clip)
            icon.setBounds(x, y, x + edge, y + edge)
            icon.draw(canvas)
            canvas.restore()
            return
        }
        val cell = (edge / 5).coerceAtLeast(1)
        val cx = x + edge / 2f
        val cy = y + edge / 2f
        val paint = Paint().apply { color = Color.WHITE }
        val half = cell / 2f
        val arm = cell * 1.5f
        canvas.drawRect(cx - arm, cy - half, cx + arm, cy + half, paint)
        canvas.drawRect(cx - half, cy - arm, cx + half, cy + arm, paint)
    }

    private fun blink(image: Bitmap): Bitmap {
        val out = image.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val face = out.getPixel((w * 0.49f).toInt(), (h * 0.42f).toInt())
        val eye = out.getPixel((w * 0.35f).toInt(), (h * 0.50f).toInt())
        val paint = Paint()
        for (center in listOf(0.344f, 0.658f)) {
            paint.color = face
            canvas.drawRect(w * (center - 0.075f), h * 0.350f, w * (center + 0.075f), h * 0.557f, paint)
            paint.color = eye
            canvas.drawRect(w * (center - 0.053f), h * 0.492f, w * (center + 0.053f), h * 0.516f, paint)
        }
        return out
    }

    private fun Bitmap.pixels() = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
}
