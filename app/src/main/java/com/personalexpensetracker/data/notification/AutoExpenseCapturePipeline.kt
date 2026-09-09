package com.personalexpensetracker.data.notification

import android.content.Context
import com.personalexpensetracker.data.database.AppDatabase
import com.personalexpensetracker.data.notification.duplicate.DuplicateTransactionDetector
import com.personalexpensetracker.data.repository.ExpenseRepositoryImpl
import com.personalexpensetracker.data.repository.IncomeRepositoryImpl

/**
 * Centralized provider for the automatic expense capture pipeline.
 *
 * Ensures that both the Android NotificationListenerService and the SMS Receiver / Reader
 * share the exact same [AutoExpenseCaptureProcessor] instance and [DuplicateTransactionDetector],
 * preventing duplicate expense insertions across channels (e.g. when both SMS and push
 * notifications arrive for the same transaction).
 */
object AutoExpenseCapturePipeline {

    val sharedDuplicateDetector = DuplicateTransactionDetector()

    @Volatile
    private var instance: AutoExpenseCaptureProcessor? = null

    /**
     * Gets or creates the shared pipeline processor.
     */
    fun getProcessor(context: Context): AutoExpenseCaptureProcessor {
        return instance ?: synchronized(this) {
            instance ?: run {
                val database = AppDatabase.getInstance(context.applicationContext)
                val expenseRepository = ExpenseRepositoryImpl(database.expenseDao())
                val incomeRepository = IncomeRepositoryImpl(database.incomeDao())
                AutoExpenseCaptureProcessor(expenseRepository, sharedDuplicateDetector, incomeRepository).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Sets a custom processor instance. Primarily used in automated tests.
     */
    fun setProcessorForTesting(customProcessor: AutoExpenseCaptureProcessor?) {
        synchronized(this) {
            instance = customProcessor
        }
    }
}
