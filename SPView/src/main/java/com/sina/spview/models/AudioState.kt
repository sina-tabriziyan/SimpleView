package com.sina.spview.models

sealed class AudioState {
    data object Initial : AudioState() // Good initial state
    data class Playing(val position: Int, val progress: Int, val duration: Int) : AudioState()
    data class Paused(val position: Int, val progress: Int) : AudioState()
    data class Stopped(val position: Int) : AudioState()
    data class Error(val itemTag: Int, val message: String): AudioState()
    data class AudioName(val fileName: String) : AudioState()
}