package com.baithebook.herrera.error

import com.baithebook.rubio.resilience.NetworkResilience
import com.baithebook.rubio.resilience.RubioInputValidator
import org.json.JSONException
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Herrera's centralized error handling module.
 *
 * Per Herrera/README.md:
 * "handle no internet, invalid files, and request timeouts"
 * "hook error handler into MainViewModel state and api service calls"
 *
 * Integrates cleanly with Rubio's resilience and validation layers to provide
 * a unified error contract across the entire application.
 */
class HerreraErrorHandler {

    enum class AppErrorCategory {
        NETWORK_OFFLINE,
        TIMEOUT,
        HTTP_STATUS,
        AUTHENTICATION,
        RATE_LIMITED,
        FILE_INVALID,
        PARSING_FAILED,
        UNKNOWN
    }

    data class AppError(
        val category: AppErrorCategory,
        val title: String,
        val userMessage: String,
        val debugDetails: String? = null,
        val httpCode: Int? = null,
        val canRetry: Boolean = true,
        val recoveryHint: String? = null
    )

    // ============================================================
    // Primary Exception Handler
    // ============================================================

    /**
     * Translates any thrown exception into a structured, user-friendly [AppError].
     */
    fun handleException(e: Throwable): AppError {
        return when (e) {
            is SocketTimeoutException -> AppError(
                category = AppErrorCategory.TIMEOUT,
                title = "Request Timed Out",
                userMessage = "The request took too long to finish. The server or AI engine might be busy.",
                debugDetails = e.message ?: "SocketTimeoutException",
                canRetry = true,
                recoveryHint = "Tap Retry to send the request again."
            )

            is UnknownHostException, is ConnectException -> AppError(
                category = AppErrorCategory.NETWORK_OFFLINE,
                title = "No Internet Connection",
                userMessage = "Could not reach the server. Please check your Wi-Fi or mobile data.",
                debugDetails = e.message ?: e.javaClass.simpleName,
                canRetry = true,
                recoveryHint = "Ensure internet connectivity and try again."
            )

            is HttpException -> handleHttpError(e.code(), try { e.response()?.errorBody()?.string() } catch (_: Exception) { null })

            is JSONException -> AppError(
                category = AppErrorCategory.PARSING_FAILED,
                title = "AI Response Parsing Error",
                userMessage = "The AI service responded in an unexpected format. Please try generating again.",
                debugDetails = e.message ?: "JSONException",
                canRetry = true,
                recoveryHint = "Try requesting a summary with a different prompt or smaller file."
            )

            is FileNotFoundException -> AppError(
                category = AppErrorCategory.FILE_INVALID,
                title = "File Not Found",
                userMessage = "The selected file could not be accessed. It may have been moved or deleted.",
                debugDetails = e.message,
                canRetry = false,
                recoveryHint = "Please pick the document again."
            )

            is IOException -> AppError(
                category = AppErrorCategory.NETWORK_OFFLINE,
                title = "Connection Interrupted",
                userMessage = "A communication error occurred while processing data. Please try again.",
                debugDetails = e.message ?: "IOException",
                canRetry = true,
                recoveryHint = "Verify connection stability."
            )

            else -> AppError(
                category = AppErrorCategory.UNKNOWN,
                title = "Unexpected Error",
                userMessage = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "An unexpected issue occurred. Please try again.",
                debugDetails = e.stackTraceToString().take(400),
                canRetry = true,
                recoveryHint = "If this persists, restart the app."
            )
        }
    }

    // ============================================================
    // HTTP Status Code Mapping
    // ============================================================

    fun handleHttpError(code: Int, responseBody: String? = null): AppError {
        return when (code) {
            401, 403 -> AppError(
                category = AppErrorCategory.AUTHENTICATION,
                title = "Authentication Error",
                userMessage = "API key is invalid or unauthorized. Please check your Gemini or service credentials.",
                debugDetails = "HTTP $code: ${responseBody?.take(200)}",
                httpCode = code,
                canRetry = false,
                recoveryHint = "Contact project maintainers or verify API key configuration."
            )

            404 -> AppError(
                category = AppErrorCategory.HTTP_STATUS,
                title = "Service Endpoint Missing",
                userMessage = "The requested service endpoint could not be found (404).",
                debugDetails = "HTTP 404: ${responseBody?.take(200)}",
                httpCode = 404,
                canRetry = false,
                recoveryHint = "Check endpoint configuration."
            )

            408 -> AppError(
                category = AppErrorCategory.TIMEOUT,
                title = "Server Timeout",
                userMessage = "The server timed out waiting for the request to complete.",
                debugDetails = "HTTP 408",
                httpCode = 408,
                canRetry = true,
                recoveryHint = "Tap Retry to attempt the connection again."
            )

            429 -> AppError(
                category = AppErrorCategory.RATE_LIMITED,
                title = "Rate Limit Reached",
                userMessage = "Too many requests to the AI service. Please wait a moment before trying again.",
                debugDetails = "HTTP 429: ${responseBody?.take(200)}",
                httpCode = 429,
                canRetry = true,
                recoveryHint = "Wait 10-15 seconds before retrying."
            )

            in 500..599 -> AppError(
                category = AppErrorCategory.HTTP_STATUS,
                title = "Server Unavailable",
                userMessage = "The server is temporarily unavailable or encountered an error ($code).",
                debugDetails = "HTTP $code: ${responseBody?.take(200)}",
                httpCode = code,
                canRetry = true,
                recoveryHint = "The service might be restarting. Please retry shortly."
            )

            else -> AppError(
                category = AppErrorCategory.HTTP_STATUS,
                title = "HTTP Error",
                userMessage = "The server returned HTTP error code $code.",
                debugDetails = "HTTP $code: ${responseBody?.take(200)}",
                httpCode = code,
                canRetry = code in 500..599,
                recoveryHint = "Check service logs or try again later."
            )
        }
    }

    // ============================================================
    // Rubio Bridge Methods
    // ============================================================

    /**
     * Converts a [RubioInputValidator.InputError] into a centralized [AppError].
     */
    fun handleValidationError(inputError: RubioInputValidator.InputError): AppError {
        return AppError(
            category = AppErrorCategory.FILE_INVALID,
            title = when (inputError.type) {
                RubioInputValidator.InputErrorType.NO_FILE_SELECTED -> "No File Selected"
                RubioInputValidator.InputErrorType.INVALID_FILE_TYPE -> "Unsupported File Format"
                RubioInputValidator.InputErrorType.FILE_TOO_LARGE -> "File Exceeds Size Limit"
                RubioInputValidator.InputErrorType.FILE_EMPTY -> "Empty File"
                RubioInputValidator.InputErrorType.NOTES_EMPTY -> "Empty Notes"
                RubioInputValidator.InputErrorType.NOTES_TOO_LONG -> "Notes Too Long"
            },
            userMessage = inputError.userMessage,
            debugDetails = inputError.debugMessage,
            canRetry = false,
            recoveryHint = "Please check your document or notes input and try again."
        )
    }

    /**
     * Converts a [NetworkResilience.ApiError] into a centralized [AppError].
     */
    fun fromApiError(apiError: NetworkResilience.ApiError): AppError {
        val category = when (apiError.type) {
            NetworkResilience.ApiErrorType.MISSING_API_KEY -> AppErrorCategory.AUTHENTICATION
            NetworkResilience.ApiErrorType.NETWORK_ERROR -> AppErrorCategory.NETWORK_OFFLINE
            NetworkResilience.ApiErrorType.REQUEST_TIMEOUT -> AppErrorCategory.TIMEOUT
            NetworkResilience.ApiErrorType.HTTP_ERROR -> when (apiError.httpCode) {
                401, 403 -> AppErrorCategory.AUTHENTICATION
                429 -> AppErrorCategory.RATE_LIMITED
                else -> AppErrorCategory.HTTP_STATUS
            }
            NetworkResilience.ApiErrorType.INVALID_RESPONSE -> AppErrorCategory.PARSING_FAILED
            NetworkResilience.ApiErrorType.UNKNOWN -> AppErrorCategory.UNKNOWN
        }

        return AppError(
            category = category,
            title = when (category) {
                AppErrorCategory.AUTHENTICATION -> "Authentication Error"
                AppErrorCategory.NETWORK_OFFLINE -> "Network Error"
                AppErrorCategory.TIMEOUT -> "Request Timeout"
                AppErrorCategory.RATE_LIMITED -> "Rate Limited"
                AppErrorCategory.PARSING_FAILED -> "Invalid AI Response"
                else -> "Service Error"
            },
            userMessage = apiError.userMessage,
            debugDetails = apiError.debugMessage,
            httpCode = apiError.httpCode,
            canRetry = category in setOf(AppErrorCategory.NETWORK_OFFLINE, AppErrorCategory.TIMEOUT, AppErrorCategory.RATE_LIMITED)
        )
    }

    // ============================================================
    // Legacy Compatibility (Herrera/README.md)
    // ============================================================

    /**
     * Legacy signature kept so any existing caller compiles without disruption.
     */
    fun handleNetworkError(exception: Throwable): String {
        return handleException(exception).userMessage
    }
}
