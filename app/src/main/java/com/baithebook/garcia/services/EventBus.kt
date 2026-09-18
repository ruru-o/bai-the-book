package com.baithebook.garcia.services

sealed class AppEvent {
    data class FileSelected(val fileName: String, val fileBytes: ByteArray) : AppEvent()
    data class ErrorOccurred(val message: String) : AppEvent()
    data class LanguageChanged(val isBisaya: Boolean) : AppEvent()
}

object EventBus {
    private val listeners = mutableListOf<(AppEvent) -> Unit>()

    fun subscribe(listener: (AppEvent) -> Unit) {
        listeners.add(listener)
    }

    fun publish(event: AppEvent) {
        listeners.forEach { it(event) }
    }
}
