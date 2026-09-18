package com.baithebook.garcia.ui.foundation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baithebook.garcia.services.AppScreen
import com.baithebook.garcia.services.MainViewModel
import com.baithebook.garcia.ui.components.AppButton
import com.baithebook.garcia.ui.screens.ConverterScreen
import com.baithebook.garcia.ui.screens.DashboardScreen
import com.baithebook.garcia.ui.screens.ReviewSessionScreen
import com.baithebook.garcia.ui.screens.ReviewersScreen
import com.baithebook.garcia.ui.screens.SettingsScreen
import com.baithebook.garcia.ui.theme.BaiTheBookTheme
import com.baithebook.herrera.error.ErrorUIComponent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppShell(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    BaiTheBookTheme(darkTheme = viewModel.isDarkMode) {
        val backgroundColor by animateColorAsState(
            targetValue = MaterialTheme.colorScheme.background,
            label = "BackgroundColor"
        )

        Scaffold(
            containerColor = backgroundColor,
            topBar = {
                Header(
                    currentScreen = viewModel.currentScreen,
                    isDarkMode = viewModel.isDarkMode,
                    isBisayaMode = viewModel.isBisayaMode,
                    onDarkModeToggled = { viewModel.toggleDarkMode() },
                    onBisayaToggled = { viewModel.toggleBisayaMode() },
                    localize = { viewModel.localize(it) }
                )
            },
            bottomBar = {
                BottomNav(
                    currentScreen = viewModel.currentScreen,
                    onScreenSelected = { screen -> viewModel.navigateTo(screen) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Herrera Error Banner
                    val currentErr = viewModel.currentError
                    val errorMsg = viewModel.errorMessage
                    if (!errorMsg.isNullOrBlank()) {
                        ErrorUIComponent.ErrorBanner(
                            message = errorMsg,
                            title = currentErr?.title,
                            recoveryHint = currentErr?.recoveryHint,
                            canRetry = currentErr?.canRetry == true && viewModel.lastFailedOperation != null,
                            onRetry = { viewModel.retryLastOperation() },
                            onDismiss = { viewModel.clearError() }
                        )
                    }

                    // Loading Pill Banner
                    if (viewModel.isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = viewModel.statusMessage ?: "Generating study deck...",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = viewModel.currentScreen,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + slideInHorizontally { it / 6 }) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.DASHBOARD -> {
                                    DashboardScreen(
                                        reviewers = viewModel.reviewers,
                                        onReviewerSelected = { rev -> viewModel.selectReviewer(rev) },
                                        onViewAllClicked = { viewModel.navigateTo(AppScreen.REVIEWERS) },
                                        onFileSelected = { uri -> viewModel.processUploadedFile(context, uri) },
                                        onNotesSubmitted = { notes -> viewModel.processRawNotes(notes) },
                                        localize = { viewModel.localize(it) }
                                    )
                                }
                                AppScreen.REVIEWERS -> {
                                    ReviewersScreen(
                                        subjects = viewModel.subjects,
                                        reviewers = viewModel.reviewers,
                                        onReviewerSelected = { rev -> viewModel.selectReviewer(rev) },
                                        localize = { viewModel.localize(it) }
                                    )
                                }
                                AppScreen.REVIEW_SESSION -> {
                                    val reviewer = viewModel.selectedReviewer
                                    if (reviewer != null) {
                                        ReviewSessionScreen(
                                            reviewer = reviewer,
                                            activeQuestionIndex = viewModel.activeQuizQuestionIndex,
                                            selectedAnswerIndex = viewModel.userSelectedAnswerIndex,
                                            isSubmitted = viewModel.isAnswerSubmitted,
                                            onOptionSelected = { idx -> viewModel.selectQuizOption(idx) },
                                            onNextQuestionClicked = { viewModel.nextQuizQuestion() },
                                            localize = { viewModel.localize(it) }
                                        )
                                    } else {
                                        // Minimalist Apple Calculator Empty State
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(32.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.School,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(24.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = "No Active Study Deck",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Select a deck from the library or create a new reviewer from notes.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    AppButton(
                                                        text = "Browse Library",
                                                        onClick = { viewModel.navigateTo(AppScreen.REVIEWERS) },
                                                        isPrimary = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                AppScreen.CONVERTER -> {
                                    ConverterScreen(
                                        localize = { viewModel.localize(it) }
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        isDarkMode = viewModel.isDarkMode,
                                        isBisayaMode = viewModel.isBisayaMode,
                                        onDarkModeToggled = { viewModel.toggleDarkMode() },
                                        onBisayaToggled = { viewModel.toggleBisayaMode() },
                                        localize = { viewModel.localize(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

