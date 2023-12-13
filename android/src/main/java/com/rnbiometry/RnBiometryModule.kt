package com.rnbiometry

import SimplePromptCallback
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.crypto.Cipher
import android.util.Base64
import android.util.Log


class RnBiometryModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private val TAG = "BioModule"

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
fun showBiometricPromptForEncryption(params: ReadableMap, promise: Promise) {
    if (isCurrentSDKMarshmallowOrLater()) {
        UiThreadUtil.runOnUiThread(
            Runnable {
                try {
                    val promptMessage: String = params.getString("promptMessage")!!
                    val token: String = params.getString("token")!!  // Assuming 'token' is the data to be encrypted
                    val cancelButtonText: String = params.getString("cancelButtonText")!!
                    val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")

                    // Assuming CryptographyManager is accessible and initialized
                    val cryptographyManager: CryptographyManager = CryptographyManager()

                    // Initialize the cipher for encryption
                    val symmetricKeyAlias = "encryptionKeyAlias"
                    val cipher: Cipher = cryptographyManager.getInitializedCipherForEncryption(symmetricKeyAlias)

                    val fragmentActivity = getCurrentActivity() as FragmentActivity
                    val executor: Executor = Executors.newSingleThreadExecutor()

                    // Set up the biometric prompt callback
                    val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            try {
                                // Encrypt the token after successful authentication
                                val cipherTextWrapper = cryptographyManager.encryptData(token, result.cryptoObject?.cipher!!)
                                val encryptedDataString = Base64.encodeToString(cipherTextWrapper.cipherText, Base64.DEFAULT)
                                val ivString = Base64.encodeToString(cipherTextWrapper.initializationVector, Base64.DEFAULT)
                                val combinedString = "$ivString:$encryptedDataString"

                                promise.resolve(combinedString)
                            } catch (e: Exception) {
                                promise.reject("Encryption error", "Error occurred during encryption: ${e.message}")
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            promise.reject("Biometric Authentication error", errString.toString())
                        }

                        // Handle other authentication cases (failure, cancellation) as needed
                    }

                    val biometricPrompt = BiometricPrompt(fragmentActivity, executor, authCallback)
                    
                    // Create a CryptoObject associated with the cipher
                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                    // Start the biometric authentication
                    biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials), cryptoObject)
                } catch (e: Exception) {
                    promise.reject("Error displaying local biometric prompt: " + e.message, "Error displaying local biometric prompt: " + e.message)
                }
            })
    } else {
        promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0")
    }
}

@ReactMethod
fun showBiometricPromptForDecryption(params: ReadableMap, promise: Promise) {
    if (isCurrentSDKMarshmallowOrLater()) {
        UiThreadUtil.runOnUiThread(
            Runnable {
                try {
                    val promptMessage: String = params.getString("promptMessage")!!
                    val combinedString: String = params.getString("encryptedToken")!!  // The combined 'iv:encryptedData' string
                    val cancelButtonText: String = params.getString("cancelButtonText")!!
                    val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")

                    // Split the combined string into IV and encrypted data
                    val parts = combinedString.split(":")
                    if (parts.size != 2) {
                        promise.reject("Decryption error", "Invalid combined string format")
                        return
                    }
                    Log.d(TAG, combinedString)
                    val iv = Base64.decode(parts[0], Base64.DEFAULT)
                    val encryptedData = Base64.decode(parts[1], Base64.DEFAULT)
                    Log.d(TAG, iv)
                    Log.d(TAG, encryptedData)
                    // Assuming CryptographyManager is accessible and initialized
                    val cryptographyManager: CryptographyManager = CryptographyManager()

                    // Initialize the cipher for decryption
                    val symmetricKeyAlias = "encryptionKeyAlias"
                    val cipher: Cipher = cryptographyManager.getInitializedCipherForDecryption(symmetricKeyAlias, iv)
                    val cryptoObject = BiometricPrompt.CryptoObject(cipher)

                    val fragmentActivity = getCurrentActivity() as FragmentActivity
                    val executor: Executor = Executors.newSingleThreadExecutor()

                    // Set up the biometric prompt callback
                    val authCallback = object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            try {
                                // Decrypt the token after successful authentication
                                val decryptedToken = cryptographyManager.decryptData(encryptedData, result.cryptoObject?.cipher!!)
                                promise.resolve(decryptedToken)
                            } catch (e: Exception) {
                                promise.reject("Decryption error", "Error occurred during decryption: ${e.message}")
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            promise.reject("Biometric Authentication error", errString.toString())
                        }

                        // Handle other authentication cases (failure, cancellation) as needed
                    }

                    val biometricPrompt = BiometricPrompt(fragmentActivity, executor, authCallback)

                    // Start the biometric authentication
                    biometricPrompt.authenticate(getPromptInfo(promptMessage, cancelButtonText, allowDeviceCredentials), cryptoObject)
                } catch (e: Exception) {
                    Log.e("BiometricPrompt", "Error in biometric prompt: ", e)
                    promise.reject("Error displaying local biometric prompt: " + e.message, "Error displaying local biometric prompt: " + e.message)
                }
            })
    } else {
        promise.reject("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0")
    }
}


  @ReactMethod
  fun isSensorAvailable(params: ReadableMap, promise: Promise) {
    try {
      if (isCurrentSDKMarshmallowOrLater()) {
        val allowDeviceCredentials: Boolean = params.getBoolean("allowDeviceCredentials")
        val reactApplicationContext: ReactApplicationContext = getReactApplicationContext()
        val biometricManager = BiometricManager.from(reactApplicationContext)
        val canAuthenticate = biometricManager.canAuthenticate(getAllowedAuthenticators(allowDeviceCredentials))
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
    } catch (e: java.lang.Exception) {
      promise.reject("Error detecting biometrics availability: " + e.message, "Error detecting biometrics availability: " + e.message)
    }
  }

  private fun getPromptInfo(promptMessage: String, cancelButtonText: String, allowDeviceCredentials: Boolean): PromptInfo {
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

  private fun isCurrentSDKMarshmallowOrLater(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
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
