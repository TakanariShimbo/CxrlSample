package com.rokid.cxrlsample.activities.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.rokid.cxrlsample.R
import com.rokid.cxrlsample.activities.customAppType.CustomAppTypeActivity
import com.rokid.cxrlsample.activities.customViewType.CustomViewTypeActivity
import com.rokid.cxrlsample.dataBean.CONSTANT
import com.example.cxrglobal.auth.AuthResult
import com.example.cxrglobal.auth.AuthorizationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.jvm.java

/**
 * Business logic for the home screen.
 *
 * Responsibilities:
 * 1) Verify whether the required Rokid AI app is installed.
 * 2) Start authorization and parse authorization callback results.
 * 3) Navigate to demo pages while carrying the communication token.
 */
class MainViewModel: ViewModel() {
    private var appContext: Context? = null

    private val _isRokidAIAppInstalled = MutableStateFlow(false)
    val isRokidAIAppInstalled = _isRokidAIAppInstalled.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _tokenResult = MutableStateFlow( "")
    val tokenResult = _tokenResult.asStateFlow()

    private var token = ""

    /**
     * Checks whether the required companion app is installed.
     *
     * The result directly controls which action buttons are available on the home screen.
     */
    fun checkRokidAIAppInstalled(act: Activity) {
        appContext = act.applicationContext
        _isRokidAIAppInstalled.value = AuthorizationHelper.isRequiredRokidAppInstalled(act)
    }

    /**
     * Starts the SDK authorization flow.
     *
     * The callback result is returned via the host Activity's `onActivityResult`,
     * then [checkAuthorizationResult] must be called to parse and persist state.
     */
    fun requestAuthorization(act: Activity, requestCode: Int) {
        AuthorizationHelper.requestAuthorization(act, requestCode)
    }

    /**
     * Opens the CustomAppType scenario and passes the current token.
     */
    fun startCustomAppType(context: Context){
        context.startActivity(Intent(context, CustomAppTypeActivity::class.java).apply {
            putExtra(CONSTANT.EXTRA_TOKEN, token)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    /**
     * Opens the CustomViewType scenario and passes the current token.
     */
    fun startCustomViewType(context: Context){
        context.startActivity(Intent(context, CustomViewTypeActivity::class.java).apply {
            putExtra(CONSTANT.EXTRA_TOKEN, token)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    /**
     * Parses authorization callback data and updates state exposed to the UI.
     *
     * @param requestCode In this project we pass `resultCode` from `onActivityResult`,
     * because that matches the current SDK parser expectation.
     * @param data Intent payload returned by the authorization activity.
     */
    fun checkAuthorizationResult(requestCode: Int, data: Intent?) {
        val result = AuthorizationHelper.parseAuthorizationResult(requestCode, data)
        Log.d("CXRLink", "checkAuthorizationResult: ${result.javaClass.name}")

        token = when (result) {
            is AuthResult.AuthSuccess -> {
                _isAuthorized.value = true
                _tokenResult.value = tr(R.string.auth_success)
                result.token
            }
            is AuthResult.AuthFail -> {
                _isAuthorized.value = false
                _tokenResult.value = tr(R.string.auth_failed)
                ""
            }
            is AuthResult.AuthCancel -> {
                _isAuthorized.value = false
                _tokenResult.value = tr(R.string.auth_cancelled)
                ""
            }
        }
    }

    private fun tr(@StringRes resId: Int, vararg args: Any): String {
        val context = appContext ?: return ""
        return context.getString(resId, *args)
    }
}