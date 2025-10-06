package cc.cans.canscloud.sdk.utils

object SecureUtils {
    fun md5(s: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
