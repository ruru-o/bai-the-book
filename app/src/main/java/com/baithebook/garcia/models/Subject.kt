package com.baithebook.garcia.models

data class Subject(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String = "book",
    val colorHex: String = "#6366F1",
    val topicCount: Int = 0
)
