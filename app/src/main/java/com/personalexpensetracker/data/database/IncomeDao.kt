package com.personalexpensetracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for incomes table operations.
 */
@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<IncomeEntity>): List<Long>

    @Update
    suspend fun updateIncome(income: IncomeEntity): Int

    @Delete
    suspend fun deleteIncome(income: IncomeEntity): Int

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long): Int

    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes(): Int

    @Query("SELECT * FROM incomes WHERE id = :id")
    suspend fun getIncomeById(id: Long): IncomeEntity?

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    fun getAllIncomes(): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes ORDER BY date DESC")
    suspend fun getAllIncomesSync(): List<IncomeEntity>

    @Query("SELECT * FROM incomes WHERE source = :source ORDER BY date DESC")
    fun getIncomesBySource(source: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getIncomesByDateRange(startDate: Instant, endDate: Instant): Flow<List<IncomeEntity>>

    @Query("SELECT SUM(amount_in_cents) FROM incomes")
    fun getTotalIncomeInCents(): Flow<Long?>
}

