package cc.cans.canscloud.sdk.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SignInUserStoreTest {
    private val storage = HashMap<String, String>()
    private lateinit var store: SignInUserStore

    private val accountA = "9000@tenant.example.test"
    private val accountB = "9001@tenant.example.test"

    @Before
    fun setUp() {
        storage.clear()
        store = SignInUserStore(
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
    fun storesTheTextUnchanged() {
        val user = """{"user_id":"user-1","profile_image_url":null,"future_field":{"a":[1,2]}}"""
        store.set(accountA, user)
        assertEquals(user, store.get(accountA))
    }

    @Test
    fun nullRemovesTheStoredUser() {
        store.set(accountA, """{"user_id":"user-1"}""")
        store.set(accountA, null)
        assertNull(store.get(accountA))
        assertFalse(storage.containsKey(SignInUserStore.keyFor(accountA)))
    }

    @Test
    fun notAnObject_isNotStored() {
        store.set(accountA, """{"user_id":"user-1"}""")
        store.set(accountA, """["not", "an", "object"]""")
        assertNull(store.get(accountA))
    }

    @Test
    fun unreadableStoredText_isUnknown() {
        storage[SignInUserStore.keyFor(accountA)] = "{broken"
        assertNull(store.get(accountA))
    }

    @Test
    fun clearTouchesOnlyThatAccount() {
        store.set(accountA, """{"user_id":"a"}""")
        store.set(accountB, """{"user_id":"b"}""")
        store.clear(accountA)
        assertNull(store.get(accountA))
        assertEquals("""{"user_id":"b"}""", store.get(accountB))
    }
}
