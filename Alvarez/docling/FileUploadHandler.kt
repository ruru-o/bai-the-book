package com.baithebook.alvarez.docling

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileUploadHandler(private val context: Context) {

    // 1. SERVICE INITIALIZATION: Creates an instance of our API connection class
    private val doclingService = AlvarezDoclingService()

    suspend fun processSelectedFile(uri: Uri): String {
        // 2. BACKGROUND THREAD: Dispatchers.IO ensures that reading the file and
        // making the network request doesn't freeze the app's main user interface.
        return withContext(Dispatchers.IO) {

            // 3. MIME TYPE VALIDATION: Check what kind of file the user just picked
            val mimeType = context.contentResolver.getType(uri)

            // 4. REJECTION LOGIC: Strictly enforces the requirement to reject .mp4 files.
            // If the file is a video, we immediately stop processing and return an error.
            if (mimeType != null && mimeType.startsWith("video/")) {
                val errorMsg = "Upload Rejected: Video files like .mp4 are not supported."
                Log.e("FileUpload", errorMsg)
                return@withContext errorMsg
            }

            try {
                // 5. FILE PREPARATION: Android file pickers return a "Uri", but our network
                // client (Retrofit) requires a physical "File". We open an input stream to read
                // the Uri, and copy its contents into a newly created temporary file in the cache.
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext "Failed to open file"
                val tempFile = File.createTempFile("upload", null, context.cacheDir)
                tempFile.outputStream().use { output -> inputStream.copyTo(output) }

                // 6. API EXECUTION: Pass the temporary file to our Docling service to extract the text
                val extractedText = doclingService.extractTextFromFile(tempFile)

                // 7. PIPELINE HANDOFF: Send the extracted text over to Kuya Joswens data flow
                // so it can eventually be processed by the Gemini API.
                PincasDataFlow.receiveDoclingText(extractedText)

                return@withContext "Success! Text extracted and sent to pipeline."
            } catch (e: Exception) {
                // 8. ERROR HANDLING: Catch any issues (like running out of storage or missing files)
                return@withContext "File processing failed: ${e.message}"
            }
        }
    }
}

// 9. PIPELINE HANDOFF: Connected to PINCA's actual data pipeline
object PincasDataFlow {
    fun receiveDoclingText(text: String) {
        Log.d("DataFlow", "Handing off to Gemini API pipeline: \n$text")
        com.baithebook.pinca.pipeline.PincasDataFlow.receiveDoclingText(text)
    }
}
