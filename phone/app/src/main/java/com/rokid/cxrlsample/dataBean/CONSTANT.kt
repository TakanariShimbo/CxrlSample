package com.rokid.cxrlsample.dataBean

/**
 * Global constant definitions.
 *
 * Centralized keys for authorization request code, target app identity,
 * navigation extras, and entry type markers.
 */
object CONSTANT {
    // Request code used when authorization is launched from home screen.
    const val AUTH_REQUEST_CODE = 1001
    // Package name of sample app on glasses (for CUSTOMAPP install/start).
    val APP_PACKAGE_NAME = "com.rokid.cxrswithcxrl"
    // Entry Activity of sample app on glasses.
    val MAIN_PAGE = ".activities.main.MainActivity"

    // Shared extra key: communication token.
    const val EXTRA_TOKEN = "token"

    // Shared extra key: source entry type.
    const val EXTRA_ENTRY_TYPE = "entryType"
    const val ENTRY_TYPE_CUSTOM_APP = "customApp"
    const val ENTRY_TYPE_CUSTOM_VIEW = "customView"
}