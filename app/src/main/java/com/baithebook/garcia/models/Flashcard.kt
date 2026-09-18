package com.baithebook.garcia.models

data class Flashcard(
    val id: String,
    val frontQuestion: String,
    val backAnswer: String,
    val isMastered: Boolean = false
)
