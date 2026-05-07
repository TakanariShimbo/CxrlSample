package com.rokid.cxrlsample.activities.customAppType

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.net.Uri
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
import com.rokid.cxrlsample.R
import com.rokid.cxrlsample.activities.audio.AudioUsageActivity
import com.rokid.cxrlsample.activities.customCMD.CustomCmdActivity
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
 * Entry page for the CustomApp scenario.
 *
 * Shows install/open state and provides navigation to custom command,
 * audio, and photo feature pages.
 */
class CustomAppTypeActivity : ComponentActivity() {

    private val viewModel by viewModels<CustomAppTypeViewModel>()

    // Request install-related permissions dynamically, then continue installation.
    private val mediaPermissionRequestLauncher =
        registerForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            if (it) {
                viewModel.installApp()
            }
        }

    private val allFilesAccessPermissionLauncher =
        registerForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (hasManageAllFilesAccessPermission()) {
                if (needRequestReadExternalStoragePermission()) {
                    mediaPermissionRequestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                } else {
                    viewModel.installApp()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CXRLSampleTheme {
                // Compose only renders state and dispatches events; business logic stays in ViewModel.
                CustomAppTypeScreen(
                    viewModel = viewModel,
                    toCustomCmd = {
                        startActivity(Intent(this@CustomAppTypeActivity, CustomCmdActivity::class.java).apply {
                            putExtra(CONSTANT.EXTRA_TOKEN, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
                            putExtra(CONSTANT.EXTRA_ENTRY_TYPE, CONSTANT.ENTRY_TYPE_CUSTOM_APP)
                        })
                    },
                    toAudio = {
                        startActivity(Intent(this@CustomAppTypeActivity, AudioUsageActivity::class.java).apply {
                            putExtra(CONSTANT.EXTRA_TOKEN, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
                            putExtra(CONSTANT.EXTRA_ENTRY_TYPE, CONSTANT.ENTRY_TYPE_CUSTOM_APP)
                        })
                    },
                    toPhoto = {
                        startActivity(Intent(this@CustomAppTypeActivity, PhotoUsageActivity::class.java).apply {
                            putExtra(CONSTANT.EXTRA_TOKEN, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
                            putExtra(CONSTANT.EXTRA_ENTRY_TYPE, CONSTANT.ENTRY_TYPE_CUSTOM_APP)
                        })
                    },
                    toInstall = {
                        if (needRequestManageAllFilesAccessPermission()) {
                            requestManageAllFilesAccessPermission()
                        } else if (needRequestReadExternalStoragePermission()) {
                            mediaPermissionRequestLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        } else {
                            viewModel.installApp()
                        }
                    }
                )
            }
        }
        // Token comes from home authorization and is required to establish CUSTOMAPP session.
        viewModel.init(this, intent.getStringExtra(CONSTANT.EXTRA_TOKEN))
    }

    /**
     * Dynamic read permission is needed only on Android 12 and below
     * when accessing shared external directories.
     */
    private fun needRequestReadExternalStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    }

    private fun needRequestManageAllFilesAccessPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        return !hasManageAllFilesAccessPermission()
    }

    private fun hasManageAllFilesAccessPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    private fun requestManageAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:$packageName")
        )
        allFilesAccessPermissionLauncher.launch(intent)
    }
}

/**
 * Compose page for the CustomApp scenario.
 *
 * Action order is state-driven:
 * connection ready -> app installed -> scene opened -> feature pages.
 */
@Composable
fun CustomAppTypeScreen(
    viewModel: CustomAppTypeViewModel,
    toCustomCmd: () -> Unit = {},
    toAudio: () -> Unit = {},
    toPhoto: () -> Unit = {},
    toInstall: () -> Unit = {}
) {
    val tokenGot by viewModel.tokenGot.collectAsState()
    val connectSuccess by viewModel.connectSuccess.collectAsState()
    val appInstalled by viewModel.appInstalled.collectAsState()
    val appOpened by viewModel.appOpened.collectAsState()
    val installing by viewModel.installing.collectAsState()

    SampleScreenShell(
        title = stringResource(id = R.string.screen_title_custom_app),
        subtitle = stringResource(id = R.string.custom_app_subtitle)
    ) {
        StatusPanel(
            lines = statusLines(
                booleanStatusLine(
                    formatResId = R.string.custom_app_token_status,
                    trueResId = R.string.custom_app_token_ok,
                    falseResId = R.string.custom_app_token_missing,
                    condition = tokenGot
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_app_connection_status,
                    trueResId = R.string.custom_app_connection_ok,
                    falseResId = R.string.custom_app_connection_waiting,
                    condition = connectSuccess
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_app_install_status,
                    trueResId = R.string.custom_app_install_ok,
                    falseResId = R.string.custom_app_install_not,
                    condition = appInstalled
                ),
                booleanStatusLine(
                    formatResId = R.string.custom_app_scene_status,
                    trueResId = R.string.custom_app_scene_opened,
                    falseResId = R.string.custom_app_scene_closed,
                    condition = appOpened
                )
            )
        )
        CommonActionsSectionTitle()
        ActionButtonGroup {
            if (!requireActionPrecondition(tokenGot, R.string.token_required_hint)) {
                return@ActionButtonGroup
            }
            if (!requireActionPrecondition(connectSuccess, R.string.custom_app_wait_connection)) {
                return@ActionButtonGroup
            }
            if (!appInstalled) {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toInstall,
                    enabled = !installing
                ) { Text(stringResource(id = R.string.custom_app_install_to_glasses)) }
                return@ActionButtonGroup
            }

            Button(
                modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                onClick = { viewModel.uninstallApp() }
            ) { Text(stringResource(id = R.string.custom_app_uninstall)) }

            if (!appOpened) {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = { viewModel.openApp() }
                ) { Text(stringResource(id = R.string.custom_app_open_scene)) }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toCustomCmd
                ) { Text(stringResource(id = R.string.custom_app_enter_cmd)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toAudio
                ) { Text(stringResource(id = R.string.custom_app_enter_audio)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = toPhoto
                ) { Text(stringResource(id = R.string.custom_app_enter_photo)) }
                Button(
                    modifier = Modifier.fillMaxWidth(PRIMARY_BUTTON_WIDTH),
                    onClick = { viewModel.stopApp() }
                ) { Text(stringResource(id = R.string.custom_app_stop_scene)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomAppTypeScreenPreview() {
    CXRLSampleTheme {
        CustomAppTypeScreen(viewModel = viewModel() { CustomAppTypeViewModel() })
    }
}