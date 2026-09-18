package com.baithebook.garcia.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.ui.components.AppButton
import com.baithebook.garcia.ui.components.SectionHeader
import com.baithebook.garcia.ui.widgets.QuickUploadWidget
import com.baithebook.garcia.ui.widgets.ReviewerCardWidget

@Composable
fun DashboardScreen(
    reviewers: List<Reviewer>,
    onReviewerSelected: (Reviewer) -> Unit,
    onViewAllClicked: () -> Unit,
    onFileSelected: (Uri) -> Unit = {},
    onNotesSubmitted: (String) -> Unit = {},
    localize: (String) -> String = { it }
) {
    val visibleState = remember { MutableTransitionState(false) }.apply { targetState = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = localize("Welcome back"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = localize("What would you like to learn today?"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            QuickUploadWidget(
                onFileSelected = onFileSelected,
                onNotesSubmitted = onNotesSubmitted
            )
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = localize("Study Modes"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyModePill(
                        title = localize("Speed Quiz"),
                        icon = Icons.Rounded.Quiz,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            reviewers.firstOrNull()?.let { onReviewerSelected(it) } ?: onViewAllClicked()
                        }
                    )
                    StudyModePill(
                        title = localize("Flashcards"),
                        icon = Icons.Rounded.Style,
                        accentColor = Color(0xFF0A84FF),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            reviewers.firstOrNull()?.let { onReviewerSelected(it) } ?: onViewAllClicked()
                        }
                    )
                    StudyModePill(
                        title = localize("AI Summary"),
                        icon = Icons.Rounded.AutoAwesome,
                        accentColor = Color(0xFFBF5AF2),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            reviewers.firstOrNull()?.let { onReviewerSelected(it) } ?: onViewAllClicked()
                        }
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = localize("Recent Reviewer"),
                actionText = localize("Browse Library"),
                onActionClick = onViewAllClicked
            )
        }

        if (reviewers.isNotEmpty()) {
            items(reviewers.take(3)) { reviewer ->
                ReviewerCardWidget(
                    reviewer = reviewer,
                    onClick = { onReviewerSelected(reviewer) }
                )
            }
        } else {
            item {
                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                ) {
                    EmptyReviewerCard(onViewAllClicked = onViewAllClicked)
                }
            }
        }

        // Bottom space so content scrolls past floating capsule dock menu bar
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
fun StudyModePill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyReviewerCard(onViewAllClicked: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No Reviewers Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Upload a document above or explore sample subjects in the library.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            AppButton(
                text = "Browse Reviewers",
                onClick = onViewAllClicked,
                isPrimary = false
            )
        }
    }
}

