package com.expense.tracker.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import com.expense.tracker.ui.components.CategorySelector
import com.expense.tracker.ui.components.CustomNumpad
import com.expense.tracker.ui.components.DateStrip

/**
 * Main Expense Entry Screen.
 * Handles user input for amount, details, category, and date.
 * Integrates SMS scanning ("Magic Wand") for automated transaction entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val detailsFocusRequester = remember { FocusRequester() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    // Listen for user messages (feedback)
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }
    
    // Launcher for requesting SMS reading permissions necessary for the "Magic Wand" feature
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.READ_SMS] == true) {
            viewModel.scanForSms(context)
        } else {
            // Provide feedback if permission denied
            // We can't easily trigger the snackbar from here without a viewmodel interaction
             android.util.Log.d("ExpenseScreen", "SMS Permission denied")
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrollable upper content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header with Centered Toggle, Right-Aligned Settings AND Magic Wand
                Spacer(modifier = Modifier.height(2.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ExpenseTypeSelector(
                        selectedType = uiState.type,
                        onTypeSelected = viewModel::onTypeChange,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        // Magic Wand removed from here

                        androidx.compose.material3.IconButton(
                            onClick = onNavigateToSettings
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Date Strip
                DateStrip(
                    selectedDate = uiState.selectedDate,
                    onDateSelected = viewModel::onDateSelect,
                    onCalendarClick = { viewModel.toggleDatePicker(true) }
                )

                Spacer(modifier = Modifier.height(1.dp))

                // 3. Amount + Backspace
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = "₹${uiState.amount}",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    androidx.compose.material3.IconButton(
                        onClick = viewModel::onBackspace
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Backspace, 
                            contentDescription = "Backspace",
                            tint = Color.Gray
                        )
                    }
                }

                // Reduced spacing for vendor
                
                // 3.5 Vendor Display
                if (uiState.vendor.isNotEmpty()) {
                    Text(
                        text = "Vendor: ${uiState.vendor}",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 0.dp, bottom = 0.dp) // Reduced bottom from 2 to 0
                            .clickable { /* TODO: Allow editing vendor */ }
                    )
                } else {
                    // No spacer needed here anymore as requested
                }

                // 4. Details Input
                androidx.compose.material3.OutlinedTextField(
                    value = uiState.details,
                    onValueChange = viewModel::onDetailsChange,
                    label = { Text("Details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp) // Gap on both sides
                        .focusRequester(detailsFocusRequester),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Category Button and Magic Wand
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Camera Button (Placeholder)
                    androidx.compose.material3.IconButton(
                        onClick = { /* TODO: Implement AI Camera */ },
                        modifier = Modifier
                            .background(Color.Black, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt,
                            contentDescription = "AI Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    ChipItem(
                        text = uiState.selectedCategory?.name ?: "Select Category",
                        icon = true,
                        onClick = { viewModel.toggleCategorySheet(true) }
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Magic Wand Moved Here
                    androidx.compose.material3.IconButton(
                        onClick = {
                            android.util.Log.d("ExpenseScreen", "Magic Wand Clicked")
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_SMS
                                )
                            )
                        },
                        modifier = Modifier
                            .background(Color.Black, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.AutoFixHigh,
                            contentDescription = "Scan SMS",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 6. Sub-Category Button (Conditionally Visible)
                if (uiState.selectedCategory != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ChipItem(
                        text = if (uiState.selectedSubCategory.isNotEmpty()) uiState.selectedSubCategory else "Select Sub-Category",
                        icon = false,
                        onClick = { viewModel.toggleCategorySheet(true) } // Re-open sheet for sub-category selection
                    )
                }
            }

            // 7. Numpad (Fixed at bottom, outside scroll)
            val isNextButton = uiState.amount.isNotEmpty() && uiState.details.isEmpty()
            CustomNumpad(
                onNumberClick = viewModel::onAmountChange,
                onDotClick = { viewModel.onAmountChange(".") },
                onDoneClick = {
                    if (isNextButton) {
                        detailsFocusRequester.requestFocus()
                    } else {
                        viewModel.onSave()
                    }
                },
                isNext = isNextButton
            )
        }

        if (uiState.isCategorySheetOpen) {
            CategorySelector(
                categories = uiState.categories,
                onCategorySelected = viewModel::onCategorySelect,
                onSubCategorySelected = viewModel::onSubCategorySelect,
                selectedCategory = uiState.selectedCategory,
                onDismiss = { viewModel.toggleCategorySheet(false) },
                sheetState = sheetState
            )
        }

        if (uiState.isDatePickerOpen) {
             com.expense.tracker.ui.components.ExpenseDatePickerDialog(
                 onDateSelected = { date ->
                     viewModel.onDateSelect(date)
                 },
                 onDismiss = { viewModel.toggleDatePicker(false) }
             )
        }
        
        if (uiState.showTransactionDialog) {
            com.expense.tracker.ui.components.SmsSelectionDialog(
                transactions = uiState.detectedTransactions,
                onTransactionSelected = viewModel::onTransactionSelected,
                onDismiss = viewModel::onTransactionDialogDismiss
            )
        }
    }
}

@Composable
fun ExpenseTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(24.dp))
            .padding(4.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp)) // Light grey background
    ) {
        TypeButton(
            text = "expenses",
            isSelected = selectedType == "Expense",
            onClick = { onTypeSelected("Expense") }
        )
        TypeButton(
            text = "Analytics",
            isSelected = selectedType == "Analytics",
            onClick = { onTypeSelected("Analytics") }
        )
    }
}

@Composable
fun TypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (isSelected) Color.White else Color.Transparent,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 24.dp, vertical = 8.dp)
            // Add shadow if selected?
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ChipItem(
    text: String,
    icon: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(Color.Black, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon) {
            Icon(
                imageVector = Icons.Default.Fastfood, // Placeholder
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
