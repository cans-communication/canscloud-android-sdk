package cc.cans.canscloud.sdk.core

import com.google.gson.JsonParser

/**
 * Stores per-account `data.user` JSON from `POST api/v3/sign-in/cc` under the `sipAddress` key .
 * The SDK is agnostic, allowing the host app to consume required fields without SDK updates.
 */
class SignInUserStore(
    private val read: (key: String) -> String?,
    private val write: (key: String, value: String?) -> Unit,
) {
    fun get(sipAddress: String): String? = read(keyFor(sipAddress)).takeIf { isJsonObject(it) }

    fun set(sipAddress: String, userJson: String?) {
        write(keyFor(sipAddress), userJson.takeIf { isJsonObject(it) })
    }

    fun clear(sipAddress: String) {
        write(keyFor(sipAddress), null)
    }

    companion object {
        fun keyFor(sipAddress: String): String = "sign_in_user_$sipAddress"

        fun isJsonObject(text: String?): Boolean {
            if (text.isNullOrBlank()) return false
            return try {
                JsonParser.parseString(text).isJsonObject
            } catch (e: Exception) {
                false
            }
        }
    }
}
