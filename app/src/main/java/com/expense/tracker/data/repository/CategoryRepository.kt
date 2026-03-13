package com.expense.tracker.data.repository

import com.expense.tracker.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun updateCategoryOrders(categories: List<Category>)
    suspend fun deleteCategory(categoryId: String) // Actually hides/archives it
    suspend fun seedDefaultCategories()
}
