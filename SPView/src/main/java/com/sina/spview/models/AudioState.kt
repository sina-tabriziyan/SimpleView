package com.sina.spview.models

sealed class AudioState {
    data class Playing(val position: Int, val progress: Int, val duration: Int) : AudioState()
    data class Paused(val position: Int, val progress: Int) : AudioState()
    data class Stopped(val position: Int) : AudioState()
    data class AudioName(val fileName: String) : AudioState()
}