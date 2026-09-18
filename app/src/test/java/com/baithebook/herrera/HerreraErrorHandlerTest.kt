package com.baithebook.herrera

import com.baithebook.herrera.error.ErrorUIComponent
import com.baithebook.herrera.error.HerreraErrorHandler
import com.baithebook.rubio.resilience.NetworkResilience
import com.baithebook.rubio.resilience.RubioInputValidator
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for Herrera's error handling and UI component specifications.
 * Fulfills Herrera/README.md: "task: test app and handle error cases".
 */
class HerreraErrorHandlerTest {

    private val errorHandler = HerreraErrorHandler()
    private val errorUI = ErrorUIComponent()

    @Test
    fun testSocketTimeoutExceptionMapping() {
        val timeoutException = SocketTimeoutException("Read timed out after 10000ms")
        val appError = errorHandler.handleException(timeoutException)

        assertEquals(HerreraErrorHandler.AppErrorCategory.TIMEOUT, appError.category)
        assertEquals("Request Timed Out", appError.title)
        assertTrue(appError.canRetry)
        assertNotNull(appError.recoveryHint)
    }

    @Test
    fun testNetworkOfflineExceptionMapping() {
        val offlineException = UnknownHostException("api.docling.example.com")
        val appError = errorHandler.handleException(offlineException)

        assertEquals(HerreraErrorHandler.AppErrorCategory.NETWORK_OFFLINE, appError.category)
        assertEquals("No Internet Connection", appError.title)
        assertTrue(appError.canRetry)

        val connectException = ConnectException("Connection refused")
        val connectError = errorHandler.handleException(connectException)
        assertEquals(HerreraErrorHandler.AppErrorCategory.NETWORK_OFFLINE, connectError.category)
    }

    @Test
    fun testJsonParsingErrorMapping() {
        val jsonException = JSONException("Value at questions not found")
        val appError = errorHandler.handleException(jsonException)

        assertEquals(HerreraErrorHandler.AppErrorCategory.PARSING_FAILED, appError.category)
        assertEquals("AI Response Parsing Error", appError.title)
        assertTrue(appError.canRetry)
    }

    @Test
    fun testFileNotFoundExceptionMapping() {
        val fileException = FileNotFoundException("deck_upload.tmp does not exist")
        val appError = errorHandler.handleException(fileException)

        assertEquals(HerreraErrorHandler.AppErrorCategory.FILE_INVALID, appError.category)
        assertFalse(appError.canRetry)
    }

    @Test
    fun testHttpStatusMapping() {
        // 401 Unauthorized
        val authError = errorHandler.handleHttpError(401, "API key expired")
        assertEquals(HerreraErrorHandler.AppErrorCategory.AUTHENTICATION, authError.category)
        assertFalse(authError.canRetry)
        assertEquals(401, authError.httpCode)

        // 429 Rate Limit
        val rateLimitError = errorHandler.handleHttpError(429, "Quota exceeded")
        assertEquals(HerreraErrorHandler.AppErrorCategory.RATE_LIMITED, rateLimitError.category)
        assertTrue(rateLimitError.canRetry)
        assertEquals(429, rateLimitError.httpCode)

        // 503 Server Unavailable
        val serverError = errorHandler.handleHttpError(503, "Service Unavailable")
        assertEquals(HerreraErrorHandler.AppErrorCategory.HTTP_STATUS, serverError.category)
        assertTrue(serverError.canRetry)
        assertEquals(503, serverError.httpCode)
    }

    @Test
    fun testRubioInputValidatorBridge() {
        val inputError = RubioInputValidator.InputError(
            type = RubioInputValidator.InputErrorType.FILE_TOO_LARGE,
            userMessage = "File exceeds 25MB limit",
            debugMessage = "size=30000000"
        )

        val appError = errorHandler.handleValidationError(inputError)
        assertEquals(HerreraErrorHandler.AppErrorCategory.FILE_INVALID, appError.category)
        assertEquals("File Exceeds Size Limit", appError.title)
        assertEquals("File exceeds 25MB limit", appError.userMessage)
        assertFalse(appError.canRetry)
    }

    @Test
    fun testRubioApiErrorBridge() {
        val apiError = NetworkResilience.ApiError(
            type = NetworkResilience.ApiErrorType.REQUEST_TIMEOUT,
            userMessage = "The request took too long.",
            debugMessage = "Timeout after 8000ms"
        )

        val appError = errorHandler.fromApiError(apiError)
        assertEquals(HerreraErrorHandler.AppErrorCategory.TIMEOUT, appError.category)
        assertTrue(appError.canRetry)
    }

    @Test
    fun testLegacyMethodsCompatibility() {
        // HerreraErrorHandler legacy
        val legacyMsg = errorHandler.handleNetworkError(UnknownHostException("test"))
        assertTrue(legacyMsg.contains("internet") || legacyMsg.contains("reach"))

        // ErrorUIComponent legacy
        val code401Msg = errorUI.getErrorDisplayMessage(401)
        assertTrue(code401Msg.contains("Authentication"))

        val code429Msg = errorUI.getErrorDisplayMessage(429)
        assertTrue(code429Msg.contains("Too many requests"))

        val code500Msg = errorUI.getErrorDisplayMessage(500)
        assertTrue(code500Msg.contains("unavailable"))
    }
}
