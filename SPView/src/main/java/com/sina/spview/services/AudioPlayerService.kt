package com.sina.spview.services

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.sina.spview.models.AudioState
import java.io.File

class AudioPlayerService(private val context: Context) {

        private var exoPlayer: ExoPlayer? = null
        var audioStateLiveData = MutableLiveData<AudioState>()


        fun playAudio(filePath: String, position: Int) {
            stopAudio(position) // Stop previous audio if any

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                setMediaItem(mediaItem)
                prepare()
                play()

                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == ExoPlayer.STATE_READY) {
                            val duration = exoPlayer!!.duration.toInt()
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed(object : Runnable {
                                override fun run() {
                                    if (exoPlayer?.isPlaying == true) {
                                        audioStateLiveData.postValue(
                                            AudioState.Playing(position, exoPlayer!!.currentPosition.toInt(), duration)
                                        )
                                        handler.postDelayed(this, 500)
                                    }
                                }
                            }, 500)
                        }
                        if (state == ExoPlayer.STATE_ENDED) {
                            audioStateLiveData.postValue(AudioState.Stopped(position))
                        }
                    }
                })
            }
        }

        fun pauseAudio(position: Int) {
            exoPlayer?.pause()
            audioStateLiveData.postValue(AudioState.Paused(position, exoPlayer?.currentPosition?.toInt() ?: 0))
        }

        fun stopAudio(position: Int) {
            exoPlayer?.release()
            exoPlayer = null
            audioStateLiveData.postValue(AudioState.Stopped(position))
        }

        fun seekTo(position: Int, progress: Int) {
            exoPlayer?.seekTo(progress.toLong())
        }
    }
