# Fernandez - Gemini AI Integration

## Task: Google Gemini API Connection and Study Content Generation

### Files in this module (`Fernandez/gemini/`):
1. **`FernandezGeminiService.kt`** - Connects to Google Gemini API (via Google AI GenerativeModel SDK with direct REST API fallback) for generating quizzes, reviewers, and reviewing answers.
2. **`GeminiConfig.kt`** - Central configuration manager for API key resolution and overrides.
3. **`PromptGenerator.kt`** - Builds structured prompts for multiple-choice, true/false, identification quizzes, and Markdown reviewers.
4. **`GeminiModels.kt`** - Domain data models for quiz types, reviewer formats, and difficulty levels.

---


## How it connects to the rest of the app:
- **Pinca's Pipeline**: `PincaDataPipelineService` instantiates `FernandezGeminiService()`, which automatically picks up your configured key from `GeminiConfig` or `local.properties`.
- **Garcia's UI**: Displays generated quizzes, flashcards, and summary decks directly in the study screens.
