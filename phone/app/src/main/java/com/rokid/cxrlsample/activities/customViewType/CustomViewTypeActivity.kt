package com.rokid.cxrlsample.activities.customViewType

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.rokid.cxrlsample.R
import com.rokid.cxrlsample.activities.audio.AudioUsageActivity
import com.rokid.cxrlsample.activities.photo.PhotoUsageActivity
import com.rokid.cxrlsample.dataBean.CONSTANT
import com.rokid.cxrlsample.ui.components.ActionButtonGroup
import com.rokid.cxrlsample.ui.components.CommonActionsSectionTitle
import com.rokid.cxrlsample.ui.components.PRIMARY_BUTTON_WIDTH
import com.rokid.cxrlsample.ui.components.SampleScreenShell
import com.rokid.cxrlsample.ui.components.StatusPanel
import com.rokid.cxrlsample.ui.components.booleanStatusLine
import com.rokid.cxrlsample.ui.components.requireActionPrecondition
import com.rokid.cxrlsample.ui.components.statusLines
import com.rokid.cxrlsample.ui.theme.CXRLSampleTheme

/**
 * Entry page for the CustomView scenario.
 *
 * Hosts the custom-view UI flow and navigates to audio/photo subpages.
 */
class CustomViewTypeActivity : ComponentActivity() {

    private val viewModel by viewModels<CustomViewTypeViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CXRLSampleTheme {
                    // Page only hosts interactions; link/session logic is implemented in ViewModel.
                    CustomViewTypeScreen(
                        viewModel = viewModel,
                        toAudioUsage = {
                            startActivity(Intent(this@CustomViewTypeActivity, AudioUsageActivity::class.java).apply {
                                putExtra(CONSTANT.EXTRA_TOKEN, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
                                putExtra(CONSTANT.EXTRA_ENTRY_TYPE, CONSTANT.ENTRY_TYPE_CUSTOM_VIEW)
                            })
                        },
                        toPhotoUsage = {
                            startActivity(Intent(this@CustomViewTypeActivity, PhotoUsageActivity::class.java).apply {
                                putExtra(CONSTANT.EXTRA_TOKEN, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
                                putExtra(CONSTANT.EXTRA_ENTRY_TYPE, CONSTANT.ENTRY_TYPE_CUSTOM_VIEW)
                            })
                        }
                    )
            }
        }
        val token = intent.getStringExtra(CONSTANT.EXTRA_TOKEN)
        // Token is a prerequisite for creating CXRLink session.
        viewModel.init(this@CustomViewTypeActivity, token)
    }
}

/**
 * Compose UI for the CustomView scenario.
 *
 * Button availability is gated by connection and view states
 * to prevent protocol calls at invalid timing.
 */
@Composable
fun CustomViewTypeScreen(
    viewModel: CustomViewTypeViewModel,
    toAudioUsage: () -> Unit = {},
    toPhotoUsage: () -> Unit = {},
) {
    val tokenGot by viewModel.tokenGot.collectAsState(true)
    val connected by viewModel.connected.collectAsState()
    val btConnected by viewModel.btConnected.collectAsState()
    val customViewOpened by viewModel.customViewOpened.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_custom_view),
        subtitle = stringResource(id = R.string.custom_view_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                booleanStatusLine(
                    formatResId = R.string.custom_view_token_status,
                    trueResId = R.string.custom_app_token_ok,
                    falseResId = R.string.custom_app_token_missing,
                    condition = tokenGot
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_view_service_status,
                    trueResId = R.string.custom_view_connected,
                    falseResId = R.string.custom_view_not_connected,
                    condition = connected
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_view_bt_status,
                    trueResId = R.string.custom_view_connected,
                    falseResId = R.string.custom_view_not_connected,
                    condition = btConnected
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_view_view_status,
                    trueResId = R.string.custom_view_opened,
                    falseResId = R.string.custom_view_not_opened,
                    condition = customViewOpened
                )
            )
        )
        CommonActionsSectionTitle()
        ActionButtonGroup {
            if (!requireActionPrecondition(tokenGot, R.string.token_required_hint)) {
                return@ActionButtonGroup
            }
            if (!requireActionPrecondition(connected, R.string.custom_view_wait_service)) {
                return@ActionButtonGroup
            }
            if (!requireActionPrecondition(btConnected, R.string.custom_view_wait_bt)) {
                return@ActionButtonGroup
            }
            if (!customViewOpened) {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = { viewModel.openCustomView() }
                ) { Text(stringResource(id = R.string.custom_view_open)) }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = { viewModel.updateCustomView() }
                ) { Text(stringResource(id = R.string.custom_view_update)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = { viewModel.closeCustomView() }
                ) { Text(stringResource(id = R.string.custom_view_close)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toAudioUsage
                ) { Text(stringResource(id = R.string.custom_app_enter_audio)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toPhotoUsage
                ) { Text(stringResource(id = R.string.custom_app_enter_photo)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomViewTypeScreenPreview() {
    CXRLSampleTheme {
        CustomViewTypeScreen(viewModel = viewModel() { CustomViewTypeViewModel() })
    }
}