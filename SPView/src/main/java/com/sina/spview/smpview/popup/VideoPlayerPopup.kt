package com.sina.spview.smpview.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
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
    val fullscreenButton: ImageButton = popupView.findViewById(R.id.fullscreen_toggle_button)

    val player = ExoPlayer.Builder(context).build().also { exoPlayer ->
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Calculate initial popup size (e.g., 80% width, 40% height)
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    val smallWidth = (screenWidth * 0.8).toInt()
    val smallHeight = (screenHeight * 0.4).toInt()

    val popupWindow = PopupWindow(popupView, smallWidth, smallHeight, true).apply {
        isOutsideTouchable = true
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        elevation = 10f
        setOnDismissListener {
            player.release()
            // Optionally restore system UI here if hidden
        }
    }

    popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)

    var isFullscreen = false

    fullscreenButton.setOnClickListener {
        if (isFullscreen) {
            // Exit fullscreen: shrink popup window & player container
            popupWindow.update(smallWidth, smallHeight)
            val params = playerContainer.layoutParams
            params.width = smallWidth
            params.height = smallHeight
            playerContainer.layoutParams = params

            // Restore system UI (status bar, nav bar)
            showSystemUI(hostView)

            fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
            isFullscreen = false
        } else {
            // Enter fullscreen: expand popup window & player container to fill screen
            popupWindow.update(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val params = playerContainer.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerContainer.layoutParams = params

            // Hide system UI (fullscreen immersive)
            hideSystemUI(hostView)

            fullscreenButton.setImageResource(R.drawable.ic_close)
            isFullscreen = true
        }
    }
}

// Helpers to hide/show system UI (status & nav bars)

fun hideSystemUI(windowDecorView: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = windowDecorView.windowInsetsController
        controller?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        windowDecorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
}

fun showSystemUI(windowDecorView: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val controller = windowDecorView.windowInsetsController
        controller?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    } else {
        @Suppress("DEPRECATION")
        windowDecorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}