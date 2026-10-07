package cc.cans.canscloud.sdk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccountPermissionStoreTest {
    private val storage = HashMap<String, String>()
    private lateinit var store: AccountPermissionStore

    private val accountA = "9000@tenant.example.test"
    private val accountB = "9001@tenant.example.test"

    @Before
    fun setUp() {
        storage.clear()
        store = AccountPermissionStore(
            // Linphone's Config answers the default ("") for a missing key.
            read = { key -> storage[key] ?: "" },
            write = { key, value -> if (value == null) storage.remove(key) else storage[key] = value },
        )
    }

    @Test
    fun nothingStored_isUnknown() {
        assertNull(store.get(accountA))
    }

    @Test
    fun storesTheRawValuesUnchanged() {
        val permissions = listOf("contacts:create", "chats:delete@full_access")
        store.set(accountA, permissions)
        assertEquals(permissions, store.get(accountA))
    }

    @Test
    fun emptyList_staysAnEmptyList_notUnknown() {
        store.set(accountA, emptyList())
        assertEquals(emptyList<String>(), store.get(accountA))
    }

    @Test
    fun signingInAgain_overwritesTheOldList() {
        store.set(accountA, listOf("contacts:create", "contacts:delete"))
        store.set(accountA, listOf("contacts:view"))
        assertEquals(listOf("contacts:view"), store.get(accountA))
    }

    @Test
    fun responseWithoutPermissions_removesTheOldList() {
        store.set(accountA, listOf("contacts:create"))
        store.set(accountA, null)
        assertNull(store.get(accountA))
        assertFalse(storage.containsKey(AccountPermissionStore.keyFor(accountA)))
    }

    @Test
    fun signOut_clearsOnlyThatAccount() {
        store.set(accountA, listOf("contacts:create"))
        store.set(accountB, listOf("contacts:view"))
        store.clear(accountA)
        assertNull(store.get(accountA))
        assertEquals(listOf("contacts:view"), store.get(accountB))
    }

    @Test
    fun accountsDoNotMix() {
        store.set(accountA, listOf("contacts:create"))
        store.set(accountB, emptyList())
        assertEquals(listOf("contacts:create"), store.get(accountA))
        assertEquals(emptyList<String>(), store.get(accountB))
        assertNull(store.get("9000@other.example.test"))
        // Keys are exact strings: a different case is a different account.
        assertNull(store.get("9000@Tenant.Example.Test"))
    }

    @Test
    fun nullEntriesAreDropped() {
        store.set(accountA, listOf("contacts:create", null))
        assertEquals(listOf("contacts:create"), store.get(accountA))
    }

    @Test
    fun unreadableStoredValue_isUnknown() {
        val key = AccountPermissionStore.keyFor(accountA)
        for (stored in listOf("not json", "{\"a\":1}", "\"contacts:create\"", "   ")) {
            storage[key] = stored
            assertNull(stored, store.get(accountA))
        }
    }
}
