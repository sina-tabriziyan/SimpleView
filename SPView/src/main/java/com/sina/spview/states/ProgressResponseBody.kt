/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.states

import androidx.media3.common.util.Log
import com.sina.spview.donwload.DownloadFileUtils
import com.sina.spview.donwload.ProgressListener
import kotlinx.coroutines.channels.Channel
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.ranges.coerceIn
import kotlin.text.toDouble

class ProgressRequestBody(
    private val file: File,
    private val contentType: MediaType?,
    val listener: ProgressListener
) : RequestBody() {

    interface ProgressListener {
        fun onProgressUpdate(percentage: Int)
    }

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = file.length()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        if (fileLength == 0L) {
            listener.onProgressUpdate(100)
            return
        }
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded: Long = 0
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
                val progress = ((uploaded.toDouble() / fileLength.toDouble()) * 100).toInt()
                listener.onProgressUpdate(progress.coerceIn(0, 100))
            }
            if (uploaded == fileLength) { // Ensure 100% is sent if loop finishes exactly
                listener.onProgressUpdate(100)
            }
        } catch (e: Exception) { // Catch generic Exception
            Log.e("ProgressRequestBody", "Error writing to sink", e)
            throw e // Re-throw
        } finally {
            fis?.closeQuietly()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 4096
    }
}