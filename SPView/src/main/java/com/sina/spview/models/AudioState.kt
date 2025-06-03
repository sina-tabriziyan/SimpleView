package com.sina.spview.models

sealed class AudioState {
    abstract val mediaId: String? // Changed from itemTagId, type is now String (or your ID type)

    data object Initial : AudioState() {
        override val mediaId: String? = null
    }

    data class Playing(
        override val mediaId: String, // Now a String
        val currentPosition: Int,
        val duration: Int
    ) : AudioState()

    data class Paused(
        override val mediaId: String, // Now a String
        val currentPosition: Int,
        val duration: Int // Ensure Paused state also has duration
    ) : AudioState()

    data class Stopped(
        override val mediaId: String // Now a String
    ) : AudioState()

    data class Error(
        override val mediaId: String, // Now a String
        val message: String
    ) : AudioState()

    data class AudioDetails( // This might still be relevant or could also use mediaId
        override val mediaId: String,
        val fileName: String
    ) : AudioState()
}