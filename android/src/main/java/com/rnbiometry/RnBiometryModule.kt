package com.rnbiometry

import SimplePromptCallback
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class RnBiometryModule(reactContext: ReactApplicationContext?) : ReactContextBaseJavaModule(reactContext) {
  protected var biometricKeyAlias = "biometric_key"
    override fun getName() = "RnBiometryModule"

  @ReactMethod
  fun isSensorAvailable(params: ReadableMap, promise: Promise) {
    try {
      if (isCurrentSDKMarshmallowOrLater) {
        val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")
        val reactApplicationContext: ReactApplicationContext = getReactApplicationContext()
        val biometricManager: BiometricManager = BiometricManager.from(reactApplicationContext)
        val canAuthenticate: Int = biometricManager.canAuthenticate(getAllowedAuthenticators(allowDeviceCredentials))
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
          val resultMap: WritableMap = WritableNativeMap()
          resultMap.putBoolean("available", true)
          resultMap.putString("biometryType", "Biometrics")
          promise.resolve(resultMap)
        } else {
          val resultMap: WritableMap = WritableNativeMap()
          resultMap.putBoolean("available", false)
          when (canAuthenticate) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> resultMap.putString("error", "BIOMETRIC_ERROR_NO_HARDWARE")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> resultMap.putString("error", "BIOMETRIC_ERROR_HW_UNAVAILABLE")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> resultMap.putString("error", "BIOMETRIC_ERROR_NONE_ENROLLED")
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> resultMap.putString("error", "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED")
          }
          promise.resolve(resultMap)
        }
      } else {
        val resultMap: WritableMap = WritableNativeMap()
        resultMap.putBoolean("available", false)
        resultMap.putString("error", "Unsupported android version")
        promise.resolve(resultMap)
      }
    } catch (e: Exception) {
      promise.reject("Error detecting biometrics availability: " + e.message, "Error detecting biometrics availability: " + e.message)
    }
  }

//  @ReactMethod
//  fun createKeys(params: ReadableMap?, promise: Promise) {
//    try {
//      if (isCurrentSDKMarshmallowOrLater) {
//        deleteBiometricKey()
//        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
//        val keyGenParameterSpec: KeyGenParameterSpec = Builder(biometricKeyAlias, KeyProperties.PURPOSE_SIGN)
//          .setDigests(KeyProperties.DIGEST_SHA256)
//          .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
//          .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
//          .setUserAuthenticationRequired(true)
//          .build()
//        keyPairGenerator.initialize(keyGenParameterSpec)
//        val keyPair: KeyPair = keyPairGenerator.generateKeyPair()
//        val publicKey: PublicKey = keyPair.getPublic()
//        val encodedPublicKey: ByteArray = publicKey.getEncoded()
//        var publicKeyString: String = Base64.encodeToString(encodedPublicKey, Base64.DEFAULT)
//        publicKeyString = publicKeyString.replace("\r".toRegex(), "").replace("\n".toRegex(), "")
//        val resultMap: WritableMap = WritableNativeMap()
//        resultMap.putString("publicKey", publicKeyString)
//        promise.resolve(resultMap)
//      } else {
//        promise.reject("Cannot generate keys on android versions below 6.0", "Cannot generate keys on android versions below 6.0")
//      }
//    } catch (e: Exception) {
//      promise.reject("Error generating public private keys: " + e.message, "Error generating public private keys")
//    }
//  }

  private val isCurrentSDKMarshmallowOrLater: Boolean
    private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

  @ReactMethod
  fun deleteKeys(promise: Promise) {
    if (doesBiometricKeyExist()) {
      val deletionSuccessful = deleteBiometricKey()
      if (deletionSuccessful) {
        val resultMap: WritableMap = WritableNativeMap()
        resultMap.putBoolean("keysDeleted", true)
        promise.resolve(resultMap)
      } else {
        promise.reject("Error deleting biometric key from keystore", "Error deleting biometric key from keystore")
      }
    } else {
      val resultMap: WritableMap = WritableNativeMap()
      resultMap.putBoolean("keysDeleted", false)
      promise.resolve(resultMap)
    }
  }

//  @ReactMethod
//  fun createSignature(params: ReadableMap, promise: Promise) {
//    if (isCurrentSDKMarshmallowOrLater) {
//      UiThreadUtil.runOnUiThread(
//        object : Runnable {
//          override fun run() {
//            try {
//              val promptMessage: String = params.getString("promptMessage")
//              val payload: String = params.getString("payload")
//              val cancelButtonText: String = params.getString("cancelButtonText")
//              val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")
//              val signature: java.security.Signature = java.security.Signature.getInstance("SHA256withRSA")
//              val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
//              keyStore.load(null)
//              val privateKey: PrivateKey = keyStore.getKey(biometricKeyAlias, null) as PrivateKey
//              signature.initSign(privateKey)
//              val cryptoObject: BiometricPrompt.CryptoObject = CryptoObject(signature)
//              val authCallback: AuthenticationCallback = CreateSignatureCallback(promise, payload)
//              val fragmentActivity: FragmentActivity = getCurrentActivity() as FragmentActivity
//              val executor: Executor = Executors.newSingleThreadExecutor()
//              val biometricPrompt = BiometricPrompt(fragmentActivity, executor, authCallback)
//              biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials), cryptoObject)
//            } catch (e: Exception) {
//              promise.reject("Error signing payload: " + e.message, "Error generating signature: " + e.message)
//            }
//          }
//        })
//    } else {
//      promise.reject("Cannot generate keys on android versions below 6.0", "Cannot generate keys on android versions below 6.0")
//    }
//  }

//  private fun getPromptInfo(promptMessage: String, cancelButtonText: String, allowDeviceCredentials: Boolean): PromptInfo {
//    val builder: PromptInfo.Builder = Builder().setTitle(promptMessage)
//    builder.setAllowedAuthenticators(getAllowedAuthenticators(allowDeviceCredentials))
//    if (allowDeviceCredentials == false || isCurrentSDK29OrEarlier) {
//      builder.setNegativeButtonText(cancelButtonText)
//    }
//    return builder.build()
//  }

  private fun getAllowedAuthenticators(allowDeviceCredentials: Boolean): Int {
    return if (allowDeviceCredentials && !isCurrentSDK29OrEarlier) {
      BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else BiometricManager.Authenticators.BIOMETRIC_STRONG
  }

  private val isCurrentSDK29OrEarlier: Boolean
    private get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

  @ReactMethod
  fun simplePrompt(params: ReadableMap, promise: Promise) {
    if (isCurrentSDKMarshmallowOrLater) {
      UiThreadUtil.runOnUiThread(
        object : Runnable {
          override fun run() {
            try {
              val promptMessage: String? = params.getString("promptMessage")
              val cancelButtonText: String? = params.getString("cancelButtonText")
              val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")
              val authCallback: SimplePromptCallback = SimplePromptCallback(promise)
              val fragmentActivity: FragmentActivity = getCurrentActivity() as FragmentActivity
              val executor: Executor = Executors.newSingleThreadExecutor()
              val biometricPrompt = BiometricPrompt(fragmentActivity, executor, authCallback)
              biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials))
            } catch (e: Exception) {
              promise.reject("Error displaying local biometric prompt: " + e.message, "Error displaying local biometric prompt: " + e.message)
            }
          }
        })
    } else {
      promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0")
    }
  }

  @ReactMethod
  fun biometricKeysExist(promise: Promise) {
    try {
      val doesBiometricKeyExist = doesBiometricKeyExist()
      val resultMap: WritableMap = WritableNativeMap()
      resultMap.putBoolean("keysExist", doesBiometricKeyExist)
      promise.resolve(resultMap)
    } catch (e: Exception) {
      promise.reject("Error checking if biometric key exists: " + e.message, "Error checking if biometric key exists: " + e.message)
    }
  }

  protected fun doesBiometricKeyExist(): Boolean {
    return try {
      val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      keyStore.containsAlias(biometricKeyAlias)
    } catch (e: Exception) {
      false
    }
  }

  protected fun deleteBiometricKey(): Boolean {
    return try {
      val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      keyStore.deleteEntry(biometricKeyAlias)
      true
    } catch (e: Exception) {
      false
    }
  }
}
