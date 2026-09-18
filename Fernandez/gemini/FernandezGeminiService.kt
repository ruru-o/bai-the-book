package com.baithebook.fernandez.gemini

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Service to interface with Google Gemini API for:
 * 1. Generating academic reviewers and study guides.
 * 2. Generating quizzes and practice questions.
 * 3. Reviewing and evaluating student answers.
 *
 * Supports both the official Google AI Client SDK ([GenerativeModel])
 * and direct HTTPS REST API fallback.
 */
class FernandezGeminiService(
    apiKey: String = "",
    private val modelName: String = DEFAULT_MODEL,
    private val promptGenerator: PromptGenerator = PromptGenerator()
) {
    /**
     * Active API key resolved from explicit argument, GeminiConfig, local.properties,
     * or environment variables.
     */
    val apiKey: String = apiKey.ifBlank { GeminiConfig.getApiKey() }

    /**
     * Checks whether an authentic, non-placeholder API key is currently configured.
     */
    val isConfigured: Boolean
        get() = GeminiConfig.isConfigured() || (apiKey.isNotBlank() && apiKey != GeminiConfig.DEFAULT_API_KEY)

    companion object {
        const val DEFAULT_MODEL = "gemini-1.5-flash"
        private const val GEMINI_REST_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    /**
     * Lazily initialized GenerativeModel instance from Google AI Client SDK.
     */
    private val generativeModel: GenerativeModel by lazy {
        val config = generationConfig {
            temperature = 0.7f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
        )

        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings
        )
    }

    // =========================================================================
    // 1. QUESTION GENERATION
    // =========================================================================

    /**
     * Backward-compatible synchronous quiz generation method.
     * Generates a 5-question multiple choice quiz from the given text.
     *
     * @param extractedText Source text extracted from notes or documents.
     * @return Generated quiz string.
     */
    fun generateQuizQuestions(extractedText: String): String {
        return runBlocking {
            generateQuizQuestions(
                extractedText = extractedText,
                topic = "",
                questionCount = 5,
                questionType = QuizType.MULTIPLE_CHOICE,
                difficulty = Difficulty.MEDIUM,
                asJson = false
            )
        }
    }

    /**
     * Coroutine-based quiz generator with customizable parameters.
     *
     * @param extractedText The study material or lecture notes.
     * @param topic Optional subject or chapter title.
     * @param questionCount Number of questions to generate (default 5).
     * @param questionType Type of questions (Multiple Choice, True/False, Identification, Mixed).
     * @param difficulty Difficulty level (Easy, Medium, Hard).
     * @param asJson Whether to request raw JSON output.
     * @return Formatted quiz questions or JSON string.
     */
    suspend fun generateQuizQuestions(
        extractedText: String,
        topic: String = "",
        questionCount: Int = 5,
        questionType: QuizType = QuizType.MULTIPLE_CHOICE,
        difficulty: Difficulty = Difficulty.MEDIUM,
        asJson: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        if (extractedText.isBlank() && topic.isBlank()) {
            return@withContext "Error: Please provide extracted text or a topic to generate questions."
        }

        val prompt = promptGenerator.buildQuizPrompt(
            topic = topic,
            textContent = extractedText,
            questionCount = questionCount,
            questionType = questionType,
            difficulty = difficulty,
            asJson = asJson
        )

        generateContentInternal(prompt)
    }

    // =========================================================================
    // 2. REVIEWER / STUDY GUIDE GENERATION
    // =========================================================================

    /**
     * Generates a comprehensive study reviewer / revision notes from extracted text.
     *
     * @param extractedText Lecture notes, textbook chapter, or source text.
     * @param topic Topic or subject header.
     * @param format Reviewer style (Comprehensive, Bulleted Summary, Flashcard style, Glossary).
     * @return Structured study reviewer in Markdown.
     */
    suspend fun generateReviewer(
        extractedText: String,
        topic: String = "",
        format: ReviewerFormat = ReviewerFormat.COMPREHENSIVE
    ): String = withContext(Dispatchers.IO) {
        if (extractedText.isBlank() && topic.isBlank()) {
            return@withContext "Error: Please provide extracted text or a topic to generate a reviewer."
        }

        val prompt = promptGenerator.buildReviewerPrompt(
            topic = topic,
            textContent = extractedText,
            format = format
        )

        generateContentInternal(prompt)
    }

    /**
     * Synchronous wrapper for [generateReviewer].
     */
    fun generateReviewerBlocking(
        extractedText: String,
        topic: String = "",
        format: ReviewerFormat = ReviewerFormat.COMPREHENSIVE
    ): String = runBlocking {
        generateReviewer(extractedText, topic, format)
    }

    // =========================================================================
    // 3. ANSWER EVALUATION / REVIEW
    // =========================================================================

    /**
     * Reviews and evaluates a student's answer to a review/quiz question.
     *
     * @param question The question asked.
     * @param studentAnswer The answer submitted by the student.
     * @param contextText Source text reference (optional).
     * @param correctAnswer Expected/correct answer reference (optional).
     * @return Constructive feedback, evaluation score, and model answer.
     */
    suspend fun reviewStudentAnswer(
        question: String,
        studentAnswer: String,
        contextText: String = "",
        correctAnswer: String = ""
    ): String = withContext(Dispatchers.IO) {
        if (question.isBlank() || studentAnswer.isBlank()) {
            return@withContext "Error: Question and student answer cannot be blank."
        }

        val prompt = promptGenerator.buildAnswerReviewPrompt(
            question = question,
            studentAnswer = studentAnswer,
            contextText = contextText,
            correctAnswer = correctAnswer
        )

        generateContentInternal(prompt)
    }

    // =========================================================================
    // INTERNAL GENERATION ENGINE (SDK with REST Fallback)
    // =========================================================================

    /**
     * Dispatches the prompt to Gemini via Google AI SDK if available,
     * or seamlessly falls back to standard HTTPS REST call.
     */
    private suspend fun generateContentInternal(prompt: String): String {
        if (apiKey.isBlank() || apiKey == GeminiConfig.DEFAULT_API_KEY) {
            return "Error: Gemini API Key is missing or not configured. Please add your Gemini API key in local.properties (GEMINI_API_KEY=your_key) or in Fernandez/gemini/GeminiConfig.kt."
        }

        return try {
            // Attempt generation via official Google AI GenerativeModel SDK
            val response = generativeModel.generateContent(prompt)
            response.text ?: "Error: Gemini returned an empty response."
        } catch (sdkError: NoClassDefFoundError) {
            // If the Android SDK dependency isn't in classpath, use direct REST API
            callGeminiRestApi(prompt)
        } catch (classNotFound: ClassNotFoundException) {
            callGeminiRestApi(prompt)
        } catch (e: Exception) {
            // If an SDK exception occurs (e.g. network/config), fall back or return error message
            try {
                callGeminiRestApi(prompt)
            } catch (fallbackError: Exception) {
                "Error communicating with Gemini API: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    /**
     * Direct HTTPS REST API call to Gemini endpoint.
     * Allows the service to function even without external Android SDK dependencies.
     */
    private fun callGeminiRestApi(prompt: String): String {
        val endpointUrl = "$GEMINI_REST_ENDPOINT/$modelName:generateContent?key=$apiKey"
        val url = URL(endpointUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 30000
            readTimeout = 60000
            doOutput = true
        }

        // Sanitize prompt for JSON payload
        val escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val jsonPayload = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "$escapedPrompt"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(jsonPayload)
            writer.flush()
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }

        return if (responseCode in 200..299) {
            extractTextFromRestResponse(responseBody)
        } else {
            "Gemini API HTTP $responseCode Error: $responseBody"
        }
    }

    /**
     * Extracts text content from the Gemini REST JSON response body.
     */
    private fun extractTextFromRestResponse(json: String): String {
        // Find "text": "..." within candidates
        val pattern = "\"text\":\\s*\"((?:\\\\.|[^\"\\\\])*)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: json
    }
}
