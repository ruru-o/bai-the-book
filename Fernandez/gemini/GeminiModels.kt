package com.baithebook.fernandez.gemini

/**
 * Supported quiz question types for generation.
 */
enum class QuizType(val displayName: String) {
    MULTIPLE_CHOICE("Multiple Choice"),
    TRUE_FALSE("True or False"),
    IDENTIFICATION("Identification / Short Answer"),
    MIXED("Mixed Format")
}

/**
 * Difficulty levels for quiz questions.
 */
enum class Difficulty(val level: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

/**
 * Output styles / formats for reviewer notes.
 */
enum class ReviewerFormat(val styleName: String) {
    COMPREHENSIVE("Comprehensive Study Guide (Concepts, Summary, Vocabulary, and Self-Test)"),
    SUMMARY_BULLETS("High-Yield Bulleted Summary"),
    FLASHCARDS("Question and Answer / Flashcard Style"),
    VOCABULARY_GLOSSARY("Glossary of Terms & Key Definitions")
}

/**
 * Represents an individual quiz question.
 */
data class QuizQuestion(
    val id: Int,
    val question: String,
    val type: QuizType = QuizType.MULTIPLE_CHOICE,
    val options: List<String> = emptyList(), // e.g. ["A) ...", "B) ...", "C) ...", "D) ..."]
    val correctAnswer: String,              // e.g. "A" or the exact answer
    val explanation: String = "",           // brief rationale of why the answer is correct
    val difficulty: Difficulty = Difficulty.MEDIUM
)

/**
 * Represents the structured quiz response container.
 */
data class QuizResponse(
    val topic: String,
    val totalQuestions: Int,
    val questions: List<QuizQuestion>
)

/**
 * Represents the evaluation of a student's answer.
 */
data class AnswerEvaluation(
    val question: String,
    val studentAnswer: String,
    val score: Int,                  // e.g., 0 to 10
    val isCorrect: Boolean,
    val feedback: String,
    val keyPointsMissed: List<String> = emptyList(),
    val modelAnswer: String
)
