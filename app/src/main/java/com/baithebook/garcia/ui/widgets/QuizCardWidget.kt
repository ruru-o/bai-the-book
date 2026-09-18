package com.baithebook.garcia.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baithebook.garcia.models.Question
import com.baithebook.garcia.ui.components.AppButton
import com.baithebook.garcia.ui.components.AppCard

@Composable
fun QuizCardWidget(
    question: Question,
    selectedIndex: Int?,
    isSubmitted: Boolean,
    onOptionSelected: (Int) -> Unit,
    onNextClicked: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = question.type.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        question.options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            val isCorrect = index == question.correctAnswerIndex

            val badgeBgColor by animateColorAsState(
                targetValue = when {
                    isSubmitted && isCorrect -> Color(0xFF30D158)
                    isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "BadgeBg"
            )

            val badgeTextColor by animateColorAsState(
                targetValue = when {
                    isSubmitted && (isCorrect || isSelected) -> Color.White
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                label = "BadgeText"
            )

            val rowBorderColor by animateColorAsState(
                targetValue = when {
                    isSubmitted && isCorrect -> Color(0xFF30D158).copy(alpha = 0.8f)
                    isSubmitted && isSelected && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "RowBorder"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, rowBorderColor, RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected && !isSubmitted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable(enabled = !isSubmitted) { onOptionSelected(index) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Apple Calculator circular button option badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${('A' + index)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isSubmitted) {
                    if (isCorrect) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color(0xFF30D158),
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Incorrect",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (isSubmitted) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp)
            ) {
                Text(
                    text = "Explanation",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = question.explanation.ifBlank { "No explanation provided for this question." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AppButton(
                text = "Next Question",
                onClick = onNextClicked,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true,
                enabled = true
            )
        }
    }
}

