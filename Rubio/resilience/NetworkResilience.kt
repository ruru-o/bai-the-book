package com.baithebook.rubio.resilience

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay
import org.json.JSONException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.min

/**
 * Rubio's API / Internet code handler — fully self-contained, no dependency on a shared
 * error model.
 *
 * Per Rubio/README.md: "implement exponential backoff retries for network drops."
 *
 * Wraps calls to Alvarez's Docling API and Fernandez's Gemini API with:
 *  - connectivity checks (isNetworkAvailable)
 *  - typed error mapping for "wrong API" cases (bad key, 4xx/5xx, malformed response)
 *  - exponential backoff retries — but only for genuinely transient failures
 */
class NetworkResilience {

    // ============================================================
    // Local result / error types (no external dependency)
    // ============================================================

    enum class ApiErrorType {
        MISSING_API_KEY,
        NETWORK_ERROR,
        REQUEST_TIMEOUT,
        HTTP_ERROR,
        INVALID_RESPONSE,
        UNKNOWN
    }

    data class ApiError(
        val type: ApiErrorType,
        val userMessage: String,
        val debugMessage: String? = null,
        val httpCode: Int? = null
    )

    sealed class NetworkResult<out T> {
        data class Success<out T>(val data: T) : NetworkResult<T>()
        data class Failure(val error: ApiError) : NetworkResult<Nothing>()
    }

    companion object {
        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_INITIAL_DELAY_MS = 500L
        private const val DEFAULT_MAX_DELAY_MS = 8_000L
        private const val BACKOFF_FACTOR = 2.0
    }

    // ============================================================
    // Legacy signature — kept so anything already wired to the old stub still compiles.
    // ============================================================

    @Deprecated("Use the suspend executeWithRetry(block) overload", ReplaceWith("executeWithRetry(block = { })"))
    fun executeWithRetry(attemptCount: Int): Boolean = attemptCount > 0

    // ============================================================
    // Real retry wrapper
    // ============================================================

    /**
     * Runs [block] (a Docling or Gemini call) with exponential backoff retries.
     * Only retries errors that are actually transient (dropped connections, timeouts,
     * 429 rate limits, 5xx server errors). Non-retryable errors — missing/bad API key,
     * other 4xx, malformed response — fail immediately instead of burning retry attempts.
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        initialDelayMillis: Long = DEFAULT_INITIAL_DELAY_MS,
        maxDelayMillis: Long = DEFAULT_MAX_DELAY_MS,
        block: suspend () -> T
    ): NetworkResult<T> {
        var attempt = 0
        var delayMillis = initialDelayMillis
        var lastError: ApiError

        while (true) {
            attempt++
            try {
                return NetworkResult.Success(block())
            } catch (e: Exception) {
                lastError = mapException(e)
            }

            if (attempt >= maxAttempts || !isRetryable(lastError)) {
                return NetworkResult.Failure(lastError)
            }

            delay(delayMillis)
            delayMillis = min((delayMillis * BACKOFF_FACTOR).toLong(), maxDelayMillis)
        }
    }

    /** True for errors worth retrying: dropped connections, timeouts, rate limits, server errors. */
    fun isRetryable(error: ApiError): Boolean = when (error.type) {
        ApiErrorType.NETWORK_ERROR, ApiErrorType.REQUEST_TIMEOUT -> true
        ApiErrorType.HTTP_ERROR -> error.httpCode == 429 || (error.httpCode ?: 0) in 500..599
        else -> false // bad key, invalid response, other 4xx -> don't retry
    }

    // ============================================================
    // Connectivity
    // ============================================================

    /** Check before even attempting a call, so "no internet" fails fast with a clear message. */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // ============================================================
    // Exception / HTTP response -> typed "wrong API" error
    // ============================================================

    /** For exceptions thrown by Retrofit calls (Docling) or the Gemini SDK. */
    fun mapException(e: Throwable): ApiError = when (e) {
        is SocketTimeoutException -> ApiError(
            type = ApiErrorType.REQUEST_TIMEOUT,
            userMessage = "The request took too long to respond. Please try again.",
            debugMessage = e.message ?: "Socket timeout"
        )

        is UnknownHostException, is ConnectException -> ApiError(
            type = ApiErrorType.NETWORK_ERROR,
            userMessage = "Couldn't reach the server. Check your internet connection and try again.",
            debugMessage = e.message ?: "Host unreachable"
        )

        is HttpException -> {
            val code = e.code()
            val body = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            httpError(code, body)
        }

        is JSONException -> ApiError(
            type = ApiErrorType.INVALID_RESPONSE,
            userMessage = "The AI service returned an unexpected response. Please try again.",
            debugMessage = e.message ?: "Malformed JSON response"
        )

        // Covers other network-layer failures not caught above (e.g. SSL handshake failures).
        is IOException -> ApiError(
            type = ApiErrorType.NETWORK_ERROR,
            userMessage = "Couldn't reach the server. Check your internet connection and try again.",
            debugMessage = e.message ?: "I/O error during request"
        )

        else -> ApiError(
            type = ApiErrorType.UNKNOWN,
            userMessage = "Something went wrong. Please try again.",
            debugMessage = e.message ?: e.toString()
        )
    }

    /**
     * For manual HttpURLConnection calls (Fernandez's Gemini REST fallback), where you
     * already have a response code + body instead of a thrown exception.
     */
    fun mapHttpResponse(responseCode: Int, responseBody: String?): ApiError {
        return if (responseCode in 200..299) {
            ApiError(
                type = ApiErrorType.INVALID_RESPONSE,
                userMessage = "The AI service returned an unexpected response. Please try again.",
                debugMessage = "Expected error but got 2xx with unparsable body"
            )
        } else {
            httpError(responseCode, responseBody)
        }
    }

    private fun httpError(code: Int, body: String?): ApiError = ApiError(
        type = ApiErrorType.HTTP_ERROR,
        httpCode = code,
        userMessage = when (code) {
            401, 403 -> "We couldn't authenticate with the AI service. The API key may be invalid or expired."
            404 -> "The AI service endpoint couldn't be found. It may have changed or moved."
            408 -> "The request timed out waiting for a response."
            429 -> "Too many requests right now. Please wait a moment and try again."
            in 500..599 -> "The AI service is currently unavailable. Please try again in a bit."
            else -> "The AI service returned an unexpected error (code $code)."
        },
        debugMessage = "HTTP $code: ${body?.take(300)}"
    )

    /** Checks a required API key/config value before a call is even attempted. */
    fun checkApiKey(apiKey: String): ApiError? =
        if (apiKey.isBlank()) ApiError(
            type = ApiErrorType.MISSING_API_KEY,
            userMessage = "The app isn't configured with a valid API key yet. Please contact support."
        ) else null
}
