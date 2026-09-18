package com.baithebook.garcia.models

data class StudyMaterial(
    val id: String,
    val sourceName: String,
    val rawTextContent: String,
    val fileTypeExtension: String,
    val extractedHeadingList: List<String> = emptyList()
)
