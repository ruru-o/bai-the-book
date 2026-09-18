package com.baithebook.pinca

import com.baithebook.garcia.models.QuestionType
import com.baithebook.garcia.services.MainViewModel
import com.baithebook.pinca.pipeline.PincaDataPipelineService
import com.baithebook.pinca.pipeline.ResultsPresenter
import org.junit.Assert.*
import org.junit.Test

class PincaPipelineTest {

    private val pipelineService = PincaDataPipelineService()
    private val presenter = ResultsPresenter()

    @Test
    fun testCleanAndStructureDoclingText() {
        val rawDocling = """
            # Operating Systems and Memory Management
            
            Memory management is the functionality of an OS which handles or manages primary memory.
            
            | Heading 1 | Heading 2 |
            |---|---|
            | Value 1   | Value 2   |
            
            ## Virtual Memory
            Virtual memory allows the execution of processes that may not be completely in memory.
        """.trimIndent()

        val structured = pipelineService.cleanAndStructureDoclingText(rawDocling)
        assertEquals("Operating Systems and Memory Management", structured.inferredTopic)
        assertTrue(structured.headings.contains("Operating Systems and Memory Management"))
        assertTrue(structured.headings.contains("Virtual Memory"))
        assertTrue(structured.wordCount > 10)
        assertFalse(structured.cleanText.contains("|---|---|"))
    }

    @Test
    fun testParseGeminiQuizJson() {
        val geminiJson = """
            ```json
            {
              "topic": "Operating Systems",
              "totalQuestions": 3,
              "questions": [
                {
                  "id": 1,
                  "question": "What is the primary function of virtual memory?",
                  "type": "MULTIPLE_CHOICE",
                  "options": ["A) Increase CPU clock", "B) Allow processes larger than RAM", "C) Speed up GPU", "D) Format disks"],
                  "correctAnswer": "B",
                  "explanation": "Virtual memory maps virtual addresses to physical storage."
                },
                {
                  "id": 2,
                  "question": "Paging is a memory management scheme.",
                  "type": "TRUE_FALSE",
                  "options": ["A) True", "B) False"],
                  "correctAnswer": "A",
                  "explanation": "Paging avoids external fragmentation."
                },
                {
                  "id": 3,
                  "question": "What does TLB stand for?",
                  "type": "IDENTIFICATION",
                  "options": [],
                  "correctAnswer": "Translation Lookaside Buffer",
                  "explanation": "TLB is a hardware cache."
                }
              ]
            }
            ```
        """.trimIndent()

        val questions = presenter.parseGeminiQuizJson(geminiJson)
        assertEquals(3, questions.size)

        // Multiple choice check
        val q1 = questions[0]
        assertEquals("What is the primary function of virtual memory?", q1.prompt)
        assertEquals(QuestionType.MULTIPLE_CHOICE, q1.type)
        assertEquals(1, q1.correctAnswerIndex) // B -> index 1
        assertEquals("Allow processes larger than RAM", q1.options[1])

        // True/False check
        val q2 = questions[1]
        assertEquals(QuestionType.TRUE_FALSE, q2.type)
        assertEquals(0, q2.correctAnswerIndex) // A -> index 0

        // Identification / Open ended check
        val q3 = questions[2]
        assertEquals(QuestionType.OPEN_ENDED, q3.type)
        assertEquals("Translation Lookaside Buffer", q3.expectedAnswer)
    }

    @Test
    fun testParseFlashcards() {
        val flashcardContent = """
            Card 1
            - Front: What is Cache Memory?
            - Back: High-speed SRAM used to accelerate CPU access.
            
            Card 2
            - Front: What is Paging?
            - Back: A storage mechanism used to retrieve processes from secondary storage in pages.
        """.trimIndent()

        val cards = presenter.parseFlashcards(flashcardContent)
        assertEquals(2, cards.size)
        assertEquals("What is Cache Memory?", cards[0].frontQuestion)
        assertEquals("High-speed SRAM used to accelerate CPU access.", cards[0].backAnswer)
    }

    @Test
    fun testAssembleReviewerAndPassToViewModel() {
        val reviewer = presenter.assembleReviewer(
            topicTitle = "Data Structures",
            subjectId = "subj_cs",
            summaryText = "Data structures organize and store data for efficient access.",
            quizJson = """
                {"topic":"Data Structures","totalQuestions":1,"questions":[{"id":1,"question":"What is a stack?","type":"MULTIPLE_CHOICE","options":["LIFO","FIFO"],"correctAnswer":"A","explanation":"Last-in, first-out."}]}
            """.trimIndent()
        )

        assertEquals("Data Structures", reviewer.topicTitle)
        assertEquals(1, reviewer.questions.size)

        val viewModel = MainViewModel()
        pipelineService.passToViewModel(reviewer, viewModel)

        assertNotNull(viewModel.selectedReviewer)
        assertEquals("Data Structures", viewModel.selectedReviewer?.topicTitle)
    }

    @Test
    fun testAlvarezDoclingHandoff() {
        val extractedDoclingText = """
            # Cloud Computing Architecture
            Cloud computing is the on-demand delivery of IT resources over the Internet.
            ## Key Models
            - IaaS: Infrastructure as a Service
            - PaaS: Platform as a Service
            - SaaS: Software as a Service
        """.trimIndent()

        val response = com.baithebook.pinca.pipeline.PincasDataFlow.receiveDoclingText(extractedDoclingText)
        assertTrue(response.contains("Pipeline Ready"))
        assertTrue(response.contains("Cloud Computing Architecture"))
        assertEquals(extractedDoclingText, com.baithebook.pinca.pipeline.PincasDataFlow.lastExtractedText)
    }
}
