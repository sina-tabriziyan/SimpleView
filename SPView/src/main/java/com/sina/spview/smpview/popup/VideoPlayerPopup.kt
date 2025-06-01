package com.sina.spview.smpview.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
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
    val exoPlayer = ExoPlayer.Builder(context).build().also { player ->
        playerView.player = player
        val mediaItem = MediaItem.fromUri(videoUrl.toUri())
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }
    val popupWindow = PopupWindow(popupView, WRAP_CONTENT, WRAP_CONTENT, true).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        elevation = 8f
        setOnDismissListener { exoPlayer.release() }
    }
    popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)
}
