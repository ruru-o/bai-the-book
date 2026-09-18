package com.baithebook.alvarez.docling

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.util.concurrent.TimeUnit

// 1. DATA MODELS: These classes map exactly to the JSON response that Docling returns.
// We are extracting just the "md_content" (markdown content) containing the parsed text.
data class DoclingResponse(val document: DoclingDocument)
data class DoclingDocument(val md_content: String?)

// 2. API INTERFACE: This defines the HTTP request for Retrofit.
interface DoclingApi {
    @Multipart
    @POST("v1/convert/file") // The specific endpoint path on the Docling server
    suspend fun convertFile(@Part file: MultipartBody.Part): DoclingResponse
}

class AlvarezDoclingService {
    // 3. BASE URL: Points to the local Docker container port bridged via the ADB reverse command.
    private val doclingUrl = "http://localhost:5001/"

    // 4. TIMEOUT CONFIGURATION: This is critical. Parsing complex PDFs or documents
    // takes time. We override the default 10-second timeout with 120 seconds
    // so the app doesn't crash while waiting for the AI to finish reading the file.
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    // 5. RETROFIT SETUP: Wires the network client together, attaching our timeout rules
    // and using Gson to automatically convert the incoming JSON into our data classes above.
    private val retrofit = Retrofit.Builder()
        .baseUrl(doclingUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(DoclingApi::class.java)

    // 6. EXTRACTION FUNCTION: This is the main function called by the FileUploadHandler.
    suspend fun extractTextFromFile(file: File): String {
        // Packages the physical file into a network-safe format for upload
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("files", file.name, requestBody)

        return try {
            // Send the file to the Docling server and wait for the parsed response
            val response = api.convertFile(part)

            // Return the extracted text, or a default message if it's empty
            response.document.md_content ?: "No text returned from Docling"
        } catch (e: Exception) {
            // If the server is offline or errors out, catch it gracefully so the app doesn't crash
            "Error: ${e.message}"
        }
    }
}
