package com.expense.tracker.data.repository

import com.expense.tracker.util.TransactionDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepository @Inject constructor() {
    private val _latestTransaction = MutableSharedFlow<TransactionDetails>(replay = 0)
    val latestTransaction: SharedFlow<TransactionDetails> = _latestTransaction.asSharedFlow()

    suspend fun emitTransaction(details: TransactionDetails) {
        _latestTransaction.emit(details)
    }
}
