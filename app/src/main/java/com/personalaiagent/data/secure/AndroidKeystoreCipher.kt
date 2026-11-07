package com.personalaiagent.data.secure

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256/GCM cipher backed by a non-exportable Android Keystore key.
 *
 * The key never leaves the secure hardware / TEE; only ciphertext is written to
 * disk. Authentication tag (128 bit) protects against tampering. On devices
 * where the Keystore is unavailable (rare, broken vendors) callers receive
 * [SecureStorageException] and the UI reports that the key could not be stored.
 */
class AndroidKeystoreCipher(
    private val keyAlias: String = DEFAULT_ALIAS,
) : SecretCipher {

    override fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val payload = cipher.iv + cipherText
            Base64.encodeToString(payload, Base64.NO_WRAP)
        } catch (e: Exception) {
            throw SecureStorageException("Unable to encrypt secret", e)
        }
    }

    override fun decrypt(payload: String): String {
        return try {
            val decoded = Base64.decode(payload, Base64.NO_WRAP)
            require(decoded.size > IV_LENGTH) { "payload too short" }
            val iv = decoded.copyOfRange(0, IV_LENGTH)
            val cipherText = decoded.copyOfRange(IV_LENGTH, decoded.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_LENGTH_BITS, iv),
            )
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecureStorageException("Unable to decrypt secret", e)
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            // No user authentication requirement: the agent must be able to send a
            // request while the app is in the foreground without a biometric prompt.
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "pai_api_key_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_LENGTH_BITS = 128
        private const val IV_LENGTH = 12
    }
}
