package com.baithebook.rubio.resilience

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Rubio's input validation layer — fully self-contained, no dependency on a shared
 * error model.
 *
 * Per Rubio/README.md: "sanitize note inputs and enforce file size limits" and
 * "validate upload input in QuickUploadWidget before calling Alvarez's service."
 *
 * Covers: no file selected, wrong/unsupported file type, oversized file, empty file,
 * and blank/oversized note text.
 */
class RubioInputValidator {

    // ============================================================
    // Local result / error types (no external dependency)
    // ============================================================

    enum class InputErrorType {
        NO_FILE_SELECTED,
        INVALID_FILE_TYPE,
        FILE_TOO_LARGE,
        FILE_EMPTY,
        NOTES_EMPTY,
        NOTES_TOO_LONG
    }

    data class InputError(
        val type: InputErrorType,
        val userMessage: String,
        val debugMessage: String? = null
    )

    sealed class ValidationResult<out T> {
        data class Valid<out T>(val value: T) : ValidationResult<T>()
        data class Invalid(val error: InputError) : ValidationResult<Nothing>()
    }

    companion object {
        // Extend this list as Docling adds support for more formats.
        private val ALLOWED_MIME_TYPES = setOf(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "application/msword", // .doc
            "application/vnd.ms-powerpoint", // .ppt
            "text/plain" // .txt
        )
        private val ALLOWED_EXTENSIONS = setOf("pdf", "docx", "pptx", "doc", "ppt", "txt")

        const val MAX_FILE_SIZE_BYTES = 25L * 1024 * 1024 // 25MB
        const val MAX_NOTE_LENGTH = 20_000
    }

    // ============================================================
    // Legacy signature — kept so anything already wired to the old stub still compiles.
    // ============================================================

    @Deprecated("Use validateNotes(notesText) for a typed ValidationResult", ReplaceWith("validateNotes(notesText)"))
    fun validateInputNotes(notesText: String): Boolean =
        validateNotes(notesText) is ValidationResult.Valid

    // ============================================================
    // Note text: sanitize + validate
    // ============================================================

    fun sanitizeNotes(notesText: String): String {
        return notesText
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "") // strip control chars
            .replace(Regex("[ \\t]+"), " ")                           // collapse spaces/tabs
            .replace(Regex("\\n{3,}"), "\n\n")                        // collapse blank lines
            .trim()
    }

    fun validateNotes(notesText: String): ValidationResult<String> {
        val cleaned = sanitizeNotes(notesText)
        if (cleaned.isBlank()) {
            return ValidationResult.Invalid(
                InputError(
                    type = InputErrorType.NOTES_EMPTY,
                    userMessage = "Please type or paste some notes before continuing."
                )
            )
        }
        if (cleaned.length > MAX_NOTE_LENGTH) {
            return ValidationResult.Invalid(
                InputError(
                    type = InputErrorType.NOTES_TOO_LONG,
                    userMessage = "Your notes are too long (${cleaned.length} characters). " +
                        "Please keep it under $MAX_NOTE_LENGTH characters.",
                    debugMessage = "length=${cleaned.length}, max=$MAX_NOTE_LENGTH"
                )
            )
        }
        return ValidationResult.Valid(cleaned)
    }

    // ============================================================
    // File: no file / wrong file / too large / empty
    // ============================================================

    /**
     * @param uri the Uri returned by the file picker — nullable, since the user may have
     * cancelled the picker. A null Uri is exactly the "no file" case.
     */
    fun validateFile(uri: Uri?, context: Context): ValidationResult<Uri> {
        // 1. NO FILE
        if (uri == null) {
            return ValidationResult.Invalid(
                InputError(
                    type = InputErrorType.NO_FILE_SELECTED,
                    userMessage = "No file was selected. Please choose a file to upload."
                )
            )
        }

        val fileName = queryDisplayName(context, uri)
        val mimeType = context.contentResolver.getType(uri)
        val extension = fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()

        // 2. EXPLICIT VIDEO REJECTION (kept from the original spec: reject .mp4 files outright)
        if (mimeType?.startsWith("video/") == true || extension == "mp4") {
            return ValidationResult.Invalid(invalidFileType(mimeType, fileName))
        }

        // 3. WRONG / UNSUPPORTED FILE TYPE
        // Some content providers return a generic or null mimeType, so fall back to the
        // extension before rejecting.
        val mimeOk = mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType)
        val extOk = extension != null && ALLOWED_EXTENSIONS.contains(extension)
        if (!mimeOk && !extOk) {
            return ValidationResult.Invalid(invalidFileType(mimeType, fileName))
        }

        val sizeBytes = querySize(context, uri)

        // 4. EMPTY FILE
        if (sizeBytes == 0L) {
            return ValidationResult.Invalid(
                InputError(
                    type = InputErrorType.FILE_EMPTY,
                    userMessage = "That file appears to be empty. Please choose a different file.",
                    debugMessage = "0-byte file: $fileName"
                )
            )
        }

        // 5. FILE TOO LARGE
        if (sizeBytes != null && sizeBytes > MAX_FILE_SIZE_BYTES) {
            return ValidationResult.Invalid(
                InputError(
                    type = InputErrorType.FILE_TOO_LARGE,
                    userMessage = "This file is too large (${sizeBytes / (1024 * 1024)}MB). " +
                        "Max allowed size is ${MAX_FILE_SIZE_BYTES / (1024 * 1024)}MB.",
                    debugMessage = "sizeBytes=$sizeBytes, maxBytes=$MAX_FILE_SIZE_BYTES"
                )
            )
        }

        return ValidationResult.Valid(uri)
    }

    private fun invalidFileType(mimeType: String?, fileName: String?): InputError = InputError(
        type = InputErrorType.INVALID_FILE_TYPE,
        userMessage = "\"${fileName ?: "This file"}\" isn't a supported type. " +
            "Please upload a PDF, DOCX, or PPTX file.",
        debugMessage = "Rejected upload — mimeType=$mimeType, fileName=$fileName"
    )

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null // Non-fatal: name is only used for friendlier error messages.
        }
    }

    private fun querySize(context: Context, uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && cursor.moveToFirst() && !cursor.isNull(sizeIndex)) {
                    cursor.getLong(sizeIndex)
                } else null
            }
        } catch (e: Exception) {
            null // Non-fatal: size check is skipped if it can't be determined.
        }
    }
}
