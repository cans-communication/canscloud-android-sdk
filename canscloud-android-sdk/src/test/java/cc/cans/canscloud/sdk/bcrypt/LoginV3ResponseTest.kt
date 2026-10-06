package cc.cans.canscloud.sdk.bcrypt

import cc.cans.canscloud.sdk.bcrypt.models.LoginV3Response
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginV3ResponseTest {
    /** Shape of `POST api/v3/sign-in/cc`; every value, including the token, is made up. */
    private fun response(permissionsField: String) = """
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
              $permissionsField
            }
          },
          "message": "Login successful"
        }
    """.trimIndent()

    private fun parse(permissionsField: String) =
        Gson().fromJson(response(permissionsField), LoginV3Response::class.java)

    @Test
    fun parsesEveryPermissionAsSent() {
        val parsed = parse(
            """, "permissions": ["contacts:create", "chats:delete@full_access", "contacts:delete"]""",
        )
        assertEquals(
            listOf("contacts:create", "chats:delete@full_access", "contacts:delete"),
            parsed.data?.user?.permissions,
        )
        assertEquals("domain-1", parsed.data?.user?.domainId)
    }

    @Test
    fun missingField_isNull() {
        assertNull(parse("").data?.user?.permissions)
    }

    @Test
    fun explicitNull_isNull() {
        assertNull(parse(""", "permissions": null""").data?.user?.permissions)
    }

    @Test
    fun emptyArray_isAnEmptyList() {
        assertEquals(emptyList<String>(), parse(""", "permissions": []""").data?.user?.permissions)
    }
}
