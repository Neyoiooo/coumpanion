package com.coucou.android.core

import java.net.URI
import java.util.Base64
import java.util.UUID

/** A launch target and local appearance. Notification access uses WatchedAiApp independently. */
@ConsistentCopyVisibility
data class AiProvider private constructor(
    val id: String,
    val label: String,
    val packageName: String?,
    val website: String?,
    val accent: Int?,
    val autoTrack: Boolean?,
    val logoToken: String?,
) {
    val isCustom: Boolean get() = id.startsWith(CUSTOM_PREFIX)

    companion object {
        private const val CUSTOM_PREFIX = "custom:"
        private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

        val builtIn = WatchedAiApp.entries.map {
            AiProvider(it.packageName, it.label, it.packageName, null, null, null, null)
        }

        fun custom(
            name: String,
            native: Boolean,
            target: String,
            accent: Int?,
            autoTrack: Boolean?,
            logoToken: String? = null,
            id: String = CUSTOM_PREFIX + UUID.randomUUID(),
        ): AiProvider? {
            val label = name.trim()
            val address = target.trim()
            if (label.isEmpty() || label.length > 64 || label.any { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }) return null
            if (!id.startsWith(CUSTOM_PREFIX) || !validLogoToken(id.removePrefix(CUSTOM_PREFIX))) return null
            if (logoToken != null && !validLogoToken(logoToken)) return null
            if (accent != null && accent !in 0..0xFFFFFF) return null
            if (native && (address.length > 255 || !packagePattern.matches(address))) return null
            if (!native && (address.length > 2_048 || !validHttps(address))) return null
            return AiProvider(id, label, address.takeIf { native }, address.takeUnless { native }, accent, autoTrack, logoToken)
        }

        internal fun validLogoToken(value: String): Boolean =
            runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)

        private fun validHttps(value: String): Boolean {
            val uri = runCatching { URI(value) }.getOrNull() ?: return false
            return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null && (uri.port == -1 || uri.port in 1..65_535)
        }
    }
}

internal fun encodeLocalField(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

internal fun decodeLocalField(value: String): String =
    Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

internal fun KeyValueStore.safeString(key: String): String = runCatching { getString(key, "") }.getOrDefault("")
