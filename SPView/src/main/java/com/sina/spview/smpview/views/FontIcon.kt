package com.sina.spview.smpview.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.sina.simpleview.library.R


class FontIcon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    enum class BackgroundShape {
        OVAL, RECTANGLE, ROUNDED
    }

    init {
        // Load the custom font
        typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")

        // Default values
        var desiredShape = BackgroundShape.RECTANGLE
        var bgColor = Color.TRANSPARENT
        var enableRipple = false
        var shouldCreateCustomBackground = true // Assume we'll create it
        if (this.background != null) {
            shouldCreateCustomBackground = false
        }


        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.FontIcon).use { typedArray ->
                val iconCode = typedArray.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) {
                    setIcon(iconCode)
                }

                val tintColor = typedArray.getColor(R.styleable.FontIcon_tint, currentTextColor)
                setTextColor(tintColor)

                // Only read background-related attributes if we intend to create a custom background
                if (shouldCreateCustomBackground) {
                    val shapeValue = typedArray.getInt(R.styleable.FontIcon_backgroundShape, 1)
                    desiredShape = when (shapeValue) {
                        0 -> BackgroundShape.OVAL
                        1 -> BackgroundShape.RECTANGLE
                        2 -> BackgroundShape.ROUNDED
                        else -> BackgroundShape.RECTANGLE
                    }

                    bgColor = typedArray.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                } else {
                    // If XML background is used, we might still want to apply ripple if requested
                    // and if the XML background isn't already a RippleDrawable.
                    // This part can get a bit more complex if you want to combine XML bg with programmatic ripple.
                    // For simplicity now, we'll assume if XML bg is set, it handles its own ripple.
                    // Alternatively, you could decide that app:ripple only applies to the programmatic background.
                    enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
                    if (enableRipple && this.background != null && this.background !is RippleDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val rippleColorStateList = ColorStateList.valueOf(Color.parseColor("#33000000"))
                        this.background = RippleDrawable(rippleColorStateList, this.background, null)
                    }
                }
            }
        }

        // Apply background with optional ripple
        // OR if XML background was set, but we handled ripple above.
        if (shouldCreateCustomBackground) {
            background = createBackgroundDrawable(desiredShape, bgColor, enableRipple)
        }
        // Enable ripple effect
        isClickable = true
        isFocusable = true
    }

    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createBackgroundDrawable(
        shape: BackgroundShape,
        color: Int,
        ripple: Boolean
    ): Drawable {
        val shapeDrawable = GradientDrawable().apply {
            this.shape = when (shape) {
                BackgroundShape.OVAL -> GradientDrawable.OVAL
                else -> GradientDrawable.RECTANGLE
            }
            cornerRadius = if (shape == BackgroundShape.ROUNDED) 16f else 0f
            setColor(color)
        }

        return if (ripple && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rippleColor = ColorStateList.valueOf(Color.parseColor("#33000000")) // semi-transparent black
            RippleDrawable(rippleColor, shapeDrawable, null)
        } else {
            shapeDrawable
        }
    }

}