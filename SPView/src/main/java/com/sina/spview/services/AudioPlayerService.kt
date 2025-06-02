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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayerService(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val _audioStateFlow = MutableStateFlow<AudioState>(AudioState.Initial)
    val audioStateFlow: StateFlow<AudioState> = _audioStateFlow.asStateFlow()

    private var currentPlayingPositionTag: Int = -1
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var progressUpdateRunnable: Runnable

    // Centralized Player.Listener instance
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // This callback is on the main thread
            val player = exoPlayer ?: return // Safety check

            if (isPlaying) {
                startProgressUpdater() // Ensure this is main-safe or just posts to Handler
                _audioStateFlow.value = AudioState.Playing(
                    currentPlayingPositionTag,
                    player.currentPosition.toInt().coerceAtLeast(0),
                    player.duration.toInt().coerceAtLeast(0)
                )
            } else {
                // If playbackState is not ENDED, then it's effectively paused
                // (either by user or system e.g. audio focus loss)
                if (player.playbackState != Player.STATE_ENDED) {
                    _audioStateFlow.value = AudioState.Paused(
                        currentPlayingPositionTag,
                        player.currentPosition.toInt().coerceAtLeast(0)
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // This callback is on the main thread
            val player = exoPlayer ?: return // Safety check

            when (playbackState) {
                Player.STATE_READY -> {
                    if (player.isPlaying) {
                        startProgressUpdater()
                    }
                    // Update with potentially more accurate duration once ready
                    _audioStateFlow.value = AudioState.Playing(
                        currentPlayingPositionTag,
                        player.currentPosition.toInt().coerceAtLeast(0),
                        player.duration.toInt().coerceAtLeast(0)
                    )
                }
                Player.STATE_ENDED -> {
                    stopProgressUpdater() // Ensure this is main-safe or just posts to Handler
                    _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
                }
                Player.STATE_BUFFERING -> { /* Optionally emit Buffering state */ }
                Player.STATE_IDLE -> { /* Optionally emit Idle/Initial state */ }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // This callback is on the main thread
            stopProgressUpdater()
            _audioStateFlow.value = AudioState.Error(
                currentPlayingPositionTag,
                error.message ?: "Unknown player error"
            )
            // Consider releasing the player resources on error
            // Need to launch a coroutine to call suspend function stopAudioInternal
            // For simplicity, let's assume ViewModel will call releasePlayer or play new audio.
            // Or, if this service were a CoroutineScope: scope.launch { stopAudioInternal() }
        }
    }

    suspend fun playAudio(filePath: String, itemPositionTag: Int) {
        withContext(Dispatchers.Main) {
            // Stop and release previous player instance and its listener
            stopAudioInternalAndClearPlayer() // Modified to also clear listener from old player

            currentPlayingPositionTag = itemPositionTag

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener) // Add the single listener instance

                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
                play()

                // Initial state emission. Duration might be 0 or C.TIME_UNSET here.
                // onPlaybackStateChanged(READY) or onTimelineChanged will provide accurate duration.
                _audioStateFlow.value = AudioState.Playing(
                    itemPositionTag,
                    0,
                    this.duration.toInt().coerceAtLeast(0)
                )
            }
        }
    }

    // Progress updater runs on the main thread via Handler
    fun startProgressUpdater() {
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                val player = exoPlayer // Capture current instance
                if (player != null && player.isPlaying) {
                    _audioStateFlow.value = AudioState.Playing(
                        currentPlayingPositionTag,
                        player.currentPosition.toInt().coerceAtLeast(0),
                        player.duration.toInt().coerceAtLeast(0)
                    )
                    progressUpdateHandler.postDelayed(this, 500)
                } else {
                    stopProgressUpdater() // Stop if player is null or not playing
                }
            }
        }
        progressUpdateHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdater() {
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
    }

    suspend fun pauseAudio() {
        withContext(Dispatchers.Main) {
            exoPlayer?.pause() // This will trigger onIsPlayingChanged(false)
        }
        // stopProgressUpdater() will be called from onIsPlayingChanged or if the updater stops itself.
        // The state update to Paused will also happen via onIsPlayingChanged.
    }

    suspend fun resumeAudio() {
        withContext(Dispatchers.Main) {
            exoPlayer?.play() // This will trigger onIsPlayingChanged(true)
        }
    }

    // Renamed and modified to ensure listener is removed from old player
    private suspend fun stopAudioInternalAndClearPlayer() {
        withContext(Dispatchers.Main) {
            stopProgressUpdater()
            exoPlayer?.let { player ->
                player.removeListener(playerListener) // Remove listener before releasing
                player.release()
            }
            exoPlayer = null
        }
        // State emission outside main context if it doesn't touch player
        if (currentPlayingPositionTag != -1) { // Only emit if something was active
            _audioStateFlow.value = AudioState.Stopped(currentPlayingPositionTag)
        }
        currentPlayingPositionTag = -1
    }

    // Public stop function
    suspend fun stopAudio(itemPositionTag: Int) {
        // Stop only if it's the current playing item or if any player is active.
        // This simplifies logic: stopping means stopping the current session.
        if (currentPlayingPositionTag == itemPositionTag || exoPlayer != null) {
            stopAudioInternalAndClearPlayer()
        }
        // If you need to emit a "Stopped" state for a non-active item,
        // that logic would be separate and potentially managed by the ViewModel
        // based on UI needs, not directly by changing the global player state here.
    }

    suspend fun seekTo(progressMillis: Long) {
        withContext(Dispatchers.Main) {
            exoPlayer?.seekTo(progressMillis)
            // After seek, onPlaybackStateChanged or progress updater will update state
            // Optionally, immediately update state if critical:
            exoPlayer?.let {
                val currentPosition = it.currentPosition.toInt().coerceAtLeast(0)
                val duration = it.duration.toInt().coerceAtLeast(0)
                if (it.isPlaying) {
                    _audioStateFlow.value = AudioState.Playing(currentPlayingPositionTag, currentPosition, duration)
                } else {
                    _audioStateFlow.value = AudioState.Paused(currentPlayingPositionTag, currentPosition)
                }
            }
        }
    }

    suspend fun releasePlayer() {
        // This is the main public method to fully release resources
        stopAudioInternalAndClearPlayer()
    }
}
