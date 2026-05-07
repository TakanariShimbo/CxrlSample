package com.example.cxrlsample.host

import android.app.Application
import com.example.cxrglobal.CXRLink

/**
 * Application-level container.
 *
 * It shares the active [CXRLink] session within the same process
 * to avoid duplicate link creation and repeated `connect` calls.
 */
class CxrlSampleHostApplication : Application() {
    /**
     * Reusable global CXRLink session.
     *
     * It is initialized by entry scenarios (CustomAppType/CustomViewType),
     * while feature pages (Audio/Photo/CustomCMD) only consume it and register callbacks.
     */
    var sharedCxrLink: CXRLink? = null
}
