package com.sina.spview.smpview.popup

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.annotation.OptIn
import androidx.core.graphics.drawable.toDrawable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sina.simpleview.library.R
import androidx.core.net.toUri
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Shows a video inside a PopupWindow using Media3 ExoPlayer.
 *
 * @param hostView An anchor view from your Activity/Fragment (usually `findViewById(android.R.id.content)`).
 * @param videoUrl  The URL or URI string of the video to play.
 */

@OptIn(UnstableApi::class)
fun showVideoInPopup(context: Context, hostView: View, videoUrl: String) {
    require(context is Activity) { "Context must be an Activity" }

    // 1) Inflate the popup layout
    val inflater = LayoutInflater.from(context)
    val popupView: View = inflater.inflate(R.layout.popup_video, null)

    // 2) Find our PlayerView + container + toggle button
    val playerView: PlayerView        = popupView.findViewById(R.id.popup_player_view)
    val playerContainer: FrameLayout = popupView.findViewById(R.id.player_container)
    val fullscreenButton: ImageButton = popupView.findViewById(R.id.fullscreen_toggle_button)
    // We no longer auto‐rotate on first frame
    // var hasSetOrientation = false

    // 2b) Tell ExoPlayer how to scale inside our small popup (letterbox/pillarbox)
    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    // 3) Build ExoPlayer
    val player = ExoPlayer.Builder(context).build().also { exoPlayer ->
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // 4) (OPTIONAL) If you still want to know the video size, you can listen,
        //     but DO NOT force‐rotate the Activity here.
        exoPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // You could read videoSize.width/height if you like,
                // but do NOT call activity.requestedOrientation here.
                // That logic will live in the fullscreen toggle only.
            }
        })
    }

    // 5) Compute the “small” size (80% width × 40% height)
    val displayMetrics = context.resources.displayMetrics
    val screenWidth  = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    val smallWidth  = (screenWidth * 0.8).toInt()
    val smallHeight = (screenHeight * 0.4).toInt()

    // 6) Create PopupWindow in the small size
    val popupWindow = PopupWindow(popupView, smallWidth, smallHeight, true).apply {
        isOutsideTouchable = true
        // Transparent background so we see rounded corners, etc.
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        elevation = 10f

        setOnDismissListener {
            // Clean up player on dismiss
            player.release()
            // Restore system UI (if we dismissed while in fullscreen)
            showSystemUI(context.window.decorView)
            // Allow normal sensor orientation again
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    // 7) Show the Popup centered
    popupWindow.showAtLocation(hostView, Gravity.CENTER, 0, 0)

    // 8) Track fullscreen state
    var isFullscreen = false

    fullscreenButton.setOnClickListener {
        if (isFullscreen) {
            // ───── EXIT FULLSCREEN ─────

            // 1) Shrink back to small size
            popupWindow.update(smallWidth, smallHeight)

            // 2) Update the container’s LayoutParams
            val params = playerContainer.layoutParams
            params.width  = smallWidth
            params.height = smallHeight
            playerContainer.layoutParams = params

            // 3) Show system UI again
            showSystemUI(context.window.decorView)

            // 4) Switch fullscreen icon back to “↔︎” or whatever
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen)

            // 5) Let the orientation sensor run normally again
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

            isFullscreen = false

        } else {
            // ───── ENTER FULLSCREEN ─────

            // 1) Force the popup to occupy MATCH_PARENT × MATCH_PARENT
            popupWindow.update(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 2) Make the player container match_parent so the video layer truly fills
            val params = playerContainer.layoutParams
            params.width  = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerContainer.layoutParams = params

            // 3) Hide all system UI (immersive mode)
            hideSystemUI(context.window.decorView)

            // 4) Change the icon to “✕” (close/fullscreenExit)
            fullscreenButton.setImageResource(R.drawable.ic_close)

            // 5) Lock orientation to whatever the video’s preferred aspect ratio is:
            val videoSize = player.videoSize
            if (videoSize.width > videoSize.height) {
                // Landscape video → lock to landscape
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                // Portrait video → lock to portrait
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            isFullscreen = true
        }
    }
}


// ─────────────────────────────────────────────────────────────────
// Helpers to hide/show system UI correctly on all API levels
// ─────────────────────────────────────────────────────────────────

fun hideSystemUI(decorView: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ way
        decorView.windowInsetsController?.let { controller ->
            controller.hide(
                WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
            )
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
    }
}

fun showSystemUI(decorView: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11+ way
        decorView.windowInsetsController?.show(
            WindowInsets.Type.statusBars() or
                    WindowInsets.Type.navigationBars()
        )
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

