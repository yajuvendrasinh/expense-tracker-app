package com.expense.tracker.data.repository

import com.expense.tracker.data.model.Category
import com.expense.tracker.data.model.CategoryData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {

    private val collection = firestore.collection("categories")

    override fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.id
                        val name = doc.getString("name") ?: ""
                        val isHidden = doc.getBoolean("isHidden") ?: false
                        val order = doc.getLong("order")?.toInt() ?: 0 // Default to 0
                        
                        // Handle migration for subCategories
                        val rawSubCats = doc.get("subCategories")
                        val subCategories = when (rawSubCats) {
                            is List<*> -> {
                                rawSubCats.mapNotNull { item ->
                                    when (item) {
                                        is String -> com.expense.tracker.data.model.SubCategory(name = item) // Legacy string
                                        is Map<*, *> -> {
                                            val subName = item["name"] as? String ?: ""
                                            val subHidden = item["isHidden"] as? Boolean ?: false
                                            com.expense.tracker.data.model.SubCategory(name = subName, isHidden = subHidden)
                                        }
                                        else -> null
                                    }
                                }
                            }
                            else -> emptyList()
                        }
                        
                        Category(id = id, name = name, subCategories = subCategories, isHidden = isHidden, order = order)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()
                
                // Sort by order, then by name for stable default
                val sortedCategories = categories.sortedWith(compareBy({ it.order }, { it.name }))
                
                trySend(sortedCategories)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addCategory(category: Category) {
        val docRef = collection.document()
        val newCategory = category.copy(id = docRef.id)
        docRef.set(newCategory).await()
    }

    override suspend fun updateCategory(category: Category) {
        try {
            android.util.Log.d("CategoryRepo", "Updating category: ${category.name}, Subs: ${category.subCategories.size}")
            collection.document(category.id).set(category).await()
            android.util.Log.d("CategoryRepo", "Update success")
        } catch (e: Exception) {
            android.util.Log.e("CategoryRepo", "Update failed", e)
            throw e
        }
    }

    override suspend fun updateCategoryOrders(categories: List<Category>) {
        try {
            val batch = firestore.batch()
            categories.forEach { category ->
                val docRef = collection.document(category.id)
                batch.update(docRef, "order", category.order)
            }
            batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e("CategoryRepo", "Batch update failed", e)
            throw e
        }
    }

    override suspend fun deleteCategory(categoryId: String) {
        collection.document(categoryId).delete().await()
    }

    override suspend fun seedDefaultCategories() {
        val snapshot = collection.limit(1).get().await()
        if (snapshot.isEmpty) {
            CategoryData.categories.forEachIndexed { index, category ->
                addCategory(category.copy(order = index))
            }
        }
    }
}
