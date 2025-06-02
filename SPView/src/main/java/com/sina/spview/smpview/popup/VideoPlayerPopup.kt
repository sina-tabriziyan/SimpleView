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
import androidx.core.graphics.drawable.toDrawable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sina.simpleview.library.R
import androidx.core.net.toUri
import androidx.media3.common.VideoSize

/**
 * Shows a video inside a PopupWindow using Media3 ExoPlayer.
 *
 * @param hostView An anchor view from your Activity/Fragment (usually `findViewById(android.R.id.content)`).
 * @param videoUrl  The URL or URI string of the video to play.
 */

fun showVideoInPopup(context: Context, hostView: View, videoUrl: String) {
    require(context is Activity) { "Context must be an Activity" }

    // 1) Inflate the popup layout
    val inflater = LayoutInflater.from(context)
    val popupView: View = inflater.inflate(R.layout.popup_video, null)

    // 2) Find our PlayerView + container + toggle button
    val playerView: PlayerView        = popupView.findViewById(R.id.popup_player_view)
    val playerContainer: FrameLayout = popupView.findViewById(R.id.player_container)
    val fullscreenButton: ImageButton = popupView.findViewById(R.id.fullscreen_toggle_button)
    var hasSetOrientation = false

    // 3) Build ExoPlayer
    val player = ExoPlayer.Builder(context).build().also { exoPlayer ->
        playerView.player = exoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // 4) Listen for onVideoSizeChanged → rotate + resize popup accordingly
        exoPlayer.addListener(object : Player.Listener {
            // Use the VideoSize‐based callback (ExoPlayer 2.15+)
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)

                // Only do this once
                if (hasSetOrientation) return

                val width = videoSize.width
                val height = videoSize.height

                if (width <= 0 || height <= 0) {
                    // We don’t have a valid size yet.
                    return
                }

                // Mark that we've already applied orientation
                hasSetOrientation = true

                // Cast context to Activity so we can call requestedOrientation
                val activity = (context as? Activity)
                if (activity == null) {
                    // If context isn’t an Activity, we can’t rotate—just return.
                    return
                }

                // Decide orientation based on aspect ratio
                if (width > height) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
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
        // Transparent background so we see the rounded corners or shadows if any
        setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        elevation = 10f

        setOnDismissListener {
            // Clean up player on dismiss
            player.release()
            // Restore default UI (in case they closed while in fullscreen)
            showSystemUI(context.window.decorView)
            // Allow sensor‐based rotation again
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

            // 2) Update the container's LayoutParams
            val params = playerContainer.layoutParams
            params.width  = smallWidth
            params.height = smallHeight
            playerContainer.layoutParams = params

            // 3) Show system UI again
            showSystemUI(context.window.decorView)

            // 4) Switch button icon back
            fullscreenButton.setImageResource(R.drawable.ic_fullscreen)

            // 5) Go back to “sensor” orientation so user can rotate normally
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

            isFullscreen = false

        } else {
            // ───── ENTER FULLSCREEN ─────
            // 1) Force the popup to occupy MATCH_PARENT × MATCH_PARENT
            popupWindow.update(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // 2) Make the player container match_parent so it truly fills all pixels
            val params = playerContainer.layoutParams
            params.width  = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerContainer.layoutParams = params

            // 3) Hide all system UI (immersive mode)
            hideSystemUI(context.window.decorView)

            // 4) Change the icon to “close”
            fullscreenButton.setImageResource(R.drawable.ic_close)

            // 5) (Optionally) lock orientation to whatever it currently is,
            //    so the user doesn’t accidentally rotate mid‐play.
            //    Usually you pick whichever orientation the video best fits:
            val videoSize = player.videoSize
            if (videoSize.width > videoSize.height) {
                context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
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
