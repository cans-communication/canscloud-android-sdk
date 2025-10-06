package cc.cans.canscloud.sdk.bcrypt

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AccessTokenClaims(
    @SerializedName("domain_uuid") val domainUuid: String?,
    val exp: Long?,
    val iat: Long?,
    val permissions: Any?,          // แทนด้วย data class ที่ตรงโครงสร้างจริงได้
    @SerializedName("server_name") val serverName: String?,
    val sub: String?,
    @SerializedName("user_uuid") val userUuid: String?,
    val username: String?
)

object JwtMapper {
    private val gson = Gson()
    fun <T> decodePayload(token: String, clazz: Class<T>): T? {
        val p = JwtHelper.split(token)?.second ?: return null
        val json = runCatching { JwtHelper::class.java.getDeclaredMethod("b64UrlDecodeToString", String::class.java)
            .apply { isAccessible = true }
            .invoke(JwtHelper, p) as String
        }.getOrNull() ?: return null
        return runCatching { gson.fromJson(json, clazz) }.getOrNull()
    }
}
