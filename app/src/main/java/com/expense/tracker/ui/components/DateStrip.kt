package com.expense.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DateStrip(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate a list of dates around the selected date or today
    // For simplicity, let's show 2 days before and 2 days after the selected date, or a fixed week.
    // The prompt shows "12 Mon, 13 Tue, 14 Wed..."
    
    val dates = remember(selectedDate) {
        (-2..2).map { offset ->
            selectedDate.plusDays(offset.toLong())
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp), // Further reduced padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.weight(1f)
        ) {
            dates.forEach { date ->
                DateItem(
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) }
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))

        IconButton(onClick = onCalendarClick) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Select Date",
                tint = Color.Black
            )
        }
    }
}

@Composable
fun DateItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayFormatter = DateTimeFormatter.ofPattern("dd")
    val weekDayFormatter = DateTimeFormatter.ofPattern("EEE")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-2).dp), // Pull texts closer
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.Black else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 10.dp) // Reduced padding
    ) {
        Text(
            text = date.format(dayFormatter),
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 16.sp, // Reduced font size
            fontWeight = FontWeight.Bold
        )
        Text(
            text = date.format(weekDayFormatter).lowercase(),
            color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray,
            fontSize = 11.sp // Reduced font size
        )
    }
}
