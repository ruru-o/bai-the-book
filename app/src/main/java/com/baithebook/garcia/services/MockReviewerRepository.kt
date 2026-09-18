package com.baithebook.garcia.services

import com.baithebook.garcia.models.Flashcard
import com.baithebook.garcia.models.Question
import com.baithebook.garcia.models.QuestionType
import com.baithebook.garcia.models.Reviewer
import com.baithebook.garcia.models.Subject

interface ReviewerRepository {
    fun getSubjects(): List<Subject>
    fun getReviewers(): List<Reviewer>
    fun getReviewerById(id: String): Reviewer?
    fun addReviewer(reviewer: Reviewer)
}

class MockReviewerRepository : ReviewerRepository {

    private val subjects = listOf(
        Subject(
            id = "subj_cs",
            name = "Computer Science & IT",
            description = "Data Structures, Algorithms, APIs & Software Architecture",
            colorHex = "#6366F1",
            topicCount = 4
        ),
        Subject(
            id = "subj_bio",
            name = "General Biology",
            description = "Cellular Processes, DNA Replication, Genetics & Physiology",
            colorHex = "#10B981",
            topicCount = 3
        ),
        Subject(
            id = "subj_hist",
            name = "Philippine History",
            description = "Pre-colonial era, Revolution, Independence & Constitution",
            colorHex = "#F59E0B",
            topicCount = 2
        )
    )

    private val reviewers = mutableListOf(
        Reviewer(
            id = "rev_api_1",
            subjectId = "subj_cs",
            topicTitle = "API Integration & REST Architecture",
            description = "Comprehensive review on Docling PDF extraction and Google Gemini API integration.",
            summary = "API Integration enables application components to communicate over HTTP. Docling extracts structured text from documents, while Gemini AI synthesizes notes into interactive study decks.",
            questions = listOf(
                Question(
                    id = "q1",
                    prompt = "What is the primary role of the Docling API in the Bai The Book application?",
                    options = listOf(
                        "Generates AI quiz questions",
                        "Extracts clean text, headings, and structure from uploaded notes/files",
                        "Translates text to Bisaya",
                        "Converts video files to text"
                    ),
                    correctAnswerIndex = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    explanation = "Docling reads uploaded files (PDF, Word, PPTX, images) and transforms them into structured text for Gemini to process."
                ),
                Question(
                    id = "q2",
                    prompt = "True or False: The application allows students to upload .mp4 video files for reviewer generation.",
                    options = listOf("True", "False"),
                    correctAnswerIndex = 1,
                    type = QuestionType.TRUE_FALSE,
                    explanation = "False. Video files (.mp4) are explicitly NOT supported by the application as per project requirements."
                ),
                Question(
                    id = "q3",
                    prompt = "Explain how Gemini API and Docling API work together in Bai The Book.",
                    expectedAnswer = "Docling extracts clean text from uploaded student notes, which is then fed into Gemini API to generate quizzes, flashcards, summaries, and translations.",
                    type = QuestionType.OPEN_ENDED,
                    explanation = "Docling acts as the parser while Gemini acts as the AI intelligence layer."
                )
            ),
            flashcards = listOf(
                Flashcard(
                    id = "fc1",
                    frontQuestion = "What file formats does Docling support?",
                    backAnswer = "PDF, Word (.docx), PowerPoint (.pptx), Plain text notes, and Images with text."
                ),
                Flashcard(
                    id = "fc2",
                    frontQuestion = "What is Bisaya Mode in Bai The Book?",
                    backAnswer = "A dedicated feature that translates reviewer content and app explanations into Bisaya for enhanced comprehension."
                )
            )
        ),
        Reviewer(
            id = "rev_bio_1",
            subjectId = "subj_bio",
            topicTitle = "Cellular Respiration & ATP Production",
            description = "Glycolysis, Krebs Cycle, and Oxidative Phosphorylation notes summary.",
            summary = "Cellular respiration converts glucose into ATP energy through three main stages: Glycolysis in cytoplasm, Krebs cycle in mitochondrial matrix, and Electron Transport Chain in inner membrane.",
            questions = listOf(
                Question(
                    id = "q_bio_1",
                    prompt = "Where does Glycolysis take place inside a biological cell?",
                    options = listOf("Mitochondrial Matrix", "Cytoplasm", "Nucleus", "Ribosome"),
                    correctAnswerIndex = 1,
                    type = QuestionType.MULTIPLE_CHOICE,
                    explanation = "Glycolysis occurs in the cytoplasm and does not require oxygen."
                )
            ),
            flashcards = listOf(
                Flashcard(
                    id = "fc_bio_1",
                    frontQuestion = "What is the net ATP yield of Glycolysis?",
                    backAnswer = "2 net ATP molecules per molecule of glucose."
                )
            )
        )
    )

    override fun getSubjects(): List<Subject> = subjects

    override fun getReviewers(): List<Reviewer> = reviewers

    override fun getReviewerById(id: String): Reviewer? = reviewers.find { it.id == id }

    override fun addReviewer(reviewer: Reviewer) {
        reviewers.add(0, reviewer)
    }
}
