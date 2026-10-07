package cc.cans.canscloud.sdk.core

import com.google.gson.Gson
import com.google.gson.JsonParser

/**
 * Per-account `permissions` array from `POST api/v3/sign-in/cc`, stored raw as a JSON string array
 * under the access token's `sipAddress` key. SDK treats values opaquely; host app defines semantics.
 * `null` ("unknown/unstored") differs from an empty array ("zero permissions").
 * `read`/`write` storage parameters (`write(key, null)` deletes) enable unit testing without a Linphone `Config`.
 */
class AccountPermissionStore(
    private val read: (key: String) -> String?,
    private val write: (key: String, value: String?) -> Unit,
) {
    /** The stored list, or `null` when nothing (or nothing readable) is stored for [sipAddress]. */
    fun get(sipAddress: String): List<String>? = decode(read(keyFor(sipAddress)))

    /** Replaces the stored list. `null` removes it, leaving the account's permissions unknown. */
    fun set(sipAddress: String, permissions: List<String?>?) {
        write(keyFor(sipAddress), encode(permissions))
    }

    fun clear(sipAddress: String) {
        write(keyFor(sipAddress), null)
    }

    companion object {
        fun keyFor(sipAddress: String): String = "account_permissions_$sipAddress"

        fun encode(permissions: List<String?>?): String? =
            permissions?.let { Gson().toJson(it.filterNotNull()) }

        fun decode(stored: String?): List<String>? {
            if (stored.isNullOrBlank()) return null
            return try {
                val json = JsonParser.parseString(stored)
                if (!json.isJsonArray) return null
                json.asJsonArray
                    .filter { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                    .map { it.asString }
            } catch (e: Exception) {
                null
            }
        }
    }
}
