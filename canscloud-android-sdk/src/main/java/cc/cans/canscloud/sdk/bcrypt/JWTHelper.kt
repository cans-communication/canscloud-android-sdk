package cc.cans.canscloud.sdk.bcrypt

import android.util.Base64
import org.json.JSONObject

object JwtHelper {

    /** คืนค่า 3 ส่วนของ JWT [header, payload, signature] หรือค่าว่างถ้า format ไม่ถูก */
    fun split(token: String): Triple<String, String, String>? {
        val p = token.split(".")
        if (p.size != 3) return null
        return Triple(p[0], p[1], p[2])
    }

    /** ถอด Base64URL (no padding/no wrap) เป็น String UTF-8 */
    private fun b64UrlDecodeToString(b64url: String): String {
        val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val bytes = Base64.decode(b64url, flags)
        return String(bytes, Charsets.UTF_8)
    }

    /** อ่าน header (JSON object) */
    fun header(token: String): JSONObject? =
        split(token)?.first?.let { runCatching { JSONObject(b64UrlDecodeToString(it)) }.getOrNull() }

    /** อ่าน payload (JSON object) */
    fun payload(token: String): JSONObject? =
        split(token)?.second?.let { runCatching { JSONObject(b64UrlDecodeToString(it)) }.getOrNull() }

    /** อ่าน claim แบบ string */
    fun claimString(token: String, key: String): String? =
        payload(token)?.optString(key, null)

    /** อ่าน claim แบบ long (เช่น exp/iat เป็น Unix seconds) */
    fun claimLong(token: String, key: String): Long? {
        val v = payload(token)?.optLong(key, Long.MIN_VALUE) ?: return null
        return if (v == Long.MIN_VALUE) null else v
    }

    /** เช็คหมดอายุ โดยเผื่อ clock-skew (ค่า default 60s) */
    fun isExpired(token: String, skewSeconds: Long = 60): Boolean {
        val exp = claimLong(token, "exp") ?: return false // ไม่มี exp ถือว่าไม่หมดอายุใน client
        val now = System.currentTimeMillis() / 1000L
        return exp <= now + skewSeconds
    }

    /** ดึง claims ทั้งก้อนเป็น Map<String, Any?> แบบง่าย ๆ */
    fun claimsMap(token: String): Map<String, Any?> {
        val obj = payload(token) ?: return emptyMap()
        return obj.keys().asSequence().associateWith { k -> obj.opt(k) }
    }
}