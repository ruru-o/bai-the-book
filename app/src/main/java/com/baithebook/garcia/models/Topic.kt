package com.baithebook.garcia.models

data class Topic(
    val id: String,
    val subjectId: String,
    val title: String,
    val summaryText: String,
    val cardCount: Int = 0
)
