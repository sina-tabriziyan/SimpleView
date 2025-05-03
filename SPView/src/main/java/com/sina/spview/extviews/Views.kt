/**
 * Created by st on 2/9/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.extviews

import android.animation.ObjectAnimator
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A collection of extension functions to enhance the usage of Views and UI components.
 * Designed to be used in multiple projects as part of a library.
 */
object ViewExtensions {

    // ---------------- View Visibility & Enabling ----------------

    /** Show or hide a view using VISIBLE or GONE */
    fun View.show(isVisible: Boolean) {
        visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /** Show or hide a view using VISIBLE or INVISIBLE */
    fun View.setInvisible(isInvisible: Boolean) {
        visibility = if (isInvisible) View.INVISIBLE else View.VISIBLE
    }

    /** Enable or disable a view */
    fun View.enable(isEnabled: Boolean) {
        this.isEnabled = isEnabled
        alpha = if (isEnabled) 1f else 0.5f
    }

    /** Toggle visibility between VISIBLE and GONE */
    fun View.toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    /** Toggle visibility between VISIBLE and INVISIBLE */
    fun View.toggleInvisibility() {
        visibility = if (visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
    }

    // ---------------- Click Listeners ----------------

    /** Simplified click listener */
    inline fun View.onClick(crossinline action: () -> Unit) = setOnClickListener { action() }

    /** Prevent double clicks with a delay */
    fun View.preventDoubleClick(delay: Long = 500L, action: () -> Unit) {
        isEnabled = false
        postDelayed({ isEnabled = true }, delay)
        action()
    }

    // ---------------- Keyboard Extensions ----------------

    /** Show or hide the keyboard inside a Fragment */
    fun Fragment.toggleKeyboard(editText: AppCompatEditText, show: Boolean) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        else imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    // ---------------- SearchView Extensions ----------------

    /** Listen for query text changes in SearchView */
    inline fun SearchView.onQueryChange(crossinline listener: (String) -> Unit) {
        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true
            override fun onQueryTextChange(newText: String?) = listener(newText.orEmpty()).let { true }
        })
    }

    // ---------------- RecyclerView Extensions ----------------

    /** Scroll to a specific position with optional smooth scrolling */
    fun RecyclerView.scrollToPosition(position: Int, smooth: Boolean = true) {
        post {
            (layoutManager as? LinearLayoutManager)?.let {
                if (smooth) it.smoothScrollToPosition(this, RecyclerView.State(), position)
                else it.scrollToPosition(position)
            }
        }
    }

    /** Get the first visible item in RecyclerView */
    fun RecyclerView.getFirstVisibleItem(): Int {
        return (layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: -1
    }

    // ---------------- FloatingActionButton Extensions ----------------

    /** Auto-hide FloatingActionButton on scroll */
    fun FloatingActionButton.autoHideOnScroll(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var isVisible = true
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && isVisible) {
                    hide()
                    isVisible = false
                } else if (dy < 0 && !isVisible) {
                    show()
                    isVisible = true
                }
            }
        })
    }

    // ---------------- Toolbar Extensions ----------------

    /** Setup a Toolbar with a title and back button */
    fun Toolbar.setup(title: String, enableBackButton: Boolean = true) {
        this.title = title
        if (enableBackButton) {
            setNavigationOnClickListener { (context as? android.app.Activity)?.onBackPressed() }
        }
    }

    // ---------------- Drawable to Bitmap ----------------

    /** Convert a Drawable to Bitmap */
    fun Drawable.toBitmap(): Bitmap? {
        return (this as? BitmapDrawable)?.bitmap ?: Bitmap.createBitmap(
            intrinsicWidth,
            intrinsicHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            setBounds(0, 0, width, height)
            draw(canvas)
        }
    }

    // ---------------- SeekBar Extensions ----------------

    /** Listen for SeekBar progress changes */
    inline fun SeekBar.onProgressChanged(crossinline action: (progress: Int, fromUser: Boolean) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) action(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // ---------------- DrawerLayout Extensions ----------------

    /** Execute an action when the DrawerLayout is opened */
    inline fun DrawerLayout.onOpened(crossinline action: () -> Unit) {
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerOpened(drawerView: View) = action()
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    // ---------------- Animation Extensions ----------------

    /** Animate a view’s translation with parameters */
    fun View.animateTranslation(targetX: Float, duration: Long = 500L) {
        ObjectAnimator.ofFloat(this, "translationX", targetX).apply {
            this.duration = duration
            start()
        }
    }

    fun List<Pair<View, Any>>.actionEach(action: (Any) -> Unit) {
        forEach { (button, associatedAction) ->
            button.setOnClickListener { action(associatedAction) }
        }
    }


    fun Uri.createBitmap(context: Context): Bitmap? {
        val contentResolver: ContentResolver = context.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, this)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        }
    }


    fun AppCompatEditText.setDrawableClickListener(
        onDrawableStartClick: (() -> Unit)? = null,
        onDrawableTopClick: (() -> Unit)? = null,
        onDrawableEndClick: (() -> Unit)? = null,
        onDrawableBottomClick: (() -> Unit)? = null
    ) {
        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val editText = v as AppCompatEditText
                val compoundDrawables = editText.compoundDrawables
                compoundDrawables.forEachIndexed { index, drawable ->
                    drawable?.let {
                        val bounds = it.bounds
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        when (index) {
                            0 -> { // DrawableStart
                                if (x >= editText.paddingLeft && x <= editText.paddingLeft + bounds.width() &&
                                    y >= editText.paddingTop && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableStartClick?.invoke()
                                    editText.performClick() // Notify accessibility services
                                    return@setOnTouchListener true
                                }
                            }

                            1 -> { // DrawableTop
                                if (x >= editText.paddingLeft && x <= editText.width - editText.paddingRight &&
                                    y >= editText.paddingTop && y <= editText.paddingTop + bounds.height()
                                ) {
                                    onDrawableTopClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }

                            2 -> { // DrawableEnd
                                if (x >= editText.width - editText.paddingRight - bounds.width() && x <= editText.width - editText.paddingRight &&
                                    y >= editText.paddingTop && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableEndClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }

                            3 -> { // DrawableBottom
                                if (x >= editText.paddingLeft && x <= editText.width - editText.paddingRight &&
                                    y >= editText.height - editText.paddingBottom - bounds.height() && y <= editText.height - editText.paddingBottom
                                ) {
                                    onDrawableBottomClick?.invoke()
                                    editText.performClick()
                                    return@setOnTouchListener true
                                }
                            }
                        }
                    }
                }
            }
            false
        }
    }
}
