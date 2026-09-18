package com.baithebook.herrera.error

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Herrera's Error UI Component suite.
 *
 * Per Herrera/README.md:
 * "display user-friendly error banners and offline indicators"
 *
 * Designed with the Apple Calculator / Raena AI aesthetic:
 * - High-contrast OLED dark slate containers (#1C1C1E)
 * - Apple system error red (#FF453A) and calculator amber (#FF9F0A) accents
 * - Tactile rounded corners (20.dp) and circular action controls
 */
class ErrorUIComponent {

    // ============================================================
    // Legacy Code Lookup (Herrera/README.md)
    // ============================================================

    /**
     * Translates standard HTTP/system error codes into human-readable messages.
     */
    fun getErrorDisplayMessage(errorCode: Int): String {
        return when (errorCode) {
            400 -> "Invalid request. Please check your document format or input text."
            401, 403 -> "Authentication failed: Missing or invalid API key."
            404 -> "Service endpoint not found (404)."
            408 -> "Request timed out. Please check your connection and try again."
            413 -> "File exceeds upload limits (maximum 25MB allowed)."
            415 -> "Unsupported file type. Please upload PDF, DOCX, or PPTX."
            429 -> "Too many requests. Please wait a few moments before retrying."
            500, 502, 503, 504 -> "The remote server is currently unavailable. Please try again shortly."
            else -> "An unexpected error occurred (Code $errorCode). Please retry."
        }
    }

    companion object {

        // Apple Dark Mode Color Tokens
        private val SlateBackground = Color(0xFF1C1C1E)
        private val AppleErrorRed = Color(0xFFFF453A)
        private val AccentAmber = Color(0xFFFF9F0A)
        private val SlateBorder = Color(0xFF38383A)
        private val MutedText = Color(0xFF8E8E93)

        /**
         * Sleek Apple-style error banner displayed at the top of the content scaffold.
         */
        @Composable
        fun ErrorBanner(
            message: String,
            modifier: Modifier = Modifier,
            title: String? = null,
            recoveryHint: String? = null,
            canRetry: Boolean = false,
            onRetry: (() -> Unit)? = null,
            onDismiss: () -> Unit = {}
        ) {
            AnimatedVisibility(
                visible = message.isNotBlank(),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = SlateBackground,
                    border = BorderStroke(1.dp, AppleErrorRed.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular error badge
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AppleErrorRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = AppleErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title ?: "Attention Needed",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (recoveryHint != null) {
                            Text(
                                text = recoveryHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber
                            )
                        }

                        if (canRetry && onRetry != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentAmber,
                                    modifier = Modifier.clickable { onRetry() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Retry",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Floating offline pill indicator matching Raena AI capsule aesthetic.
         */
        @Composable
        fun OfflineIndicator(
            isOffline: Boolean,
            modifier: Modifier = Modifier,
            onRetry: (() -> Unit)? = null
        ) {
            AnimatedVisibility(
                visible = isOffline,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = SlateBackground,
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No connection. Running in offline cache mode.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        if (onRetry != null) {
                            Text(
                                text = "Reconnect",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber,
                                modifier = Modifier.clickable { onRetry() }
                            )
                        }
                    }
                }
            }
        }

        /**
         * Detailed modal error dialog for unrecoverable failures.
         */
        @Composable
        fun ErrorDialog(
            title: String,
            message: String,
            onDismiss: () -> Unit,
            debugDetails: String? = null,
            onRetry: (() -> Unit)? = null
        ) {
            Dialog(onDismissRequest = onDismiss) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SlateBackground,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AppleErrorRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = AppleErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )

                        if (!debugDetails.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black,
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = debugDetails,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedText,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Dismiss", color = MutedText)
                            }
                            if (onRetry != null) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onRetry()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                                ) {
                                    Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
