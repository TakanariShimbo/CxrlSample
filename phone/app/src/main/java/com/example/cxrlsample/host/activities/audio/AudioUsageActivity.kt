package com.example.cxrlsample.host.activities.audio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cxrlsample.host.R
import com.example.cxrlsample.host.dataBean.CONSTANT
import com.example.cxrlsample.host.ui.components.ActionButtonGroup
import com.example.cxrlsample.host.ui.components.PRIMARY_BUTTON_WIDTH
import com.example.cxrlsample.host.ui.components.SampleScreenShell
import com.example.cxrlsample.host.ui.components.SectionTitle
import com.example.cxrlsample.host.ui.components.StatusPanel
import com.example.cxrlsample.host.ui.components.requireActionPrecondition
import com.example.cxrlsample.host.ui.components.statusLines
import com.example.cxrlsample.host.ui.components.statusLine
import com.example.cxrlsample.host.ui.theme.CxrlSampleHostTheme

/**
 * Audio capability demo page.
 *
 * Demonstrates start/stop recording, local playback, sharing, and history cleanup.
 */
class AudioUsageActivity : ComponentActivity() {

    private val viewModel by viewModels<AudioUsageViewModel>()
    private val shareAudioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CxrlSampleHostTheme {
                AudioUsageScreen(
                    viewModel = viewModel,
                    onShareAudio = {
                        viewModel.buildShareIntent(this@AudioUsageActivity)?.let {
                            shareAudioLauncher.launch(
                                Intent.createChooser(
                                    it,
                                    getString(R.string.audio_share_title)
                                )
                            )
                        }
                    }
                )
            }
        }
        viewModel.init(
            context = this,
            token = intent.getStringExtra(CONSTANT.EXTRA_TOKEN),
            entryType = intent.getStringExtra(CONSTANT.EXTRA_ENTRY_TYPE)
        )
    }

    override fun onDestroy() {
        viewModel.release()
        super.onDestroy()
    }
}

/**
 * Compose UI for audio page.
 *
 * Uses state flows to control monitor/playback buttons and file info rendering.
 */
@Composable
fun AudioUsageScreen(
    viewModel: AudioUsageViewModel,
    onShareAudio: () -> Unit = {}
) {
    val tokenGot by viewModel.tokenGot.collectAsState()
    val started by viewModel.audioStarted.collectAsState()
    val status by viewModel.status.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val entryLabel by viewModel.entryLabel.collectAsState()
    val hasPlayableAudio by viewModel.hasPlayableAudio.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val savedAudioPath by viewModel.savedAudioPath.collectAsState()
    val recordDurationText by viewModel.recordDurationText.collectAsState()
    val cleanupResult by viewModel.cleanupResult.collectAsState()
    val audioFileCountText by viewModel.audioFileCountText.collectAsState()
    val recentAudioFilesText by viewModel.recentAudioFilesText.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_audio),
        subtitle = stringResource(id = R.string.audio_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                entryLabel,
                statusLine(R.string.common_status_prefix, status),
                recordDurationText,
                audioFileCountText
            )
        )
        SectionTitle(stringResource(id = R.string.audio_listen_control))
        ActionButtonGroup {
            if (!requireActionPrecondition(tokenGot, R.string.token_required_hint)) {
                return@ActionButtonGroup
            }
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { if (!started) viewModel.startAudio() else viewModel.stopAudio() },
                enabled = ready
            ) {
                Text(
                    if (!started) {
                        stringResource(id = R.string.audio_start_listen)
                    } else {
                        stringResource(id = R.string.audio_stop_listen)
                    }
                )
            }
        }
        SectionTitle(stringResource(id = R.string.audio_playback_and_file_actions))
        ActionButtonGroup {
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { viewModel.togglePlaySavedAudio() },
                enabled = hasPlayableAudio
            ) {
                Text(
                    if (isPlaying) {
                        stringResource(id = R.string.audio_stop_playback)
                    } else {
                        stringResource(id = R.string.audio_play_saved_audio)
                    }
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = onShareAudio,
                enabled = hasPlayableAudio
            ) { Text(stringResource(id = R.string.audio_share_file)) }
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { viewModel.clearHistoryAudioFiles(keepLatestCount = 3) }
            ) { Text(stringResource(id = R.string.audio_clear_history)) }
        }
        SectionTitle(stringResource(id = R.string.audio_file_info))
        StatusPanel(
            title = stringResource(id = R.string.audio_recent_recordings_title),
            lines = statusLines(
                stringResource(
                    id = R.string.common_file_prefix,
                    if (savedAudioPath.isNotBlank()) savedAudioPath else stringResource(id = R.string.common_none)
                ),
                recentAudioFilesText,
                cleanupResult
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AudioUsageScreenPreview() {
    CxrlSampleHostTheme {
        AudioUsageScreen(viewModel = viewModel { AudioUsageViewModel() })
    }
}