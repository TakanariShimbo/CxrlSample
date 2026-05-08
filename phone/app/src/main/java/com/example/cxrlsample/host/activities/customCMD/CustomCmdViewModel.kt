package com.example.cxrlsample.host.activities.customCMD

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.cxrglobal.CXRLink
import com.example.cxrglobal.callbacks.ICXRLinkCbk
import com.example.cxrglobal.callbacks.ICustomCmdCbk
import com.example.cxrglobal.callbacks.IGlassAppCbk
// Caps は本家 SDK のシリアライザを Wire 互換のため引き続き使用 (グラス側 APK との payload 互換)。
import com.rokid.cxr.Caps
import com.example.cxrlsample.host.CxrlSampleHostApplication
import com.example.cxrlsample.host.R
import com.example.cxrlsample.host.dataBean.CONSTANT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for custom command interactions.
 *
 * Responsibilities:
 * 1) Validate whether current entry type supports custom commands.
 * 2) Reuse an existing shared CXR link session created by CustomAppType flow.
 * 3) Send sample commands and parse response Caps payload for UI display.
 */
class CustomCmdViewModel : ViewModel() {
    private val tag = "CustomCmdViewModel"
    private var appContext: Context? = null

    private val _tokenGot = MutableStateFlow(false)
    val tokenGot = _tokenGot.asStateFlow()

    private val _available = MutableStateFlow(false)
    val available = _available.asStateFlow()

    private val _from = MutableStateFlow("")
    val from = _from.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()

    private val _appOpened = MutableStateFlow(false)
    val appOpened = _appOpened.asStateFlow()

    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _entryLabel = MutableStateFlow("")
    val entryLabel = _entryLabel.asStateFlow()

    private var count = 0
    private lateinit var cxrLink: CXRLink
    private var sceneStarted = false

    // CmdActivity のライフサイクルに glass 側 client app の起動/終了を紐付ける。
    // Audio / Photo は SDK のトランスポート層で完結するため client app 不要だが、
    // CustomCmd は client app の MAIN_PAGE がキー受信側となるので必須。
    private val glassAppCallback = object : IGlassAppCbk {
        override fun onOpenAppResult(success: Boolean) {
            Log.d(tag, "onOpenAppResult: $success")
            _appOpened.value = success
        }

        override fun onStopAppResult(success: Boolean) {
            Log.d(tag, "onStopAppResult: $success")
            _appOpened.value = !success
        }

        override fun onGlassAppResume(resume: Boolean) {
            Log.d(tag, "onGlassAppResume: $resume")
            // SDK の sentinel "unknow" 経由で false が誤発火するため true のみ反映 (CustomAppType と同じ理由)。
            if (resume) _appOpened.value = true
        }
    }

    /**
     * Initializes screen state and binds callbacks for link and custom command events.
     *
     * This page intentionally does not create a new link; it depends on a previously
     * established link from the CustomAppType path to keep one shared connection.
     */
    fun init(context: Context, token: String?, entryType: String?) {
        appContext = context.applicationContext
        _tokenGot.value = !token.isNullOrBlank()
        _available.value = entryType == CONSTANT.ENTRY_TYPE_CUSTOM_APP
        _status.value = tr(R.string.custom_cmd_status_waiting_connection)
        _entryLabel.value = if (entryType == CONSTANT.ENTRY_TYPE_CUSTOM_APP) {
            tr(R.string.custom_cmd_entry_custom_app)
        } else {
            tr(R.string.custom_cmd_entry_custom_view)
        }
        if (!_tokenGot.value || !_available.value) {
            return
        }
        val app = context.applicationContext as? CxrlSampleHostApplication
        cxrLink = app?.sharedCxrLink ?: run {
            _ready.value = false
            _status.value = tr(R.string.custom_cmd_need_custom_app_connection)
            return
        }
        cxrLink.setCXRLinkCbk(object : ICXRLinkCbk {
            override fun onCXRLConnected(connected: Boolean) {
                _ready.value = connected
                _status.value = if (connected) {
                    tr(R.string.custom_cmd_connected_hint)
                } else {
                    tr(R.string.common_service_not_connected)
                }
            }

            override fun onGlassBtConnected(connected: Boolean) {
                if (!connected) {
                    _ready.value = false
                    _status.value = tr(R.string.common_bt_not_connected)
                }
            }

            override fun onGlassAiAssistStart() {}
            override fun onGlassAiAssistStop() {}
        })
        cxrLink.setCXRCustomCmdCbk(object : ICustomCmdCbk {
            override fun onCustomCmdResult(key: String, payload: ByteArray) {
                // Ignore unrelated command channels and only parse the agreed demo key.
                if (key != "rk_custom_key") {
                    return
                }
                val caps = Caps.fromBytes(payload) ?: return
                _from.value = parseCaps(caps)
            }
        })
        _ready.value = true
        _status.value = tr(R.string.custom_cmd_reuse_connection)
        // glass 側 client app をこの画面のスコープで起動。Activity が destroy される
        // ときに onCleared で appStop を呼んで終了させる。config change で init() が
        // 再呼び出しされても sceneStarted フラグで二重起動を防ぐ。
        if (!sceneStarted) {
            cxrLink.appStart("${CONSTANT.APP_PACKAGE_NAME}${CONSTANT.MAIN_PAGE}", glassAppCallback)
            sceneStarted = true
        }
    }

    /**
     * Sends a demo custom command to the glasses side.
     *
     * A monotonically increasing counter is appended so responses can be distinguished
     * during manual repeated tests.
     */
    fun sendMessage() {
        if (!_available.value || !::cxrLink.isInitialized || !_ready.value || !_appOpened.value) {
            return
        }
        cxrLink.sendCustomCmd("rk_custom_client", Caps().apply {
            write("rk_custom_key")
            write("from client click times = ${count++}")
        }.serialize())
    }

    /**
     * Resets transient readiness when the page is leaving foreground.
     */
    fun release() {
        _ready.value = false
    }

    override fun onCleared() {
        if (sceneStarted && ::cxrLink.isInitialized) {
            runCatching { cxrLink.appStop(glassAppCallback) }
                .onFailure { Log.w(tag, "appStop on clear failed", it) }
        }
        super.onCleared()
    }

    /**
     * Recursively parses a Caps object into a readable string representation.
     *
     * The parser keeps type information (string/int/long/object/binary) so payload
     * structure remains debuggable from the UI without attaching a low-level inspector.
     */
    private fun parseCaps(caps: Caps): String {
        val builder = StringBuilder("{")
        for (i in 0 until caps.size()) {
            val value = caps.at(i)
            val text = when (value.type()) {
                Caps.Value.TYPE_STRING -> "string:${value.string}"
                Caps.Value.TYPE_INT32, Caps.Value.TYPE_UINT32 -> "int:${value.int}"
                Caps.Value.TYPE_INT64, Caps.Value.TYPE_UINT64 -> "long:${value.long}"
                Caps.Value.TYPE_FLOAT -> "float:${value.float}"
                Caps.Value.TYPE_DOUBLE -> "double:${value.double}"
                Caps.Value.TYPE_OBJECT -> parseCaps(value.`object`)
                Caps.Value.TYPE_BINARY -> value.binary?.let {
                    "binary:${Base64.encode(it.data, it.length)}"
                } ?: "binary:null"
                else -> "unknown:null"
            }
            builder.append(text).append(",")
        }
        if (builder.length > 1) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("}")
        return builder.toString()
    }

    private fun tr(@StringRes resId: Int, vararg args: Any): String {
        val context = appContext ?: return ""
        return context.getString(resId, *args)
    }
}
