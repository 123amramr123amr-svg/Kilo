package com.personalaiagent.data.secure

/** Symmetric cipher used to protect API keys at rest. */
interface SecretCipher {
    /** Returns an opaque, Base64 encoded payload containing IV + ciphertext. */
    fun encrypt(plainText: String): String

    /** Reverses [encrypt]. Throws [SecureStorageException] when the payload is unusable. */
    fun decrypt(payload: String): String
}

class SecureStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
