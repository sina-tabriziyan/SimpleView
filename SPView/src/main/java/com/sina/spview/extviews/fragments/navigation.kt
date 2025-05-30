/**
 * Created by ST on 5/5/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.extviews.fragments

import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.sina.simpleview.library.R
import com.sina.spview.enms.AnimationState
import com.sina.spview.enms.BackStackOption
import com.sina.spview.models.AttachIcons

fun Fragment.navigateTo(
    destinationId: Int,
    animation: AnimationState? = null,
    backStack: BackStackOption = BackStackOption.NO_CLEAR
) {
    findNavController().navigate(destinationId, buildNavOptions(animation, backStack))
}

fun Fragment.navigateTo(
    directions: NavDirections,
    animation: AnimationState? = null,
    backStack: BackStackOption = BackStackOption.NO_CLEAR
) {
    findNavController().navigate(directions, buildNavOptions(animation, backStack))
}

fun Fragment.navigateToDeepLink(
    deepLink: String,
    arguments: Map<String, String>? = null,
    animation: AnimationState? = null,
    backStack: BackStackOption = BackStackOption.NO_CLEAR
) {
    val uri = buildDeepLinkUri(deepLink, arguments)
    findNavController().navigate(uri, buildNavOptions(animation, backStack))
}

private fun Fragment.buildNavOptions(
    animation: AnimationState? = null,
    backStack: BackStackOption = BackStackOption.NO_CLEAR
): NavOptions? {
    if (animation == null && backStack == BackStackOption.NO_CLEAR) {
        return null
    }
    val navOptionsBuilder = NavOptions.Builder()

    when (backStack) {
        BackStackOption.CLEAR_CURRENT -> {
            navOptionsBuilder.setPopUpTo(
                findNavController().currentDestination?.id ?: return null,
                true
            )
        }

        BackStackOption.CLEAR_ALL -> {
            navOptionsBuilder.setPopUpTo(findNavController().graph.id, true)
        }

        BackStackOption.NO_CLEAR -> {
            // Do nothing
        }
    }

    animation?.let {
        val (enter, exit) = when (it) {
            AnimationState.FADE_IN_OUT -> android.R.anim.fade_in to android.R.anim.fade_out
            AnimationState.FADE_OUT_IN -> android.R.anim.fade_in to android.R.anim.fade_out
            AnimationState.TO_LEFT -> R.anim.slide_in_right to R.anim.slide_out_left
            AnimationState.TO_RIGHT -> android.R.anim.slide_in_left to android.R.anim.slide_out_right
        }
        navOptionsBuilder.setEnterAnim(enter).setExitAnim(exit)
        if (it == AnimationState.FADE_IN_OUT || it == AnimationState.FADE_OUT_IN) {
            navOptionsBuilder.setPopEnterAnim(enter).setPopExitAnim(exit)
        }
    }
    return navOptionsBuilder.build()
}

private fun buildDeepLinkUri(deepLink: String, arguments: Map<String, String>? = null): Uri {
    var uriString = deepLink
    if (!arguments.isNullOrEmpty()) {
        val queryParams = arguments.entries.joinToString("&") { "${it.key}=${it.value}" }
        uriString += if (uriString.contains("?")) "&$queryParams" else "?$queryParams"
    }
    return uriString.toUri()
}


fun attachIcons() = listOf(
    AttachIcons.ICON_CAMERA,
    AttachIcons.ICON_VIDEO,
    AttachIcons.ICON_GALLERY,
    AttachIcons.ICON_AUDIO,
    AttachIcons.ICON_FILES,
    AttachIcons.ICON_LOCATION,
    AttachIcons.ICON_CONTACT,
    AttachIcons.ICON_RECORDING,
)
