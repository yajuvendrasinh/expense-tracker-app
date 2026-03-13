package com.expense.tracker.data.repository

import com.expense.tracker.data.model.Expense
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    override fun getExpenses(): Flow<List<Expense>> = callbackFlow {
        val listener = firestore.collection("expenses")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addExpense(expense: Expense) {
        try {
            val docRef = firestore.collection("expenses").document()
            val newExpense = expense.copy(id = docRef.id)
            docRef.set(newExpense).await()
            android.util.Log.d("ExpenseRepository", "Expense added successfully: ${newExpense.id}")
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error adding expense", e)
            throw e
        }
    }

    override suspend fun getLatestExpenseByVendor(vendor: String): Expense? {
        return try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("vendor", vendor)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(Expense::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error getting latest expense by vendor", e)
            null
        }
    }

    override suspend fun isTransactionSaved(amount: Double, timestamp: Long): Boolean {
        // Check for transactions with same amount within a small time window (e.g., 5 mins)
        // This heuristic accounts for slight delays between the SMS timestamp and the time the user saves the expense.
        
        val start = java.util.Date(timestamp - 300000) // 5 mins before
        val end = java.util.Date(timestamp + 300000)   // 5 mins after
        
        return try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("amount", amount)
                .whereGreaterThan("timestamp", start)
                .whereLessThan("timestamp", end)
                .get()
                .await()
            
            !snapshot.isEmpty
        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "Error checking if transaction saved", e)
            false
        }
    }
}
