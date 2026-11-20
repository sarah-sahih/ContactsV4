package com.example.contactsv4

/** Message à usage unique (évite d'afficher deux fois un Toast après une rotation). */
class Event<out T>(private val content: T) {
    private var handled = false
    fun getIfNotHandled(): T? {
        if (handled) return null
        handled = true
        return content
    }
}

sealed class DbResult {
    data class Success(val message: String) : DbResult()
    data class Error(val message: String) : DbResult()
}
