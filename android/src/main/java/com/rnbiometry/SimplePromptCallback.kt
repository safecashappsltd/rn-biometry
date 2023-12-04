import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap

class SimplePromptCallback(promise: Promise) : BiometricPrompt.AuthenticationCallback() {
  private val promise: Promise

  init {
    this.promise = promise
  }

  fun onAuthenticationError(errorCode: Int, @NonNull errString: CharSequence) {
    super.onAuthenticationError(errorCode, errString)
    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
      val resultMap: WritableMap = WritableNativeMap()
      resultMap.putBoolean("success", false)
      resultMap.putString("error", "User cancellation")
      promise.resolve(resultMap)
    } else {
      promise.reject(errString.toString(), errString.toString())
    }
  }

  fun onAuthenticationSucceeded(@NonNull result: BiometricPrompt.AuthenticationResult?) {
    super.onAuthenticationSucceeded(result)
    val resultMap: WritableMap = WritableNativeMap()
    resultMap.putBoolean("success", true)
    promise.resolve(resultMap)
  }
}
