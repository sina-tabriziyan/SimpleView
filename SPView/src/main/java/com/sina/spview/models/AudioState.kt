package com.sina.spview.models

sealed class AudioState {
    data object Initial : AudioState() // Good initial state
    data class Playing(val itemTagId: Int, val progress: Int, val duration: Int) : AudioState()
    data class Paused(val itemTagId: Int, val progress: Int) : AudioState()
    data class Stopped(val itemTagId: Int) : AudioState()
    data class Error(val itemTagId: Int, val message: String) : AudioState()
    data class Name(val itemTagId: Int, val fileName: String) : AudioState()
}