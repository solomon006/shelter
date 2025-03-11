package com.solomon.shelter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomon.shelter.data.database.entities.CharCard

@Composable
fun CharacteristicCard(
    charCard: CharCard,
    isRevealed: Boolean,
    onReveal: () -> Unit,
    characteristicName: String = "Характеристика" // Default name if not provided
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevealed)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Characteristic name and utility index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Display characteristic type/name
                Text(
                    text = characteristicName,
                    fontWeight = FontWeight.Bold
                )

                // Utility index indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when {
                                charCard.utilityIndex >= 8 -> Color.Green
                                charCard.utilityIndex >= 5 -> Color(0xFFFFC107) // Amber
                                else -> Color(0xFFF44336) // Red
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = charCard.utilityIndex.toString(),
                        color = if (charCard.utilityIndex >= 5) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Card info
            Text(text = charCard.info)

            // Reveal button
            if (!isRevealed) {
                Button(
                    onClick = onReveal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Раскрыть")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Раскрыто",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}