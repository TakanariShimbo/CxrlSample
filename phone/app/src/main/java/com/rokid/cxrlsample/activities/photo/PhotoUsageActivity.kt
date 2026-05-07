package com.rokid.cxrlsample.activities.photo

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
import com.rokid.cxrlsample.ui.components.requireActionPrecondition
import com.rokid.cxrlsample.ui.components.statusLines
import com.rokid.cxrlsample.ui.components.statusLine
import com.rokid.cxrlsample.ui.theme.CXRLSampleTheme

/**
 * Photo capability demo page.
 *
 * Displays capture state and image preview.
 */
class PhotoUsageActivity : ComponentActivity() {

    private val viewModel by viewModels<PhotoUsageViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CXRLSampleTheme {
                PhotoUsageScreen(viewModel = viewModel)
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
 * Compose UI for photo page.
 */
@Composable
fun PhotoUsageScreen(viewModel: PhotoUsageViewModel) {
    val tokenGot by viewModel.tokenGot.collectAsState()
    val taking by viewModel.photoTaking.collectAsState()
    val status by viewModel.status.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val entryLabel by viewModel.entryLabel.collectAsState()
    val image by viewModel.photo.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_photo),
        subtitle = stringResource(id = R.string.photo_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                entryLabel,
                statusLine(R.string.common_status_prefix, status)
            )
        )
        CommonActionsSectionTitle()
        ActionButtonGroup {
            if (!requireActionPrecondition(tokenGot, R.string.token_required_hint)) {
                return@ActionButtonGroup
            }
            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { viewModel.takePhoto() },
                enabled = !taking && ready
            ) {
                Text(
                    if (taking) {
                        stringResource(id = R.string.photo_taking)
                    } else {
                        stringResource(id = R.string.photo_take_action)
                    }
                )
            }
        }
        SectionTitle(stringResource(id = R.string.photo_preview))
        image?.let { androidx.compose.foundation.Image(it, contentDescription = null) }
            ?: Text(stringResource(id = R.string.photo_no_result))
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoUsageScreenPreview() {
    CXRLSampleTheme {
        PhotoUsageScreen(viewModel = viewModel { PhotoUsageViewModel() })
    }
}
