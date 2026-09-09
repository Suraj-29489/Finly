package com.personalexpensetracker.domain.repository

import com.personalexpensetracker.domain.datamanagement.FinancialBackupData
import com.personalexpensetracker.domain.datamanagement.ImportSummary

/**
 * Repository defining contracts for data management: exporting, importing, backing up, and restoring (Phase 9 Step 1).
 */
interface DataManagementRepository {

    /**
     * Retrieve a snapshot of all persistent financial data in the application.
     */
    suspend fun getFullFinancialData(): FinancialBackupData

    /**
     * Atomically restore the application database from a backup snapshot.
     * Replaces existing data within a safe atomic transaction.
     */
    suspend fun restoreFinancialData(data: FinancialBackupData): ImportSummary

    /**
     * Merge imported financial data into the database.
     * Optionally skips duplicates based on amount, title, and timestamp.
     */
    suspend fun importFinancialData(data: FinancialBackupData, preventDuplicates: Boolean = true): ImportSummary

    /**
     * Clear all user data from the database safely.
     */
    suspend fun clearAllData()

    /**
     * Parse and safely import financial data from CSV content (Phase 9 Step 4).
     */
    suspend fun importFromCsv(csvContent: String, preventDuplicates: Boolean = true): ImportSummary

    /**
     * Parse and safely import financial data from JSON content (Phase 9 Step 4).
     */
    suspend fun importFromJson(jsonContent: String, preventDuplicates: Boolean = true): ImportSummary

    /**
     * Creates a versioned, optionally encrypted .finlybackup string bundle (Phase 9 Step 5 & 6).
     */
    suspend fun createBackup(password: CharArray? = null): String

    /**
     * Atomically restores database from a .finlybackup bundle, verifying integrity and decrypting if necessary (Phase 9 Step 6).
     * If decryption or validation fails, existing data remains completely untouched.
     */
    suspend fun restoreFromBackupBundle(bundleContent: String, password: CharArray? = null): ImportSummary
}
