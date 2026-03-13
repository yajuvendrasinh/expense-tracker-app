package com.expense.tracker.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.model.Category
import com.expense.tracker.data.model.CategoryData
import com.expense.tracker.data.model.Expense
import com.expense.tracker.data.repository.CategoryRepository
import com.expense.tracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class ExpenseUiState(
    val amount: String = "0",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategory: Category? = null,
    val selectedSubCategory: String = "",
    val note: String = "",
    val details: String = "",
    val vendor: String = "",
    val upiId: String = "",
    val paymentMethod: String = "",
    val expenseTimestamp: String = "",
    val type: String = "Expense", // Expense or Income
    val isLoading: Boolean = false,
    val isCategorySheetOpen: Boolean = false,
    val isDatePickerOpen: Boolean = false,
    val categories: List<Category> = emptyList(),
    val detectedTransactions: List<com.expense.tracker.util.TransactionDetails> = emptyList(),
    val showTransactionDialog: Boolean = false,
    val userMessage: String? = null // Feedback message for Snackbar
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val smsRepository: com.expense.tracker.data.repository.SmsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()
    
    init {
        loadCategories()
        observeSms()
    }

    private fun observeSms() {
        viewModelScope.launch {
            smsRepository.latestTransaction.collect { transaction ->
                updateFromSms(transaction.amount, transaction.vendor, transaction.paymentMethod, transaction.expenseTimestamp)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories().collect { categories ->
                // Filter out hidden categories for the main expense screen
                _uiState.value = _uiState.value.copy(
                    categories = categories.filter { !it.isHidden }.sortedBy { it.name }
                )
            }
        }
    }

    fun onAmountChange(input: String) {
        val currentAmount = _uiState.value.amount
        
        // Check decimal limit
        if (currentAmount.contains(".")) {
            val decimals = currentAmount.substringAfter(".")
            if (decimals.length >= 3 && input != ".") return
        }
        
        if (currentAmount == "0") {
             if (input != ".") {
                 _uiState.value = _uiState.value.copy(amount = input)
             } else {
                 _uiState.value = _uiState.value.copy(amount = "0.")
             }
        } else {
            if (input == "." && currentAmount.contains(".")) return
            _uiState.value = _uiState.value.copy(amount = currentAmount + input)
        }
    }

    fun onBackspace() {
        val currentAmount = _uiState.value.amount
        if (currentAmount.length > 1) {
            _uiState.value = _uiState.value.copy(amount = currentAmount.dropLast(1))
        } else {
            _uiState.value = _uiState.value.copy(amount = "0")
        }
    }

    fun onDateSelect(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    fun onCategorySelect(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onSubCategorySelect(subCategory: String) {
        _uiState.value = _uiState.value.copy(
            selectedSubCategory = subCategory, 
            isCategorySheetOpen = false
        )
    }

    fun onNoteChange(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onDetailsChange(details: String) {
        _uiState.value = _uiState.value.copy(details = details)
    }
    
    fun onVendorChange(vendor: String) {
        _uiState.value = _uiState.value.copy(vendor = vendor)
    }
    
    fun updateFromSms(amount: Double, vendor: String, upiId: String? = null, paymentMethod: String? = null, expenseTimestamp: String? = null) {
        _uiState.value = _uiState.value.copy(
            amount = amount.toString(),
            vendor = vendor,
            upiId = upiId ?: "",
            paymentMethod = paymentMethod ?: "",
            expenseTimestamp = expenseTimestamp ?: ""
        )
    }

    fun onMessageShown() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    /**
     * Scans the device for SMS messages received in the last 24 hours.
     * Use filters to identify transaction-related messages.
     * Prevents duplicate entries by checking against existing expenses in the repository.
     */
    fun scanForSms(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, userMessage = "Scanning for recent transactions...")
            try {
                val messages = com.expense.tracker.util.SmsReader.readRecentMessages(context)
                val newTransactions = mutableListOf<com.expense.tracker.util.TransactionDetails>()
                
                for (msg in messages) {
                    val details = com.expense.tracker.util.TransactionParser.parse(msg)
                    if (details != null) {
                        // Check if already saved
                        val isSaved = expenseRepository.isTransactionSaved(details.amount, details.timestamp)
                        if (!isSaved) {
                            newTransactions.add(details)
                        }
                    }
                }

                if (newTransactions.isEmpty()) {
                    // No new transactions found
                     _uiState.value = _uiState.value.copy(
                         isLoading = false,
                         userMessage = "No new transactions found in the last 24h."
                     )
                } else if (newTransactions.size == 1) {
                    // Single transaction - Auto-fill immediately
                    applyTransaction(newTransactions.first())
                    _uiState.value = _uiState.value.copy(userMessage = "Transaction found & applied!")
                } else {
                    // Multiple transactions - Show selection dialog to user
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        detectedTransactions = newTransactions,
                        showTransactionDialog = true,
                        userMessage = "Found ${newTransactions.size} transactions!"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpenseViewModel", "Error scanning SMS", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userMessage = "Error scanning SMS: ${e.message}"
                )
            }
        }
    }

    fun onTransactionSelected(transaction: com.expense.tracker.util.TransactionDetails) {
        applyTransaction(transaction)
    }
    
    fun onTransactionDialogDismiss() {
        _uiState.value = _uiState.value.copy(showTransactionDialog = false)
    }

    /**
     * Applies the selected transaction details to the UI state.
     * Implements "Vendor Learning":
     * 1. Checks if the vendor has been used before.
     * 2. If yes, auto-selects the Category, Sub-Category, and Details from the last expense with this vendor.
     * 3. If no, just fills the amount and vendor name.
     */
    private fun applyTransaction(transaction: com.expense.tracker.util.TransactionDetails) {
        viewModelScope.launch {
            val amountStr = transaction.amount.toString()
            val vendor = transaction.vendor
            val upiId = transaction.upiId ?: ""
            val paymentMethod = transaction.paymentMethod ?: ""
            val expenseTimestamp = transaction.expenseTimestamp ?: ""
            
            // Check for past history
            val pastExpense = expenseRepository.getLatestExpenseByVendor(vendor)
            
            if (pastExpense != null) {
                // Found history! Auto-fill Category/SubCategory/Details
                val category = _uiState.value.categories.find { it.name == pastExpense.category }
                _uiState.value = _uiState.value.copy(
                    amount = amountStr,
                    vendor = vendor,
                    upiId = upiId,
                    paymentMethod = paymentMethod,
                    expenseTimestamp = expenseTimestamp,
                    selectedCategory = category,
                    selectedSubCategory = pastExpense.subCategory,
                    details = pastExpense.details, 
                    isLoading = false,
                    showTransactionDialog = false
                )
            } else {
                // No history, just fill amount and vendor
                _uiState.value = _uiState.value.copy(
                    amount = amountStr,
                    vendor = vendor,
                    upiId = upiId,
                    paymentMethod = paymentMethod,
                    expenseTimestamp = expenseTimestamp,
                    isLoading = false,
                    showTransactionDialog = false
                )
            }
        }
    }

    fun toggleCategorySheet(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isCategorySheetOpen = isOpen)
    }

    fun toggleDatePicker(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isDatePickerOpen = isOpen)
    }
    
    fun onTypeChange(type: String) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun onSave() {
        viewModelScope.launch {
            val state = _uiState.value
            val amountValue = state.amount.toDoubleOrNull() ?: 0.0
            if (amountValue > 0 && state.selectedCategory != null) {
                // Convert LocalDate to Date
                // Combine selected date with current time
                val time = java.time.LocalTime.now()
                val dateTime = state.selectedDate.atTime(time)
                val date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
                
                val expense = Expense(
                    amount = amountValue,
                    category = state.selectedCategory.name,
                    subCategory = state.selectedSubCategory,
                    note = state.note,
                    details = state.details,
                    vendor = state.vendor,
                    upiId = state.upiId,
                    paymentMethod = state.paymentMethod,
                    expenseTimestamp = state.expenseTimestamp,
                    type = state.type,
                    timestamp = date
                )
                try {
                    expenseRepository.addExpense(expense)
                    android.util.Log.d("ExpenseViewModel", "Expense saved!")
                    // Reset State and show success message
                    _uiState.value = ExpenseUiState(
                        selectedDate = state.selectedDate,
                        userMessage = "Expense saved successfully!"
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ExpenseViewModel", "Failed to save expense", e)
                    _uiState.value = _uiState.value.copy(userMessage = "Failed to save expense.")
                }
            }
        }
    }
}
