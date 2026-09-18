package com.baithebook.pinca.pipeline

import com.baithebook.alvarez.docling.AlvarezDoclingService
import com.baithebook.fernandez.gemini.Difficulty
import com.baithebook.fernandez.gemini.FernandezGeminiService
import com.baithebook.fernandez.gemini.QuizType
import com.baithebook.fernandez.gemini.ReviewerFormat
import com.baithebook.garcia.models.Question
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.services.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Structured metadata extracted from raw Docling document text.
 */
data class CleanedDoclingData(
    val cleanText: String,
    val inferredTopic: String,
    val headings: List<String>,
    val wordCount: Int,
    val estimatedReadTimeMinutes: Int
)

/**
 * Data Pipeline Service for PINCA's component.
 *
 * Bridges Alvarez's Docling document text output into Karlo's FernandezGeminiService prompt input,
 * and coordinates with ResultsPresenter to produce ready-to-display domain models for Garcia's UI.
 */
class PincaDataPipelineService(
    private val geminiService: FernandezGeminiService = FernandezGeminiService(),
    private val presenter: ResultsPresenter = ResultsPresenter(),
    private val doclingService: AlvarezDoclingService = AlvarezDoclingService()
) {

    /**
     * Primary entry point preserving original method signature.
     * Processes raw Docling text, cleans it, and returns a summary report.
     */
    fun processPipelineData(rawDoclingText: String): String {
        val structured = cleanAndStructureDoclingText(rawDoclingText)
        if (structured.cleanText.isBlank()) {
            return "Pipeline Error: Input document text is empty or contains no readable text."
        }
        return "Pipeline Ready: Extracted ${structured.wordCount} words across ${structured.headings.size} sections for topic '${structured.inferredTopic}'."
    }

    /**
     * Entry point for receiving extracted text directly from Alvarez's FileUploadHandler.
     */
    fun receiveDoclingText(rawDoclingText: String): String {
        return processPipelineData(rawDoclingText)
    }

    /**
     * Sanitizes raw text output from Docling or user notes.
     * Cleans OCR artifacts, normalizes whitespace, extracts headings, and infers document topic.
     */
    fun cleanAndStructureDoclingText(rawDoclingText: String): CleanedDoclingData {
        if (rawDoclingText.isBlank()) {
            return CleanedDoclingData(
                cleanText = "",
                inferredTopic = "General Study",
                headings = emptyList(),
                wordCount = 0,
                estimatedReadTimeMinutes = 0
            )
        }

        // Normalize line breaks and remove consecutive blank lines
        val normalized = rawDoclingText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { it.trimEnd() }

        val cleanedLines = mutableListOf<String>()
        val headings = mutableListOf<String>()
        var inferredTopic = ""
        var consecutiveEmptyLines = 0

        for (line in normalized) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                consecutiveEmptyLines++
                if (consecutiveEmptyLines <= 1) {
                    cleanedLines.add("")
                }
                continue
            }
            consecutiveEmptyLines = 0

            // Detect headings (# Header, ## Subheader, or capitalized section headers)
            if (trimmed.startsWith("#")) {
                val headingTitle = trimmed.replace(Regex("""^#+\s*"""), "").trim()
                if (headingTitle.isNotBlank()) {
                    headings.add(headingTitle)
                    if (inferredTopic.isBlank()) {
                        inferredTopic = headingTitle
                    }
                }
            } else if (inferredTopic.isBlank() && trimmed.length in 3..60 && !trimmed.endsWith(".")) {
                inferredTopic = trimmed
            }

            // Remove Docling table divider artifacts (e.g. |---|---|)
            if (trimmed.matches(Regex("""^\|?[\s:\-|]+\|?$"""))) {
                continue
            }

            cleanedLines.add(line)
        }

        val cleanText = cleanedLines.joinToString("\n").trim()
        val words = cleanText.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val wordCount = words.size
        val readTimeMinutes = kotlin.math.max(1, (wordCount / 200))

        return CleanedDoclingData(
            cleanText = cleanText,
            inferredTopic = inferredTopic.ifBlank { "General Study" },
            headings = headings,
            wordCount = wordCount,
            estimatedReadTimeMinutes = readTimeMinutes
        )
    }

    /**
     * Bridges Alvarez's updated extractTextFromFile into PINCA's cleaning pipeline.
     */
    suspend fun processDoclingFile(file: File): CleanedDoclingData {
        val extractedText = doclingService.extractTextFromFile(file)
        return cleanAndStructureDoclingText(extractedText)
    }

    /**
     * Bridges Docling text directly to Karlo's FernandezGeminiService to generate
     * interactive quiz questions.
     */
    suspend fun executeQuizPipeline(
        rawDoclingText: String,
        topic: String = "",
        questionCount: Int = 5,
        quizType: QuizType = QuizType.MULTIPLE_CHOICE,
        difficulty: Difficulty = Difficulty.MEDIUM
    ): List<Question> = withContext(Dispatchers.IO) {
        val structured = cleanAndStructureDoclingText(rawDoclingText)
        if (structured.cleanText.isBlank()) return@withContext emptyList()

        val activeTopic = topic.ifBlank { structured.inferredTopic }

        // Karlo's FernandezGeminiService call
        val rawGeminiQuizJson = geminiService.generateQuizQuestions(
            extractedText = structured.cleanText,
            topic = activeTopic,
            questionCount = questionCount,
            questionType = quizType,
            difficulty = difficulty,
            asJson = true
        )

        // PINCA's ResultsPresenter transforms payload into Garcia's domain models
        presenter.parseGeminiQuizJson(rawGeminiQuizJson)
    }

    /**
     * Complete end-to-end pipeline:
     * 1. Sanitizes Docling text
     * 2. Generates comprehensive study guide via Karlo's Gemini API
     * 3. Generates quiz questions via Karlo's Gemini API
     * 4. Assembles full Reviewer model for Garcia's UI
     */
    suspend fun executeReviewerPipeline(
        rawDoclingText: String,
        topic: String = "",
        subjectId: String = "subj_cs",
        format: ReviewerFormat = ReviewerFormat.COMPREHENSIVE,
        questionCount: Int = 5
    ): Reviewer = withContext(Dispatchers.IO) {
        val structured = cleanAndStructureDoclingText(rawDoclingText)
        val activeTopic = topic.ifBlank { structured.inferredTopic }

        // Request study reviewer from Karlo's service
        val reviewerMarkdown = geminiService.generateReviewer(
            extractedText = structured.cleanText,
            topic = activeTopic,
            format = format
        )

        // Request quiz questions from Karlo's service
        val quizJson = geminiService.generateQuizQuestions(
            extractedText = structured.cleanText,
            topic = activeTopic,
            questionCount = questionCount,
            questionType = QuizType.MIXED,
            difficulty = Difficulty.MEDIUM,
            asJson = true
        )

        // Parse and assemble into Garcia's Reviewer model
        presenter.assembleReviewer(
            topicTitle = activeTopic,
            subjectId = subjectId,
            summaryText = reviewerMarkdown,
            quizJson = quizJson,
            flashcardsSourceText = reviewerMarkdown
        )
    }

    /**
     * End-to-end convenience method: Takes a document File from Alvarez's upload flow,
     * extracts text via Alvarez's Docling service, and generates a full Reviewer deck.
     */
    suspend fun generateFromDoclingFile(
        file: File,
        topic: String = "",
        subjectId: String = "subj_cs"
    ): Reviewer {
        val extractedText = doclingService.extractTextFromFile(file)
        return executeReviewerPipeline(extractedText, topic, subjectId)
    }

    /**
     * Synchronous / blocking wrapper for [executeReviewerPipeline].
     */
    fun executeReviewerPipelineBlocking(
        rawDoclingText: String,
        topic: String = "",
        subjectId: String = "subj_cs",
        format: ReviewerFormat = ReviewerFormat.COMPREHENSIVE,
        questionCount: Int = 5
    ): Reviewer = runBlocking {
        executeReviewerPipeline(rawDoclingText, topic, subjectId, format, questionCount)
    }

    /**
     * Directly delivers a generated [Reviewer] into Garcia's [MainViewModel],
     * selecting it for active study/quiz session.
     */
    fun passToViewModel(reviewer: Reviewer, viewModel: MainViewModel) {
        viewModel.selectReviewer(reviewer)
    }
}

/**
 * Shared data flow entry point for Alvarez's FileUploadHandler.
 * Allows FileUploadHandler to hand off extracted text directly to PINCA's pipeline.
 */
object PincasDataFlow {
    private val pipelineService = PincaDataPipelineService()
    var lastExtractedText: String = ""
        private set

    fun receiveDoclingText(text: String): String {
        lastExtractedText = text
        return pipelineService.receiveDoclingText(text)
    }
}

