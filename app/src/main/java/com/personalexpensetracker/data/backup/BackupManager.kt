package com.personalexpensetracker.data.backup

import com.personalexpensetracker.data.export.JsonDataExporter
import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant

/**
 * Result of reading a local backup file.
 */
sealed class BackupReadResult {
    data class Success(val data: FinancialBackupData, val createdAt: Instant, val version: Int) : BackupReadResult()
    data class Failure(val message: String, val cause: Throwable? = null) : BackupReadResult()
}

/**
 * Manages creation and integrity verification of versioned .finlybackup files (Phase 9 Step 5).
 * Encapsulates financial payload with magic header, timestamp, schema version, and SHA-256 checksum.
 */
object BackupManager {

    const val BACKUP_FILE_EXTENSION = ".finlybackup"
    private const val MAGIC_HEADER = "FINLY_BACKUP"
    private const val CURRENT_BACKUP_VERSION = 1
    private const val APP_VERSION = "1.0.0"

    /**
     * Packages financial data into an integrity-checked, versioned backup bundle.
     */
    fun createBackupBundle(data: FinancialBackupData): String {
        val payloadJson = JsonDataExporter.exportToJson(data, indentSpaces = 0)
        val checksum = computeSha256(payloadJson)

        val bundle = JSONObject()
        bundle.put("magic", MAGIC_HEADER)
        bundle.put("backupVersion", CURRENT_BACKUP_VERSION)
        bundle.put("appVersion", APP_VERSION)
        bundle.put("createdAt", Instant.now().toString())
        bundle.put("checksum", checksum)
        bundle.put("payload", payloadJson)

        return bundle.toString(2)
    }

    /**
     * Parses, verifies, and extracts financial data from a backup bundle string.
     */
    fun readBackupBundle(bundleString: String): BackupReadResult {
        if (bundleString.isBlank()) {
            return BackupReadResult.Failure("Backup content is empty")
        }

        return try {
            val bundle = JSONObject(bundleString)

            val magic = bundle.optString("magic")
            if (magic != MAGIC_HEADER) {
                return BackupReadResult.Failure("Not a valid Finly backup file (missing magic header)")
            }

            val version = bundle.optInt("backupVersion", -1)
            if (version > CURRENT_BACKUP_VERSION || version < 1) {
                return BackupReadResult.Failure("Unsupported backup version: $version (current version is $CURRENT_BACKUP_VERSION)")
            }

            val expectedChecksum = bundle.optString("checksum")
            val payloadJson = bundle.optString("payload")

            if (payloadJson.isBlank()) {
                return BackupReadResult.Failure("Backup file contains no payload data")
            }

            val actualChecksum = computeSha256(payloadJson)
            if (actualChecksum != expectedChecksum) {
                return BackupReadResult.Failure("Backup integrity check failed: file has been tampered with or corrupted")
            }

            val createdAt = if (bundle.has("createdAt")) {
                Instant.parse(bundle.getString("createdAt"))
            } else {
                Instant.now()
            }

            val financialData = JsonDataExporter.parseFromJson(payloadJson)
            BackupReadResult.Success(
                data = financialData,
                createdAt = createdAt,
                version = version
            )
        } catch (e: Exception) {
            BackupReadResult.Failure("Failed to parse backup bundle: ${e.localizedMessage}", e)
        }
    }

    /**
     * Generates standard default filename for backups: finly_backup_YYYYMMDD_HHMMSS.finlybackup
     */
    fun generateDefaultBackupFileName(now: Instant = Instant.now()): String {
        val cleanTimestamp = now.toString()
            .replace(":", "-")
            .replace(".", "-")
        return "finly_backup_$cleanTimestamp$BACKUP_FILE_EXTENSION"
    }

    /**
     * Computes SHA-256 hex digest for data integrity verification.
     */
    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

