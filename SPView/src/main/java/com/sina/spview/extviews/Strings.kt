/**
 * Created by ST on 2/12/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.extviews

import android.text.Spanned
import androidx.core.text.HtmlCompat


object StringExtension {
    fun String.fromURI(): Spanned = HtmlCompat.fromHtml(
        this,
        HtmlCompat.FROM_HTML_MODE_LEGACY
    )




    fun String.extractHostname(): String? {
        val regex = """Unable to resolve host "([^"]+)"""".toRegex()
        return regex.find(this)?.groups?.get(1)?.value
    }

    fun List<String>.findSidInHeaders(): String = this.find { it.startsWith("SID=") }?.substringBefore(";") ?: ""
    /** Extract latitude and longitude from a URL */
    fun String.extractLatLon(): Pair<Double, Double>? {
        val lat = "mlat=([\\d.]+)".toRegex().find(this)?.groupValues?.get(1)?.toDoubleOrNull()
        val lon = "mlon=([\\d.]+)".toRegex().find(this)?.groupValues?.get(1)?.toDoubleOrNull()
        return if (lat != null && lon != null) lat to lon else null
    }


}