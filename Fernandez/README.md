# Fernandez - Gemini AI Integration

## Task: Google Gemini API Connection and Study Content Generation

### Files in this module (`Fernandez/gemini/`):
1. **`FernandezGeminiService.kt`** - Connects to Google Gemini API (via Google AI GenerativeModel SDK with direct REST API fallback) for generating quizzes, reviewers, and reviewing answers.
2. **`GeminiConfig.kt`** - Central configuration manager for API key resolution and overrides.
3. **`PromptGenerator.kt`** - Builds structured prompts for multiple-choice, true/false, identification quizzes, and Markdown reviewers.
4. **`GeminiModels.kt`** - Domain data models for quiz types, reviewer formats, and difficulty levels.

---

## 🔑 How to Set Up Your Gemini API Key

### Step 1: Obtain a Free Gemini API Key
1. Go to **[Google AI Studio](https://aistudio.google.com/app/apikey)**.
2. Sign in with your Google account.
3. Click **"Create API Key"** and copy your generated key (begins with `AIzaSy...`).

### Step 2: Configure the Key in this Project
Choose any of the following options:

#### Option A: In `local.properties` (Recommended)
Add this line to your `local.properties` file in the project root:
```properties
GEMINI_API_KEY=AIzaSyYourActualKeyHere
```
*(This file is excluded from version control, keeping your key secure.)*

#### Option B: In `GeminiConfig.kt`
Open `Fernandez/gemini/GeminiConfig.kt` and replace the placeholder:
```kotlin
const val DEFAULT_API_KEY = "AIzaSyYourActualKeyHere"
```

#### Option C: Via Environment Variable
Set the environment variable in your terminal / system:
```bash
export GEMINI_API_KEY="AIzaSyYourActualKeyHere"
```
Or in Windows PowerShell:
```powershell
$env:GEMINI_API_KEY="AIzaSyYourActualKeyHere"
```

---

## How it connects to the rest of the app:
- **Pinca's Pipeline**: `PincaDataPipelineService` instantiates `FernandezGeminiService()`, which automatically picks up your configured key from `GeminiConfig` or `local.properties`.
- **Garcia's UI**: Displays generated quizzes, flashcards, and summary decks directly in the study screens.
