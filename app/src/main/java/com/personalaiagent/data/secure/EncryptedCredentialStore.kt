package com.personalaiagent.data.secure

import com.personalaiagent.domain.repository.CredentialStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Stores per-provider API keys as ciphertext in private storage.
 *
 * Guarantees:
 *  * nothing is ever logged;
 *  * [read] never throws — a corrupt payload or a missing Keystore key simply
 *    yields `null`, so the UI can ask the user to re-enter the key;
 *  * the backing file is excluded from cloud backup (see backup_rules.xml).
 */
class EncryptedCredentialStore(
    private val store: KeyValueStore,
    private val cipher: SecretCipher,
    private val ioDispatcher: CoroutineDispatcher,
) : CredentialStore {

    override suspend fun save(providerId: String, apiKey: String) = withContext(ioDispatcher) {
        val encrypted = cipher.encrypt(apiKey) // may throw SecureStorageException
        if (!store.put(storageKey(providerId), encrypted)) {
            throw SecureStorageException("Unable to persist secret")
        }
    }

    override suspend fun read(providerId: String): String? = withContext(ioDispatcher) {
        val stored = store.get(storageKey(providerId)) ?: return@withContext null
        try {
            cipher.decrypt(stored)
        } catch (_: SecureStorageException) {
            null
        }
    }

    override suspend fun exists(providerId: String): Boolean = withContext(ioDispatcher) {
        store.contains(storageKey(providerId))
    }

    override suspend fun delete(providerId: String) = withContext(ioDispatcher) {
        store.remove(storageKey(providerId))
        Unit
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        store.clear()
        Unit
    }

    companion object {
        /**
         * Stored under an obfuscated key so the preference file does not reveal
         * which providers exist. Exposed for tests only.
         */
        internal fun storageKey(providerId: String): String =
            "k_" + providerId.hashCode().toUInt().toString(16) + "_" + providerId.length
    }
}
