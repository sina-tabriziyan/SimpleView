package com.sina.spview.models

import com.sina.spview.models.AudioState.Initial.itemTagId
import retrofit2.http.POST

sealed class AudioState {
    abstract val itemTagId: Int? // Nullable in the base class
    data object Initial : AudioState() { override val itemTagId: Int? = null }
    data class Playing(override val itemTagId: Int, val currentPosition: Int, val duration: Int) : AudioState()
    data class Paused(override val itemTagId: Int, val currentPosition: Int) : AudioState()
    data class Stopped(override val itemTagId: Int) : AudioState()
    data class Error(override val itemTagId: Int, val message: String) : AudioState()
    data class AudioDetails(override val itemTagId: Int, val fileName: String) : AudioState()
}
