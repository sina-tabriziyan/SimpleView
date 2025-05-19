/**
 * Created by ST on 1/26/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.smpview.views

import android.net.Uri
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

fun loadThumbnailIntoPlayerView(playerView:PlayerView, videoUri:Uri) {
    // Initialize ExoPlayer
    val player = ExoPlayer.Builder(playerView.context).build()

    // Set the player to the PlayerView
    playerView.player = player

    // Create a MediaItem with the video Uri
    val mediaItem = MediaItem.fromUri(videoUri)

    // Set the MediaItem to the player
    player.setMediaItem(mediaItem)

    // Prepare the player but don't start playing
    player.prepare()

    // Optionally, you can seek to a specific position (e.g., 1 second) to display a different thumbnail
    player.seekTo(0) // Seek to the start (or any other time)

    // Release the player when it's no longer needed (like when the view is detached or recycled)
    playerView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            player.release()
        }
    })
}