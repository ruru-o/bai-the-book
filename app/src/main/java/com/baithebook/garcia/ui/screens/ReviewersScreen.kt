package com.baithebook.garcia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.models.Subject
import com.baithebook.garcia.ui.widgets.ReviewerCardWidget

@Composable
fun ReviewersScreen(
    subjects: List<Subject>,
    reviewers: List<Reviewer>,
    onReviewerSelected: (Reviewer) -> Unit,
    localize: (String) -> String = { it }
) {
    var selectedSubjectId by remember { mutableStateOf<String?>(null) } // null means "All"

    val filteredReviewers = remember(selectedSubjectId, reviewers) {
        if (selectedSubjectId == null) {
            reviewers
        } else {
            reviewers.filter { it.subjectId == selectedSubjectId }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = localize("Library"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Subject Filter Capsule Pills
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    SubjectFilterPill(
                        title = "All",
                        isSelected = selectedSubjectId == null,
                        onClick = { selectedSubjectId = null }
                    )
                }
                items(subjects) { subject ->
                    SubjectFilterPill(
                        title = subject.name.substringBefore('&').trim(),
                        isSelected = selectedSubjectId == subject.id,
                        onClick = { selectedSubjectId = subject.id }
                    )
                }
            }
        }

        if (filteredReviewers.isNotEmpty()) {
            items(filteredReviewers) { reviewer ->
                ReviewerCardWidget(
                    reviewer = reviewer,
                    onClick = { onReviewerSelected(reviewer) }
                )
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Reviewers Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No study decks for this subject yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Floating dock clearance
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun SubjectFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

