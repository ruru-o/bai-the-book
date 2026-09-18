package com.baithebook.garcia.models

enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    OPEN_ENDED
}

data class Question(
    val id: String,
    val prompt: String,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val expectedAnswer: String = "",
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val explanation: String = ""
)
