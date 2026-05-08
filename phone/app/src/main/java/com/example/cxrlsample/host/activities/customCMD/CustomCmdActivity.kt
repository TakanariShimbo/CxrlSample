package com.example.cxrlsample.host.activities.customCMD

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
import com.example.cxrlsample.host.R
import com.example.cxrlsample.host.dataBean.CONSTANT
import com.example.cxrlsample.host.ui.components.ActionButtonGroup
import com.example.cxrlsample.host.ui.components.CommonActionsSectionTitle
import com.example.cxrlsample.host.ui.components.PRIMARY_BUTTON_WIDTH
import com.example.cxrlsample.host.ui.components.SampleScreenShell
import com.example.cxrlsample.host.ui.components.SectionTitle
import com.example.cxrlsample.host.ui.components.StatusPanel
import com.example.cxrlsample.host.ui.components.booleanStatusLine
import com.example.cxrlsample.host.ui.components.requireActionPrecondition
import com.example.cxrlsample.host.ui.components.statusLines
import com.example.cxrlsample.host.ui.components.statusLine
import com.example.cxrlsample.host.ui.theme.CxrlSampleHostTheme

/**
 * Demo screen for custom command messaging.
 *
 * This screen is only functional when entered from the CustomApp flow,
 * because it depends on an already established shared CXR connection.
 */
class CustomCmdActivity : ComponentActivity() {

    private val viewModel by viewModels<CustomCmdViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CxrlSampleHostTheme {
                CustomCmdScreen(viewModel = viewModel)
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
 * Compose UI for custom command interactions.
 */
@Composable
fun CustomCmdScreen(viewModel: CustomCmdViewModel) {
    val tokenGot by viewModel.tokenGot.collectAsState()
    val available by viewModel.available.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val appOpened by viewModel.appOpened.collectAsState()
    val status by viewModel.status.collectAsState()
    val entryLabel by viewModel.entryLabel.collectAsState()
    val from by viewModel.from.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_custom_cmd),
        subtitle = stringResource(id = R.string.custom_cmd_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                entryLabel,
                statusLine(R.string.common_status_prefix, status),
                booleanStatusLine(
                    formatResId = R.string.custom_cmd_feature_status,
                    trueResId = R.string.custom_cmd_available,
                    falseResId = R.string.custom_cmd_unavailable,
                    condition = available
                )
            )
        )
        SectionTitle(stringResource(id = R.string.custom_cmd_feedback))
        StatusPanel(
            title = stringResource(id = R.string.custom_cmd_message_title),
            lines = statusLines(stringResource(id = R.string.custom_cmd_from_glasses, from))
        )
        CommonActionsSectionTitle()
        ActionButtonGroup {
            if (!requireActionPrecondition(tokenGot, R.string.token_required_hint)) {
                return@ActionButtonGroup
            }
            if (!requireActionPrecondition(available, R.string.custom_cmd_only_custom_app)) {
                return@ActionButtonGroup
            }
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { viewModel.sendMessage() },
                enabled = ready && appOpened
            ) { Text(stringResource(id = R.string.custom_cmd_send)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomCmdScreenPreview() {
    CxrlSampleHostTheme {
        CustomCmdScreen(viewModel = viewModel { CustomCmdViewModel() })
    }
}
