package com.personalexpensetracker.data.backup

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Result of a cryptographic backup operation.
 */
sealed class CryptoResult<out T> {
    data class Success<T>(val data: T) : CryptoResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : CryptoResult<Nothing>()
}

/**
 * Provides industry-standard AES-256-GCM encryption and PBKDF2WithHmacSHA256 key derivation
 * for Finly backup files (Phase 9 Step 6).
 */
object BackupCryptoManager {

    private const val MAGIC_ENCRYPTED = "FINLY_ENCRYPTED_BACKUP"
    private const val CURRENT_CRYPTO_VERSION = 1
    private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Encrypts a plain backup bundle string using AES-256-GCM derived from the user's password.
     */
    fun encryptBackup(plainBundle: String, password: CharArray): CryptoResult<String> {
        if (password.isEmpty()) {
            return CryptoResult.Failure("Password cannot be empty")
        }

        return try {
            val salt = ByteArray(SALT_LENGTH_BYTES)
            secureRandom.nextBytes(salt)

            val iv = ByteArray(IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

            val cipherText = cipher.doFinal(plainBundle.toByteArray(Charsets.UTF_8))

            val envelope = JSONObject()
            envelope.put("magic", MAGIC_ENCRYPTED)
            envelope.put("version", CURRENT_CRYPTO_VERSION)
            envelope.put("kdf", KDF_ALGORITHM)
            envelope.put("iterations", ITERATION_COUNT)
            envelope.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            envelope.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            envelope.put("cipherText", Base64.encodeToString(cipherText, Base64.NO_WRAP))

            CryptoResult.Success(envelope.toString(2))
        } catch (e: Exception) {
            CryptoResult.Failure("Encryption failed: ${e.localizedMessage}", e)
        }
    }

    /**
     * Decrypts an encrypted backup envelope using the user's password.
     */
    fun decryptBackup(encryptedEnvelope: String, password: CharArray): CryptoResult<String> {
        if (encryptedEnvelope.isBlank()) {
            return CryptoResult.Failure("Encrypted backup content is empty")
        }
        if (password.isEmpty()) {
            return CryptoResult.Failure("Password cannot be empty")
        }

        return try {
            val envelope = try {
                JSONObject(encryptedEnvelope)
            } catch (e: Exception) {
                return CryptoResult.Failure("Invalid backup file format (not valid JSON)")
            }

            val magic = envelope.optString("magic")
            if (magic != MAGIC_ENCRYPTED) {
                return CryptoResult.Failure("Not an encrypted Finly backup file")
            }

            val version = envelope.optInt("version", -1)
            if (version > CURRENT_CRYPTO_VERSION || version < 1) {
                return CryptoResult.Failure("Unsupported crypto version: $version")
            }

            val saltBase64 = envelope.getString("salt")
            val ivBase64 = envelope.getString("iv")
            val cipherTextBase64 = envelope.getString("cipherText")
            val iterations = envelope.optInt("iterations", ITERATION_COUNT)

            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)

            val secretKey = deriveKey(password, salt, iterations)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val plainBytes = try {
                cipher.doFinal(cipherText)
            } catch (e: Exception) {
                return CryptoResult.Failure("Incorrect password or corrupted backup file", e)
            }

            val plainText = String(plainBytes, Charsets.UTF_8)
            CryptoResult.Success(plainText)
        } catch (e: Exception) {
            CryptoResult.Failure("Decryption failed: ${e.localizedMessage}", e)
        }
    }

    /**
     * Checks if the content string appears to be an encrypted backup.
     */
    fun isEncryptedBackup(content: String): Boolean {
        return try {
            val json = JSONObject(content)
            json.optString("magic") == MAGIC_ENCRYPTED
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATION_COUNT): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}

