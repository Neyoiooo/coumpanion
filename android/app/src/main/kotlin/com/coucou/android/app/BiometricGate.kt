package com.coucou.android.app

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.os.PowerManager

/**
 * One device-credential check for Allow and Answer while the screen stays on. Trust is held
 * in memory and is discarded at screen-off or when Android reports that the device is locked.
 */
object BiometricGate {
    private const val AUTH = BiometricManager.Authenticators.DEVICE_CREDENTIAL
    private var screenWatch: ScreenWatch? = null
    private var screenGeneration = 0
    private var verified = false

    private fun reset() {
        verified = false
        screenGeneration++
    }

    private fun unlocked(activity: Activity): Boolean =
        activity.getSystemService(PowerManager::class.java).isInteractive &&
            !activity.getSystemService(KeyguardManager::class.java).isDeviceLocked

    fun available(activity: Activity): Boolean =
        activity.getSystemService(BiometricManager::class.java).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun confirm(activity: Activity, title: String, subtitle: String, noLockMessage: String, onSuccess: () -> Unit, onFail: (String) -> Unit) {
        if (!unlocked(activity)) { reset(); onFail(""); return }
        if (!available(activity)) { reset(); onFail(noLockMessage); return }
        if (verified) { onSuccess(); return }
        val watch = screenWatch ?: ScreenWatch(activity) { on -> if (!on) reset() }.also { screenWatch = it }
        val canReuse = watch.start()
        val generation = screenGeneration
        BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTH)
            .build()
            .authenticate(
                CancellationSignal(), activity.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (generation != screenGeneration || activity.isDestroyed || !unlocked(activity)) {
                            onFail("")
                            return
                        }
                        verified = canReuse
                        onSuccess()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        onFail(if (cancelled) "" else errString.toString()) // empty: the user backed out, say nothing
                    }
                },
            )
    }
}
