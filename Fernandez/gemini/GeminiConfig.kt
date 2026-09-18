package com.baithebook.fernandez.gemini

import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Configuration manager for Google Gemini API integration.
 *
 * Provides flexible resolution for the Gemini API key:
 * 1. Runtime override via [setApiKey]
 * 2. System environment variable `GEMINI_API_KEY`
 * 3. JVM System property `gemini.api.key`
 * 4. `local.properties` file entry (`GEMINI_API_KEY=...`)
 * 5. Static placeholder constant [DEFAULT_API_KEY]
 *
 * To get a Gemini API key:
 * 1. Visit https://aistudio.google.com/app/apikey
 * 2. Sign in with your Google account.
 * 3. Click "Create API Key" (or "Get API key").
 * 4. Paste the key into `local.properties` or set it in this config.
 */
object GeminiConfig {

    /**
     * Default placeholder API key.
     * You can either:
     * - Put your key in `local.properties`: `GEMINI_API_KEY=AIzaSy...`
     * - Or replace this placeholder string directly.
     */
    const val DEFAULT_API_KEY = "AQ.Ab8RN6IJDTNKug_5DYM9G29x8oUdoC0p1DWuOEt7wHqhj3eaWw"

    @Volatile
    private var customApiKey: String? = null

    /**
     * Sets or overrides the API key at runtime.
     */
    fun setApiKey(key: String) {
        customApiKey = key.trim()
    }

    /**
     * Retrieves the best available Gemini API key from all supported sources.
     */
    fun getApiKey(): String {
        // 1. Check runtime override
        customApiKey?.takeIf { it.isNotBlank() }?.let { return it }

        // 2. Check System Environment variable (e.g. CI/CD or local env)
        System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }?.let { return it.trim() }

        // 3. Check JVM System Property (e.g. -Dgemini.api.key=...)
        System.getProperty("gemini.api.key")?.takeIf { it.isNotBlank() }?.let { return it.trim() }

        // 4. Check local.properties file on the filesystem
        findKeyInLocalProperties()?.takeIf { it.isNotBlank() }?.let { return it }

        // 5. Fallback placeholder
        return DEFAULT_API_KEY
    }

    /**
     * Checks if a valid non-placeholder API key is currently configured.
     */
    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != DEFAULT_API_KEY
    }

    /**
     * Attempts to find and read GEMINI_API_KEY from local.properties.
     */
    private fun findKeyInLocalProperties(): String? {
        val candidatePaths = listOf(
            "local.properties",
            "../local.properties",
            "../../local.properties"
        )
        for (path in candidatePaths) {
            val file = File(path)
            if (file.exists() && file.isFile) {
                try {
                    val props = Properties()
                    FileInputStream(file).use { props.load(it) }
                    val key = props.getProperty("GEMINI_API_KEY") ?: props.getProperty("gemini.api.key")
                    if (!key.isNullOrBlank()) {
                        return key.trim()
                    }
                } catch (_: Exception) {
                    // Ignore and try next candidate
                }
            }
        }
        return null
    }
}
