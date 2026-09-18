package com.baithebook.fernandez

import com.baithebook.fernandez.gemini.FernandezGeminiService
import com.baithebook.fernandez.gemini.GeminiConfig
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FernandezGeminiServiceTest {

    @Before
    fun setUp() {
        GeminiConfig.setApiKey("")
    }

    @After
    fun tearDown() {
        GeminiConfig.setApiKey("")
    }

    @Test
    fun testDefaultApiKeyResolution() {
        val service = FernandezGeminiService()
        assertEquals(GeminiConfig.getApiKey(), service.apiKey)
    }

    @Test
    fun testExplicitConstructorApiKeyTakesPrecedence() {
        val customKey = "AIzaSyTestExplicitConstructorKey"
        val service = FernandezGeminiService(apiKey = customKey)
        assertEquals(customKey, service.apiKey)
        assertTrue(service.isConfigured)
    }

    @Test
    fun testGeminiConfigRuntimeOverride() {
        val customKey = "AIzaSyRuntimeConfiguredKey"
        GeminiConfig.setApiKey(customKey)

        assertEquals(customKey, GeminiConfig.getApiKey())
        assertTrue(GeminiConfig.isConfigured())

        val service = FernandezGeminiService()
        assertEquals(customKey, service.apiKey)
        assertTrue(service.isConfigured)
    }

    @Test
    fun testMissingOrPlaceholderKeyReturnsHelpfulError() {
        GeminiConfig.setApiKey("")
        val service = FernandezGeminiService(apiKey = "")

        val response = service.generateQuizQuestions("Sample text about operating systems")
        assertTrue("Expected helpful error message but got: $response", response.contains("Error: Gemini API Key"))
    }
}
