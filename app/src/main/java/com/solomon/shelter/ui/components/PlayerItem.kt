package com.solomon.shelter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solomon.shelter.data.database.entities.CharCard
import com.solomon.shelter.data.database.entities.Player

@Composable
fun PlayerItem(
    player: Player,
    isCurrentUser: Boolean = false,
    isHost: Boolean = false,
    canKick: Boolean = false,
    revealedCharCards: List<CharCard> = emptyList(),
    isVotingPhase: Boolean = false,
    voteCount: Int = 0,
    isSelected: Boolean = false,
    onPlayerClick: (() -> Unit)? = null,
    onKick: (() -> Unit)? = null
) {
    val isClickable = onPlayerClick != null && !isCurrentUser && isVotingPhase

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = isClickable) { onPlayerClick?.invoke() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isCurrentUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Order number indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (player.orderNumber != -1)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    Color.Gray
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (player.orderNumber != -1)
                                player.orderNumber.toString()
                            else
                                "?",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )

                            if (isHost) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Host",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (isCurrentUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(вы)",
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else
                                        Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (player.orderNumber == -1) {
                            Text(
                                text = "Выбирает номер...",
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else
                                    Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Vote count if in voting phase
                    if (isVotingPhase && voteCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.error,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$voteCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (canKick && onKick != null) {
                        IconButton(
                            onClick = onKick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kick player",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            // Revealed characteristics
            if (revealedCharCards.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Column {
                    Text(
                        text = "Раскрытые характеристики:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    revealedCharCards.forEach { card ->
                        Text(
                            text = "• ${card.info}",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}