package com.coucou.android.app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PersistableBundle

internal object ProviderPrompt {
    fun destination(context: Context, packageName: String?): Intent? {
        if (packageName == null) return null
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").setPackage(packageName)
        return runCatching {
            @Suppress("DEPRECATION")
            val component = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            intent.takeIf { component?.exported == true && component.packageName == packageName }
        }.getOrNull()
    }

    fun share(context: Context, destination: Intent, text: String): Boolean =
        runCatching { context.startActivity(Intent(destination).putExtra(Intent.EXTRA_TEXT, text)) }.isSuccess

    fun copy(context: Context, text: String) {
        val clip = ClipData.newPlainText("", text)
        clip.description.extras = PersistableBundle().apply {
            putBoolean(if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE", true)
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
}
