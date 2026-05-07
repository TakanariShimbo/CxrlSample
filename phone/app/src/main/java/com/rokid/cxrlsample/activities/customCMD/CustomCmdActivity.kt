package com.rokid.cxrlsample.activities.customCMD

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
import com.rokid.cxrlsample.dataBean.CONSTANT
import com.rokid.cxrlsample.ui.components.ActionButtonGroup
import com.rokid.cxrlsample.ui.components.CommonActionsSectionTitle
import com.rokid.cxrlsample.ui.components.PRIMARY_BUTTON_WIDTH
import com.rokid.cxrlsample.ui.components.SampleScreenShell
import com.rokid.cxrlsample.ui.components.SectionTitle
import com.rokid.cxrlsample.ui.components.StatusPanel
import com.rokid.cxrlsample.ui.components.booleanStatusLine
import com.rokid.cxrlsample.ui.components.requireActionPrecondition
import com.rokid.cxrlsample.ui.components.statusLines
import com.rokid.cxrlsample.ui.components.statusLine
import com.rokid.cxrlsample.ui.theme.CXRLSampleTheme

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
            CXRLSampleTheme {
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
                enabled = ready
            ) { Text(stringResource(id = R.string.custom_cmd_send)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomCmdScreenPreview() {
    CXRLSampleTheme {
        CustomCmdScreen(viewModel = viewModel { CustomCmdViewModel() })
    }
}
