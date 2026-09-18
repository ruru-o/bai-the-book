package com.baithebook.fernandez.gemini

/**
 * Generates tailored, structured prompts for the Google Gemini API.
 * Handles prompt engineering for:
 * 1. Quiz question generation (Multiple Choice, True/False, Identification)
 * 2. Academic reviewer / study guide creation
 * 3. Student answer evaluation and feedback
 */
class PromptGenerator {

    /**
     * Backward-compatible quiz prompt builder.
     * Generates a default 5-question multiple choice quiz.
     */
    fun buildQuizPrompt(topic: String, textContent: String): String {
        return buildQuizPrompt(
            topic = topic,
            textContent = textContent,
            questionCount = 5,
            questionType = QuizType.MULTIPLE_CHOICE,
            difficulty = Difficulty.MEDIUM,
            asJson = false
        )
    }

    /**
     * Builds a detailed prompt for generating quiz questions from extracted study material.
     *
     * @param topic Topic or subject title.
     * @param textContent The source notes, lecture transcript, or book content.
     * @param questionCount Number of questions to generate (default: 5).
     * @param questionType The type of quiz questions (Multiple Choice, True/False, Identification).
     * @param difficulty Difficulty level (Easy, Medium, Hard).
     * @param asJson If true, instructs Gemini to return strict, parseable JSON.
     */
    fun buildQuizPrompt(
        topic: String,
        textContent: String,
        questionCount: Int = 5,
        questionType: QuizType = QuizType.MULTIPLE_CHOICE,
        difficulty: Difficulty = Difficulty.MEDIUM,
        asJson: Boolean = false
    ): String {
        val topicHeader = if (topic.isNotBlank()) "Topic: $topic\n" else ""

        val typeInstructions = when (questionType) {
            QuizType.MULTIPLE_CHOICE -> """
                - Each question must have 4 plausible options labeled A, B, C, and D.
                - Only ONE option must be clearly correct.
                - Clearly specify the correct option letter.
            """.trimIndent()

            QuizType.TRUE_FALSE -> """
                - Each question must be a statement that is definitively True or False based on the text.
                - The options must be: A) True, B) False.
                - Clearly specify the correct option.
            """.trimIndent()

            QuizType.IDENTIFICATION -> """
                - Questions should prompt for a specific term, concept, person, date, or formula.
                - Do not provide multiple choice options.
                - Provide the exact correct term or phrase as the answer.
            """.trimIndent()

            QuizType.MIXED -> """
                - Provide a balanced mix of Multiple Choice, True/False, and Identification questions.
            """.trimIndent()
        }

        return if (asJson) {
            """
            You are an expert academic educator and exam creator.
            Generate $questionCount quiz questions based strictly on the provided study content.

            $topicHeader
            Difficulty: ${difficulty.level}
            Question Type: ${questionType.displayName}

            Instructions:
            $typeInstructions
            - Ensure questions test understanding and recall of core facts from the text.
            - Provide a concise explanation (1-2 sentences) for why the correct answer is correct.
            - Respond ONLY with valid JSON matching the following schema, with no markdown code fences or conversational text:

            {
              "topic": "${topic.ifBlank { "General" }}",
              "totalQuestions": $questionCount,
              "questions": [
                {
                  "id": 1,
                  "question": "Question text here",
                  "type": "${questionType.name}",
                  "options": ["A) Option 1", "B) Option 2", "C) Option 3", "D) Option 4"],
                  "correctAnswer": "A",
                  "explanation": "Brief explanation",
                  "difficulty": "${difficulty.name}"
                }
              ]
            }

            --- SOURCE CONTENT ---
            $textContent
            """.trimIndent()
        } else {
            """
            You are an expert academic educator. Create a $questionCount-question quiz based on the provided material.

            $topicHeader
            Difficulty Level: ${difficulty.level}
            Format: ${questionType.displayName}

            Requirements:
            $typeInstructions
            - Base every question solely on the provided study text.
            - After each question, include:
              * The answer choices (if applicable)
              * Answer: [Correct Answer]
              * Explanation: [1-2 sentence justification]

            Please format each question clearly:
            Q1. [Question]
            A) ...
            B) ...
            C) ...
            D) ...
            Correct Answer: ...
            Explanation: ...

            --- STUDY MATERIAL ---
            $textContent
            """.trimIndent()
        }
    }

    /**
     * Builds a prompt for generating an academic reviewer / study guide.
     *
     * @param topic Topic or chapter title.
     * @param textContent Source notes or book text.
     * @param format Reviewer style (Comprehensive, Bulleted Summary, Flashcard style, Glossary).
     */
    fun buildReviewerPrompt(
        topic: String,
        textContent: String,
        format: ReviewerFormat = ReviewerFormat.COMPREHENSIVE
    ): String {
        val topicHeader = if (topic.isNotBlank()) "Topic: $topic\n" else ""

        val structureGuide = when (format) {
            ReviewerFormat.COMPREHENSIVE -> """
                Structure the reviewer with these clear sections:
                1. 📌 Overview & Core Theme (Brief 2-3 sentence executive summary)
                2. 🔑 Key Concepts & Explanations (In-depth breakdown of main concepts with clear explanations)
                3. 📖 Essential Vocabulary & Definitions (Terms with clear, memorable definitions)
                4. 💡 High-Yield Takeaways (Bullet points that are likely exam topics)
                5. ❓ Quick Self-Check Review (3-5 practice review questions with concise answers at the end)
            """.trimIndent()

            ReviewerFormat.SUMMARY_BULLETS -> """
                Structure the reviewer as:
                - High-density, easy-to-scan bullet points.
                - Grouped by sub-topics or logical themes.
                - Highlight key names, dates, formulas, and principles in bold.
            """.trimIndent()

            ReviewerFormat.FLASHCARDS -> """
                Format the reviewer as a series of flashcard items:
                Card [Number]
                - Front (Question / Prompt / Concept): ...
                - Back (Answer / Key Definition / Explanation): ...
            """.trimIndent()

            ReviewerFormat.VOCABULARY_GLOSSARY -> """
                Extract all technical terms, jargon, theories, and key names:
                - Term (Part of speech if applicable): Definition and context within the subject.
            """.trimIndent()
        }

        return """
        You are a top-tier academic tutor helping students master their subjects through effective study reviewers.
        Create an organized, easy-to-read, and high-yield Study Reviewer based on the provided text.

        $topicHeader
        Reviewer Format: ${format.styleName}

        $structureGuide

        Formatting Guidelines:
        - Use clean Markdown with headers (`##`, `###`), bolding for emphasis, and organized lists.
        - Ensure all key facts, principles, and nuances in the source material are accurately captured.
        - Make explanations intuitive and simple without losing academic precision.

        --- SOURCE TEXT ---
        $textContent
        """.trimIndent()
    }

    /**
     * Builds a prompt to review and evaluate a student's answer to a review question.
     *
     * @param question The question posed to the student.
     * @param studentAnswer The student's written response.
     * @param contextText Optional source text reference.
     * @param correctAnswer Optional reference/model answer.
     */
    fun buildAnswerReviewPrompt(
        question: String,
        studentAnswer: String,
        contextText: String = "",
        correctAnswer: String = ""
    ): String {
        val contextSection = if (contextText.isNotBlank()) "\nReference Context:\n$contextText\n" else ""
        val modelAnswerSection = if (correctAnswer.isNotBlank()) "\nExpected Answer:\n$correctAnswer\n" else ""

        return """
        You are an encouraging and fair academic evaluator.
        Review the student's answer to the following question.

        Question:
        $question
        $contextSection$modelAnswerSection
        Student's Answer:
        $studentAnswer

        Please evaluate the answer and provide:
        1. Score: Rate from 0 to 10.
        2. Status: Correct, Partially Correct, or Incorrect.
        3. Constructive Feedback: Explain what the student got right and what was missed or inaccurate.
        4. Model Answer: An ideal, concise answer to the question for the student to learn from.
        """.trimIndent()
    }
}
