package com.coucou.android.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coucou.android.MainActivity
import com.coucou.android.R
import com.coucou.android.core.AiProvider
import com.coucou.android.core.ShareDraft
import com.coucou.android.ui.AvatarMotionHost
import com.coucou.android.ui.CoucouTheme
import com.coucou.android.ui.PillButton
import com.coucou.android.ui.ProviderChoiceList
import com.coucou.android.ui.ScreenTitle
import com.coucou.android.ui.SettingSwitch

/** Receives a bounded text/plain prompt, kept in memory until the user chooses what to do with it. */
class ShareActivity : ComponentActivity() {
    private val model get() = (application as CoucouApp).model

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = intent
        val text = if (shared?.action == Intent.ACTION_SEND && shared.type == "text/plain") {
            runCatching { shared.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() }.getOrNull()
        } else null
        val clean = ShareDraft.clean(text) ?: run { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        model.refreshWatchApps()
        setContent {
            CoucouTheme(model.settings.theme, model.settings.dynamicColour) {
                AvatarMotionHost(model.settings.animatedAvatars, model.settings.motion) {
                    var provider by remember { mutableStateOf<AiProvider?>(null) }
                    var track by remember { mutableStateOf(model.settings.autoTrackLaunches) }
                    Surface(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ScreenTitle(stringResource(R.string.ai_share_title), onBack = {
                                if (provider == null) finish() else provider = null
                            })
                            val selected = provider
                            if (selected == null) {
                                ProviderChoiceList(model) {
                                    provider = it
                                    track = it.autoTrack ?: model.settings.autoTrackLaunches
                                }
                                if (ShareDraft.decide(true, model.chatTabState) == ShareDraft.Outcome.KEEP_AND_OPEN_CHAT) {
                                    PillButton(stringResource(R.string.ai_desktop_draft), {
                                        model.offerShare(clean)
                                        startActivity(Intent(this@ShareActivity, MainActivity::class.java)
                                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                        finish()
                                    }, Modifier.fillMaxWidth())
                                }
                            } else {
                                val destination = remember(selected) { ProviderPrompt.destination(this@ShareActivity, selected.packageName) }
                                Text(selected.label)
                                SettingSwitch(stringResource(R.string.ai_auto_track), null, track) { track = it }
                                if (destination != null) {
                                    PillButton(stringResource(R.string.ai_send_to_share_target), {
                                        if (ProviderPrompt.share(this@ShareActivity, destination, clean)) {
                                            if (track) model.setManualTracking(selected.id, true)
                                            finish()
                                        } else {
                                            Toast.makeText(this@ShareActivity, R.string.ai_provider_unavailable, Toast.LENGTH_SHORT).show()
                                        }
                                    }, Modifier.fillMaxWidth())
                                }
                                Text(stringResource(R.string.ai_share_manual_hint))
                                PillButton(stringResource(R.string.ai_copy_prompt), {
                                    ProviderPrompt.copy(this@ShareActivity, clean)
                                    Toast.makeText(this@ShareActivity, R.string.ai_prompt_copied, Toast.LENGTH_SHORT).show()
                                }, Modifier.fillMaxWidth())
                                PillButton(stringResource(R.string.ai_open_provider), {
                                    if (model.launchProvider(this@ShareActivity, selected, track)) finish()
                                }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}
