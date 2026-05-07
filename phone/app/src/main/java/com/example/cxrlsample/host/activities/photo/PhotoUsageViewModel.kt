package com.example.cxrlsample.host.activities.photo

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.cxrglobal.CXRLink
import com.example.cxrglobal.callbacks.ICXRLinkCbk
import com.example.cxrglobal.callbacks.IImageStreamCbk
import com.example.cxrlsample.host.CxrlSampleHostApplication
import com.example.cxrlsample.host.R
import com.example.cxrlsample.host.dataBean.CONSTANT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Business ViewModel for photo capability.
 *
 * Reuses global connection, registers image callbacks,
 * and manages capture requests with result state.
 */
class PhotoUsageViewModel : ViewModel() {
    private var appContext: Context? = null

    private val _tokenGot = MutableStateFlow(false)
    val tokenGot = _tokenGot.asStateFlow()

    private val _photoTaking = MutableStateFlow(false)
    val photoTaking = _photoTaking.asStateFlow()

    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready = _ready.asStateFlow()

    private val _entryLabel = MutableStateFlow("")
    val entryLabel = _entryLabel.asStateFlow()

    private val _photo = MutableStateFlow<ImageBitmap?>(null)
    val photo = _photo.asStateFlow()

    private lateinit var cxrLink: CXRLink

    /**
     * Initializes page state and binds global CXRLink connection.
     */
    fun init(context: Context, token: String?, entryType: String?) {
        appContext = context.applicationContext
        _status.value = tr(R.string.photo_wait_take)
        _entryLabel.value = tr(R.string.photo_entry_unknown)
        _tokenGot.value = !token.isNullOrBlank()
        if (!_tokenGot.value) {
            return
        }
        _entryLabel.value = if (entryType == CONSTANT.ENTRY_TYPE_CUSTOM_VIEW) {
            tr(R.string.photo_entry_custom_view)
        } else {
            tr(R.string.photo_entry_custom_app)
        }
        val app = context.applicationContext as? CxrlSampleHostApplication
        cxrLink = app?.sharedCxrLink ?: run {
            _ready.value = false
            _status.value = tr(R.string.photo_need_connection)
            return
        }
        cxrLink.setCXRLinkCbk(object : ICXRLinkCbk {
            override fun onCXRLConnected(connected: Boolean) {
                _ready.value = connected
                _status.value = if (connected) {
                    tr(R.string.photo_connected_wait_take)
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
        cxrLink.setCXRImageCbk(object : IImageStreamCbk {
            override fun onImageReceived(data: ByteArray) {
                _photoTaking.value = false
                _status.value = tr(R.string.photo_success)
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                _photo.value = bitmap?.asImageBitmap()
            }

            override fun onImageError(code: Int, msg: String?) {
                _photoTaking.value = false
                _status.value = tr(R.string.photo_failed, code, msg ?: "")
                _photo.value = null
            }
        })
        _ready.value = true
        _status.value = tr(R.string.photo_reuse_connection_wait_take)
    }

    /**
     * Triggers a photo capture request.
     */
    fun takePhoto() {
        if (!::cxrLink.isInitialized || !_ready.value) return
        _photoTaking.value = true
        _status.value = tr(R.string.photo_taking_status)
        _photo.value = null
        cxrLink.takePhoto(1024, 768, 80)
    }

    /**
     * Clears transient page state when leaving the page.
     */
    fun release() {
        _photoTaking.value = false
    }

    private fun tr(@StringRes resId: Int, vararg args: Any): String {
        val context = appContext ?: return ""
        return context.getString(resId, *args)
    }
}
