package com.rokid.cxrlsample.activities.customViewType

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import com.example.cxrglobal.CXRLink
import com.example.cxrglobal.CxrDefs
import com.example.cxrglobal.IconInfo
import com.example.cxrglobal.callbacks.ICXRLinkCbk
import com.example.cxrglobal.callbacks.ICustomViewCbk
import com.rokid.cxrlsample.CXRLSampleApplication
import com.rokid.cxrlsample.R
import com.rokid.cxrlsample.dataBean.selfView.ImageViewProps
import com.rokid.cxrlsample.dataBean.selfView.LinearLayoutProps
import com.rokid.cxrlsample.dataBean.selfView.SelfViewJson
import com.rokid.cxrlsample.dataBean.selfView.TextViewProps
import com.rokid.cxrlsample.dataBean.selfView.UpdateViewJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import kotlin.collections.get

/**
 * Business ViewModel for CustomView scenario.
 *
 * Creates CUSTOMVIEW session, sends icon assets, opens/updates/closes custom views,
 * and syncs the link instance to Application scope for other feature pages.
 */
class CustomViewTypeViewModel : ViewModel() {

    private lateinit var cxrLink: CXRLink
    private val TAG = "CustomViewTypeViewModel"

    private val _tokenGot = MutableStateFlow(false)

    val tokenGot = _tokenGot.asStateFlow()
    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    private val _btConnected = MutableStateFlow(false)
    val btConnected = _btConnected.asStateFlow()

    private var iconsString = ""
    private val _customViewOpened = MutableStateFlow(false)
    val customViewOpened = _customViewOpened.asStateFlow()


    // Link callback reflects CXR service status and glasses BT status separately.
    private val connectCallback = object : ICXRLinkCbk {
        override fun onCXRLConnected(p0: Boolean) {
            Log.d(TAG, "onCXRLConnected: $p0")
            _connected.value = p0
            if (iconsString.isNotEmpty()) {
                cxrLink.customViewSetIcons(iconsString)
            }
        }

        override fun onGlassBtConnected(p0: Boolean) {
            Log.d(TAG, "onGlassBtConnected: $p0")
            _btConnected.value = p0
        }

        override fun onGlassAiAssistStart() {
        }

        override fun onGlassAiAssistStop() {
        }

    }

    // Custom view lifecycle callback: open/update/close/error.
    private val customViewCallback = object : ICustomViewCbk {
        override fun onCustomViewOpened() {
            _customViewOpened.value = true
        }

        override fun onCustomViewUpdated() {
            Log.d(TAG, "onCustomViewUpdated: ")
        }

        override fun onCustomViewClosed() {
            Log.d(TAG, "onCustomViewClosed: ")
            _customViewOpened.value = false
        }

        override fun onCustomViewIconsSent() {
            Log.d(TAG, "onCustomViewIconsSent: success")
//            _canOpenCustomView.value = true
        }

        override fun onCustomViewError(p0: Int, p1: String?) {
            Log.d(TAG, "onCustomViewError: $p0, $p1")
            _customViewOpened.value = false
        }

    }

    /**
     * Initializes and connects CUSTOMVIEW session.
     *
     * @param context Used to create CXRLink and read drawable resources.
     * @param token Authorization token. If null, only token state is updated.
     */
    fun init(context: Context, token: String?) {
        token?.let {
            Log.d(TAG, "token: $it")
            _tokenGot.value = true
            cxrLink = CXRLink(context).apply {
                // Configure session before connecting.
                configCXRSession(CxrDefs.CXRSession(CxrDefs.CXRSessionType.CUSTOMVIEW))
                setCXRLinkCbk(connectCallback)
                setCXRCustomViewCbk(customViewCallback)
            }
            (context.applicationContext as? CXRLSampleApplication)?.sharedCxrLink = cxrLink
            // Session must be configured before connecting.
            cxrLink.connect(it)
            iconsString = iconsMake(context)
        } ?: run {
            _tokenGot.value = false
        }
    }

    /**
     * Opens custom view for the first render.
     */
    fun openCustomView() {
        // `open` sends full structure (type/props/children) for initial rendering.
        cxrLink.customViewOpen(selfView.toJson())
    }

    /**
     * Closes current custom view.
     */
    fun closeCustomView() {
        cxrLink.customViewClose()
    }

    private val iconType = "icon1"

    private var count = 1
    /**
     * Updates custom view content.
     *
     * Demonstrates delta update protocol (id + props only),
     * reducing payload size and improving update efficiency.
     */
    fun updateCustomView() {
        Log.d(TAG, "updateSelfView: updating custom view, current counter: $count")
        // `update` sends changed fields only (id + props) to reduce payload size.
        val updateViewJson = if (count % 2 == 0){
            UpdateViewJson().apply {
                updateList.add(UpdateViewJson.UpdateJson(id = "imageView").apply {
                    props["name"] = "icon1"
                })

            }
        }else{
            UpdateViewJson().apply {
                Log.d(TAG, "updateSelfView: updating custom view, current counter: $count, switching to icon2")
                updateList.add(UpdateViewJson.UpdateJson(id = "imageView").apply {
                    props["name"] = "icon2"
                })
            }
        }
        updateViewJson.updateList.add(UpdateViewJson.UpdateJson(id = "textView").apply {
            props["text"] = "Hello Rokid $count"
        })

        selfView.children?.get(0)?.props = TextViewProps().apply {
            id = "textView"
            layout_width = "wrap_content"
            layout_height = "wrap_content"
            text = "Hello Rokid $count"
            textColor = "#00FF00"
            textSize = "16sp"
            gravity = "center"
            textStyle = "bold"
            paddingStart = "16dp"
            paddingEnd = "16dp"
        }.toJson()

        selfView.children?.get(1)?.props = ImageViewProps().apply {
            id = "imageView"
            layout_width = "120dp"
            layout_height = "120dp"
            name = if (count % 2 == 0) "icon1" else "icon2"
            scaleType = "center"
        }.toJson()

        count ++
        if (count >= Int.MAX_VALUE){
            count = 1
        }
        Log.d(TAG, "updateSelfView: sending update payload")
        // Docs chapter 08: updateCustomView(updateViewJson)
        cxrLink.customViewUpdate(updateViewJson.toJson())
        Log.d(TAG, "updateSelfView: custom view updated")
    }

    // Initial view tree: root container + text + image as openCustomView baseline.
    private val selfView = SelfViewJson().apply {
        type = "LinearLayout"
        props = LinearLayoutProps().apply {
            id = "root"
            layout_width = "match_parent"
            layout_height = "match_parent"
            marginTop = "160dp"
            marginBottom = "80dp"
            backgroundColor = "#FF000000"
            orientation = "vertical"
            gravity="center_horizontal"
        }.toJson()
        children = listOf(
            SelfViewJson().apply {
                type = "TextView"
                props = TextViewProps().apply {
                    id = "textView"
                    layout_width = "wrap_content"
                    layout_height = "wrap_content"
                    text = "Hello World"
                    textColor = "#00FF00"
                    textSize = "16sp"
                    gravity = "center"
                    textStyle = "bold"
                    paddingStart = "16dp"
                    paddingEnd = "16dp"
                }.toJson()
            },
            SelfViewJson().apply {
                type = "ImageView"
                props = ImageViewProps().apply {
                    id = "imageView"
                    layout_width = "120dp"
                    layout_height = "120dp"
                    name = "icon1"
                    scaleType = "center"
                }.toJson()
            }
        )
    }

    /**
     * Reads local drawables and builds protocol-required icon JSON string.
     */
    fun iconsMake(context: Context): String {
        val icon1OutputStream = ByteArrayOutputStream()
        drawableToBitmap(
            context.resources.getDrawable(
                R.drawable.icon1,
                null
            )
        ).compress(Bitmap.CompressFormat.PNG, 100, icon1OutputStream)
        val icon1Base64 = IconInfo(
            "icon1",
            Base64.encodeToString(icon1OutputStream.toByteArray(), Base64.NO_WRAP)
        )
        icon1OutputStream.close()

        val icon2OutputStream = ByteArrayOutputStream()
        drawableToBitmap(
            context.resources.getDrawable(
                R.drawable.icon2,
                null
            )
        ).compress(Bitmap.CompressFormat.PNG, 100, icon2OutputStream)
        val icon2Base64 = IconInfo(
            "icon2",
            Base64.encodeToString(icon2OutputStream.toByteArray(), Base64.NO_WRAP)
        )
        icon2OutputStream.close()

        return toJson(listOf(icon1Base64, icon2Base64))
    }

    /**
     * Serializes icon list into glasses-side protocol JSON.
     */
    fun toJson(list: List<IconInfo>): String {
        return """
         [${list.joinToString(separator = ",") {"{ \"name\": \"${it.name}\", \"data\": \"${it.data}\" }"}}]
        """.trimIndent()
    }

    /**
     * Converts Drawable object to Bitmap object.
     * @param drawable Drawable source to convert.
     * @return Converted Bitmap object.
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            Log.d(TAG, "drawableToBitmap: source drawable is already BitmapDrawable")
            return drawable.bitmap
        }

        Log.d(TAG, "drawableToBitmap: converting drawable to bitmap")
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        Log.d(TAG, "drawableToBitmap: drawable conversion finished")
        return bitmap
    }


}