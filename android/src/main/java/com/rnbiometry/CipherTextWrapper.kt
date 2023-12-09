package com.rnbiometry

object Constants {

  const val SHARED_PREFS_FILENAME = "biometric_prefs"
  const val CIPHERTEXT_WRAPPER = "ciphertext_wrapper"

}

data class CipherTextWrapper(
  val cipherText:ByteArray,
  val initalizationVector:ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CipherTextWrapper

    if (!cipherText.contentEquals(other.cipherText)) return false
    if (!initalizationVector.contentEquals(other.initalizationVector)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = cipherText.contentHashCode()
    result = 31 * result + initalizationVector.contentHashCode()
    return result
  }
}
