package com.rokid.cxrlsample.activities.main

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
import com.rokid.cxrlsample.dataBean.CONSTANT
import com.rokid.cxrlsample.ui.components.ActionButtonGroup
import com.rokid.cxrlsample.ui.components.CommonActionsSectionTitle
import com.rokid.cxrlsample.ui.components.PRIMARY_BUTTON_WIDTH
import com.rokid.cxrlsample.ui.components.SampleScreenShell
import com.rokid.cxrlsample.ui.components.StatusPanel
import com.rokid.cxrlsample.ui.components.booleanStatusLine
import com.rokid.cxrlsample.ui.components.statusLines
import com.rokid.cxrlsample.ui.components.statusLine
import com.rokid.cxrlsample.ui.theme.CXRLSampleTheme
import com.rokid.cxrlsample.utils.SmartMarketLauncher

/**
 * Entry screen of the sample app.
 *
 * Responsibilities:
 * 1) Guide users through dependency installation checks.
 * 2) Trigger authorization for communication token retrieval.
 * 3) Navigate to feature demos after prerequisites are satisfied.
 *
 * UI state is exposed by [MainViewModel] and consumed by Compose.
 */
class MainActivity : ComponentActivity() {
    // Keep business state and side effects in ViewModel; Compose only renders state.
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CXRLSampleTheme {
                // Wire UI actions as callbacks so event handling remains testable and reusable.
                MainScreen(
                    viewModel = viewModel,
                    launchAPKInstall = {
                        SmartMarketLauncher.launchMarket(
                            this@MainActivity,
                            "com.rokid.sprite.global.aiapp"
                        )
                    },
                    recheckAPPInstall = {
                        viewModel.checkRokidAIAppInstalled(this@MainActivity)
                    },
                    requestAuthorization = {
                        viewModel.requestAuthorization(
                            this@MainActivity,
                            CONSTANT.AUTH_REQUEST_CODE
                        )
                    },
                    startCustomViewType = {
                        viewModel.startCustomViewType(this@MainActivity)
                    },
                    startCustomAppType = {
                        viewModel.startCustomAppType(this@MainActivity)
                    },
                )
            }
        }
        // Perform an initial install check to decide which action branch is shown on first render.
        viewModel.checkRokidAIAppInstalled(this@MainActivity)
    }

    @Deprecated("")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Keep compatibility with SDK callback style that returns authorization through onActivityResult.
        if (requestCode == CONSTANT.AUTH_REQUEST_CODE) {
            viewModel.checkAuthorizationResult(resultCode, data)
        }
    }

}

/**
 * Compose UI for the home screen.
 *
 * The action flow intentionally follows:
 * install status -> authorization status -> feature entry actions.
 * This sequencing prevents users from entering unsupported scenarios too early.
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    launchAPKInstall: () -> Unit,
    recheckAPPInstall: () -> Unit,
    requestAuthorization: () -> Unit = {},
    startCustomAppType: () -> Unit = {},
    startCustomViewType: () -> Unit = {},
) {
    // StateFlow drives the complete UI progression: install state -> auth state -> feature entry.
    val isRokidAIAppInstalled by viewModel.isRokidAIAppInstalled.collectAsState()
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    val tokenResult by viewModel.tokenResult.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_main),
        subtitle = stringResource(id = R.string.home_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                booleanStatusLine(
                    formatResId = R.string.home_install_status,
                    trueResId = R.string.home_install_state_installed,
                    falseResId = R.string.home_install_state_not_installed,
                    condition = isRokidAIAppInstalled
                ),
                booleanStatusLine(
                    formatResId = R.string.home_auth_status,
                    trueResId = R.string.home_auth_state_authorized,
                    falseResId = R.string.home_auth_state_unauthorized,
                    condition = isAuthorized
                ),
                statusLine(R.string.home_hint_line, tokenResult)
            )
        )
        CommonActionsSectionTitle()
        ActionButtonGroup {
            if (isRokidAIAppInstalled) {
                if (isAuthorized) {
                    Button(
                        modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                        onClick = startCustomViewType
                    ) { Text(text = stringResource(id = R.string.home_enter_custom_view)) }
                    Button(
                        modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                        onClick = startCustomAppType
                    ) { Text(text = stringResource(id = R.string.home_enter_custom_app)) }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                        onClick = requestAuthorization
                    ) { Text(text = stringResource(id = R.string.home_request_authorization)) }
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = launchAPKInstall
                ) { Text(text = stringResource(id = R.string.home_install_rokid_ai_app)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = recheckAPPInstall
                ) { Text(text = stringResource(id = R.string.home_recheck_install)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CXRLSampleTheme {
        MainScreen(
            viewModel = viewModel { MainViewModel() },
            launchAPKInstall = {},
            recheckAPPInstall = {})
    }
}