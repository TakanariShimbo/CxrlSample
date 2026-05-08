package com.example.cxrlsample.host.activities.customAppType

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.cxrglobal.CXRLink
import com.example.cxrglobal.CxrDefs
import com.example.cxrglobal.callbacks.ICXRLinkCbk
import com.example.cxrglobal.callbacks.IGlassAppCbk
import com.example.cxrlsample.host.CxrlSampleHostApplication
import com.example.cxrlsample.host.dataBean.CONSTANT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ViewModel for the CustomApp scenario.
 *
 * Responsibilities:
 * 1) Create and maintain a CUSTOMAPP CXR session.
 * 2) Track connectivity and installation status.
 * 3) Expose install / uninstall operations.
 * 4) Publish the created [CXRLink] into Application scope for sub-pages to reuse.
 *
 * Note: glass 側 client app の起動/終了 (`appStart` / `appStop`) はこの画面ではなく
 * [com.example.cxrlsample.host.activities.customCMD.CustomCmdViewModel] が
 * Activity ライフサイクルに紐付けて管理する。
 */
class CustomAppTypeViewModel : ViewModel() {
    private val tag = "CustomAppTypeViewModel"

    private val _tokenGot = MutableStateFlow(false)
    val tokenGot = _tokenGot.asStateFlow()

    private val _connectSuccess = MutableStateFlow(false)
    val connectSuccess = _connectSuccess.asStateFlow()

    private var isLConnected = false
        set(value) {
            val wasConnected = _connectSuccess.value
            field = value
            val nowConnected = isBTConnected && value
            _connectSuccess.value = nowConnected
            if (!wasConnected && nowConnected) checkApkInstalled()
        }

    private var isBTConnected = false
        set(value) {
            val wasConnected = _connectSuccess.value
            field = value
            val nowConnected = value && isLConnected
            _connectSuccess.value = nowConnected
            if (!wasConnected && nowConnected) checkApkInstalled()
        }

    private val _appInstalled = MutableStateFlow(false)
    val appInstalled = _appInstalled.asStateFlow()

    private val _installing = MutableStateFlow(false)
    val installing = _installing.asStateFlow()

    private lateinit var cxrLink: CXRLink
    private var appContext: Context? = null

    // この画面では install / uninstall / query のみを扱う。
    // glass 側 client app の起動/終了 (appStart/appStop) は CustomCmdViewModel が
    // Activity ライフサイクルに紐付けて管理する (Audio/Photo は client app 不要のため
    // 親画面で scene を立ち上げる責務を持たない)。
    private val appCallback = object : IGlassAppCbk {
        override fun onInstallAppResult(p0: Boolean) {
            Log.d("CustomAppTypeViewModel", "onInstallAppResult: $p0")
            _installing.value = false

            if (p0) {
                checkApkInstalled()
            } else {
                _appInstalled.value = false
            }
        }

        override fun onUnInstallAppResult(p0: Boolean) {
            Log.d("CustomAppTypeViewModel", "onUnInstallAppResult: $p0")
        }

        override fun onQueryAppResult(p0: Boolean) {
            Log.d("CustomAppTypeViewModel", "onQueryAppResult: $p0")
            _appInstalled.value = p0
        }
    }

    // Link is considered ready only when both CXR transport and Bluetooth are connected.
    private val connectCallback = object : ICXRLinkCbk {
        override fun onCXRLConnected(p0: Boolean) {
            Log.d("CustomAppTypeViewModel", "onCXRLConnected: $p0")
            isLConnected = p0
        }

        override fun onGlassBtConnected(p0: Boolean) {
            Log.d("CustomAppTypeViewModel", "onGlassBtConnected: $p0")
            isBTConnected = p0
        }

        override fun onGlassAiAssistStart() {
            Log.d("CustomAppTypeViewModel", "onGlassAiAssistStart: ")
        }

        override fun onGlassAiAssistStop() {
            Log.d("CustomAppTypeViewModel", "onGlassAiAssistStop: ")
        }

    }

    /**
     * Queries whether the target app is installed on the glasses device.
     */
    fun checkApkInstalled() {
        cxrLink.appIsInstalled(appCallback)
    }

    /**
     * Initializes and starts a CUSTOMAPP session.
     *
     * @param context Used to create link instance and expose it to Application-wide scope.
     * @param token Authorization token; if null/blank, connection will not start.
     */
    fun init(context: Context, token: String?) {
        appContext = context.applicationContext
        token?.let {
            Log.d("CustomAppTypeViewModel", "token: $it")
            _tokenGot.value = true
            cxrLink = CXRLink(context).apply {
                // Configure the session type and target package before connecting.
                configCXRSession(
                    CxrDefs.CXRSession(
                        CxrDefs.CXRSessionType.CUSTOMAPP,
                        CONSTANT.APP_PACKAGE_NAME
                    )
                )
                setCXRLinkCbk(connectCallback)
            }
            (context.applicationContext as? CxrlSampleHostApplication)?.sharedCxrLink = cxrLink
            cxrLink.connect(it)
        }
    }

    /**
     * Requests uninstalling the target app from glasses.
     */
    fun uninstallApp() {
        cxrLink.appUninstall(appCallback)
    }

    /**
     * Uploads and installs APK onto the glasses device.
     *
     * The method tries multiple candidate paths to handle different storage models.
     * It marks installing state only after SDK accepts one valid file path.
     */
    fun installApp() {
        val candidates = resolveInstallApkCandidates()
        if (candidates.isEmpty()) {
            Log.e(tag, "installApp failed: cannot find readable cxrL.apk")
            _installing.value = false
            _appInstalled.value = false
            return
        }
        candidates.forEach { apkFile ->
            val result = runCatching {
                cxrLink.appUploadAndInstall(apkFile.absolutePath, appCallback)
            }
            if (result.isSuccess) {
                _installing.value = true
                Log.d(tag, "installApp start upload with path=${apkFile.absolutePath}")
                return
            }
            // SDK opens the file directly; inaccessible paths can throw FileNotFoundException(EACCES).
            Log.e(tag, "installApp try path failed: ${apkFile.absolutePath}", result.exceptionOrNull())
        }
        Log.e(tag, "installApp failed: all candidate paths rejected")
        _installing.value = false
        _appInstalled.value = false
    }

    /**
     * Resolves possible APK file locations for upload/install.
     *
     * Priority is app-private storage first, then shared/public external locations.
     * This order reduces permission-related failures on modern Android versions.
     */
    private fun resolveInstallApkCandidates(): List<File> {
        val appCtx = appContext ?: return emptyList()
        return listOfNotNull(
            appCtx.getExternalFilesDir(Environment.DIRECTORY_DCIM + File.separator + "Rokid")?.resolve("cxrL.apk"),
            appCtx.filesDir.resolve("cxrL.apk"),
            File("/sdcard/DCIM/Rokid/cxrL.apk"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + File.separator + "Rokid")?.resolve("cxrL.apk")
        )
            .filter { it.exists() && it.isFile }
    }

}