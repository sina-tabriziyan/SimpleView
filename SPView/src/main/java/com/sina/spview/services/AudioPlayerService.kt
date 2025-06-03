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

    private var currentPlayingMediaId: String? = null // Changed from Int to String?
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var progressUpdateRunnable: Runnable

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val player = exoPlayer ?: return
            val activeMediaId = currentPlayingMediaId ?: return // If no mediaId, can't update state meaningfully

            if (isPlaying) {
                startProgressUpdater()
                _audioStateFlow.value = AudioState.Playing(
                    activeMediaId,
                    player.currentPosition.toInt().coerceAtLeast(0),
                    player.duration.toInt().coerceAtLeast(0)
                )
            } else {
                if (player.playbackState != Player.STATE_ENDED) {
                    _audioStateFlow.value = AudioState.Paused(
                        activeMediaId,
                        player.currentPosition.toInt().coerceAtLeast(0),
                        player.duration.toInt().coerceAtLeast(0) // Ensure Paused has duration
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            val activeMediaId = currentPlayingMediaId // Capture current mediaId

            when (playbackState) {
                Player.STATE_READY -> {
                    if (activeMediaId != null) { // Only update if we have a mediaId
                        if (player.isPlaying) {
                            startProgressUpdater() // Start updater only if actually playing
                            _audioStateFlow.value = AudioState.Playing(
                                activeMediaId,
                                player.currentPosition.toInt().coerceAtLeast(0),
                                player.duration.toInt().coerceAtLeast(0)
                            )
                        } else {
                            // If ready but not playing, it might be paused or about to play
                            // Consider if Paused state should be emitted here if not auto-playing
                            _audioStateFlow.value = AudioState.Paused( // Or keep as Playing if autoPlay=true
                                activeMediaId,
                                player.currentPosition.toInt().coerceAtLeast(0),
                                player.duration.toInt().coerceAtLeast(0)
                            )
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    stopProgressUpdater()
                    activeMediaId?.let { // Only emit Stopped if there was an active mediaId
                        _audioStateFlow.value = AudioState.Stopped(it)
                    }
                    // Reset currentPlayingMediaId after it's stopped for good
                    // Or keep it if you want to allow replaying the last item easily
                    // currentPlayingMediaId = null // Decide based on desired "replay" behavior
                }
                Player.STATE_BUFFERING -> { /* Optionally emit Buffering state with activeMediaId */ }
                Player.STATE_IDLE -> {
                    // This often means the player is reset or an error occurred before ready.
                    // If an error didn't explicitly occur, emitting Initial might be okay.
                    // Or if currentPlayingMediaId is set, it might be an implicit stop.
                    // For safety, only reset to Initial if no mediaId is active.
                    if (activeMediaId == null) {
                        _audioStateFlow.value = AudioState.Initial
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressUpdater()
            val erroredMediaId = currentPlayingMediaId
            erroredMediaId?.let {
                _audioStateFlow.value = AudioState.Error(
                    it,
                    error.message ?: "Unknown player error"
                )
            } ?: run {
                // Error without a specific mediaId, perhaps during initial load
                _audioStateFlow.value = AudioState.Error(
                    "unknown_media_id_error", // Placeholder
                    error.message ?: "Unknown player error during init"
                )
            }
            // Do not reset currentPlayingMediaId here, let stop/play new handle it.
        }
    }

    suspend fun playAudio(filePath: String, mediaId: String) { // Takes mediaId: String
        withContext(Dispatchers.Main) {
            stopAudioInternalAndClearPlayer() // Stops previous, clears listener

            currentPlayingMediaId = mediaId // Set the new mediaId

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)

                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
                play() // playWhenReady = true by default after prepare + play

                // Initial state emission. Duration might be 0.
                // onPlaybackStateChanged(READY) or onTimelineChanged will provide accurate duration.
                // This emission assumes playback starts immediately.
                _audioStateFlow.value = AudioState.Playing(
                    mediaId,
                    0, // Current position is 0 initially
                    this.duration.toInt().coerceAtLeast(0) // Duration might be C.TIME_UNSET initially
                )
            }
        }
    }

    private fun startProgressUpdater() {
        // Only start if we have a valid mediaId and player
        if (exoPlayer == null || currentPlayingMediaId == null) {
            stopProgressUpdater()
            return
        }
        if (this::progressUpdateRunnable.isInitialized) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        }
        progressUpdateRunnable = object : Runnable {
            override fun run() {
                val player = exoPlayer
                val activeMediaId = currentPlayingMediaId
                if (player != null && player.isPlaying && activeMediaId != null) {
                    _audioStateFlow.value = AudioState.Playing(
                        activeMediaId,
                        player.currentPosition.toInt().coerceAtLeast(0),
                        player.duration.toInt().coerceAtLeast(0)
                    )
                    progressUpdateHandler.postDelayed(this, 500)
                } else {
                    stopProgressUpdater()
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
            exoPlayer?.pause() // Triggers onIsPlayingChanged(false)
        }
    }

    suspend fun resumeAudio() {
        withContext(Dispatchers.Main) {
            // Only resume if we have a current mediaId and player
            if (exoPlayer != null && currentPlayingMediaId != null) {
                exoPlayer?.play() // Triggers onIsPlayingChanged(true)
            } else {
                // Log warning or handle case where resume is called without active player/media
            }
        }
    }

    private suspend fun stopAudioInternalAndClearPlayer() {
        withContext(Dispatchers.Main) {
            stopProgressUpdater()
            exoPlayer?.let { player ->
                player.removeListener(playerListener)
                player.release()
            }
            exoPlayer = null
        }
        val stoppedMediaId = currentPlayingMediaId
        if (stoppedMediaId != null) {
            _audioStateFlow.value = AudioState.Stopped(stoppedMediaId)
        } else if (_audioStateFlow.value !is AudioState.Initial && _audioStateFlow.value !is AudioState.Error) {
            // If no specific media was playing but player was active, reset to Initial
            _audioStateFlow.value = AudioState.Initial
        }
        currentPlayingMediaId = null // Clear the current media ID
    }

    // Public stop function, now takes the mediaId of the item requesting the stop
    suspend fun stopAudio(mediaIdToStop: String) {
        // Stop only if the mediaIdToStop matches the currently playing one.
        if (currentPlayingMediaId == mediaIdToStop && exoPlayer != null) {
            stopAudioInternalAndClearPlayer()
        }
        // If mediaIdToStop is different, we don't stop the ongoing playback by default.
        // The ViewModel would decide if clicking "stop" on a non-playing item
        // should affect the currently playing item.
    }

    suspend fun seekTo(progressMillis: Long) {
        val activeMediaId = currentPlayingMediaId
        if (exoPlayer == null || activeMediaId == null) {
            // Cannot seek if no player or no active media
            return
        }
        withContext(Dispatchers.Main) {
            exoPlayer?.seekTo(progressMillis)
            exoPlayer?.let {
                val currentPosition = it.currentPosition.toInt().coerceAtLeast(0)
                val duration = it.duration.toInt().coerceAtLeast(0)
                if (it.isPlaying) {
                    _audioStateFlow.value = AudioState.Playing(activeMediaId, currentPosition, duration)
                } else {
                    // If paused after seek, update with Paused state
                    _audioStateFlow.value = AudioState.Paused(activeMediaId, currentPosition, duration)
                }
            }
        }
    }

    // Call this when the component managing this service is being destroyed
    suspend fun releasePlayerCompletely() {
        stopAudioInternalAndClearPlayer()
        // _audioStateFlow.value = AudioState.Initial // Reset to initial state
    }
}
