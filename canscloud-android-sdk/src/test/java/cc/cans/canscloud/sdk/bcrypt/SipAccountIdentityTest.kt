package cc.cans.canscloud.sdk.bcrypt

import cc.cans.canscloud.sdk.bcrypt.models.LoginSipCredentialsData
import cc.cans.canscloud.sdk.bcrypt.models.sipAccountIdentity
import cc.cans.canscloud.sdk.core.AccountPermissionStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The login stores the permission list under the identity the account is created with, and
 * lookup and sign-out read that same identity back from the account. Every value is made up.
 */
class SipAccountIdentityTest {
    private val loginUsername = "user@example.test"
    private val loginDomain = "login.example.test"
    private val permissions = listOf("contacts:create", "chats:delete@full_access")

    private val storage = HashMap<String, String>()
    private lateinit var store: AccountPermissionStore

    @Before
    fun setUp() {
        storage.clear()
        store = AccountPermissionStore(
            read = { key -> storage[key] ?: "" },
            write = { key, value -> if (value == null) storage.remove(key) else storage[key] = value },
        )
    }

    private fun credentials(extension: String?, domainName: String?) =
        LoginSipCredentialsData(extension = extension, domainName = domainName, sipCreds = "fake-ha1")

    @Test
    fun usesTheExtensionAndDomainFromTheResponse() {
        val identity = credentials("9000", "sip.example.test").sipAccountIdentity(loginUsername, loginDomain)
        assertEquals("9000", identity.username)
        assertEquals("sip.example.test", identity.domain)
        assertEquals("9000@sip.example.test", identity.address)
    }

    @Test
    fun dropsThePortFromTheDomain() {
        val identity = credentials("9000", "sip.example.test:8446").sipAccountIdentity(loginUsername, loginDomain)
        assertEquals("9000@sip.example.test", identity.address)
    }

    @Test
    fun matchingDomains_keepTheKeyTheLoginAlwaysUsed() {
        val identity = credentials("9000", loginDomain).sipAccountIdentity(loginUsername, loginDomain)
        assertEquals("9000@$loginDomain", identity.address)
    }

    @Test
    fun responseWithAnotherSipDomain_isFoundAndClearedByTheAccountIdentity() {
        val identity = credentials("9000", "sip.example.test").sipAccountIdentity(loginUsername, loginDomain)
        store.set(identity.address, permissions)

        // What the registered account reports: `<identity username>@<identity domain>`.
        assertEquals(permissions, store.get("9000@sip.example.test"))
        // The old key, `<extension>@<login domain>`, holds nothing.
        assertNull(store.get("9000@$loginDomain"))

        store.clear("9000@sip.example.test")
        assertNull(store.get("9000@sip.example.test"))
        assertEquals(emptyMap<String, String>(), storage)
    }

    @Test
    fun responseWithoutExtension_isFoundAndClearedByTheAccountIdentity() {
        val identity = credentials(null, null).sipAccountIdentity(loginUsername, loginDomain)
        assertEquals(loginUsername, identity.username)
        assertEquals(loginDomain, identity.domain)
        store.set(identity.address, permissions)

        assertEquals(permissions, store.get("$loginUsername@$loginDomain"))
        // The old key had an empty username.
        assertNull(store.get("@$loginDomain"))

        store.clear("$loginUsername@$loginDomain")
        assertEquals(emptyMap<String, String>(), storage)
    }

    @Test
    fun sameExtensionOnTwoSipDomains_keepSeparateLists() {
        val first = credentials("9000", "a.example.test").sipAccountIdentity(loginUsername, loginDomain)
        val second = credentials("9000", "b.example.test").sipAccountIdentity(loginUsername, loginDomain)
        store.set(first.address, permissions)
        store.set(second.address, emptyList())

        store.clear(first.address)
        assertNull(store.get(first.address))
        assertEquals(emptyList<String>(), store.get(second.address))
    }
}
