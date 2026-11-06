package com.personalaiagent.data.secure

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic, reversible fake of the Android Keystore cipher. */
private class FakeCipher : SecretCipher {
    override fun encrypt(plainText: String): String = "enc:$plainText"

    override fun decrypt(payload: String): String {
        if (!payload.startsWith("enc:")) throw SecureStorageException("bad payload")
        return payload.removePrefix("enc:")
    }
}

private class MapKeyValueStore : KeyValueStore {
    val values = mutableMapOf<String, String>()

    override fun get(key: String): String? = values[key]
    override fun contains(key: String): Boolean = values.containsKey(key)
    override fun put(key: String, value: String): Boolean {
        values[key] = value
        return true
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun clear() = values.clear()
}

class EncryptedCredentialStoreTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = MapKeyValueStore()
    private val credentials = EncryptedCredentialStore(store, FakeCipher(), dispatcher)

    @Test
    fun `saves ciphertext and never plaintext`() = runTest(dispatcher) {
        credentials.save("provider-1", "sk-secret-value")

        val persisted = store.values.values.single()
        assertFalse(persisted.contains("sk-secret-value"))
        assertTrue(persisted.startsWith("enc:"))
        assertEquals("sk-secret-value", credentials.read("provider-1"))
    }

    @Test
    fun `exists reflects stored state`() = runTest(dispatcher) {
        assertFalse(credentials.exists("p1"))
        credentials.save("p1", "key")
        assertTrue(credentials.exists("p1"))
        credentials.delete("p1")
        assertFalse(credentials.exists("p1"))
    }

    @Test
    fun `corrupt payload yields null instead of throwing`() = runTest(dispatcher) {
        store.values[EncryptedCredentialStore.storageKey("p1")] = "not-encrypted"
        assertNull(credentials.read("p1"))
    }

    @Test
    fun `clear removes every key`() = runTest(dispatcher) {
        credentials.save("a", "1")
        credentials.save("b", "2")
        credentials.clear()
        assertTrue(store.values.isEmpty())
    }
}
