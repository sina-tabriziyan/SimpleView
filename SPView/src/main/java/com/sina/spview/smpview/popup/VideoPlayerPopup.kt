package com.sina.spview.smpview.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sina.simpleview.library.R
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri

/**
 * Shows a video inside a PopupWindow using Media3 ExoPlayer.
 *
 * @param hostView An anchor view from your Activity/Fragment (usually `findViewById(android.R.id.content)`).
 * @param videoUrl  The URL or URI string of the video to play.
 */
fun showVideoInPopup(context: Context, hostView: View, videoUrl: String) {
    val inflater = LayoutInflater.from(context)
    val popupView: View = inflater.inflate(R.layout.popup_video, null)

    val playerView: PlayerView = popupView.findViewById(R.id.popup_player_view)
    val playerContainer: View = popupView.findViewById(R.id.player_container)

    val player = ExoPlayer.Builder(context).build().also { exoPlayer ->
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Get screen size
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // Calculate popup size as % of screen size
    val popupWidth = (screenWidth * 0.8).toInt()  // 80% width
    val popupHeight = (screenHeight * 0.4).toInt() // 40% height

    // Set initial size to popupWindow
    val popupWindow = PopupWindow(popupView, popupWidth, popupHeight, true).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        elevation = 10f

        setOnDismissListener {
            player.release()
        }
    }

    popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)

    // Support fullscreen toggle as before, resizing popupWindow and playerContainer
    var isFullscreen = false

    playerContainer.setOnClickListener {
        if (isFullscreen) {
            popupWindow.update(popupWidth, popupHeight)
            val params = playerContainer.layoutParams
            params.width = popupWidth
            params.height = popupHeight
            playerContainer.layoutParams = params
            isFullscreen = false
        } else {
            popupWindow.update(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val params = playerContainer.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerContainer.layoutParams = params
            isFullscreen = true
        }
    }
}
