package com.baithebook.garcia.models

data class Reviewer(
    val id: String,
    val subjectId: String,
    val topicTitle: String,
    val description: String,
    val questions: List<Question> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val summary: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
