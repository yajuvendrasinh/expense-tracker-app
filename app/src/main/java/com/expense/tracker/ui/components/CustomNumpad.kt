package com.expense.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom Numeric Keypad Composable.
 * Used for entering expense amounts.
 * Includes numbers 0-9, decimal point, and a "Done" action.
 */
@Composable
fun CustomNumpad(
    onNumberClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNext: Boolean = false
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "check")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), // Restored spacing
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { item ->
                    NumpadButton(
                        text = item,
                        onClick = {
                            when (item) {
                                "." -> onDotClick()
                                "check" -> onDoneClick()
                                else -> onNumberClick(item)
                            }
                        },
                        isNext = if (item == "check") isNext else false
                    )
                }
            }
        }
    }
}

/**
 * Custom Numeric Keypad Composable.
 * Used for entering expense amounts.
 * Includes numbers 0-9, decimal point, and a "Done" action.
 */
@Composable
fun NumpadButton(
    text: String,
    onClick: () -> Unit,
    isNext: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(72.dp) // Restored size
            .clip(CircleShape)
            .background(
                if (text == "check") MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "check") {
            Icon(
                imageVector = if (isNext) Icons.Default.ArrowForward else Icons.Default.Check,
                contentDescription = if (isNext) "Next" else "Done",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 32.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
