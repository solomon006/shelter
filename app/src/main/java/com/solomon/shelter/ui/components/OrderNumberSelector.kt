package com.solomon.shelter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrderNumberSelector(
    maxPlayers: Int,
    takenNumbers: List<Int>,
    selectedNumber: Int,
    onNumberSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "Выберите порядковый номер",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items((1..maxPlayers).toList()) { number ->
                OrderNumberButton(
                    number = number,
                    isSelected = number == selectedNumber,
                    isAvailable = number !in takenNumbers || number == selectedNumber,
                    onNumberSelected = onNumberSelected
                )
            }
        }
    }
}

@Composable
fun OrderNumberButton(
    number: Int,
    isSelected: Boolean,
    isAvailable: Boolean,
    onNumberSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    !isAvailable -> Color.Gray.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                enabled = isAvailable,
                onClick = { onNumberSelected(number) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                !isAvailable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}