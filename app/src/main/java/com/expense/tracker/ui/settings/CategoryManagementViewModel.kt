package com.expense.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expense.tracker.data.model.Category
import com.expense.tracker.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showAddSubCategoryDialog: Boolean = false,
    val isSortMode: Boolean = false, // Sort mode
    val selectedCategory: Category? = null // For editing
)

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    private var lastWriteTimestamp: Long = 0
    private val SUPPRESSION_DELAY = 1000L // 1 second

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getCategories().collect { categories ->
                // Fix for "Flashback": Ignore updates from repo immediately after a local write
                // to prevent stale data from overwriting our optimistic state.
                if (System.currentTimeMillis() - lastWriteTimestamp < SUPPRESSION_DELAY) {
                    return@collect
                }

                // If in sort mode, we might want to ignore external updates to avoid jumping
                // But for now let's just accept them. The repo already sorts by order.
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    isLoading = false
                )
            }
        }
        // Ensure default categories exist on first load
        viewModelScope.launch {
            repository.seedDefaultCategories()
        }
    }
    
    fun onToggleSortMode() {
        if (_uiState.value.isSortMode) {
            // Saving changes when exiting sort mode
            saveOrder()
        }
        _uiState.value = _uiState.value.copy(isSortMode = !_uiState.value.isSortMode)
    }
    
    fun onMoveCategory(from: Int, to: Int) {
        val currentList = _uiState.value.categories.toMutableList()
        if (from in currentList.indices && to in currentList.indices) {
            val item = currentList.removeAt(from)
            currentList.add(to, item)
            // Update order fields locally
            val updatedList = currentList.mapIndexed { index, category -> 
                category.copy(order = index) 
            }
            _uiState.value = _uiState.value.copy(categories = updatedList)
        }
    }
    
    fun onMoveSubCategory(category: Category, from: Int, to: Int) {
        val subCats = category.subCategories.toMutableList()
        if (from in subCats.indices && to in subCats.indices) {
            val item = subCats.removeAt(from)
            subCats.add(to, item)
            // Subcategories don't have explicit order field, order is array index.
            // updateCategory will save the new array order.
            
            val updatedCategory = category.copy(subCategories = subCats)
             
            // Update the category in the main list
            val updatedList = _uiState.value.categories.map {
                if (it.id == category.id) updatedCategory else it
            }
            _uiState.value = _uiState.value.copy(categories = updatedList)
            
            // We should probably save immediately or mark as dirty?
            // User requirement says "when clicked... 6 dots appear... to drag...". 
            // Often save happens on "Done".
            // Since subcategory reordering inside a category might be frequent, let's defer save to "Done" or explicit save?
            // Actually, for subcategories, let's just optimistically update UI. 
            // Real save happens when `saveOrder` is called on exit sort mode, OR we can trigger a background save.
            // Let's trigger a background save for subcategory changes immediately to avoid complex dirty state tracking for nested items,
            // or just rely on the final "Done" button if we implementing a global "Sort Mode".
            // If "Sort Mode" is global, we should probably wait. 
            // But let's verify if `saveOrder` handles subcategories too.
            // `saveOrder` calls `updateCategoryOrders` which updates `Category.order`.
            // It DOES NOT update subcategory lists. 
            // So we need to save the category specifically if its subcats changed.
            // Let's just update the category immediately in persistence for subcats, 
            // similar to how we handle hidden toggles.
             viewModelScope.launch {
                repository.updateCategory(updatedCategory)
            }
        }
    }
    
    private fun saveOrder() {
        val categories = _uiState.value.categories
        lastWriteTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateCategoryOrders(categories)
        }
    }

    fun onAddCategory(name: String, subCategories: List<String>) {
        viewModelScope.launch {
            // New categories go to the end
            val maxOrder = _uiState.value.categories.maxOfOrNull { it.order } ?: 0
            repository.addCategory(Category(
                name = name, 
                subCategories = subCategories.map { com.expense.tracker.data.model.SubCategory(name = it) },
                order = maxOrder + 1
            ))
            _uiState.value = _uiState.value.copy(showAddDialog = false)
        }
    }

    fun onUpdateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _uiState.value = _uiState.value.copy(showEditDialog = false, selectedCategory = null)
        }
    }
    
    fun onToggleHidden(category: Category) {
        lastWriteTimestamp = System.currentTimeMillis()
        
        // Optimistic update
        val updatedList = _uiState.value.categories.map {
            if (it.id == category.id) it.copy(isHidden = !it.isHidden) else it
        }
        _uiState.value = _uiState.value.copy(categories = updatedList)

        viewModelScope.launch {
            val updated = category.copy(isHidden = !category.isHidden)
            repository.updateCategory(updated)
        }
    }

    fun onToggleSubCategoryHidden(category: Category, subCategory: com.expense.tracker.data.model.SubCategory) {
        android.util.Log.d("CategoryViewModel", "Toggling subcategory: ${subCategory.name} in ${category.name}")
        lastWriteTimestamp = System.currentTimeMillis()

        // Optimistic update
        val updatedSubCats = category.subCategories.map {
            if (it.name == subCategory.name) it.copy(isHidden = !it.isHidden) else it
        }
        val updatedCategory = category.copy(subCategories = updatedSubCats)
        
        val updatedList = _uiState.value.categories.map {
            if (it.id == category.id) updatedCategory else it
        }
        _uiState.value = _uiState.value.copy(categories = updatedList)

        viewModelScope.launch {
            repository.updateCategory(updatedCategory)
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }
    
    fun showEditDialog(category: Category) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, selectedCategory = category)
    }
    
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, selectedCategory = null)
    }
    
    fun showAddSubCategoryDialog(category: Category) {
        _uiState.value = _uiState.value.copy(showAddSubCategoryDialog = true, selectedCategory = category)
    }
    
    fun hideAddSubCategoryDialog() {
        _uiState.value = _uiState.value.copy(showAddSubCategoryDialog = false, selectedCategory = null)
    }
    
    fun onAddSingleSubCategory(name: String) {
        val category = _uiState.value.selectedCategory ?: return
        if (name.isBlank()) return
        
        // Check if exists
        if (category.subCategories.any { it.name.equals(name, ignoreCase = true) }) {
             // Handle duplicate? For now, just ignore or maybe close dialog
             _uiState.value = _uiState.value.copy(showAddSubCategoryDialog = false, selectedCategory = null)
             return
        }

        val newSub = com.expense.tracker.data.model.SubCategory(name = name)
        val updatedSubCats = category.subCategories + newSub
        val updatedCategory = category.copy(subCategories = updatedSubCats)
        
        onUpdateCategory(updatedCategory) // Reuses existing update logic, which handles UI update & repo call
        
        _uiState.value = _uiState.value.copy(showAddSubCategoryDialog = false, selectedCategory = null)
    }
}
