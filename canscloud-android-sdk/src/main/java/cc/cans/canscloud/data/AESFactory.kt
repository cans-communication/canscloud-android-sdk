package cc.cans.canscloud.data

import cc.cans.canscloud.sdk.BuildConfig
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESFactory {
    companion object {
        fun decrypt(cipherText: String): String? {
            return try {
                val algorithm = "AES/CBC/PKCS5Padding"
                val key = SecretKeySpec(BuildConfig.KEY_AES.toByteArray(), "AES")
                val iv = IvParameterSpec(ByteArray(16))

                val cipher = Cipher.getInstance(algorithm)
                cipher.init(Cipher.DECRYPT_MODE, key, iv)
                val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText))
                String(plainText)
            } catch (e: Exception) {
                null
            }
        }
    }
}