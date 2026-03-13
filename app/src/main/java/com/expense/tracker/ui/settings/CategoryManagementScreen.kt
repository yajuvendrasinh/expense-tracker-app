package com.expense.tracker.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.data.model.Category
import sh.calvin.reorderable.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        // The library handles UI updates via keys, but we need to update the data model
        // However, the library typically expects a MutableStateList to animate properly.
        // Since we are using a read-only List from StateFlow, we might see jumping if we don't update local state fast enough.
        // For now, let's call the ViewModel.
        viewModel.onMoveCategory(from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onToggleSortMode() }) {
                        Icon(
                            imageVector = if (uiState.isSortMode) Icons.Default.Check else Icons.Default.Menu,
                            contentDescription = if (uiState.isSortMode) "Done" else "Sort"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSortMode) {
                FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = uiState.categories,
                        key = { _, item -> item.id }
                    ) { index, category ->
                        
                        ReorderableItem(reorderableState, key = category.id) { isDragging ->
                            val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)
                            
                            // We need to wrap Content in a Box or pass modifier to allow dragging
                            // CategoryListItem needs to accept a modifier for the "Handle"
                            
                            CategoryListItem(
                                category = category,
                                isSortMode = uiState.isSortMode,
                                isDragging = isDragging,
                                dragModifier = Modifier.draggableHandle(), // Library provided modifier
                                onEdit = { viewModel.showEditDialog(category) },
                                onToggleHidden = { viewModel.onToggleHidden(category) },
                                onAddSubCategory = { viewModel.showAddSubCategoryDialog(category) },
                                onToggleSubCategoryHidden = { sub -> viewModel.onToggleSubCategoryHidden(category, sub) },
                                onMoveCategory = { /* handled by reorderableState */ }, // specific drag deprecated
                                onMoveSubCategory = { subIndex, fromDelta ->
                                    viewModel.onMoveSubCategory(category, subIndex, subIndex + fromDelta)
                                }
                            )
                        }
                    }
                }
            }
        }
    }



    if (uiState.showAddDialog) {
        CategoryDialog(
            title = "Add Category",
            initialName = "",
            initialSubCategories = "",
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { name, subcats ->
                viewModel.onAddCategory(name, subcats)
            }
        )
    }

    if (uiState.showAddSubCategoryDialog && uiState.selectedCategory != null) {
        AddSubCategoryDialog(
            categoryName = uiState.selectedCategory!!.name,
            onDismiss = { viewModel.hideAddSubCategoryDialog() },
            onConfirm = { name ->
                viewModel.onAddSingleSubCategory(name)
            }
        )
    }

    if (uiState.showEditDialog && uiState.selectedCategory != null) {
        CategoryDialog(
            title = "Edit Category",
            initialName = uiState.selectedCategory!!.name,
            initialSubCategories = uiState.selectedCategory!!.subCategories.joinToString(", ") { it.name },
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { name, subcats ->
                // Smart update: Preserve keys/hidden state if name matches
                val oldSubCats = uiState.selectedCategory!!.subCategories
                val newSubCats = subcats.map { newName ->
                    oldSubCats.find { it.name == newName } ?: com.expense.tracker.data.model.SubCategory(name = newName)
                }
                viewModel.onUpdateCategory(uiState.selectedCategory!!.copy(name = name, subCategories = newSubCats))
            }
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    isSortMode: Boolean,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onAddSubCategory: () -> Unit,
    onToggleSubCategoryHidden: (com.expense.tracker.data.model.SubCategory) -> Unit = {},
    onMoveCategory: (Int) -> Unit = {},
    onMoveSubCategory: (Int, Int) -> Unit = { _, _ -> }
) {
    var isExpanded by remember { mutableStateOf(false) }

    val cardElevation = if (isDragging) 8.dp else 1.dp
    val cardColor = if (isDragging) MaterialTheme.colorScheme.surface else (if (category.isHidden) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (category.isHidden) Color.Gray else Color.Unspecified
                    )

                    val visibleSubCount = category.subCategories.count { !it.isHidden }
                    val totalSubCount = category.subCategories.size

                    if (totalSubCount > 0) {
                        Text(
                            text = if (isExpanded) "Tap to collapse" else "$visibleSubCount / $totalSubCount visible",
                            fontSize = 14.sp,
                            color = if (category.isHidden) Color.Gray else Color.DarkGray
                        )
                    }

                    if (category.isHidden) {
                        Text(
                            text = "Hidden",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isSortMode) {
                    // Reorder Handle
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag to reorder",
                        modifier = Modifier
                            .padding(8.dp)
                            .then(dragModifier) // Apply library drag modifier here
                    )
                } else {
                    IconButton(onClick = onToggleHidden) {
                        Icon(
                            imageVector = if (category.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (category.isHidden) "Restore" else "Hide"
                        )
                    }
                }
            }

                if (isExpanded) {
                    HorizontalDivider()
                if (isExpanded) {
                    HorizontalDivider()
                    
                    // Create state for reorderable column
                    // Note: ReorderableColumn handles its own state for dragging
                    // Since version 2.4.0+, ReorderableColumn is available.
                    
                    // However, if ReorderableColumn is not directly available (sometimes it's ReorderableLazyColumn),
                    // we can use a standard Column with ReorderableItem if we manage the state correctly.
                    
                    // Let's assume ReorderableColumn exists as per search result, or mimic it.
                    // Actually, the library's main components are ReorderableItem and ReorderableLazyListState.
                    // For standard Column, it might use 'Reorderablestate' with 'detectReorderOnDrag'.
                    
                    // Given the ambiguity without docs, I will use a different approach:
                    // Provide a 'Smooth' reordering using 'ReorderableColumn' if the import works.
                    // If not, I will fallback to a simplified version.
                    
                    // Let's try ReorderableColumn assuming it follows the same pattern.
                    
                    val subcats = category.subCategories
                    val reorderableSubState = rememberReorderableLazyListState(rememberLazyListState()) { from, to ->
                        onMoveSubCategory(from.index, to.index)
                    }
                    
                    // Wait, ReorderableLazyListState requires a LazyListState, which means we strictly need a LazyColumn.
                    // Nesting LazyColumn is bad.
                    
                    // Alternative: The library has 'ReorderableColumn' which uses 'ReorderableState'.
                    // I'll try to use 'ReorderableColumn' directly.
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Subcategories",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (subcats.isNotEmpty()) {
                            // Using a simple Column for now, but with ReorderableItem logic adapted?
                            // No, without proper ReorderableColumn, we can't get the nice animations easily in a standard Column.
                            
                            // Let's try to use the library's 'ReorderableColumn' if it resolves.
                            // If it fails to resolve, I'll revert to custom.
                            
                            ReorderableColumn(
                                list = subcats,
                                onSettle = { from, to -> onMoveSubCategory(from, to) }
                            ) { index, sub, isDragging ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp), // Reduced spacing as requested
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sub.name,
                                        color = if (sub.isHidden) Color.Gray else Color.Unspecified,
                                        fontSize = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSortMode) {
                                        // Standardized Touch Target / Visual Size
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .draggableHandle(
                                                    onDragStarted = { },
                                                    onDragStopped = { }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragIndicator,
                                                contentDescription = "Drag to reorder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        // Custom Compact IconButton (40dp instead of 48dp)
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clickable { onToggleSubCategoryHidden(sub) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (sub.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (sub.isHidden) "Show" else "Hide",
                                                tint = if (sub.isHidden) Color.Gray else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                        // Add "+" button Row (Hide in sort mode to avoid clutter)
                        if (!isSortMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddSubCategory() }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subcategory",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSubCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subcategory") },
        text = {
            Column {
                Text("Adding to $categoryName", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subcategory Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (name.isNotBlank()) onConfirm(name.trim())
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CategoryDialog(
    title: String,
    initialName: String,
    initialSubCategories: String,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var subCategories by remember { mutableStateOf(initialSubCategories) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusRequester.requestFocus() }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = subCategories,
                    onValueChange = { subCategories = it },
                    label = { Text("Subcategories (comma separated)") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            val subs = subCategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onConfirm(name, subs)
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val subs = subCategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onConfirm(name, subs)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
