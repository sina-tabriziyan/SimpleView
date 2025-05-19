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

        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.FontIcon).use { typedArray ->
                val iconCode = typedArray.getString(R.styleable.FontIcon_setIcon)
                if (!iconCode.isNullOrEmpty()) {
                    setIcon(iconCode)
                }

                val tintColor = typedArray.getColor(R.styleable.FontIcon_tint, currentTextColor)
                setTextColor(tintColor)

                val shapeValue = typedArray.getInt(R.styleable.FontIcon_backgroundShape, 1)
                desiredShape = when (shapeValue) {
                    0 -> BackgroundShape.OVAL
                    1 -> BackgroundShape.RECTANGLE
                    2 -> BackgroundShape.ROUNDED
                    else -> BackgroundShape.RECTANGLE
                }

                bgColor = typedArray.getColor(R.styleable.FontIcon_backgroundColor, Color.TRANSPARENT)
                enableRipple = typedArray.getBoolean(R.styleable.FontIcon_ripple, false)
            }
        }

        // Apply background with optional ripple
        background = createBackgroundDrawable(desiredShape, bgColor, enableRipple)

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