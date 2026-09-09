package com.personalexpensetracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCryptoManagerTest {

    private val sampleBackupPayload = """
        {
            "magic": "FINLY_BACKUP",
            "backupVersion": 1,
            "checksum": "abc123hash",
            "payload": "{\"expenses\":[{\"title\":\"Coffee\",\"amount\":\"4.50\"}]}"
        }
    """.trimIndent()

    @Test
    fun encryptAndDecrypt_withCorrectPassword_restoresOriginalContent() {
        val password = "StrongMasterPassword123!".toCharArray()

        val encryptResult = BackupCryptoManager.encryptBackup(sampleBackupPayload, password)
        assertTrue(encryptResult is CryptoResult.Success)
        val encryptedJson = (encryptResult as CryptoResult.Success).data

        // Ensure ciphertext does not reveal plain text
        assertFalse(encryptedJson.contains("FINLY_BACKUP"))
        assertFalse(encryptedJson.contains("Coffee"))
        assertTrue(BackupCryptoManager.isEncryptedBackup(encryptedJson))

        // Decrypt with same password
        val decryptResult = BackupCryptoManager.decryptBackup(encryptedJson, password)
        assertTrue(decryptResult is CryptoResult.Success)
        val decryptedContent = (decryptResult as CryptoResult.Success).data
        assertEquals(sampleBackupPayload, decryptedContent)
    }

    @Test
    fun decrypt_withIncorrectPassword_failsCleanly() {
        val password = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encryptResult = BackupCryptoManager.encryptBackup(sampleBackupPayload, password)
        val encryptedJson = (encryptResult as CryptoResult.Success).data

        val decryptResult = BackupCryptoManager.decryptBackup(encryptedJson, wrongPassword)
        assertTrue(decryptResult is CryptoResult.Failure)
        val failure = decryptResult as CryptoResult.Failure
        assertTrue(failure.message.contains("Incorrect password or corrupted backup file"))
    }

    @Test
    fun decrypt_withCorruptedCiphertext_failsCleanly() {
        val password = "ValidPassword".toCharArray()
        val encryptResult = BackupCryptoManager.encryptBackup(sampleBackupPayload, password)
        val encryptedJson = (encryptResult as CryptoResult.Success).data

        // Corrupt characters inside ciphertext
        val corruptedJson = encryptedJson.replace("a", "b")
        val decryptResult = BackupCryptoManager.decryptBackup(corruptedJson, password)

        assertTrue(decryptResult is CryptoResult.Failure)
    }

    @Test
    fun encrypt_withEmptyPassword_fails() {
        val result = BackupCryptoManager.encryptBackup(sampleBackupPayload, charArrayOf())
        assertTrue(result is CryptoResult.Failure)
    }

    @Test
    fun decrypt_withNonEncryptedContent_failsCleanly() {
        val nonEncrypted = "{ \"hello\": \"world\" }"
        val result = BackupCryptoManager.decryptBackup(nonEncrypted, "password".toCharArray())
        assertTrue(result is CryptoResult.Failure)
        assertFalse(BackupCryptoManager.isEncryptedBackup(nonEncrypted))
    }
}

