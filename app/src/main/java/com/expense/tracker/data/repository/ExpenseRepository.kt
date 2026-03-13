package com.expense.tracker.data.repository

import com.expense.tracker.data.model.Expense
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing expense data.
 */
interface ExpenseRepository {
    /**
     * Observes the list of expenses.
     */
    fun getExpenses(): Flow<List<Expense>>
    
    /**
     * Adds a new expense to the repository.
     */
    suspend fun addExpense(expense: Expense)
    
    /**
     * Retrieves the latest expense associated with a specific vendor.
     * Used for "Vendor Learning" to suggest categories.
     */
    suspend fun getLatestExpenseByVendor(vendor: String): Expense?
    
    /**
     * Checks if a transaction with the given amount and timestamp has likely already been saved.
     * This helps prevent duplicate entries from SMS.
     */
    suspend fun isTransactionSaved(amount: Double, timestamp: Long): Boolean
}
