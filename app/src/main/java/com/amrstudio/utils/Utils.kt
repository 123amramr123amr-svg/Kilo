package com.amrstudio.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtils {
    private const val ALIAS = "amr_studio_master_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private var masterKey: MasterKey? = null
    private var keyStore: KeyStore? = null

    fun initialize(context: Context) {
        if (masterKey == null) {
            val spec = KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        }
    }

    fun encrypt(data: String): String {
        val masterKey = masterKey ?: throw IllegalStateException("CryptoUtils not initialized")
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey.getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun decrypt(encryptedData: String): String {
        val masterKey = masterKey ?: throw IllegalStateException("CryptoUtils not initialized")
        val combined = android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val encrypted = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey.getSecretKey(), spec)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, StandardCharsets.UTF_8)
    }

    fun createEncryptedFile(context: Context, fileName: String): EncryptedFile {
        val masterKey = masterKey ?: throw IllegalStateException("CryptoUtils not initialized")
        val file = File(context.filesDir, fileName)
        return EncryptedFile.Builder(
            file,
            context,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    fun writeEncryptedFile(context: Context, fileName: String, data: String) {
        val encryptedFile = createEncryptedFile(context, fileName)
        val outputStream = encryptedFile.openFileOutput()
        outputStream.write(data.toByteArray(StandardCharsets.UTF_8))
        outputStream.close()
    }

    fun readEncryptedFile(context: Context, fileName: String): String? {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null
        val encryptedFile = createEncryptedFile(context, fileName)
        val inputStream = encryptedFile.openFileInput()
        val bytes = inputStream.readBytes()
        inputStream.close()
        return String(bytes, StandardCharsets.UTF_8)
    }
}

object IdGenerator {
    fun generate(): String = java.util.UUID.randomUUID().toString()

    fun generateShort(): String = java.util.UUID.randomUUID().toString().substring(0, 8)
}

object DateTimeUtils {
    fun formatRelative(time: kotlinx.datetime.LocalDateTime): String {
        val now = kotlinx.datetime.LocalDateTime.now()
        val diff = time.until(now, kotlinx.datetime.TimeUnit.Seconds)

        return when {
            diff < 60 -> "الآن"
            diff < 3600 -> "${diff / 60} دقيقة"
            diff < 86400 -> "${diff / 3600} ساعة"
            diff < 604800 -> "${diff / 86400} يوم"
            else -> time.format(kotlinx.datetime.format.DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    fun formatRemaining(hours: Long): String {
        return when {
            hours < 1 -> "أقل من ساعة"
            hours < 24 -> "$hours ساعة"
            hours < 168 -> "${hours / 24} يوم ${hours % 24} ساعة"
            else -> "${hours / 168} أسبوع ${(hours % 168) / 24} يوم"
        }
    }
}

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace("\\s|-|\\(|\\)".toRegex(), "")
        return cleaned.matches("^(\\+?966|0)?5\\d{8}$".toRegex()) || // Saudi
               cleaned.matches("^(\\+?971|0)?5\\d{8}$".toRegex()) || // UAE
               cleaned.matches("^(\\+?20|0)?1\\d{9}$".toRegex()) || // Egypt
               cleaned.matches("^\\+?[1-9]\\d{7,14}$".toRegex()) // International
    }

    fun isStrongPassword(password: String): Boolean {
        return password.length >= 8 &&
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() } &&
               password.any { it.isDigit() }
    }

    fun passwordsMatch(password: String, confirm: String): Boolean = password == confirm
}

object FileUtils {
    fun getFileExtension(fileName: String): String {
        val index = fileName.lastIndexOf('.')
        return if (index > 0) fileName.substring(index + 1).lowercase() else ""
    }

    fun getLanguageFromExtension(ext: String): String = when (ext) {
        "html", "htm" -> "html"
        "css" -> "css"
        "js", "jsx", "ts", "tsx" -> "javascript"
        "json" -> "json"
        "md", "markdown" -> "markdown"
        "py" -> "python"
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "cpp", "cc", "cxx", "hpp", "h" -> "cpp"
        "cs" -> "csharp"
        "go" -> "go"
        "rs" -> "rust"
        "php" -> "php"
        "rb" -> "ruby"
        "swift" -> "swift"
        "sql" -> "sql"
        "sh", "bash" -> "bash"
        "yaml", "yml" -> "yaml"
        "xml" -> "xml"
        "svg" -> "xml"
        "dockerfile" -> "dockerfile"
        else -> "plaintext"
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}

object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

object HashUtils {
    fun hashPassword(password: String): String {
        // In production, use Argon2 or bcrypt via a proper library
        // This is a simplified version for demo
        val salt = java.util.UUID.randomUUID().toString()
        val hash = (password + salt).toByteArray(StandardCharsets.UTF_8)
            .let { java.security.MessageDigest.getInstance("SHA-256").digest(it) }
            .let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
        return "$salt:$hash"
    }

    fun verifyPassword(password: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val hash = parts[1]
        val computedHash = (password + salt).toByteArray(StandardCharsets.UTF_8)
            .let { java.security.MessageDigest.getInstance("SHA-256").digest(it) }
            .let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }
        return hash == computedHash
    }
}