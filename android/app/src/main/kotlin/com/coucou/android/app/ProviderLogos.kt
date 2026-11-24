package com.coucou.android.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.coucou.android.core.AiProvider
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** Imports only the rendered logo into private app storage. Source URI and metadata are discarded. */
internal object ProviderLogos {
    private const val EDGE = 128

    fun file(context: Context, token: String): File? = if (AiProvider.validLogoToken(token)) {
        File(File(context.filesDir, "ai-provider-logos"), "$token.png")
    } else null

    fun import(context: Context, uri: Uri): String? {
        val token = UUID.randomUUID().toString()
        val target = checkNotNull(file(context, token))
        val saved = runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth in 1..32_768 && bounds.outHeight in 1..32_768)
            val edge = maxOf(bounds.outWidth, bounds.outHeight)
            val sampling = Integer.highestOneBit((edge / EDGE).coerceAtLeast(1))
            val sample = BitmapFactory.Options().apply { inSampleSize = sampling }
            val original = checkNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, sample) })
            val factor = (EDGE.toFloat() / maxOf(original.width, original.height)).coerceAtMost(1f)
            val output = Bitmap.createScaledBitmap(original, (original.width * factor).roundToInt().coerceAtLeast(1),
                (original.height * factor).roundToInt().coerceAtLeast(1), true)
            target.parentFile?.mkdirs()
            target.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.getOrDefault(false)
        if (!saved) target.delete()
        return token.takeIf { saved }
    }

    fun forget(context: Context, token: String?) {
        token?.let { file(context, it)?.delete() }
    }
}
