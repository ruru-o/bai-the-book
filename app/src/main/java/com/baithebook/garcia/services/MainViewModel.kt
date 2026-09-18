package com.baithebook.garcia.services

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.baithebook.alvarez.docling.FileUploadHandler
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.models.Subject
import com.baithebook.herrera.error.HerreraErrorHandler
import com.baithebook.pimentel.bisaya.BisayaUIOverlay
import com.baithebook.pimentel.bisaya.PimentelBisayaService
import com.baithebook.pinca.pipeline.PincaDataPipelineService
import com.baithebook.rubio.resilience.NetworkResilience
import com.baithebook.rubio.resilience.RubioInputValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class AppScreen {
    DASHBOARD,
    REVIEWERS,
    REVIEW_SESSION,
    CONVERTER,
    SETTINGS
}

class MainViewModel(
    private val repository: ReviewerRepository = MockReviewerRepository(),
    private val pipelineService: PincaDataPipelineService = PincaDataPipelineService(),
    private val inputValidator: RubioInputValidator = RubioInputValidator(),
    private val networkResilience: NetworkResilience = NetworkResilience(),
    private val errorHandler: HerreraErrorHandler = HerreraErrorHandler(),
    val bisayaService: PimentelBisayaService = PimentelBisayaService(),
    val bisayaOverlay: BisayaUIOverlay = BisayaUIOverlay()
) {
    var currentScreen by mutableStateOf(AppScreen.DASHBOARD)
    var isDarkMode by mutableStateOf(true) // Default to Apple Calculator OLED dark mode
    var isBisayaMode by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
    var statusMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var currentError by mutableStateOf<HerreraErrorHandler.AppError?>(null)
    var lastFailedOperation by mutableStateOf<(() -> Unit)?>(null)

    val subjects: List<Subject> = repository.getSubjects()
    val reviewers = mutableStateListOf<Reviewer>().apply {
        addAll(repository.getReviewers())
    }

    var selectedReviewer by mutableStateOf<Reviewer?>(null)
    var activeQuizQuestionIndex by mutableStateOf(0)
    var userSelectedAnswerIndex by mutableStateOf<Int?>(null)
    var isAnswerSubmitted by mutableStateOf(false)

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }

    fun selectReviewer(reviewer: Reviewer) {
        selectedReviewer = reviewer
        activeQuizQuestionIndex = 0
        userSelectedAnswerIndex = null
        isAnswerSubmitted = false
        currentScreen = AppScreen.REVIEW_SESSION
    }

    fun selectQuizOption(index: Int) {
        userSelectedAnswerIndex = index
        isAnswerSubmitted = true
    }

    fun nextQuizQuestion() {
        selectedReviewer?.let { rev ->
            if (activeQuizQuestionIndex < rev.questions.size - 1) {
                activeQuizQuestionIndex++
                userSelectedAnswerIndex = null
                isAnswerSubmitted = false
            }
        }
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    fun toggleBisayaMode() {
        isBisayaMode = !isBisayaMode
    }

    fun setError(appError: HerreraErrorHandler.AppError) {
        currentError = appError
        errorMessage = appError.userMessage
    }

    fun clearError() {
        currentError = null
        errorMessage = null
    }

    fun retryLastOperation() {
        val op = lastFailedOperation
        clearError()
        op?.invoke()
    }

    fun clearStatus() {
        statusMessage = null
    }

    /**
     * Translates or adapts a UI string when Bisaya Mode is enabled.
     */
    fun localize(text: String): String {
        return if (isBisayaMode) {
            when (text) {
                "Bai The Book" -> "Bai The Book (Bisaya)"
                "Welcome back" -> "Maayong pagbalik"
                "What would you like to learn today?" -> "Unsay gusto nimong tun-an karon?"
                "Quick Upload" -> "Paspas nga Pag-upload"
                "Turn Notes into Reviewer" -> "Himoa ang mga Notang Reviewer"
                "Study Modes" -> "Mga Paagi sa Pagtuon"
                "Recent Reviewer" -> "Bag-ong Reviewer"
                "Library" -> "Bibliyoteka"
                "Browse Library" -> "Tan-awa ang Bibliyoteka"
                "Flashcards" -> "Mga Flashcard"
                "Speed Quiz" -> "Paspas nga Pagsulay"
                "AI Summary" -> "Katingbanan sa AI"
                "Generate Reviewer" -> "Paghimo og Reviewer"
                "File Converter" -> "Tig-usab sa File"
                "Settings" -> "Mga Setting"
                "Dark Mode" -> "Mangitngit nga Mode"
                "Bisaya Mode" -> "Bisaya nga Mode"
                else -> text
            }
        } else {
            text
        }
    }

    /**
     * End-to-end file upload and reviewer generation using Rubio validation,
     * Alvarez Docling processing, and Pinca AI pipeline.
     */
    fun processUploadedFile(context: Context, uri: Uri?, topicName: String = "") {
        // 1. Validate file with Rubio's validator
        val validationResult = inputValidator.validateFile(uri, context)
        if (validationResult is RubioInputValidator.ValidationResult.Invalid) {
            val appError = errorHandler.handleValidationError(validationResult.error)
            setError(appError)
            return
        }

        val validUri = (validationResult as RubioInputValidator.ValidationResult.Valid).value

        isLoading = true
        statusMessage = "Processing document with Alvarez Docling..."
        clearError()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Copy uri to temp file for Docling processing
                val inputStream = context.contentResolver.openInputStream(validUri)
                val tempFile = File.createTempFile("deck_upload", ".tmp", context.cacheDir)
                inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }

                withContext(Dispatchers.Main) {
                    statusMessage = "Synthesizing reviewer with Fernandez & Pinca..."
                }

                // Generate reviewer via Pinca pipeline
                val activeTopic = topicName.ifBlank { "Study Guide" }
                val newReviewer = pipelineService.generateFromDoclingFile(tempFile, activeTopic)

                repository.addReviewer(newReviewer)

                withContext(Dispatchers.Main) {
                    reviewers.add(0, newReviewer)
                    isLoading = false
                    statusMessage = "Reviewer ready: ${newReviewer.topicTitle}"
                    selectReviewer(newReviewer)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    lastFailedOperation = { processUploadedFile(context, uri, topicName) }
                    val appError = errorHandler.handleException(e)
                    setError(appError)
                }
            }
        }
    }

    /**
     * Process raw text/notes using Rubio sanitization and Pinca pipeline.
     */
    fun processRawNotes(notesText: String, topicName: String = "") {
        val validation = inputValidator.validateNotes(notesText)
        if (validation is RubioInputValidator.ValidationResult.Invalid) {
            val appError = errorHandler.handleValidationError(validation.error)
            setError(appError)
            return
        }

        val sanitized = (validation as RubioInputValidator.ValidationResult.Valid).value
        isLoading = true
        statusMessage = "Synthesizing notes with AI..."
        clearError()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newReviewer = pipelineService.executeReviewerPipeline(
                    rawDoclingText = sanitized,
                    topic = topicName.ifBlank { "Notes Summary" }
                )

                repository.addReviewer(newReviewer)

                withContext(Dispatchers.Main) {
                    reviewers.add(0, newReviewer)
                    isLoading = false
                    statusMessage = "Reviewer ready: ${newReviewer.topicTitle}"
                    selectReviewer(newReviewer)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    lastFailedOperation = { processRawNotes(notesText, topicName) }
                    val appError = errorHandler.handleException(e)
                    setError(appError)
                }
            }
        }
    }
}

