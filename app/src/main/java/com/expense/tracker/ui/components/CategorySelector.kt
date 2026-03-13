package com.expense.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.expense.tracker.data.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    categories: List<Category>,
    onCategorySelected: (Category) -> Unit,
    onSubCategorySelected: (String) -> Unit,
    selectedCategory: Category?,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    // Local state to track if we are viewing subcategories or the main list
    // Initialize based on whether we have a selected category passed in, 
    // BUT we want to allow going back. 
    // Actually, the issue is that the parent passes `selectedCategory`, so it always thinks we are in sub-category mode.
    // We need a local state to override this, OR the parent needs to handle "clearing" the selection.
    
    // Better approach: Use a local state for the *current view*, defaulting to Categories if we want to change it.
    // However, if we just want to change the category, we should probably start at the Category list.
    // But the user might want to just change the sub-category of the *current* category.
    
    // Let's implement a Back button.
    
    // We need to know if we are currently *displaying* subcategories.
    // If selectedCategory is NOT null, we default to showing its subcategories.
    // But we need a way to say "Go back to main list".
    
    var isShowingSubCategories by androidx.compose.runtime.remember(selectedCategory) {
        androidx.compose.runtime.mutableStateOf(selectedCategory != null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (isShowingSubCategories) {
                    androidx.compose.material3.IconButton(onClick = { isShowingSubCategories = false }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
                
                Text(
                    text = if (isShowingSubCategories) selectedCategory?.name ?: "Select Subcategory" else "Select Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = if (isShowingSubCategories) 0.dp else 8.dp)
                )
            }
            
            LazyColumn {
                if (!isShowingSubCategories) {
                    items(categories) { category ->
                        CategoryItem(
                            text = category.name,
                            onClick = { 
                                onCategorySelected(category)
                                isShowingSubCategories = true
                            }
                        )
                    }
                } else {
                    // Safety check: if selectedCategory is null but isShowingSubCategories is true (shouldn't happen logic-wise but safe to fallback)
                    val subs = selectedCategory?.subCategories?.filter { !it.isHidden } ?: emptyList()
                    items(subs) { subCategory ->
                        CategoryItem(
                            text = subCategory.name,
                            onClick = { onSubCategorySelected(subCategory.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = Color.Gray
        )
    }
    Divider(color = Color.LightGray.copy(alpha = 0.5f))
}
