package com.rnbiometry

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap


import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class RnBiometryModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  @ReactMethod
  fun simplePrompt(params: ReadableMap, promise: Promise) {
    if (isCurrentSDKMarshmallowOrLater()) {
      UiThreadUtil.runOnUiThread(
        Runnable {
          try {
            val promptMessage: String = params.getString("promptMessage")
            val cancelButtonText: String = params.getString("cancelButtonText")
            val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")
            val authCallback: BiometricPrompt.AuthenticationCallback = SimplePromptCallback(promise)
            val fragmentActivity = getCurrentActivity() as FragmentActivity
            val executor: Executor = Executors.newSingleThreadExecutor()
            val biometricPrompt = BiometricPrompt(fragmentActivity, executor, authCallback)
            biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials))
          } catch (e: Exception) {
            promise.reject("Error displaying local biometric prompt: " + e.message, "Error displaying local biometric prompt: " + e.message)
          }
        })
    } else {
      promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0")
    }
  }

  private fun getPromptInfo(promptMessage: String, cancelButtonText: String, allowDeviceCredentials: Boolean): PromptInfo? {
    val builder = PromptInfo.Builder().setTitle(promptMessage)
    builder.setAllowedAuthenticators(getAllowedAuthenticators(allowDeviceCredentials))
    if (allowDeviceCredentials == false || isCurrentSDK29OrEarlier()) {
      builder.setNegativeButtonText(cancelButtonText)
    }
    return builder.build()
  }

  private fun isCurrentSDK29OrEarlier(): Boolean {
    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
  }

  private fun getAllowedAuthenticators(allowDeviceCredentials: Boolean): Int {
    return if (allowDeviceCredentials && !isCurrentSDK29OrEarlier()) {
      BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else BiometricManager.Authenticators.BIOMETRIC_STRONG
  }

  companion object {
    const val NAME = "RnBiometry"
  }
}
