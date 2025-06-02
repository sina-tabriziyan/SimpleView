package com.sina.spview.network

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

// Define your AudioState sealed class/interface (assuming it's already defined)
// sealed class AudioState {
//     data class Playing(val position: Int, val currentPosition: Int, val duration: Int) : AudioState()
//     data class Paused(val position: Int, val currentPosition: Int) : AudioState()
//     data class Stopped(val position: Int) : AudioState()
//     data object Initial : AudioState() // Good to have an initial state
// }

class AudioPlayerService(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null

    // Use MutableStateFlow internally
    private val _audioStateFlow = MutableStateFlow<AudioState>(AudioState.Initial)
    // Expose it as a read-only StateFlow
    val audioStateFlow: StateFlow<AudioState> = _audioStateFlow.asStateFlow()

    private var currentPlayingPositionTag: Int = -1 // To keep track of which item's state we are emitting
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var progressUpdateRunnable: Runnable

    fun playAudio(filePath: String, itemPositionTag: Int) {
        stopAudioInternal() // Stop previous audio and clear state before starting new

        currentPlayingPositionTag = itemPositionTag
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
            play() // Start playback

            _audioStateFlow.value = AudioState.Playing(itemPositionTag, 0, duration.toInt().coerceAtLeast(0)) // Initial playing state

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        // Start progress updater if not already running for this playback session
                        startProgressUpdater()
                        // Ensure the state reflects playing with current details
                        _audioStateFlow.value = AudioState.Playing(
                            currentPlayingPositionTag,
                            currentPosition.toInt().coerceAtLeast(0),
                            duration.toInt().coerceAtLeast(0)
                        )
                    } else {
                        // If it's not playing, it could be paused or ended.
                        // The onPlaybackStateChanged will handle STATE_ENDED.
                        // If it's paused by user action, pauseAudio() will set the state.
                        // If paused by system (e.g. audio focus loss), this might be a point to update.
                        if (playbackState != Player.STATE_ENDED) { // Avoid overriding a deliberate pause state
                            _audioStateFlow.value = AudioState.Paused(
                                currentPlayingPositionTag,
                                currentPosition.toInt().coerceAtLeast(0)
                            )
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            // If isPlaying is true, onIsPlayingChanged(true) will handle it.
                            // If not playing yet but ready, we could update duration here if it changed.
                             if (isPlaying) { // Ensure progress updater starts if we become ready and are playing
                                 startProgressUpdater()
                             }
                            // Update with potentially more accurate duration once ready
                            _audioStateFlow.value = AudioState.Playing(
                                currentPlayingPositionTag,
                                currentPosition.toInt().coerceAtLeast(0),
                                duration.toInt().coerceAtLeast(0)
                            )

                        }
                        Player.STATE_ENDED -> {
                            stopProgressUpdater()
                            _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
                            // Optionally, you might want to reset currentPlayingPositionTag here or in stopAudioInternal
                        }
                        Player.STATE_BUFFERING -> {
                            // Optionally emit a Buffering state
                            // _audioStateFlow.value = AudioState.Buffering(currentPlayingPositionTag)
                        }
                        Player.STATE_IDLE -> {
                             // This can happen after stop or if player is reset
                             // _audioStateFlow.value = AudioState.Initial // Or a specific Idle state
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    stopProgressUpdater()
                    _audioStateFlow.value = AudioState.Error(currentPlayingPositionTag, error.message ?: "Unknown error")
                    // Consider calling stopAudioInternal() to release resources
                    stopAudioInternal()
                }
            })
        }
    }


    private fun startProgressUpdater() {
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _audioStateFlow.value = AudioState.Playing(
                            currentPlayingPositionTag,
                            player.currentPosition.toInt().coerceAtLeast(0),
                            player.duration.toInt().coerceAtLeast(0)
                        )
                        progressUpdateHandler.postDelayed(this, 500) // Update every 500ms
                    } else {
                        // If player is no longer playing but updater is still running, stop it.
                        stopProgressUpdater()
                    }
                }
            }
        }
        // Remove any old callbacks and post the new one
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        progressUpdateHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdater() {
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
    }

    fun pauseAudio() { // No need for position tag if we only manage one player instance
        exoPlayer?.pause()
        stopProgressUpdater() // Stop updates when paused
        exoPlayer?.let {
            _audioStateFlow.value = AudioState.Paused(currentPlayingPositionTag, it.currentPosition.toInt().coerceAtLeast(0))
        }
    }

    fun resumeAudio() { // Added resume functionality
        exoPlayer?.play()
        // onIsPlayingChanged(true) should trigger the progress updater and Playing state
    }


    // Make this private if only called internally or ensure correct positionTag usage
    private fun stopAudioInternal() {
        stopProgressUpdater()
        exoPlayer?.release()
        exoPlayer = null
        if (currentPlayingPositionTag != -1) { // Only emit stopped if something was conceptually playing/paused
             _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
        }
        currentPlayingPositionTag = -1 // Reset the tag
    }

    // Public stop if needed, might be used to stop playback initiated for a specific item
    fun stopAudio(itemPositionTag: Int) {
        if (currentPlayingPositionTag == itemPositionTag || exoPlayer != null) { // Stop if it's the current item or any player active
            stopAudioInternal()
        } else {
            // If stop is called for an item that isn't the current one,
            // and no player is active, we might just emit a stopped state for that specific tag
            // if the UI needs to react to "ensure this item is shown as stopped".
            // However, this can be complex if another item starts playing immediately.
            // For simplicity, stopAudioInternal usually handles the active player.
            // If the goal is to just tell the UI that a specific item's playback should be considered stopped,
            // you might emit: _audioStateFlow.value = AudioState.Stopped(itemPositionTag)
            // But be careful with global player state.
        }
    }


    fun seekTo(progressMillis: Long) {
        exoPlayer?.seekTo(progressMillis)
        // After seek, the player state might change, and current position updates.
        // The progress updater or state listeners should reflect the new position.
        // You might want to immediately update the state if needed:
        exoPlayer?.let {
             if(it.isPlaying) {
                 _audioStateFlow.value = AudioState.Playing(
                     currentPlayingPositionTag,
                     it.currentPosition.toInt().coerceAtLeast(0),
                     it.duration.toInt().coerceAtLeast(0)
                 )
             } else {
                 _audioStateFlow.value = AudioState.Paused(
                     currentPlayingPositionTag,
                     it.currentPosition.toInt().coerceAtLeast(0)
                 )
             }
        }
    }

    // Call this when the service is being destroyed or no longer needed
    fun releasePlayer() {
        stopAudioInternal()
        // Any other cleanup for the service itself
    }
}

// --- Define/Update your AudioState sealed class ---
sealed class AudioState {
    data object Initial : AudioState() // Good initial state
    data class Playing(val itemTag: Int, val currentPosition: Int, val duration: Int) : AudioState()
    data class Paused(val itemTag: Int, val currentPosition: Int) : AudioState()
    data class Stopped(val itemTag: Int) : AudioState()
    data class Error(val itemTag: Int, val message: String): AudioState()
    // data class Buffering(val itemTag: Int) : AudioState() // Optional
}