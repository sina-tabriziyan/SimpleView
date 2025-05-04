package com.sina.spview.smpview.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import com.sina.simpleview.library.R

class FontIcon @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // Load the custom font
        typeface = Typeface.createFromAsset(context.assets, "fonticons.ttf")

        // Set the icon if 'setIcon' attribute is present
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.IconTextView) {
                val iconCode = getString(R.styleable.IconTextView_setIcon)
                if (!iconCode.isNullOrEmpty()) {
                    setIcon(iconCode)
                }
            }
        }
    }

    fun setIcon(iconCode: String) {
        try {
            val iconChar = String(Character.toChars(iconCode.toInt(16)))
            text = iconChar
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}