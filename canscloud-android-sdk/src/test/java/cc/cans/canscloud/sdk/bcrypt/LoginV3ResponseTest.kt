package cc.cans.canscloud.sdk.bcrypt

import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Response
import cc.cans.canscloud.sdk.bcrypt.models.rawSignInUser
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginV3ResponseTest {
    /** Shape of `POST api/v3/sign-in/cc`; every value, including the token, is made up. */
    private fun response(extraFields: String) = """
        {
          "data": {
            "token": "fake-token-for-tests",
            "user": {
              "user_id": "user-1",
              "domain_id": "domain-1",
              "username": "9000",
              "display_name": "9000",
              "extension": "9000",
              "extension_id": "extension-1",
              "is_active": true,
              "password_reset_required": false
              $extraFields
            }
          },
          "message": "Login successful"
        }
    """.trimIndent()

    private fun parse(extraFields: String) =
        Gson().fromJson(response(extraFields), LoginV3Response::class.java)

    @Test
    fun parsesTheFieldsTheSdkUses() {
        val parsed = parse("")
        assertEquals("domain-1", parsed.data?.user?.domainId)
        assertEquals("9000", parsed.data?.user?.extensionNumber)
    }

    @Test
    fun rawUser_keepsPermissionsAsSent() {
        val body = JsonParser.parseString(
            response(""", "permissions": ["contacts:create", "chats:delete@full_access", "contacts:delete"]"""),
        ).asJsonObject
        val user = JsonParser.parseString(rawSignInUser(body)).asJsonObject
        assertEquals(
            listOf("contacts:create", "chats:delete@full_access", "contacts:delete"),
            user["permissions"].asJsonArray.map { it.asString },
        )
    }

    @Test
    fun rawUser_keepsFieldsTheModelDoesNotHave() {
        val body = JsonParser.parseString(
            response(""", "profile_image_url": "https://example.test/a.png", "future_field": 1"""),
        ).asJsonObject
        val user = JsonParser.parseString(rawSignInUser(body)).asJsonObject
        assertEquals("https://example.test/a.png", user["profile_image_url"].asString)
        assertEquals(1, user["future_field"].asInt)
        assertTrue(user.has("user_id"))
    }

    @Test
    fun rawUser_isNullWithoutAUserObject() {
        assertNull(rawSignInUser(null))
        assertNull(rawSignInUser(JsonParser.parseString("""{"data":null}""").asJsonObject))
        assertNull(rawSignInUser(JsonParser.parseString("""{"data":{"user":[]}}""").asJsonObject))
        assertNull(rawSignInUser(JsonObject()))
    }
}
