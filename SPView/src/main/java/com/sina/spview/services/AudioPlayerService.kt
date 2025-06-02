package com.sina.spview.services

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sina.spview.models.AudioState
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerService(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _audioStateFlow = MutableStateFlow<AudioState>(AudioState.Initial)
    val audioStateFlow: StateFlow<AudioState> = _audioStateFlow.asStateFlow()
    // To keep track of which item's state we are emitting
    private var currentPlayingPositionTag: Int = -1
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var progressUpdateRunnable: Runnable

    fun playAudio(filePath: String, itemPositionTag: Int) {
        stopAudioInternal() // Stop previous audio and clear state before starting new
        currentPlayingPositionTag = itemPositionTag

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
            setMediaItem(mediaItem)
            prepare()
            play()
            _audioStateFlow.value =
                AudioState.Playing(itemPositionTag, 0, duration.toInt().coerceAtLeast(0))

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startProgressUpdater()
                        _audioStateFlow.value = AudioState.Playing(
                            currentPlayingPositionTag,
                            currentPosition.toInt().coerceAtLeast(0),
                            duration.toInt().coerceAtLeast(0)
                        )
                    } else {
                        if (playbackState != Player.STATE_ENDED) {
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

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    stopProgressUpdater()
                    _audioStateFlow.value = AudioState.Error(
                        currentPlayingPositionTag,
                        error.message ?: "Unknown error"
                    )
                    stopAudioInternal()
                }
            })
        }
    }

    fun startProgressUpdater() {
        // 1. If a progressUpdateRunnable instance already exists from a previous call for this service instance,
        //    remove any pending posts of it. This ensures we don't have multiple updaters running
        //    if startProgressUpdater is called rapidly or in overlapping scenarios.
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }

        // 2. Create and initialize the new Runnable instance.
        //    This new instance will be used for the current playback session's progress updates.
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                // Capture the current exoPlayer instance for safety within this execution of run()
                val currentPlayer = exoPlayer

                if (currentPlayer != null && currentPlayer.isPlaying) {
                    // Player exists and is actively playing: update the state
                    _audioStateFlow.value = AudioState.Playing(
                        currentPlayingPositionTag,
                        currentPlayer.currentPosition.toInt().coerceAtLeast(0),
                        currentPlayer.duration.toInt().coerceAtLeast(0)
                    )
                    // Schedule the next update
                    progressUpdateHandler.postDelayed(this, 500) // 'this' refers to the current Runnable instance
                } else {
                    // Player is null OR not playing:
                    // The updater should stop itself to prevent further unnecessary checks or updates.
                    // Calling stopProgressUpdater() will handle removing this specific runnable
                    // from the handler's queue if this 'run' method was the last one posted.
                    stopProgressUpdater()
                }
            }
        }

        // 3. Post the newly created runnable to the handler to start the updates.
        progressUpdateHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdater() {
        // Check if progressUpdateRunnable has been initialized before trying to use it
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
    }
    fun pauseAudio() {
        exoPlayer?.pause()
        stopProgressUpdater()
        exoPlayer?.let {
            _audioStateFlow.value = AudioState.Paused(
                currentPlayingPositionTag,
                it.currentPosition.toInt().coerceAtLeast(0)
            )
        }
    }

    fun resumeAudio() = exoPlayer?.play()


    // Make this private if only called internally or ensure correct positionTag usage
    private fun stopAudioInternal() {
        stopProgressUpdater()
        exoPlayer?.release()
        exoPlayer = null
        // Only emit the stopped state if something was conceptually playing/pause
        if (currentPlayingPositionTag != -1) _audioStateFlow.value =
            AudioState.Stopped(currentPlayingPositionTag)
        currentPlayingPositionTag = -1 // reset the position tag
    }

    fun stopAudio(itemPositionTag: Int) {
        // Stop if it's the current item or any player active
        if (currentPlayingPositionTag == itemPositionTag || exoPlayer != null) stopAudioInternal()
        else {
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
            if (it.isPlaying) {
                _audioStateFlow.value = AudioState.Playing(
                    currentPlayingPositionTag,
                    it.currentPosition.toInt().coerceAtLeast(0),
                    it.duration.toInt().coerceAtLeast(0)
                )
            } else {
                _audioStateFlow.value = AudioState.Paused(
                    currentPlayingPositionTag, it.currentPosition.toInt().coerceAtLeast(0)
                )
            }
        }
    }

    fun releasePlayer() {
        stopAudioInternal()
    }
}
