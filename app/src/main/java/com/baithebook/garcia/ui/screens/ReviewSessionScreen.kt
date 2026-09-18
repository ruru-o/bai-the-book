package com.baithebook.garcia.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.ui.components.AppButton
import com.baithebook.garcia.ui.components.EmptyState
import com.baithebook.garcia.ui.widgets.QuizCardWidget

@Composable
fun ReviewSessionScreen(
    reviewer: Reviewer,
    activeQuestionIndex: Int,
    selectedAnswerIndex: Int?,
    isSubmitted: Boolean,
    onOptionSelected: (Int) -> Unit,
    onNextQuestionClicked: () -> Unit,
    localize: (String) -> String = { it }
) {
    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Quiz, 1: Flashcards, 2: Summary
    val tabs = listOf(
        Triple("Speed Quiz", Icons.Rounded.Quiz, MaterialTheme.colorScheme.primary),
        Triple("Flashcards", Icons.Rounded.Style, Color(0xFF0A84FF)),
        Triple("AI Summary", Icons.Rounded.AutoAwesome, Color(0xFFBF5AF2))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Study Header & Raena AI Segmented Capsule Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = reviewer.topicTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2
            )

            // Raena AI Floating Capsule Mode Switcher
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, (title, icon, accent) ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = localize(title),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Study Body
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Speed Quiz Tab
                    if (reviewer.questions.isEmpty()) {
                        EmptyState(
                            title = "No Questions Available",
                            description = "This study deck does not have generated quiz questions.",
                            icon = { Icon(Icons.Rounded.Quiz, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary) }
                        )
                    } else if (activeQuestionIndex < reviewer.questions.size) {
                        val question = reviewer.questions[activeQuestionIndex]

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Progress bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Question ${activeQuestionIndex + 1} of ${reviewer.questions.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${((activeQuestionIndex.toFloat() / reviewer.questions.size) * 100).toInt()}% completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LinearProgressIndicator(
                                progress = (activeQuestionIndex + 1).toFloat() / reviewer.questions.size.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            QuizCardWidget(
                                question = question,
                                selectedIndex = selectedAnswerIndex,
                                isSubmitted = isSubmitted,
                                onOptionSelected = onOptionSelected,
                                onNextClicked = onNextQuestionClicked
                            )
                        }
                    } else {
                        // Quiz Finished Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Quiz,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Quiz Completed!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "You finished all questions in this reviewer deck.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AppButton(
                                    text = "Review AI Summary",
                                    onClick = { selectedTabIndex = 2 },
                                    modifier = Modifier.fillMaxWidth(),
                                    isPrimary = true
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Flashcards Tab
                    if (reviewer.flashcards.isEmpty()) {
                        EmptyState(
                            title = "No Flashcards",
                            description = "This study deck does not contain flashcard pairs.",
                            icon = { Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary) }
                        )
                    } else {
                        var currentCardIndex by remember { mutableStateOf(0) }
                        var isFlipped by remember { mutableStateOf(false) }
                        val flashcard = reviewer.flashcards[currentCardIndex]

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Card ${currentCardIndex + 1} of ${reviewer.flashcards.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Flip,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isFlipped) "Answer Side" else "Question Side",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Interactive Flip Card
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clickable { isFlipped = !isFlipped },
                                shape = RoundedCornerShape(24.dp),
                                color = if (isFlipped) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(28.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isFlipped) "ANSWER" else "QUESTION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = if (isFlipped) flashcard.backAnswer else flashcard.frontQuestion,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Tap card to flip",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Navigation Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppButton(
                                    text = "Previous",
                                    onClick = {
                                        if (currentCardIndex > 0) {
                                            currentCardIndex--
                                            isFlipped = false
                                        }
                                    },
                                    isPrimary = false,
                                    enabled = currentCardIndex > 0,
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = { Icon(Icons.Rounded.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                AppButton(
                                    text = "Next Card",
                                    onClick = {
                                        if (currentCardIndex < reviewer.flashcards.size - 1) {
                                            currentCardIndex++
                                            isFlipped = false
                                        }
                                    },
                                    isPrimary = true,
                                    enabled = currentCardIndex < reviewer.flashcards.size - 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // AI Summary Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "AI Overview & Key Concepts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = reviewer.summary.ifEmpty { reviewer.description },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

