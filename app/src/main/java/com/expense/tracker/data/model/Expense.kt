package com.expense.tracker.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val category: String = "",
    val subCategory: String = "",
    val details: String = "",
    val vendor: String = "",
    val upiId: String = "",
    val paymentMethod: String = "",
    val expenseTimestamp: String = "",
    val type: String = "Expense", // Expense or Income
    @ServerTimestamp
    val timestamp: Date? = null
)
