package com.baithebook.pinca.pipeline

import com.baithebook.garcia.models.Flashcard
import com.baithebook.garcia.models.Question
import com.baithebook.garcia.models.QuestionType
import com.baithebook.garcia.models.Reviewer
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Presenter responsible for parsing and transforming Gemini AI payloads into
 * Garcia's domain models (Reviewer, Question, Flashcard) for presentation in the UI.
 *
 * Implements PINCA's component in the data flow pipeline.
 */
class ResultsPresenter {

    /**
     * Backward-compatible helper that formats a Gemini response into a user-friendly UI summary.
     */
    fun formatResultsForUI(geminiResponseJson: String): String {
        if (geminiResponseJson.isBlank()) {
            return "No review data available."
        }
        val questions = parseGeminiQuizJson(geminiResponseJson)
        if (questions.isNotEmpty()) {
            return "Successfully generated ${questions.size} quiz questions ready for review session."
        }
        val summary = extractOverviewSummary(geminiResponseJson)
        return if (summary.isNotBlank()) {
            "Reviewer generated: $summary"
        } else {
            "Content processed successfully (${geminiResponseJson.length} characters)."
        }
    }

    /**
     * Parses a Gemini quiz output (JSON format requested via PromptGenerator asJson=true)
     * into a list of domain [Question] objects compatible with Garcia's UI.
     */
    fun parseGeminiQuizJson(geminiPayload: String): List<Question> {
        val cleanJson = extractJsonPayload(geminiPayload)
        if (cleanJson.isBlank()) return parseQuizQuestionsFromTextFallback(geminiPayload)

        return try {
            val rootObj = JSONObject(cleanJson)
            val questionsArray = rootObj.optJSONArray("questions") ?: JSONArray()
            val questionList = mutableListOf<Question>()

            for (i in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(i)
                val id = qObj.optString("id", UUID.randomUUID().toString())
                val prompt = qObj.optString("question", "").trim()
                val typeStr = qObj.optString("type", "MULTIPLE_CHOICE").uppercase()
                val rawOptions = qObj.optJSONArray("options")
                val correctAnswerStr = qObj.optString("correctAnswer", "").trim()
                val explanation = qObj.optString("explanation", "").trim()

                val optionsList = mutableListOf<String>()
                if (rawOptions != null) {
                    for (j in 0 until rawOptions.length()) {
                        optionsList.add(cleanOptionLabel(rawOptions.getString(j)))
                    }
                }

                val questionType = when {
                    typeStr.contains("TRUE") || (optionsList.size == 2 && optionsList.any { it.contains("True", ignoreCase = true) }) ->
                        QuestionType.TRUE_FALSE
                    typeStr.contains("IDENTIFICATION") || typeStr.contains("OPEN") || optionsList.isEmpty() ->
                        QuestionType.OPEN_ENDED
                    else ->
                        QuestionType.MULTIPLE_CHOICE
                }

                val correctIndex = resolveCorrectAnswerIndex(correctAnswerStr, optionsList)

                questionList.add(
                    Question(
                        id = if (id.isNotBlank()) "q_$id" else "q_${UUID.randomUUID().toString().take(8)}",
                        prompt = prompt,
                        options = optionsList,
                        correctAnswerIndex = correctIndex,
                        expectedAnswer = if (questionType == QuestionType.OPEN_ENDED) correctAnswerStr else "",
                        type = questionType,
                        explanation = explanation
                    )
                )
            }
            if (questionList.isEmpty()) parseQuizQuestionsFromTextFallback(geminiPayload) else questionList
        } catch (e: Exception) {
            parseQuizQuestionsFromTextFallback(geminiPayload)
        }
    }

    /**
     * Parses flashcard pairs from Gemini response text/markdown.
     * Supports standard flashcard formats, Q&A blocks, and Glossary/Vocabulary definitions.
     */
    fun parseFlashcards(text: String): List<Flashcard> {
        val flashcards = mutableListOf<Flashcard>()
        if (text.isBlank()) return flashcards

        // 1. Check for Card [Number] or Flashcard [Number] blocks
        val cardDelimiter = Regex("""(?i)(?:^|\r?\n)\s*(?:Card|Flashcard)\s*\d+[:.]?\s*""")
        if (cardDelimiter.containsMatchIn(text)) {
            val sections = text.split(cardDelimiter).filter { it.isNotBlank() }
            for ((index, section) in sections.withIndex()) {
                val frontMatch = Regex("""(?i)[-*]?\s*(?:Front|Question|Concept)(?:\s*\([^)]*\))?[:\s]+([^\r\n]+)""").find(section)
                val backMatch = Regex("""(?i)[-*]?\s*(?:Back|Answer|Definition|Explanation)(?:\s*\([^)]*\))?[:\s]+([^\r\n]+)""").find(section)
                val front = frontMatch?.groupValues?.get(1)?.trim() ?: ""
                val back = backMatch?.groupValues?.get(1)?.trim() ?: ""
                if (front.isNotBlank() && back.isNotBlank()) {
                    flashcards.add(
                        Flashcard(
                            id = "fc_${index + 1}_${UUID.randomUUID().toString().take(6)}",
                            frontQuestion = front,
                            backAnswer = back,
                            isMastered = false
                        )
                    )
                }
            }
            if (flashcards.isNotEmpty()) return flashcards
        }

        // 2. Pattern: Glossary/Vocabulary terms: - **Term**: Definition
        val glossaryPattern = Regex("""^[-*]\s*\*\*([^*]+)\*\*:\s*(.+)$""", RegexOption.MULTILINE)
        val glossaryMatches = glossaryPattern.findAll(text).toList()
        if (glossaryMatches.isNotEmpty()) {
            for ((index, match) in glossaryMatches.withIndex()) {
                val term = match.groupValues[1].trim()
                val definition = match.groupValues[2].trim()
                if (term.isNotBlank() && definition.isNotBlank()) {
                    flashcards.add(
                        Flashcard(
                            id = "fc_term_${index + 1}",
                            frontQuestion = "What is $term?",
                            backAnswer = definition,
                            isMastered = false
                        )
                    )
                }
            }
            return flashcards
        }

        // 3. Fallback Pattern: Q: ... \n A: ...
        val qaDelimiter = Regex("""(?i)(?:^|\r?\n)\s*Q(?:uestion)?\s*\d*[:.]\s*""")
        if (qaDelimiter.containsMatchIn(text)) {
            val qaSections = text.split(qaDelimiter).filter { it.isNotBlank() }
            for ((index, section) in qaSections.withIndex()) {
                val parts = section.split(Regex("""(?i)(?:\r?\n)\s*A(?:nswer)?\s*[:.]\s*"""))
                if (parts.size >= 2) {
                    val q = parts[0].trim().lines().firstOrNull() ?: ""
                    val a = parts[1].trim().lines().firstOrNull() ?: ""
                    if (q.isNotBlank() && a.isNotBlank()) {
                        flashcards.add(
                            Flashcard(
                                id = "fc_qa_${index + 1}",
                                frontQuestion = q,
                                backAnswer = a,
                                isMastered = false
                            )
                        )
                    }
                }
            }
        }

        return flashcards
    }

    /**
     * Parses structured markdown study guides generated by Gemini into a complete [Reviewer]
     * domain model for Garcia's screens.
     */
    fun parseStudyReviewerMarkdown(
        markdownContent: String,
        topicTitle: String = "Study Guide",
        subjectId: String = "subj_cs"
    ): Reviewer {
        val summary = extractOverviewSummary(markdownContent)
        val flashcards = parseFlashcards(markdownContent)
        val questions = parseQuizQuestionsFromTextFallback(markdownContent)

        return Reviewer(
            id = "rev_${UUID.randomUUID().toString().take(8)}",
            subjectId = subjectId,
            topicTitle = topicTitle.ifBlank { "Study Guide" },
            description = summary.take(160).ifBlank { "Generated study reviewer from uploaded notes." },
            questions = questions,
            flashcards = flashcards,
            summary = summary.ifBlank { markdownContent.take(500) },
            createdAtTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Assembles a unified [Reviewer] domain model combining quiz questions and reviewer notes.
     */
    fun assembleReviewer(
        topicTitle: String,
        subjectId: String = "subj_cs",
        summaryText: String,
        quizJson: String = "",
        flashcardsSourceText: String? = null
    ): Reviewer {
        val questions = if (quizJson.isNotBlank()) parseGeminiQuizJson(quizJson) else emptyList()
        val flashcards = if (!flashcardsSourceText.isNullOrBlank()) {
            parseFlashcards(flashcardsSourceText)
        } else {
            emptyList()
        }

        val cleanSummary = if (summaryText.contains("##")) {
            extractOverviewSummary(summaryText)
        } else {
            summaryText.trim()
        }

        return Reviewer(
            id = "rev_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            subjectId = subjectId,
            topicTitle = topicTitle.ifBlank { "Reviewer Deck" },
            description = cleanSummary.take(150).ifBlank { "Comprehensive study deck with ${questions.size} questions and ${flashcards.size} flashcards." },
            questions = questions,
            flashcards = flashcards,
            summary = cleanSummary.ifBlank { "Generated study guide for $topicTitle." },
            createdAtTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Formats reviewer summary metadata into a clean UI banner or sub-header string.
     */
    fun formatReviewerSummaryCard(reviewer: Reviewer): String {
        return buildString {
            append("Topic: ${reviewer.topicTitle} | ")
            append("${reviewer.questions.size} Questions | ")
            append("${reviewer.flashcards.size} Flashcards")
        }
    }

    // =========================================================================
    // PRIVATE PARSING HELPERS
    // =========================================================================

    /**
     * Extracts pure JSON string by stripping any markdown code fences or conversational prefixes.
     */
    private fun extractJsonPayload(rawPayload: String): String {
        var text = rawPayload.trim()
        if (text.startsWith("```json", ignoreCase = true)) {
            text = text.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```").substringBeforeLast("```").trim()
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1)
        } else {
            ""
        }
    }

    /**
     * Maps answer letters or text ("A", "Option 1", "True") to a zero-based option index.
     */
    private fun resolveCorrectAnswerIndex(correctAnswer: String, options: List<String>): Int {
        if (options.isEmpty()) return 0

        // Direct letter matching (A -> 0, B -> 1, C -> 2, D -> 3)
        val letter = correctAnswer.trim().take(1).uppercase()
        val letterIndex = when (letter) {
            "A" -> 0
            "B" -> 1
            "C" -> 2
            "D" -> 3
            else -> -1
        }
        if (letterIndex in options.indices) return letterIndex

        // Numeric match
        correctAnswer.toIntOrNull()?.let { num ->
            if (num in options.indices) return num
            if (num - 1 in options.indices) return num - 1
        }

        // Substring or exact match in options list
        val matchIndex = options.indexOfFirst { opt ->
            opt.equals(correctAnswer, ignoreCase = true) ||
            opt.contains(correctAnswer, ignoreCase = true)
        }
        if (matchIndex != -1) return matchIndex

        return 0
    }

    /**
     * Cleans prefixes like "A) ", "B. ", "1. " from option strings so the UI displays cleanly.
     */
    private fun cleanOptionLabel(rawOption: String): String {
        return rawOption.replace(Regex("""^[A-Da-d0-9][).:\s-]+\s*"""), "").trim()
    }

    /**
     * Extracts the overview summary section from Markdown formatted reviewer text.
     */
    private fun extractOverviewSummary(markdownText: String): String {
        val overviewRegex = Regex("""(?:📌\s*Overview|##\s*1?[.]?\s*Overview|Executive Summary)(.*?)(?=\n##|\n---|\z)""", RegexOption.DOT_MATCHES_ALL)
        val match = overviewRegex.find(markdownText)
        if (match != null) {
            return match.groupValues[1].trim().lines().filter { it.isNotBlank() && !it.startsWith("#") }.joinToString(" ")
        }
        // Fallback: take the first non-header paragraph
        val paragraphs = markdownText.split(Regex("""\r?\n\r?\n"""))
        for (para in paragraphs) {
            val cleaned = para.trim()
            if (cleaned.isNotBlank() && !cleaned.startsWith("#") && !cleaned.startsWith("---")) {
                return cleaned
            }
        }
        return ""
    }

    /**
     * Fallback parser for quiz questions formatted in plain text / markdown
     * (e.g. "Q1. ... A) ... B) ... Answer: ... Explanation: ...").
     */
    private fun parseQuizQuestionsFromTextFallback(rawText: String): List<Question> {
        val questions = mutableListOf<Question>()
        if (rawText.isBlank()) return questions

        val qBlocks = rawText.split(Regex("""(?:\r?\n\s*|^)(?=Q\d+[:.]|\d+[:.]\s+)"""))
        for ((idx, block) in qBlocks.withIndex()) {
            val cleanBlock = block.trim()
            if (cleanBlock.isBlank()) continue

            val lines = cleanBlock.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val promptLine = lines.first().replace(Regex("""^(?:Q\d+|\d+)[:.]\s*"""), "").trim()
            val options = mutableListOf<String>()
            var answerStr = ""
            var explanation = ""

            for (line in lines.drop(1)) {
                when {
                    line.matches(Regex("""^[A-D][).]\s*.*""")) -> {
                        options.add(cleanOptionLabel(line))
                    }
                    line.startsWith("Answer:", ignoreCase = true) || line.startsWith("Correct Answer:", ignoreCase = true) -> {
                        answerStr = line.substringAfter(":").trim()
                    }
                    line.startsWith("Explanation:", ignoreCase = true) -> {
                        explanation = line.substringAfter(":").trim()
                    }
                }
            }

            if (promptLine.isNotBlank()) {
                val isTrueFalse = options.size == 2 && options.any { it.contains("True", ignoreCase = true) }
                val type = when {
                    isTrueFalse -> QuestionType.TRUE_FALSE
                    options.isEmpty() -> QuestionType.OPEN_ENDED
                    else -> QuestionType.MULTIPLE_CHOICE
                }

                questions.add(
                    Question(
                        id = "q_text_${idx + 1}",
                        prompt = promptLine,
                        options = options,
                        correctAnswerIndex = resolveCorrectAnswerIndex(answerStr, options),
                        expectedAnswer = if (type == QuestionType.OPEN_ENDED) answerStr else "",
                        type = type,
                        explanation = explanation
                    )
                )
            }
        }
        return questions
    }
}
